package ai.familystop.feature.recommend

import ai.familystop.core.Facility
import ai.familystop.core.FacilitySource
import ai.familystop.core.GeoPoint
import ai.familystop.core.StopNeed
import ai.familystop.core.StopRequest
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendTest {
    // 앱이 싣는 실제 시연 JSON을 그대로 파싱해 형식이 깨지면 여기서 실패한다.
    private val demo =
        parseFacilities(java.io.File("../../app/src/main/assets/demo/facilities.json").readText())
    private val here = GeoPoint(37.28, 127.05, 0)
    private val all = setOf(StopNeed.MEAL, StopNeed.RESTROOM, StopNeed.KIDS_REST)

    @Test
    fun ranksByMetNeedsThenDelayAndCapsAtThree() {
        val r = recommend(StopRequest(here, all), demo)
        assertEquals(
            listOf("가상 휴게소 C", "가상 가족식당 A", "가상 키즈카페 B"),
            r.map { it.place.name }
        )
        assertEquals("가상 휴게소 C", r[0].place.name)
        assertTrue(r[0].missed.isEmpty())
        val partial =
            recommend(StopRequest(here, setOf(StopNeed.MEAL, StopNeed.KIDS_REST)), demo)
        assertEquals(
            setOf(StopNeed.KIDS_REST),
            partial.first {
                it.place.name == "가상 가족식당 A"
            }.missed
        )
    }

    @Test
    fun emptyWhenNothingNearbyOrNoNeed() {
        assertTrue(recommend(StopRequest(GeoPoint(35.0, 129.0, 0), all), demo).isEmpty())
        assertTrue(recommend(StopRequest(here, emptySet()), demo).isEmpty())
    }

    @Test
    fun lateResponseDoesNotOverwriteCurrentResult() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        val source = FacilitySource {
            if (calls.incrementAndGet() == 1) {
                entered.complete(Unit)
                withContext(NonCancellable) { gate.await() } // 취소를 무시하는 느린 첫 응답
                finished.complete(Unit)
            }
            demo
        }
        val c = RecommendController(CoroutineScope(Dispatchers.Default), source)
        c.request(StopRequest(here, setOf(StopNeed.RESTROOM)))
        withTimeout(2_000) { entered.await() }
        c.request(StopRequest(here, setOf(StopNeed.MEAL)))
        val second = withTimeout(2_000) { c.state.first { it is RecommendState.Success } }
        gate.complete(Unit)
        withTimeout(2_000) { finished.await() }
        delay(50) // 첫 응답이 게시를 시도할 시간
        assertEquals(second, c.state.value)
    }

    @Test
    fun timeoutBecomesFailedState() = runBlocking {
        val c =
            RecommendController(CoroutineScope(Dispatchers.Default), {
                CompletableDeferred<List<Facility>>().await()
            }, timeoutMs = 50)
        c.request(StopRequest(here, all))
        withTimeout(2_000) { c.state.first { it is RecommendState.Failed } }
        Unit
    }

    @Test
    fun timeoutAlsoCoversSourcesThatIgnoreCancellation() = runBlocking {
        val never = CompletableDeferred<List<Facility>>()
        val c = RecommendController(
            CoroutineScope(Dispatchers.Default),
            { withContext(NonCancellable) { never.await() } },
            timeoutMs = 50
        )
        c.request(StopRequest(here, all))
        withTimeout(2_000) { c.state.first { it is RecommendState.Failed } }
        Unit
    }

    @Test
    fun sourceFailureDoesNotKillCallerScope() = runBlocking {
        val scope = CoroutineScope(Job()) // 일반 Job: 자식 실패가 전파되면 scope가 죽는다
        var fail = true
        val c = RecommendController(scope, { if (fail) error("boom") else demo })
        c.request(StopRequest(here, all))
        withTimeout(2_000) { c.state.first { it is RecommendState.Failed } }
        fail = false
        c.request(StopRequest(here, all))
        withTimeout(2_000) { c.state.first { it is RecommendState.Success } }
        Unit
    }
}
