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

package ovh.rwx.habbo.game.room

import org.slf4j.LoggerFactory

/**
 * Telemetria de tick da sala: ring buffers de latência de processamento e de intervalo
 * de major ticks, com resumo p95 a cada ~10s no logger `ovh.rwx.habbo.stress`.
 * Interface de um método; no-op quando o stress test está desligado.
 */
class RoomTickMonitor(private val roomId: Int, private val enabled: Boolean) {
    private val stressLog = LoggerFactory.getLogger("ovh.rwx.habbo.stress")

    private val tickProcessingBuffer = LongArray(if (enabled) 200 else 0)
    private val majorTickIntervalBuffer = LongArray(if (enabled) 20 else 0)
    private var stressTickIdx = 0
    private var stressMajorIdx = 0
    private var stressTickCount = 0
    private var stressMajorCount = 0
    private var stressLogCounter = 0
    private var lastMajorTickNano = 0L

    fun onTick(elapsedMs: Long, isMajorTick: Boolean) {
        if (!enabled) return

        tickProcessingBuffer[stressTickIdx] = elapsedMs
        stressTickIdx = (stressTickIdx + 1) % tickProcessingBuffer.size
        stressTickCount++
        stressLogCounter++

        if (isMajorTick) {
            val nowNano = System.nanoTime()
            if (lastMajorTickNano > 0) {
                val interval = (nowNano - lastMajorTickNano) / 1_000_000
                majorTickIntervalBuffer[stressMajorIdx] = interval
                stressMajorIdx = (stressMajorIdx + 1) % majorTickIntervalBuffer.size
                stressMajorCount++
            }
            lastMajorTickNano = nowNano
        }

        // Log rolling summary every ~10 seconds (200 ticks * 50ms)
        if (stressLogCounter >= 200) {
            stressLogCounter = 0
            val procCount = minOf(stressTickCount, tickProcessingBuffer.size)
            val (procAvg, procP95, procMax) = computeStats(tickProcessingBuffer, procCount)

            val majCount = minOf(stressMajorCount, majorTickIntervalBuffer.size)
            var majInfo = "n/a"
            if (majCount >= 2) {
                val (majAvg, majP95, majMax) = computeStats(majorTickIntervalBuffer, majCount)
                val drift = majAvg - 500.0
                majInfo = "avg=%.1fms p95=%dms max=%dms drift=%.1fms".format(majAvg, majP95, majMax, drift)
            }

            val procInfo = "avg=%.1fms p95=%dms max=%dms".format(procAvg, procP95, procMax)

            stressLog.info(
                "[stress-test] room={} ticks={} proc: {} | major-interval: {}",
                roomId, procCount, procInfo, majInfo
            )
        }
    }

    private fun computeStats(buffer: LongArray, count: Int): Triple<Double, Long, Long> {
        val times = LongArray(count)
        for (i in 0 until count) times[i] = buffer[i]

        val sorted = times.sorted()
        return Triple(
            sorted.average(),
            sorted[(count * 0.95).toInt().coerceIn(0, count - 1)],
            sorted.last(),
        )
    }
}
