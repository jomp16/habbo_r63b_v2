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
import org.junit.Test

class MetricsStructuresTest {
    @Test
    fun reservoirComputesStatsAndResets() {
        val r = Reservoir(1000)
        for (i in 1..100) r.add(i * 1_000_000L)
        val s = r.drain()
        assertEquals(100, s.count)
        assertEquals(50.5, s.avgMs, 0.001)
        assertEquals(96.0, s.p95Ms, 0.001)
        assertEquals(100.0, s.maxMs, 0.001)
        assertEquals(0, r.drain().count)
    }

    @Test
    fun reservoirComputesP99() {
        val r = Reservoir(1000)
        for (i in 1..100) r.add(i * 1_000_000L)
        assertEquals(100.0, r.drain().p99Ms, 0.001)
    }

    @Test
    fun reservoirCountsEveryOverBudgetSampleEvenBeyondTheRing() {
        // orçamento de 50 ms; o anel guarda só 10 amostras mas a contagem de estouros vale para a janela inteira
        val r = Reservoir(10, budgetNanos = 50_000_000L)
        repeat(1000) { r.add(if (it % 100 == 0) 80_000_000L else 5_000_000L) }
        val s = r.drain()
        assertEquals(1000, s.count)
        assertEquals(10, s.overBudget)
        // a janela seguinte começa zerada
        r.add(51_000_000L)
        assertEquals(1, r.drain().overBudget)
        assertEquals(0, r.drain().overBudget)
    }

    @Test
    fun aSampleExactlyOnTheBudgetIsNotAnOverrun() {
        val r = Reservoir(10, budgetNanos = 50_000_000L)
        r.add(50_000_000L)
        assertEquals(0, r.drain().overBudget)
    }

    @Test
    fun reservoirWithoutBudgetNeverReportsOverruns() {
        val r = Reservoir(10)
        r.add(Long.MAX_VALUE / 2)
        assertEquals(0, r.drain().overBudget)
    }

    @Test
    fun reservoirIsBounded() {
        val r = Reservoir(10)
        for (i in 1..1000) r.add(i.toLong())
        val s = r.drain()
        assertEquals(1000, s.count)
        assertEquals(1000.0, s.maxNanos, 0.0)
    }

    @Test
    fun ringBufferKeepsNewestInOrder() {
        val b = RingBuffer<Int>(3)
        for (i in 1..5) b.add(i)
        assertEquals(listOf(3, 4, 5), b.snapshot())
    }
}
