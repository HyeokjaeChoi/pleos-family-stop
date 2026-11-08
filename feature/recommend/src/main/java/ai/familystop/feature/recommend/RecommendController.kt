package ai.familystop.feature.recommend

import ai.familystop.core.FacilitySource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface RecommendState {
    data object Idle : RecommendState

    data object Loading : RecommendState

    data class Success(val candidates: List<StopCandidate>) : RecommendState

    /** 조건에 맞는 후보 없음 → 안내 + 재검색. */
    data object Empty : RecommendState

    /** 데이터 미수신(타임아웃·오류) → 이유 한 줄 + 재검색. */
    data class Failed(val reason: String, val cause: Throwable? = null) : RecommendState
}

/** 주행 중 화면용: 큰 카드 최대 2개(ADR-0003). */
fun RecommendState.driveCards(): List<StopCandidate> =
    (this as? RecommendState.Success)?.candidates.orEmpty().take(DRIVING_MAX_CANDIDATES)

/**
 * 추천 요청 상태. 요청은 사용자가 할 때만(ADR-0004), 새 요청은 이전 요청을 무효화해
 * 늦게 온 응답이 현재 결과를 덮지 않는다.
 */
class RecommendController(
    private val scope: CoroutineScope,
    private val source: FacilitySource,
    // ponytail: 5s 타임아웃·3s 응답 목표는 잠정값(ADR-0001). 3s는 측정 목표라 강제하지 않고 측정 후 확정.
    private val timeoutMs: Long = 5_000
) {
    private val _state = MutableStateFlow<RecommendState>(RecommendState.Idle)
    val state: StateFlow<RecommendState> = _state

    private var job: Job? = null
    private var seq = 0

    @Synchronized
    fun request(req: StopRequest) {
        job?.cancel()
        val mine = ++seq
        _state.value = RecommendState.Loading
        job = scope.launch {
            val result = try {
                // 취소를 무시하는 제공처여도 타임아웃으로 상태가 풀리도록 fetch를 별도 async로 둔다.
                val fetch = scope.async { source.facilities() }
                val facilities = try {
                    withTimeout(timeoutMs) { fetch.await() }
                } finally {
                    fetch.cancel()
                }
                val found = recommend(req, facilities)
                if (found.isEmpty()) RecommendState.Empty else RecommendState.Success(found)
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                RecommendState.Failed("시설 정보를 받지 못했어요. 다시 시도해 주세요.")
            } catch (e: Exception) {
                RecommendState.Failed("시설 정보를 불러오지 못했어요. 다시 시도해 주세요.", e)
            }
            publish(mine, result)
        }
    }

    // 확인과 쓰기를 request()와 같은 락에서 해, 늦은 응답이 새 요청의 Loading을 덮지 못하게 한다.
    @Synchronized
    private fun publish(mine: Int, result: RecommendState) {
        if (mine == seq) _state.value = result
    }
}
