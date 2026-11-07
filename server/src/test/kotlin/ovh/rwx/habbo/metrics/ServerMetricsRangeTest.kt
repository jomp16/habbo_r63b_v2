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

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Os handlers mais lentos nos recortes de 5 min, 15 min e 1 h passando pelo amostrador de verdade, com relógio falso
 * para andar minutos sem esperar. Os handlers deste teste têm prefixo próprio, porque o ServerMetrics é um singleton
 * compartilhado com os outros testes.
 */
class ServerMetricsRangeTest {
    private val minute = 60_000L

    private companion object {
        // bem à frente do relógio real, para os minutos destes testes ficarem depois de qualquer balde criado por outro teste
        val origin = System.currentTimeMillis() + 10L * 24 * 60 * 60_000L
        var testsRun = 0
    }

    // cada teste ganha um bloco de 3 h só dele, sempre crescente: o agregador é um singleton e o que um teste
    // grava (ainda dentro de 1 h) apareceria no seguinte. Em produção o tempo só avança, então isso não ocorre.
    private var base = 0L
    private var now = 0L

    private fun ms(v: Int) = v * 1_000_000L

    @Before
    fun fakeClock() {
        base = origin + (testsRun++) * 3 * 60 * minute
        now = base
        ServerMetrics.clock = { now }
        ServerMetrics.sampleNow()
    }

    @After
    fun realClock() {
        ServerMetrics.clock = System::currentTimeMillis
    }

    private fun handlersIn(range: String) =
        ServerMetrics.snapshot().topHandlersByRange.getValue(range).filter { it.name.startsWith("RANGE_TEST_") }.map { it.name }

    @Test
    fun theSnapshotCarriesTheThreeRanges() {
        ServerMetrics.sampleNow()

        assertEquals(listOf("5m", "15m", "1h"), ServerMetrics.snapshot().topHandlersByRange.keys.toList())
        assertEquals(listOf("5m", "15m", "1h"), ServerMetrics.snapshot().topRoomsByRange.keys.toList())
    }

    @Test
    fun aHandlerOnlyShowsUpInTheRangesThatStillCoverIt() {
        ServerMetrics.recordHandler("RANGE_TEST_OLD", ms(900))
        ServerMetrics.sampleNow() // há 40 min

        now = base + 30 * minute
        ServerMetrics.recordHandler("RANGE_TEST_MID", ms(700))
        ServerMetrics.sampleNow() // há 10 min

        now = base + 38 * minute
        ServerMetrics.recordHandler("RANGE_TEST_RECENT", ms(100))
        ServerMetrics.sampleNow() // há 2 min

        now = base + 40 * minute
        ServerMetrics.sampleNow()

        assertEquals(listOf("RANGE_TEST_RECENT"), handlersIn("5m"))
        assertEquals(listOf("RANGE_TEST_MID", "RANGE_TEST_RECENT"), handlersIn("15m"))
        assertEquals(listOf("RANGE_TEST_OLD", "RANGE_TEST_MID", "RANGE_TEST_RECENT"), handlersIn("1h"))
    }

    @Test
    fun theRangesStayFilledWhenTheLastWindowIsIdle() {
        // o defeito que motivou isto: a tabela só enxergava a última janela de 1 s, quase sempre vazia
        ServerMetrics.recordHandler("RANGE_TEST_SSO", ms(400))
        ServerMetrics.sampleNow()

        repeat(30) {
            now += 1_000
            ServerMetrics.sampleNow() // 30 janelas ociosas
        }

        assertTrue("a última janela deveria estar vazia", ServerMetrics.snapshot().topHandlers.none { it.name == "RANGE_TEST_SSO" })
        assertEquals(listOf("RANGE_TEST_SSO"), handlersIn("5m"))
    }

    @Test
    fun theRangeAggregatesCallsAcrossWindows() {
        ServerMetrics.recordHandler("RANGE_TEST_PING", ms(2))
        ServerMetrics.sampleNow()
        now += 1_000
        ServerMetrics.recordHandler("RANGE_TEST_PING", ms(4))
        ServerMetrics.recordHandler("RANGE_TEST_PING", ms(6))
        ServerMetrics.sampleNow()

        val ping = ServerMetrics.snapshot().topHandlersByRange.getValue("5m").single { it.name == "RANGE_TEST_PING" }

        assertEquals(3, ping.calls)
        assertEquals(4.0, ping.avgMs, 0.001)
        assertEquals(6.0, ping.maxMs, 0.001)
    }
}
