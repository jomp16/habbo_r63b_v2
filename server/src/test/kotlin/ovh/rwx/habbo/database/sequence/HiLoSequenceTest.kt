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

package ovh.rwx.habbo.database.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class HiLoSequenceTest {
    /** Banco em memória: `nextHi` faz o papel de `sequences.next_hi`. */
    private class FakeSequence(blockSize: Int, start: Int = 1) : HiLoSequence("t", blockSize = blockSize) {
        val nextHi = AtomicInteger(start)
        val fetches = AtomicInteger()

        override fun fetchNextHi(): Int {
            fetches.incrementAndGet()
            Thread.yield() // alarga a janela da troca de bloco
            return nextHi.getAndIncrement()
        }

        override fun returnBlock(hi: Int): Boolean = nextHi.compareAndSet(hi + 1, hi)
    }

    @Test
    fun idsFollowBlocks() {
        val seq = FakeSequence(blockSize = 3, start = 5)
        assertEquals(listOf(15, 16, 17, 18, 19, 20, 21), List(7) { seq.nextId() })
        assertEquals(3, seq.fetches.get())
    }

    @Test
    fun noDuplicatesUnderContention() {
        // bloco minúsculo: quase todo nextId() cruza uma troca de bloco, que é onde a versão antiga duplicava ids
        val seq = FakeSequence(blockSize = 2)
        val ids = ConcurrentHashMap.newKeySet<Int>()
        val dupes = AtomicInteger()
        val start = CountDownLatch(1)
        val perThread = 5_000
        val threads = List(8) {
            thread {
                start.await()
                repeat(perThread) { if (!ids.add(seq.nextId())) dupes.incrementAndGet() }
            }
        }
        start.countDown()
        threads.forEach { it.join() }

        assertEquals(0, dupes.get())
        assertEquals(8 * perThread, ids.size)
    }

    @Test
    fun releasesOnlyAnUntouchedBlock() {
        val seq = FakeSequence(blockSize = 100, start = 7)
        seq.warmUp()
        assertEquals(8, seq.nextHi.get())
        assertTrue(seq.releaseUnusedBlock())
        assertEquals(7, seq.nextHi.get()) // o próximo boot reusa o hi 7
        assertFalse(seq.releaseUnusedBlock()) // já sem bloco

        // depois de devolver, um nextId tardio reserva de novo (sem reusar ids de um bloco devolvido)
        assertEquals(700, seq.nextId())
        assertFalse(seq.releaseUnusedBlock()) // bloco em uso: não devolve
        assertEquals(8, seq.nextHi.get())
    }

    @Test
    fun doesNotReleaseWhenSomeoneReservedAfter() {
        val seq = FakeSequence(blockSize = 10, start = 3)
        seq.warmUp()
        seq.nextHi.incrementAndGet() // outra instância reservou o hi 4
        assertFalse(seq.releaseUnusedBlock())
        assertEquals(5, seq.nextHi.get())
    }
}
