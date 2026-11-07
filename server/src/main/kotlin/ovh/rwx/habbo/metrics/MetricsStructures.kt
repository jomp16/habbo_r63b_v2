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

/**
 * Estatísticas de uma janela de medição.
 */
data class WindowStats(
    val count: Long,
    val avgNanos: Double,
    val p95Nanos: Double,
    val maxNanos: Double,
    /** Percentil 99 da janela (mesma convenção do p95: índice floor(n × 0,99)). */
    val p99Nanos: Double = 0.0,
    /** Quantas amostras da janela passaram do orçamento do [Reservoir] (0 se ele não tem orçamento). */
    val overBudget: Long = 0,
) {
    val avgMs: Double get() = avgNanos / 1_000_000.0
    val p95Ms: Double get() = p95Nanos / 1_000_000.0
    val p99Ms: Double get() = p99Nanos / 1_000_000.0
    val maxMs: Double get() = maxNanos / 1_000_000.0

    companion object {
        val EMPTY = WindowStats(0, 0.0, 0.0, 0.0)
    }
}

/**
 * Reservatório limitado e thread-safe: guarda no máximo [capacity] amostras recentes (anel),
 * mas soma/contagem/máximo/estouros valem para a janela inteira. Sem alocação no caminho quente.
 *
 * [budgetNanos] é o orçamento da métrica (ex.: 50 ms para o tick de wired); toda amostra acima dele conta como estouro
 * na janela. É uma contagem exata, não depende do tamanho do anel.
 */
class Reservoir(private val capacity: Int, private val budgetNanos: Long = Long.MAX_VALUE) {
    private val lock = Any()
    private var values = LongArray(capacity)
    private var count = 0L
    private var sum = 0L
    private var max = 0L
    private var overBudget = 0L

    fun add(nanos: Long) {
        synchronized(lock) {
            values[(count % capacity).toInt()] = nanos
            count++
            sum += nanos
            if (nanos > max) max = nanos
            if (nanos > budgetNanos) overBudget++
        }
    }

    /** Calcula avg/p95/max da janela e zera o reservatório. */
    fun drain(): WindowStats {
        val copy: LongArray
        val n: Long
        val total: Long
        val mx: Long
        val over: Long
        synchronized(lock) {
            if (count == 0L) return WindowStats.EMPTY
            n = count
            total = sum
            mx = max
            over = overBudget
            val size = minOf(n, capacity.toLong()).toInt()
            copy = values.copyOf(size)
            count = 0; sum = 0; max = 0; overBudget = 0
        }
        copy.sort()
        val p95 = copy[((copy.size * 0.95).toInt()).coerceIn(0, copy.size - 1)]
        val p99 = copy[((copy.size * 0.99).toInt()).coerceIn(0, copy.size - 1)]
        return WindowStats(n, total.toDouble() / n, p95.toDouble(), mx.toDouble(), p99.toDouble(), over)
    }
}

/**
 * Anel de tamanho fixo (mais antigo -> mais novo no snapshot), thread-safe.
 */
class RingBuffer<T : Any>(private val capacity: Int) {
    private val items = arrayOfNulls<Any>(capacity)
    private var next = 0
    private var size = 0

    @Synchronized
    fun add(item: T) {
        items[next] = item
        next = (next + 1) % capacity
        if (size < capacity) size++
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun snapshot(): List<T> {
        val out = ArrayList<T>(size)
        val start = if (size < capacity) 0 else next
        for (i in 0 until size) out.add(items[(start + i) % capacity] as T)
        return out
    }
}
