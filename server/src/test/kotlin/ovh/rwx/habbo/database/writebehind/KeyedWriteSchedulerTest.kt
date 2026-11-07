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
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

class KeyedWriteSchedulerTest {
    private val errors = Collections.synchronizedList(mutableListOf<Throwable>())
    private fun scheduler(parallelism: Int = 4) = KeyedWriteScheduler(parallelism) { errors += it }

    private fun key(id: Int) = WriteKey.item(id)

    @Test
    fun actionsSharingAKeyRunInSubmissionOrder() {
        val s = scheduler()
        val order = Collections.synchronizedList(mutableListOf<Int>())
        repeat(30) { i -> s.submit(listOf(key(1))) { order += i } }

        s.drain()

        assertEquals((0 until 30).toList(), order)
        assertTrue(s.isEmpty())
    }

    @Test
    fun disjointKeysRunInParallel() {
        val s = scheduler()
        // 4 keys x 2 writes of 150ms: serial = 1200ms, one lane per key = about 300ms
        repeat(4) { k -> repeat(2) { s.submit(listOf(key(k))) { Thread.sleep(150) } } }

        val start = System.nanoTime()
        s.drain()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertTrue("took ${elapsedMs}ms, a serial drain needs 1200ms", elapsedMs < 900)
    }

    @Test
    fun actionsWithoutKeysRunInParallel() {
        val s = scheduler()
        repeat(4) { s.submit(emptyList()) { Thread.sleep(200) } }

        val start = System.nanoTime()
        s.drain()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertTrue("took ${elapsedMs}ms, a serial drain needs 800ms", elapsedMs < 600)
    }

    @Test
    fun anActionWithSeveralKeysWaitsForEveryLaneOfThem() {
        val s = scheduler()
        val events = Collections.synchronizedList(mutableListOf<String>())

        // parent rows A (key 1) and B (key 2) are slow to insert; the child touching both must come after both
        s.submit(listOf(key(1))) { Thread.sleep(200); events += "A" }
        s.submit(listOf(key(2))) { Thread.sleep(200); events += "B" }
        s.submit(listOf(key(1), key(2))) { events += "child" }
        // unrelated row: must not wait for any of the above
        s.submit(listOf(key(3))) { events += "unrelated" }

        s.drain()

        assertEquals("child", events.last())
        assertTrue(events.indexOf("unrelated") < events.indexOf("child"))
        assertEquals(setOf("A", "B", "child", "unrelated"), events.toSet())
    }

    @Test
    fun neverRunsMoreThanTheParallelismLimitAtOnce() {
        val s = scheduler(parallelism = 3)
        val running = AtomicInteger()
        val peak = AtomicInteger()
        repeat(12) { k ->
            s.submit(listOf(key(k))) {
                val now = running.incrementAndGet()
                peak.updateAndGet { maxOf(it, now) }
                Thread.sleep(40)
                running.decrementAndGet()
            }
        }

        s.drain()

        assertTrue("peak concurrency was ${peak.get()}", peak.get() in 2..3)
    }

    @Test
    fun aFailingActionDoesNotStopTheOthersAndIsReported() {
        errors.clear()
        val s = scheduler()
        val ran = Collections.synchronizedList(mutableListOf<String>())
        s.submit(listOf(key(1))) { ran += "first" }
        s.submit(listOf(key(1))) { error("boom") }
        s.submit(listOf(key(1))) { ran += "after-failure-same-key" }
        s.submit(listOf(key(2))) { ran += "other-key" }

        s.drain()

        assertEquals(listOf("first", "after-failure-same-key"), ran.filter { it != "other-key" })
        assertTrue("other-key" in ran)
        assertEquals(1, errors.size)
    }

    @Test
    fun actionsSubmittedWhileDrainingStillRunAfterTheirPredecessors() {
        val s = scheduler()
        val order = Collections.synchronizedList(mutableListOf<String>())
        s.submit(listOf(key(1))) {
            order += "parent"
            // a running write queues a dependent one (e.g. a DAO calling another DAO that queues)
            s.submit(listOf(key(1))) { order += "child" }
        }

        s.drain()
        s.drain()

        assertEquals(listOf("parent", "child"), order)
        assertTrue(s.isEmpty())
    }

    @Test
    fun randomKeySetsKeepPerKeyOrderAndNeverDeadlock() {
        val s = scheduler(parallelism = 4)
        val random = Random(42)
        val submitted = HashMap<Int, MutableList<Int>>()
        val observed = HashMap<Int, MutableList<Int>>()
        val lock = Any()
        val total = 400

        for (task in 0 until total) {
            val keys = (0 until 1 + random.nextInt(3)).map { random.nextInt(8) }.distinct()
            keys.forEach { submitted.getOrPut(it) { mutableListOf() } += task }
            s.submit(keys.map { key(it) }) {
                if (task % 50 == 0) Thread.sleep(5)
                synchronized(lock) { keys.forEach { observed.getOrPut(it) { mutableListOf() } += task } }
            }
        }

        val finished = Thread { s.drain() }.apply { start() }
        finished.join(30_000)

        assertTrue("drain did not finish: deadlock?", !finished.isAlive)
        assertTrue(s.isEmpty())
        assertEquals(submitted, observed)
    }
}
