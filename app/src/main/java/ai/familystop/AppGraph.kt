package ai.familystop

import ai.familystop.core.DestinationSource
import ai.familystop.core.FacilitySource
import ai.familystop.core.LocationSource
import ai.familystop.core.NaviGateway
import ai.familystop.core.VehicleEnergySource
import ai.familystop.core.VoiceOutput
import ai.familystop.feature.handoff.HandoffController
import ai.familystop.feature.metrics.FileMetricsStore
import ai.familystop.feature.metrics.MetricsCollector
import ai.familystop.feature.recommend.RecommendController
import ai.familystop.feature.recommend.RecommendState
import ai.familystop.feature.recommend.parseFacilities
import ai.familystop.feature.voice.VoiceGuide
import ai.familystop.feature.voice.spokenText
import ai.familystop.impl.platform.PlatformLocationSource
import ai.familystop.impl.platform.PlatformVoiceOutput
import ai.familystop.impl.pleos.NaviSession
import ai.familystop.impl.pleos.PleosDestinationSource
import ai.familystop.impl.pleos.PleosNaviGateway
import ai.familystop.impl.pleos.PleosVehicleEnergySource
import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Single wiring point: swap an implementation here, nowhere else (ADR-0006). */
class AppGraph(context: Context) {
    private val app = context.applicationContext

    val location: LocationSource = PlatformLocationSource(app)
    val voice: VoiceOutput = PlatformVoiceOutput(app)
    private val naviSession = NaviSession(app)
    val navi: NaviGateway = PleosNaviGateway(naviSession)
    val destinations: DestinationSource = PleosDestinationSource(naviSession)
    val energy: VehicleEnergySource = PleosVehicleEnergySource(app)
    val metrics = MetricsCollector(FileMetricsStore(File(app.filesDir, "metrics")))

    /** 시연 구간 시설 JSON (ADR-0001: 가상 데이터는 시연 한정, 화면에 출처 표시). */
    val facilities = FacilitySource {
        parseFacilities(
            app.assets.open("demo/facilities.json").bufferedReader().use {
                it.readText()
            }
        )
    }

    // Process-lifetime state so Activity recreation (day/night) keeps results and voice status.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val recommend = RecommendController(scope, facilities)
    val guide = VoiceGuide(voice, { app.resources.configuration.locales[0] }) { u, locale ->
        spokenText(app, u, locale)
    }
    val session = Session(metrics)

    /** Navi seen by every screen: voice result + metrics on each successful add. */
    val shellNavi = ShellNavi(navi, guide, session) { place ->
        (recommend.state.value as? RecommendState.Success)
            ?.candidates?.firstOrNull { it.place == place }
    }
    val handoff = HandoffController(scope, shellNavi, held = shellNavi.held)
}
