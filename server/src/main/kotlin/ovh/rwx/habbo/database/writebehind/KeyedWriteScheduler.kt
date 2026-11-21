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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Runs write actions in parallel while keeping the order of every row they touch.
 *
 * Each action declares the keys of the rows it touches (see [WriteKey]). Two actions that share a key always run in
 * the order they were submitted; actions whose keys are disjoint run at the same time, up to [parallelism] at once.
 * An action with no key touches nothing others depend on and may run at any moment.
 *
 * How it stays correct: submitting an action appends it to the FIFO of each of its keys, atomically. An action runs
 * only when it is the head of every one of its FIFOs. Because all FIFOs follow the same global submission order, the
 * oldest unfinished action is always runnable, so the scheduler can neither deadlock nor reorder a row.
 */
class KeyedWriteScheduler(
    private val parallelism: Int,
    /** Called with the failed action's keys (diagnostics); [onError] still gets every failure. */
    private val onFailure: (keys: List<Any>, error: Throwable) -> Unit = { _, _ -> },
    private val onError: (Throwable) -> Unit,
) {
    private class Task(val keys: List<Any>, val action: () -> Unit) {
        var started = false
        val submittedNanos = System.nanoTime()
    }

    private val lock = Any()
    private val lanes = HashMap<Any, ArrayDeque<Task>>()
    private val independent = ArrayDeque<Task>()
    private var pending = 0

    // only one drain at a time, so a task is never handed out twice and flushAllSync can wait for a running drain
    private val drainLock = ReentrantLock()

    init {
        require(parallelism >= 1) { "parallelism must be at least 1" }
    }

    /** Queues [action]; it runs after every earlier action that shares any of [keys]. */
    fun submit(keys: Collection<Any>, action: () -> Unit) {
        val task = Task(keys.distinct(), action)
        synchronized(lock) {
            if (task.keys.isEmpty()) independent.addLast(task)
            else task.keys.forEach { lanes.getOrPut(it) { ArrayDeque() }.addLast(task) }
            pending++
            WriteBehindStats.onSubmit(pending)
        }
    }

    /** Actions submitted and not finished yet (queued or running). */
    fun pendingCount(): Int = synchronized(lock) { pending }

    /** Debug view of the queue: totals, the oldest pending action and the longest lanes. */
    data class Snapshot(
        val pending: Int,
        val independent: Int,
        val lanes: Int,
        val oldestPendingMs: Long?,
        val longestLanes: List<Pair<String, Int>>,
    )

    fun snapshot(topLanes: Int = 10): Snapshot = synchronized(lock) {
        val now = System.nanoTime()
        val oldest = (lanes.values.mapNotNull { it.firstOrNull() } + independent)
            .minOfOrNull { it.submittedNanos }

        Snapshot(
            pending = pending,
            independent = independent.size,
            lanes = lanes.size,
            oldestPendingMs = oldest?.let { (now - it) / 1_000_000 },
            longestLanes = lanes.entries.sortedByDescending { it.value.size }.take(topLanes).map { it.key.toString() to it.value.size },
        )
    }

    fun isEmpty(): Boolean = pendingCount() == 0

    /** Runs everything submitted so far, and whatever is submitted while draining, then returns. */
    fun drain() {
        drainLock.withLock {
            if (isEmpty()) return

            runBlocking(Dispatchers.IO.limitedParallelism(parallelism)) {
                val finished = Channel<Task>(Channel.UNLIMITED)
                var inFlight = 0

                fun launchReady() {
                    for (task in takeReady()) {
                        inFlight++
                        val inFlightNow = inFlight
                        launch {
                            val startNanos = System.nanoTime()
                            WriteBehindStats.onStart(startNanos - task.submittedNanos, inFlightNow)
                            var failedAction = false
                            try {
                                task.action()
                            } catch (e: Throwable) {
                                failedAction = true
                                onFailure(task.keys, e)
                                onError(e)
                            } finally {
                                WriteBehindStats.onFinish(System.nanoTime() - startNanos, failedAction)
                                finished.send(task)
                            }
                        }
                    }
                }

                launchReady()
                while (inFlight > 0) {
                    complete(finished.receive())
                    inFlight--
                    launchReady()
                }
            }
        }
    }

    /** Marks as started, and returns, every action that is now allowed to run. */
    private fun takeReady(): List<Task> = synchronized(lock) {
        val ready = ArrayList<Task>()

        while (independent.isNotEmpty()) {
            val task = independent.removeFirst()
            task.started = true
            ready += task
        }

        for (lane in lanes.values) {
            val head = lane.firstOrNull() ?: continue
            if (head.started) continue
            if (head.keys.all { lanes[it]?.firstOrNull() === head }) {
                head.started = true
                ready += head
            }
        }

        ready
    }

    private fun complete(task: Task) = synchronized(lock) {
        for (key in task.keys) {
            val lane = lanes[key] ?: continue
            lane.removeFirst()
            if (lane.isEmpty()) lanes.remove(key)
        }
        pending--
    }
}
