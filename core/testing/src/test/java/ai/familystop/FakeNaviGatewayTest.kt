package ai.familystop

import ai.familystop.core.StopPlace
import ai.familystop.fake.FakeNaviGateway
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNaviGatewayTest {
    @Test
    fun thirdWaypointFails() {
        val navi = FakeNaviGateway()
        val results = (1..3).map { i ->
            var r: Result<Unit>? = null
            navi.addWaypoint(StopPlace("p$i", 0.0, 0.0)) { r = it }
            r!!
        }
        assertTrue(results[0].isSuccess && results[1].isSuccess && results[2].isFailure)
    }
}
