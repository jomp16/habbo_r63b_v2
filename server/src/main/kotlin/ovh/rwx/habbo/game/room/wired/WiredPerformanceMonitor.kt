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

package ovh.rwx.habbo.game.room.wired

import ovh.rwx.habbo.game.room.Room
import java.util.concurrent.ConcurrentLinkedQueue

class WiredPerformanceMonitor(private val room: Room) {
    private val executionHistory = ConcurrentLinkedQueue<WiredExecution>()

    companion object {
        const val EXECUTION_COST_CAP = 1000.0
        const val MAX_EXECUTIONS_PER_SECOND = 100
        const val HEAVY_THRESHOLD = 0.8
        const val HISTORY_WINDOW_MS = 60_000L // 1 minuto
    }

    fun recordExecution(cost: Double, durationMs: Long) {
        val now = System.currentTimeMillis()
        executionHistory.offer(WiredExecution(cost, durationMs, now))
        cleanOldHistory(now)
    }

    private fun cleanOldHistory(now: Long) {
        while (true) {
            val head = executionHistory.peek() ?: break
            if (now - head.timestamp > HISTORY_WINDOW_MS) {
                executionHistory.poll()
            } else {
                break
            }
        }
    }

    fun getExecutionCost(): Double {
        cleanOldHistory(System.currentTimeMillis())
        return executionHistory.sumOf { it.cost }
    }

    fun getExecutionCostCap(): Double = EXECUTION_COST_CAP

    fun isHeavy(): Boolean {
        cleanOldHistory(System.currentTimeMillis())
        val currentCost = getExecutionCost()
        val executionsLastSecond = getExecutionsLastSecond()
        return currentCost > (EXECUTION_COST_CAP * HEAVY_THRESHOLD) ||
                executionsLastSecond > MAX_EXECUTIONS_PER_SECOND
    }

    fun getExecutionsLastSecond(): Int {
        val oneSecondAgo = System.currentTimeMillis() - 1000L
        return executionHistory.count { it.timestamp >= oneSecondAgo }
    }
}
