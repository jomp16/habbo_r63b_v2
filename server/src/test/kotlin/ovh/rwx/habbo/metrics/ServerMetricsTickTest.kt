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
import org.junit.Before
import org.junit.Test

/**
 * O tick tem duas cadências com orçamentos diferentes: minor (50 ms, gatilhos periódicos do wired) e major
 * (500 ms, física e animação). Elas precisam ser medidas em separado, senão o major, sempre mais caro, esconde o minor.
 */
class ServerMetricsTickTest {
    private fun ms(v: Int) = v * 1_000_000L

    @Before
    fun startFromAnEmptyWindow() {
        ServerMetrics.sampleNow()
    }

    @Test
    fun minorAndMajorTicksAreMeasuredSeparately() {
        repeat(18) { ServerMetrics.recordRoomTick(1, ms(2), isMajor = false) }
        ServerMetrics.recordRoomTick(1, ms(120), isMajor = true)
        ServerMetrics.recordRoomTick(1, ms(130), isMajor = true)

        val s = ServerMetrics.sampleNow()

        assertEquals(18, s.roomTickMinorCount)
        assertEquals(2.0, s.roomTickMinorMaxMs, 0.001)
        assertEquals(2, s.roomTickMajorCount)
        assertEquals(130.0, s.roomTickMajorMaxMs, 0.001)
        // o total continua somando as duas cadências
        assertEquals(20, s.roomTicks)
    }

    @Test
    fun aSlowMinorTickIsNotHiddenBehindTheMajorOne() {
        // o minor de 90 ms atrasa o wired; o major de 100 ms é o pior tick da janela e o esconderia no total
        repeat(17) { ServerMetrics.recordRoomTick(1, ms(2), isMajor = false) }
        ServerMetrics.recordRoomTick(1, ms(90), isMajor = false)
        ServerMetrics.recordRoomTick(1, ms(100), isMajor = true)

        val s = ServerMetrics.sampleNow()

        assertEquals(90.0, s.roomTickMinorMaxMs, 0.001)
        assertEquals(1, s.roomTickMinorOverruns)
        assertEquals(0, s.roomTickMajorOverruns)
    }

    @Test
    fun overrunsUseTheBudgetOfEachCadence() {
        // 60 ms estoura o orçamento do minor (50 ms), mas está longe de estourar o do major (500 ms)
        ServerMetrics.recordRoomTick(1, ms(60), isMajor = false)
        ServerMetrics.recordRoomTick(1, ms(60), isMajor = true)
        // 600 ms estoura o major
        ServerMetrics.recordRoomTick(1, ms(600), isMajor = true)

        val s = ServerMetrics.sampleNow()

        assertEquals(1, s.roomTickMinorOverruns)
        assertEquals(1, s.roomTickMajorOverruns)
    }

    @Test
    fun majorIntervalAndSchedulingLagAreReported() {
        ServerMetrics.recordMajorInterval(ms(500))
        ServerMetrics.recordMajorInterval(ms(560))
        ServerMetrics.recordSchedulingLag(ms(0))
        ServerMetrics.recordSchedulingLag(ms(35))

        val s = ServerMetrics.sampleNow()

        assertEquals(530.0, s.majorIntervalAvgMs, 0.001)
        assertEquals(560.0, s.majorIntervalMaxMs, 0.001)
        assertEquals(35.0, s.schedLagMaxMs, 0.001)
    }

    @Test
    fun aWindowWithoutTicksReportsNoData() {
        val s = ServerMetrics.sampleNow()

        assertEquals(0, s.roomTickMinorCount)
        assertEquals(0, s.roomTickMajorCount)
        assertEquals(0, s.roomTickMinorOverruns)
        assertEquals(0, s.roomTickMajorOverruns)
    }
}
