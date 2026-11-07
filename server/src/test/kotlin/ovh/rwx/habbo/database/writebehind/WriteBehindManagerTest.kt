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
import java.util.Collections

/**
 * The guarantees the rest of the server relies on when it queues deferred writes: writes without keys stay in strict
 * global FIFO, writes sharing a key keep their order, different keys run in parallel, and flushAllSync() drains all.
 */
class WriteBehindManagerTest {
    @Test
    fun writesWithoutKeysKeepGlobalFifoOrder() {
        val order = Collections.synchronizedList(mutableListOf<Int>())
        repeat(20) { i -> WriteBehindManager.queue { order += i } }

        WriteBehindManager.flushAllSync()

        assertEquals((0 until 20).toList(), order)
    }

    @Test
    fun writesWithTheSameKeyKeepTheirOrder() {
        val order = Collections.synchronizedList(mutableListOf<String>())
        repeat(10) { i -> WriteBehindManager.queue(WriteKey.user(1)) { order += "login$i" } }
        WriteBehindManager.queue(WriteKey.user(1)) { order += "logout" }

        WriteBehindManager.flushAllSync()

        assertEquals((0 until 10).map { "login$it" } + "logout", order)
    }

    @Test
    fun aChildWriteWaitsForEveryParentItDeclares() {
        val events = Collections.synchronizedList(mutableListOf<String>())
        WriteBehindManager.queue(WriteKey.room(7)) { Thread.sleep(150); events += "room" }
        WriteBehindManager.queue(WriteKey.item(9)) { Thread.sleep(150); events += "item" }
        // an item moved into a room that was just created declares both rows
        WriteBehindManager.queue(WriteKey.item(9), WriteKey.room(7)) { events += "move item into room" }

        WriteBehindManager.flushAllSync()

        assertEquals("move item into room", events.last())
        assertEquals(setOf("room", "item", "move item into room"), events.toSet())
    }

    @Test
    fun differentKeysRunInParallel() {
        // 4 keys x 2 writes of 150ms: serial would take 1200ms, one parallel lane per key about 300ms
        repeat(4) { id -> repeat(2) { WriteBehindManager.queue(WriteKey.user(id)) { Thread.sleep(150) } } }

        val start = System.nanoTime()
        WriteBehindManager.flushAllSync()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertTrue("took ${elapsedMs}ms, expected well under the 1200ms a serial drain needs", elapsedMs < 900)
    }

    @Test
    fun aFailingWriteDoesNotStopTheOthers() {
        val ran = Collections.synchronizedList(mutableListOf<String>())
        WriteBehindManager.queue { ran += "default-before" }
        WriteBehindManager.queue { error("boom") }
        WriteBehindManager.queue { ran += "default-after" }
        WriteBehindManager.queue(WriteKey.user(1)) { error("boom") }
        WriteBehindManager.queue(WriteKey.user(1)) { ran += "keyed-after" }
        WriteBehindManager.queue(WriteKey.user(2)) { ran += "other-key" }

        WriteBehindManager.flushAllSync()

        assertEquals(setOf("default-before", "default-after", "keyed-after", "other-key"), ran.toSet())
    }

    @Test
    fun independentWritesWithNoKeysAreAlsoDrained() {
        val ran = Collections.synchronizedList(mutableListOf<Int>())
        repeat(5) { i -> WriteBehindManager.queue(emptyList()) { ran += i } }

        WriteBehindManager.flushAllSync()

        assertEquals(setOf(0, 1, 2, 3, 4), ran.toSet())
    }
}
