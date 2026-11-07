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
 * Resume, por minuto e durante 1 h, os handlers e os quartos mais lentos, para responder "quem foi o pior nos últimos
 * 5 min / 15 min / 1 h" sem depender de a tela estar aberta. O amostrador entrega a estatística de cada janela de 1 s
 * e este agregador a junta no balde do minuto.
 *
 * Como cada janela de 1 s já vem resumida, não há como recalcular um percentil do período: o p95 de um recorte é o
 * PIOR p95 de janela visto nele ("o pior que aconteceu"). Já a média é ponderada pelo número de chamadas/ticks, e o
 * máximo é o máximo de verdade. A resolução é de 1 minuto: um recorte de 5 min usa os baldes dos últimos 5 minutos
 * fechados mais o minuto em curso.
 *
 * Cada balde guarda só os [maxPerBucket] piores de cada tipo, para a memória não crescer com o número de handlers.
 */
class TopStatsAggregator(private val maxPerBucket: Int = 30, private val keepMinutes: Int = 60) {
    private class HandlerAgg(var calls: Long = 0, var sumMs: Double = 0.0, var p95Ms: Double = 0.0, var maxMs: Double = 0.0)

    private class RoomAgg(
        var name: String,
        var users: Int,
        var ticks: Long = 0,
        var sumMs: Double = 0.0,
        var p95Ms: Double = 0.0,
        var maxMs: Double = 0.0,
    )

    private class Bucket(val minute: Long) {
        val handlers = HashMap<String, HandlerAgg>()
        val rooms = HashMap<Int, RoomAgg>()
    }

    private val buckets = ArrayDeque<Bucket>()

    @Synchronized
    fun addHandlers(nowMs: Long, stats: List<TopHandlerStat>) {
        if (stats.isEmpty()) return
        val bucket = bucketFor(nowMs)
        for (s in stats) {
            val a = bucket.handlers.getOrPut(s.name) { HandlerAgg() }
            a.calls += s.calls
            a.sumMs += s.avgMs * s.calls
            a.p95Ms = maxOf(a.p95Ms, s.p95Ms)
            a.maxMs = maxOf(a.maxMs, s.maxMs)
        }
        if (bucket.handlers.size > maxPerBucket * 2) trim(bucket.handlers) { it.p95Ms }
    }

    @Synchronized
    fun addRooms(nowMs: Long, stats: List<RoomTickStat>) {
        if (stats.isEmpty()) return
        val bucket = bucketFor(nowMs)
        for (s in stats) {
            val weight = maxOf(1L, s.ticks)
            val a = bucket.rooms.getOrPut(s.roomId) { RoomAgg(s.name, s.users) }
            a.name = s.name
            a.users = s.users
            a.ticks += weight
            a.sumMs += s.tickAvgMs * weight
            a.p95Ms = maxOf(a.p95Ms, s.tickP95Ms)
            a.maxMs = maxOf(a.maxMs, s.tickMaxMs)
        }
        if (bucket.rooms.size > maxPerBucket * 2) trim(bucket.rooms) { it.p95Ms }
    }

    /** Os [limit] handlers de pior p95 nos últimos [rangeMs], juntando os baldes do recorte. */
    @Synchronized
    fun handlers(nowMs: Long, rangeMs: Long, limit: Int): List<TopHandlerStat> {
        val total = HashMap<String, HandlerAgg>()
        for (b in inRange(nowMs, rangeMs)) {
            for ((name, h) in b.handlers) {
                val a = total.getOrPut(name) { HandlerAgg() }
                a.calls += h.calls
                a.sumMs += h.sumMs
                a.p95Ms = maxOf(a.p95Ms, h.p95Ms)
                a.maxMs = maxOf(a.maxMs, h.maxMs)
            }
        }
        return total.map { (name, a) -> TopHandlerStat(name, a.calls, if (a.calls > 0) a.sumMs / a.calls else 0.0, a.p95Ms, a.maxMs) }
            .sortedWith(compareByDescending<TopHandlerStat> { it.p95Ms }.thenByDescending { it.maxMs })
            .take(limit)
    }

    /** Os [limit] quartos de pior p95 de tick nos últimos [rangeMs]. */
    @Synchronized
    fun rooms(nowMs: Long, rangeMs: Long, limit: Int): List<RoomTickStat> {
        val total = HashMap<Int, RoomAgg>()
        for (b in inRange(nowMs, rangeMs)) {
            for ((id, r) in b.rooms) {
                val a = total.getOrPut(id) { RoomAgg(r.name, r.users) }
                a.name = r.name
                a.users = r.users
                a.ticks += r.ticks
                a.sumMs += r.sumMs
                a.p95Ms = maxOf(a.p95Ms, r.p95Ms)
                a.maxMs = maxOf(a.maxMs, r.maxMs)
            }
        }
        return total.map { (id, a) ->
            RoomTickStat(id, a.name, a.users, if (a.ticks > 0) a.sumMs / a.ticks else 0.0, a.p95Ms, a.maxMs, a.ticks)
        }
            .sortedWith(compareByDescending<RoomTickStat> { it.tickP95Ms }.thenByDescending { it.tickMaxMs })
            .take(limit)
    }

    private fun inRange(nowMs: Long, rangeMs: Long): List<Bucket> {
        val oldestMinute = (nowMs - rangeMs) / MINUTE_MS
        return buckets.filter { it.minute >= oldestMinute }
    }

    private fun bucketFor(nowMs: Long): Bucket {
        val minute = nowMs / MINUTE_MS
        val last = buckets.lastOrNull()
        // relógio que andou para trás: continua no balde mais recente em vez de criar um fora de ordem
        if (last != null && minute <= last.minute) return last

        val bucket = Bucket(minute)
        buckets.addLast(bucket)
        while (buckets.isNotEmpty() && buckets.first().minute < minute - keepMinutes) buckets.removeFirst()
        return bucket
    }

    private fun <K, V> trim(map: HashMap<K, V>, p95: (V) -> Double) {
        val keep = map.entries.sortedByDescending { p95(it.value) }.take(maxPerBucket).map { it.key }.toHashSet()
        map.keys.retainAll(keep)
    }

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}
