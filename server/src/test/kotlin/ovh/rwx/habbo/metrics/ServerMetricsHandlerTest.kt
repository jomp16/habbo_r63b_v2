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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Métricas de handler (tempo, espera antes de rodar, mais lentos), de banco e do pool de conexões. */
class ServerMetricsHandlerTest {
    private fun ms(v: Int) = v * 1_000_000L

    @Before
    fun startFromAnEmptyWindow() {
        ServerMetrics.sampleNow()
    }

    @Test
    fun handlerTimeIsAggregatedAndOverrunsUseTheBudget() {
        ServerMetrics.recordHandler("ROOM_OPEN_FLAT", ms(300))
        ServerMetrics.recordHandler("ROOM_OPEN_FLAT", ms(500)) // exatamente no orçamento: não é estouro
        ServerMetrics.recordHandler("SSO_TICKET", ms(600))

        val s = ServerMetrics.sampleNow()

        assertEquals(3, s.handlerCount)
        assertEquals(600.0, s.handlerMaxMs, 0.001)
        assertEquals(1, s.handlerOverruns)
    }

    @Test
    fun slowestHandlersAreKeyedBySemanticNameAndOrderedByP95() {
        repeat(50) { ServerMetrics.recordHandler("MISC_PONG", ms(1)) }
        repeat(2) { ServerMetrics.recordHandler("ROOM_OPEN_FLAT", ms(300)) }
        ServerMetrics.recordHandler("SSO_TICKET", ms(420))

        ServerMetrics.sampleNow()
        val top = ServerMetrics.snapshot().topHandlers

        assertEquals(listOf("SSO_TICKET", "ROOM_OPEN_FLAT", "MISC_PONG"), top.map { it.name })
        assertEquals(50, top.first { it.name == "MISC_PONG" }.calls)
        assertEquals(300.0, top.first { it.name == "ROOM_OPEN_FLAT" }.avgMs, 0.001)
        // a chave é o nome do enum, nunca o id do cabeçalho (que muda a cada release)
        assertTrue(top.none { it.name.all(Char::isDigit) || it.name.startsWith("header") })
    }

    @Test
    fun onlyTheTopHandlersAreKept() {
        repeat(ServerMetrics.TOP_HANDLERS + 5) { ServerMetrics.recordHandler("HANDLER_$it", ms(it + 1)) }

        ServerMetrics.sampleNow()
        val top = ServerMetrics.snapshot().topHandlers

        assertEquals(ServerMetrics.TOP_HANDLERS, top.size)
        // os mais lentos ficam; o mais rápido (1 ms) sai
        assertEquals("HANDLER_${ServerMetrics.TOP_HANDLERS + 4}", top.first().name)
        assertTrue(top.none { it.name == "HANDLER_0" })
    }

    @Test
    fun theTopHandlersStartFromScratchEveryWindow() {
        ServerMetrics.recordHandler("SSO_TICKET", ms(400))
        ServerMetrics.sampleNow()

        ServerMetrics.sampleNow()

        assertTrue(ServerMetrics.snapshot().topHandlers.isEmpty())
    }

    @Test
    fun queueWaitBeforeTheHandlerIsReportedApartFromItsDuration() {
        ServerMetrics.recordHandlerQueueWait(ms(2))
        ServerMetrics.recordHandlerQueueWait(ms(40))
        ServerMetrics.recordHandler("SSO_TICKET", ms(1))

        val s = ServerMetrics.sampleNow()

        assertEquals(21.0, s.handlerQueueWaitAvgMs, 0.001)
        assertEquals(40.0, s.handlerQueueWaitMaxMs, 0.001)
        assertEquals(1.0, s.handlerMaxMs, 0.001)
    }

    @Test
    fun databaseLatencyCountsOverrunsOfTheQueryBudget() {
        ServerMetrics.recordDbQuery(ms(5))
        ServerMetrics.recordDbQuery(ms(130))
        ServerMetrics.recordDbQuery(ms(300)) // acima de 250 ms

        val s = ServerMetrics.sampleNow()

        assertEquals(3, s.dbQueries)
        assertEquals(300.0, s.dbMaxMs, 0.001)
        assertEquals(1, s.dbOverruns)
    }

    @Test
    fun theConnectionPoolIsReportedAsUnavailableWhenThereIsNone() {
        // sem o servidor de pé não existe pool: -1 em tudo, nunca 0 (0 diria "pool ocioso")
        val s = ServerMetrics.sampleNow()

        assertEquals(-1, s.hikariActive)
        assertEquals(-1, s.hikariIdle)
        assertEquals(-1, s.hikariTotal)
        assertEquals(-1, s.hikariWaiting)
        assertEquals(-1, s.hikariMax)
    }
}
