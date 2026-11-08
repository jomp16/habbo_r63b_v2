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

package ovh.rwx.habbo.database.writebehind

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WriteBehindStatsTest {
    @Test
    fun `o scheduler alimenta as estatisticas de escritas feitas, falhas, espera e execucao`() {
        WriteBehindStats.drain(0) // zera o que outros testes deixaram na janela

        val scheduler = KeyedWriteScheduler(2) { /* erro esperado */ }
        val key = WriteKey("teste", 1)
        repeat(5) { scheduler.submit(listOf(key)) { Thread.sleep(5) } }
        scheduler.submit(listOf(key)) { error("falha proposital") }
        scheduler.drain()

        val window = WriteBehindStats.drain(scheduler.pendingCount())
        assertEquals(6, window.submitted)
        assertEquals(6, window.completed)
        assertEquals(1, window.failed)
        assertEquals(0, window.pendingNow)
        assertTrue("o pico da fila deve ver as 6 escritas pendentes", window.pendingPeak >= 6)
        assertEquals(6, window.exec.count)
        assertEquals(6, window.wait.count)
        // mesma chave: a 6ª espera as 5 anteriores (~25 ms)
        assertTrue("espera maxima ${window.wait.maxMs} ms", window.wait.maxMs >= 20.0)

        // janela fechada = zerada
        assertEquals(0, WriteBehindStats.drain(0).submitted)
    }

    @Test
    fun `ciclos e entidades sujas entram na janela`() {
        WriteBehindStats.drain(0)
        WriteBehindStats.onCycle(30_000_000L, 12)
        WriteBehindStats.onCycle(10_000_000L, 3)
        WriteBehindStats.onDirtyFlushed(15)

        val window = WriteBehindStats.drain(0)
        assertEquals(2, window.cycle.count)
        assertEquals(12, window.dirtyPeak)
        assertEquals(15, window.dirtyFlushed)
        assertEquals(30.0, window.cycle.maxMs, 0.001)
    }
}
