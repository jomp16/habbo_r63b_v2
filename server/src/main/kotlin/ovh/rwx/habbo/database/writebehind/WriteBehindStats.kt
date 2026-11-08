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

import ovh.rwx.habbo.metrics.Reservoir
import ovh.rwx.habbo.metrics.WindowStats
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder

/**
 * Contadores do write-behind para o log de stress ([WriteBehindStressLog]). Barato o bastante para ficar sempre ligado:
 * alguns LongAdder e um reservatório sincronizado por escrita (que já custa uma ida ao banco).
 *
 * Responde: as escritas estão sendo produzidas mais rápido do que persistidas? (fila crescendo, espera na fila alta)
 * e quanto custa cada ciclo de flush do conjunto sujo?
 */
object WriteBehindStats {
    private val submitted = LongAdder()
    private val completed = LongAdder()
    private val failed = LongAdder()
    private val dirtyFlushed = LongAdder()

    /** Da submissão até o início da execução: quanto a escrita esperou na fila (ordem por linha + paralelismo). */
    private val waitNanos = Reservoir(4096)

    /** Duração da escrita em si (inclui a espera por conexão do pool Hikari). */
    private val execNanos = Reservoir(4096)

    /** Duração de cada ciclo de flush (fila de escritas + conjunto sujo). */
    private val cycleNanos = Reservoir(256)

    /** Maior profundidade da fila e maior número de escritas simultâneas na janela. */
    private val pendingPeak = AtomicInteger()
    private val inFlightPeak = AtomicInteger()
    private val dirtyPeak = AtomicInteger()

    data class Window(
        val submitted: Long,
        val completed: Long,
        val failed: Long,
        val dirtyFlushed: Long,
        val pendingNow: Int,
        val pendingPeak: Int,
        val inFlightPeak: Int,
        val dirtyPeak: Int,
        val wait: WindowStats,
        val exec: WindowStats,
        val cycle: WindowStats,
    )

    fun onSubmit(pendingAfter: Int) {
        submitted.increment()
        pendingPeak.accumulateAndGet(pendingAfter) { a, b -> if (b > a) b else a }
    }

    fun onStart(waitedNanos: Long, inFlight: Int) {
        waitNanos.add(waitedNanos)
        inFlightPeak.accumulateAndGet(inFlight) { a, b -> if (b > a) b else a }
    }

    fun onFinish(execNanosSpent: Long, failedAction: Boolean) {
        execNanos.add(execNanosSpent)
        completed.increment()
        if (failedAction) failed.increment()
    }

    fun onCycle(elapsedNanos: Long, dirtyBefore: Int) {
        cycleNanos.add(elapsedNanos)
        dirtyPeak.accumulateAndGet(dirtyBefore) { a, b -> if (b > a) b else a }
    }

    fun onDirtyFlushed(entities: Int) = dirtyFlushed.add(entities.toLong())

    /** Fecha a janela: devolve os números desde a última chamada e zera. */
    fun drain(pendingNow: Int): Window = Window(
        submitted = submitted.sumThenReset(),
        completed = completed.sumThenReset(),
        failed = failed.sumThenReset(),
        dirtyFlushed = dirtyFlushed.sumThenReset(),
        pendingNow = pendingNow,
        pendingPeak = pendingPeak.getAndSet(pendingNow),
        inFlightPeak = inFlightPeak.getAndSet(0),
        dirtyPeak = dirtyPeak.getAndSet(0),
        wait = waitNanos.drain(),
        exec = execNanos.drain(),
        cycle = cycleNanos.drain(),
    )
}
