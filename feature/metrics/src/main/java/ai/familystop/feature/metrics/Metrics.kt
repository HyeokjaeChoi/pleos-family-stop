package ai.familystop.feature.metrics

import java.io.File
import java.util.Properties
import java.util.UUID

/** 지표 값이 SDK 연계 값인지 가상 데이터인지 구분해 기록한다(F-QNPGYP). */
enum class DataOrigin { SDK, VIRTUAL }

/**
 * 여행 1건의 정차 가치 지표. 개인·계정 식별값 없이 여행 단위 임의 표식([tripId])만 쓴다(PL-NGEOJQ).
 * 계획 소요 시간(분): 기존 방식 vs 시제품.
 */
data class TripMetrics(
    val tripId: String,
    val origin: DataOrigin,
    val recruitChannel: String,
    val baselinePlanMinutes: Int?,
    val prototypePlanMinutes: Int?,
    val choiceReasons: List<String>,
    val unresolvedNeeds: List<String>,
    val spendWon: Int?
)

/** 로컬 저장소. 외부 전송 없음. */
interface MetricsStore {
    fun save(metrics: TripMetrics)

    fun deleteTrip(tripId: String)

    fun load(tripId: String): TripMetrics?

    /** 저장된 여행 표식 전부. 재시작 후 고아 기록 정리용. */
    fun listTrips(): List<String>
}

/** 여행당 파일 1개(`<tripId>.properties`). 철회 시 파일 삭제. */
class FileMetricsStore(private val dir: File) : MetricsStore {
    private fun file(tripId: String): File {
        require(tripId.isNotEmpty() && tripId.all { it.isLetterOrDigit() || it == '-' }) {
            "invalid tripId"
        }
        return File(dir, "$tripId.properties")
    }

    override fun save(metrics: TripMetrics) {
        dir.mkdirs()
        val p = Properties()
        p["origin"] = metrics.origin.name
        p["channel"] = metrics.recruitChannel
        metrics.baselinePlanMinutes?.let { p["baseline"] = it.toString() }
        metrics.prototypePlanMinutes?.let { p["prototype"] = it.toString() }
        p["reasons"] = metrics.choiceReasons.joinToString("\n")
        p["unresolved"] = metrics.unresolvedNeeds.joinToString("\n")
        metrics.spendWon?.let { p["spend"] = it.toString() }
        val target = file(metrics.tripId)
        val tmp = File(dir, "${metrics.tripId}.tmp")
        tmp.outputStream().use { p.store(it, null) }
        check(tmp.renameTo(target)) { "rename failed" }
    }

    override fun deleteTrip(tripId: String) {
        val f = file(tripId)
        check(f.delete() || !f.exists()) { "delete failed" }
    }

    override fun listTrips(): List<String> =
        dir.listFiles { f -> f.extension == "properties" }.orEmpty().map { it.nameWithoutExtension }

    override fun load(tripId: String): TripMetrics? {
        val f = file(tripId).takeIf { it.exists() } ?: return null
        return runCatching {
            val p = Properties().also { pr -> f.inputStream().use { pr.load(it) } }
            fun lines(k: String) = p.getProperty(k).orEmpty().lines().filter { it.isNotEmpty() }
            TripMetrics(
                tripId,
                DataOrigin.valueOf(p.getProperty("origin")),
                p.getProperty("channel").orEmpty(),
                p.getProperty("baseline")?.toInt(),
                p.getProperty("prototype")?.toInt(),
                lines("reasons"),
                lines("unresolved"),
                p.getProperty("spend")?.toInt()
            )
        }.getOrNull() // 손상된 파일은 없는 것으로 취급
    }
}

/**
 * 동의 없이는 수집하지 않는다. 저장 실패는 삼켜서 핵심 기능을 막지 않는다.
 * 동의는 여행 단위이며 메모리에만 둔다(앱 재시작 시 다시 묻는다).
 */
class MetricsCollector(private val store: MetricsStore) {
    private val consented = mutableSetOf<String>()

    /** 새 여행 표식. 임의 UUID라 사람·계정과 연결되지 않는다. */
    fun startTrip(): String = UUID.randomUUID().toString()

    /** 측정 동의는 운전자 본인만 할 수 있다. 동승자면 false. */
    @Synchronized
    fun grantConsent(tripId: String, isDriver: Boolean): Boolean {
        if (!isDriver) return false
        consented += tripId
        return true
    }

    /** 동의 전이면 저장하지 않고 false. 저장 실패도 false(예외 전파 안 함). */
    @Synchronized
    fun record(metrics: TripMetrics): Boolean {
        if (metrics.tripId !in consented) return false
        return runCatching { store.save(metrics) }.isSuccess
    }

    /** 저장된 여행 표식. 재시작 후에도 [withdraw]로 지울 수 있게 한다. */
    fun storedTrips(): List<String> = runCatching { store.listTrips() }.getOrDefault(emptyList())

    /** 철회: 동의를 지우고 해당 여행 기록을 삭제한다(PL-NGEOJQ). 삭제 실패 시 false. */
    @Synchronized
    fun withdraw(tripId: String): Boolean {
        consented -= tripId
        return runCatching { store.deleteTrip(tripId) }.isSuccess
    }
}
