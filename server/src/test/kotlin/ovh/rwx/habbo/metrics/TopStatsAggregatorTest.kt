/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

package ovh.rwx.habbo.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** O resumo por minuto que faz as tabelas "mais lentos" obedecerem o seletor de 5 min / 15 min / 1 h. */
class TopStatsAggregatorTest {
    private val minute = 60_000L
    private val t0 = 1_700_000_000_000L // um instante qualquer, alinhado só pelo "minuto" em que cai

    private fun handler(name: String, calls: Long, avg: Double, p95: Double, max: Double) = TopHandlerStat(name, calls, avg, p95, max)

    private fun room(id: Int, name: String, users: Int, avg: Double, p95: Double, max: Double, ticks: Long) =
        RoomTickStat(id, name, users, avg, p95, max, ticks)

    @Test
    fun handlersOfTheSameMinuteAreCombinedWithWeightedAverageWorstP95AndRealMax() {
        val agg = TopStatsAggregator()
        agg.addHandlers(t0, listOf(handler("SSO_TICKET", calls = 1, avg = 400.0, p95 = 400.0, max = 400.0)))
        agg.addHandlers(t0 + 5_000, listOf(handler("SSO_TICKET", calls = 3, avg = 200.0, p95 = 250.0, max = 300.0)))

        val sso = agg.handlers(t0 + 10_000, 5 * minute, 10).single()

        assertEquals("SSO_TICKET", sso.name)
        assertEquals(4, sso.calls)
        assertEquals(250.0, sso.avgMs, 0.001) // (400*1 + 200*3) / 4: ponderada pelas chamadas, não (400+200)/2
        assertEquals(400.0, sso.p95Ms, 0.001) // o pior p95 de janela visto
        assertEquals(400.0, sso.maxMs, 0.001)
    }

    @Test
    fun eachRangeOnlyCountsTheMinutesInsideIt() {
        val agg = TopStatsAggregator()
        agg.addHandlers(t0, listOf(handler("OLD", 1, 900.0, 900.0, 900.0)))            // há 40 min
        agg.addHandlers(t0 + 30 * minute, listOf(handler("MID", 1, 700.0, 700.0, 700.0))) // há 10 min
        agg.addHandlers(t0 + 38 * minute, listOf(handler("RECENT", 1, 100.0, 100.0, 100.0))) // há 2 min
        val now = t0 + 40 * minute

        assertEquals(listOf("RECENT"), agg.handlers(now, 5 * minute, 10).map { it.name })
        assertEquals(listOf("MID", "RECENT"), agg.handlers(now, 15 * minute, 10).map { it.name })
        assertEquals(listOf("OLD", "MID", "RECENT"), agg.handlers(now, 60 * minute, 10).map { it.name })
    }

    @Test
    fun handlersAreOrderedByWorstP95AndLimited() {
        val agg = TopStatsAggregator()
        agg.addHandlers(t0, (1..30).map { handler("H$it", 1, it.toDouble(), it.toDouble(), it.toDouble()) })

        val top = agg.handlers(t0, 5 * minute, 15)

        assertEquals(15, top.size)
        assertEquals("H30", top.first().name)
        assertEquals((16..30).reversed().map { "H$it" }, top.map { it.name })
    }

    @Test
    fun minutesOlderThanAnHourAreDiscarded() {
        val agg = TopStatsAggregator()
        agg.addHandlers(t0, listOf(handler("ANCIENT", 1, 50.0, 50.0, 50.0)))
        agg.addHandlers(t0 + 90 * minute, listOf(handler("NOW", 1, 5.0, 5.0, 5.0)))

        // mesmo pedindo "1 dia", o que passou de 60 min já saiu da memória
        val all = agg.handlers(t0 + 90 * minute, 24 * 60 * minute, 10).map { it.name }

        assertEquals(listOf("NOW"), all)
    }

    @Test
    fun aBucketKeepsOnlyTheWorstHandlersSoMemoryDoesNotGrow() {
        val agg = TopStatsAggregator(maxPerBucket = 10)
        // 200 handlers diferentes no mesmo minuto; o mais lento é o H200
        agg.addHandlers(t0, (1..200).map { handler("H$it", 1, it.toDouble(), it.toDouble(), it.toDouble()) })

        val kept = agg.handlers(t0, 5 * minute, 1000)

        assertTrue("guardou ${kept.size}, esperava no máximo ${10 * 2}", kept.size <= 20)
        assertEquals("H200", kept.first().name)
        assertTrue(kept.none { it.name == "H1" })
    }

    @Test
    fun roomsAreCombinedWeightedByTicksWithTheLatestNameAndUsers() {
        val agg = TopStatsAggregator()
        agg.addRooms(t0, listOf(room(14, "Wireds 2.0", users = 1, avg = 2.0, p95 = 5.0, max = 6.0, ticks = 20)))
        agg.addRooms(t0 + 5_000, listOf(room(14, "Wireds 2.0 v2", users = 3, avg = 4.0, p95 = 9.0, max = 30.0, ticks = 60)))

        val r = agg.rooms(t0 + 10_000, 5 * minute, 10).single()

        assertEquals(80, r.ticks)
        assertEquals(3.5, r.tickAvgMs, 0.001) // (2*20 + 4*60) / 80
        assertEquals(9.0, r.tickP95Ms, 0.001)
        assertEquals(30.0, r.tickMaxMs, 0.001)
        assertEquals("Wireds 2.0 v2", r.name)
        assertEquals(3, r.users)
    }

    @Test
    fun roomsFollowTheRangeAndAreOrderedByP95() {
        val agg = TopStatsAggregator()
        agg.addRooms(t0, listOf(room(1, "antigo", 1, 1.0, 400.0, 400.0, 20)))
        agg.addRooms(t0 + 38 * minute, listOf(room(2, "lento", 1, 1.0, 50.0, 50.0, 20), room(3, "rápido", 1, 1.0, 2.0, 2.0, 20)))
        val now = t0 + 40 * minute

        assertEquals(listOf("lento", "rápido"), agg.rooms(now, 5 * minute, 10).map { it.name })
        assertEquals(listOf("antigo", "lento", "rápido"), agg.rooms(now, 60 * minute, 10).map { it.name })
    }

    @Test
    fun aClockThatGoesBackwardsKeepsUsingTheNewestBucket() {
        val agg = TopStatsAggregator()
        agg.addHandlers(t0 + 10 * minute, listOf(handler("A", 1, 10.0, 10.0, 10.0)))

        agg.addHandlers(t0, listOf(handler("A", 1, 30.0, 30.0, 30.0))) // relógio voltou 10 min

        val a = agg.handlers(t0 + 10 * minute, 5 * minute, 10).single()
        assertEquals(2, a.calls)
        assertEquals(20.0, a.avgMs, 0.001)
    }

    @Test
    fun anEmptyAggregatorReturnsEmptyLists() {
        val agg = TopStatsAggregator()

        assertTrue(agg.handlers(t0, 60 * minute, 10).isEmpty())
        assertTrue(agg.rooms(t0, 60 * minute, 10).isEmpty())
        assertNotNull(agg)
    }
}
