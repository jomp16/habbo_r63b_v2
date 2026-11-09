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

import com.sun.management.OperatingSystemMXBean
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.atomic.AtomicReference

/**
 * Métricas do servidor: os recorders são baratos e thread-safe (chamados dos ticks das salas,
 * do wired e do netty); um sampler daemon fecha uma janela a cada 5 s e guarda a [Sample]
 * num anel de 720 posições (1 h).
 */
object ServerMetrics {
    const val DEFAULT_SAMPLE_INTERVAL_MS = 1_000L
    private const val HISTORY_WINDOW_MS = 3_600_000L
    const val TOP_ROOMS = 10
    const val TOP_HANDLERS = 10

    /** Quantos handlers a tabela de um recorte (5 min / 15 min / 1 h) mostra. */
    const val TOP_HANDLERS_RANGE = 15

    /** Recortes das tabelas "mais lentos", com a chave que a API e a tela usam. A resolução é de 1 minuto. */
    val RANGES: Map<String, Long> = linkedMapOf("5m" to 5 * 60_000L, "15m" to 15 * 60_000L, "1h" to 60 * 60_000L)

    private val log = LoggerFactory.getLogger(javaClass)

    /** Relógio do amostrador; os testes o trocam para andar minutos sem esperar. */
    @Volatile
    internal var clock: () -> Long = System::currentTimeMillis

    // resumo por minuto (1 h) dos handlers e quartos mais lentos, para as tabelas obedecerem o seletor de período
    private val topAggregator = TopStatsAggregator()

    private val osBean = ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean
    private val memBean = ManagementFactory.getMemoryMXBean()
    private val threadBean = ManagementFactory.getThreadMXBean()
    private val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()

    /** Orçamento do tick minor (a cada 50 ms): é o que mantém os gatilhos periódicos do wired no compasso. */
    const val MINOR_TICK_BUDGET_NANOS = 50_000_000L

    /** Orçamento do tick major (a cada 500 ms): física e animação; passar disso pula um ciclo inteiro. */
    const val MAJOR_TICK_BUDGET_NANOS = 500_000_000L

    /** Orçamento de um handler: a meta do hotel é responder a um pacote em menos de 500 ms. */
    const val HANDLER_BUDGET_NANOS = 500_000_000L

    /** Orçamento de uma chamada ao banco (db { } ou dbTransaction { }), já contando a espera por uma conexão do pool. */
    const val DB_QUERY_BUDGET_NANOS = 250_000_000L

    // todos os ticks juntos (compatibilidade com o que o CMS já mostrava) + as duas cadências separadas
    private val roomTicks = Reservoir(4096)
    private val roomTicksMinor = Reservoir(4096, MINOR_TICK_BUDGET_NANOS)
    private val roomTicksMajor = Reservoir(1024, MAJOR_TICK_BUDGET_NANOS)
    private val majorInterval = Reservoir(1024)
    private val schedLag = Reservoir(4096)
    private val wired = Reservoir(4096)
    private val ping = Reservoir(1024)
    private val handlerTime = Reservoir(4096, HANDLER_BUDGET_NANOS)
    private val handlerQueueWait = Reservoir(4096)
    private val dbQuery = Reservoir(4096, DB_QUERY_BUDGET_NANOS)
    private val perRoom = ConcurrentHashMap<Int, Reservoir>()

    // por tipo de pacote, pelo nome semântico do enum (SSO_TICKET): o id do cabeçalho muda a cada release.
    // O mapa é limitado pelo número de handlers (centenas); cada reservatório é pequeno.
    private val perHandler = ConcurrentHashMap<String, Reservoir>()
    // LongAdder (e não AtomicLong): incrementado a CADA pacote por dezenas de event loops; o CAS de um AtomicLong
    // vira disputa de cache line sob carga. sumThenReset() mantém a semântica de "ler e zerar" do sampler.
    private val packetsIn = LongAdder()
    private val packetsOut = LongAdder()

    @Volatile
    var sampleIntervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS
        private set

    // capacidade = 1 h de amostras no intervalo configurado
    @Volatile
    private var history = RingBuffer<Sample>((HISTORY_WINDOW_MS / DEFAULT_SAMPLE_INTERVAL_MS).toInt())
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(Sample, TopStats) -> Unit>()
    private val topStats = AtomicReference(TopStats())
    @Volatile
    private var executor: ScheduledExecutorService? = null

    private var lastGcCount = 0L
    private var lastGcTimeMs = 0L
    private var lastCpuSampleTime = 0L
    private var cachedProcessCpu = -1.0
    private var cachedSystemCpu = -1.0

    /**
     * Registra o tempo de PROCESSAMENTO de um tick. [isMajor] diz a cadência: o major (a cada 500 ms) faz física,
     * animação, entidades e itens; o minor (a cada 50 ms) só roda os gatilhos periódicos do wired. Os dois têm
     * orçamentos diferentes, então são medidos em separado além do total.
     */
    fun recordRoomTick(roomId: Int, elapsedNanos: Long, isMajor: Boolean) =
        recordRoomTick(roomTickReservoir(roomId), elapsedNanos, isMajor)

    /**
     * Reservatório da sala. Quem faz tick a cada 50 ms guarda a referência e usa a sobrecarga abaixo: buscar no
     * mapa por roomId boxeia o Int (ids de sala passam de 127) e alocava a cada tick de cada sala.
     */
    fun roomTickReservoir(roomId: Int): Reservoir = perRoom.computeIfAbsent(roomId) { Reservoir(256) }

    fun recordRoomTick(roomReservoir: Reservoir, elapsedNanos: Long, isMajor: Boolean) {
        roomTicks.add(elapsedNanos)
        (if (isMajor) roomTicksMajor else roomTicksMinor).add(elapsedNanos)
        roomReservoir.add(elapsedNanos)
    }

    /** Período real entre dois major ticks consecutivos da mesma sala (esperado 500 ms). */
    fun recordMajorInterval(intervalNanos: Long) = majorInterval.add(intervalNanos)

    /** Quanto o loop de uma sala acordou depois do tempo que pediu ao delay (0 = pontual). */
    fun recordSchedulingLag(lagNanos: Long) = schedLag.add(lagNanos)

    fun recordWired(elapsedNanos: Long) = wired.add(elapsedNanos)

    fun recordPing(rttNanos: Long) = ping.add(rttNanos)

    /** Tempo que um handler levou rodando. [name] é o nome do enum do pacote (nunca o id do cabeçalho). */
    fun recordHandler(name: String, elapsedNanos: Long) {
        handlerTime.add(elapsedNanos)
        perHandler.computeIfAbsent(name) { Reservoir(64) }.add(elapsedNanos)
    }

    /** Espera entre o pedido nascer no decoder e o handler começar a rodar. */
    fun recordHandlerQueueWait(waitNanos: Long) = handlerQueueWait.add(waitNanos)

    /** Latência de uma chamada ao banco, contando a espera por uma conexão do pool. */
    fun recordDbQuery(elapsedNanos: Long) = dbQuery.add(elapsedNanos)

    fun recordPacketIn() {
        packetsIn.increment()
    }

    fun recordPacketOut() {
        packetsOut.increment()
    }

    /** Registra um ouvinte chamado pela thread do sampler a cada nova amostra (não pode bloquear). */
    fun addListener(l: (Sample, TopStats) -> Unit) { listeners.add(l) }

    fun removeListener(l: (Sample, TopStats) -> Unit) { listeners.remove(l) }

    @Synchronized
    fun start(intervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS) {
        if (executor != null) return

        sampleIntervalMs = intervalMs.coerceIn(100L, 60_000L)
        history = RingBuffer((HISTORY_WINDOW_MS / sampleIntervalMs).toInt().coerceAtLeast(1))

        executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "server-metrics-sampler").apply { isDaemon = true }
        }.also {
            it.scheduleAtFixedRate({
                try {
                    sampleNow()
                } catch (e: Throwable) {
                    log.warn("Falha ao coletar métricas", e)
                }
            }, sampleIntervalMs, sampleIntervalMs, TimeUnit.MILLISECONDS)
        }
    }

    @Synchronized
    fun stop() {
        executor?.shutdownNow()
        executor = null
    }

    fun snapshot(sinceMs: Long? = null): MetricsSnapshot {
        val all = history.snapshot()
        val top = topStats.get()
        return MetricsSnapshot(
            sampleIntervalMs,
            all.lastOrNull(),
            if (sinceMs == null) all else all.filter { it.timestamp > sinceMs },
            top.rooms,
            top.handlers,
            top.roomsByRange,
            top.handlersByRange,
        )
    }

    /** Pool de conexões no instante da amostra: (ativas, ociosas, total, esperando, máximo); -1 se indisponível. */
    private fun hikariGauges(): IntArray = try {
        val ds = HabboServer.hikariDataSource
        val pool = ds.hikariPoolMXBean
        if (pool == null) intArrayOf(-1, -1, -1, -1, -1)
        else intArrayOf(pool.activeConnections, pool.idleConnections, pool.totalConnections, pool.threadsAwaitingConnection, ds.maximumPoolSize)
    } catch (e: Throwable) {
        intArrayOf(-1, -1, -1, -1, -1)
    }

    /** Fecha a janela atual. Chamado só pelo sampler (e pelos testes). */
    @Synchronized
    internal fun sampleNow(): Sample {
        val now = clock()
        val heap = memBean.heapMemoryUsage

        var gcCount = 0L
        var gcTime = 0L
        for (gc in gcBeans) {
            if (gc.collectionCount > 0) gcCount += gc.collectionCount
            if (gc.collectionTime > 0) gcTime += gc.collectionTime
        }
        val gcCountDelta = (gcCount - lastGcCount).coerceAtLeast(0)
        val gcTimeDelta = (gcTime - lastGcTimeMs).coerceAtLeast(0)
        lastGcCount = gcCount
        lastGcTimeMs = gcTime

        val rt = roomTicks.drain()
        val rtMinor = roomTicksMinor.drain()
        val rtMajor = roomTicksMajor.drain()
        val interval = majorInterval.drain()
        val lag = schedLag.drain()
        val w = wired.drain()
        val p = ping.drain()
        val hikari = hikariGauges()
        val (users, rooms) = try {
            HabboServer.habboSessionManager.sessionsCount to HabboServer.habboGame.roomManager.roomTaskManager.rooms.size
        } catch (e: Throwable) {
            0 to 0
        }

        val handlers = handlerTime.drain()
        val queueWait = handlerQueueWait.drain()
        val db = dbQuery.drain()

        // Amostra de CPU a cada no mínimo 2.5s para evitar I/O repetitivo em cgroups no Linux
        if (now - lastCpuSampleTime >= 2_500L) {
            lastCpuSampleTime = now
            cachedProcessCpu = osBean?.processCpuLoad?.takeIf { it >= 0 } ?: -1.0
            cachedSystemCpu = osBean?.cpuLoad?.takeIf { it >= 0 } ?: -1.0
        }

        val sample = Sample(
            timestamp = now,
            processCpu = cachedProcessCpu,
            systemCpu = cachedSystemCpu,
            heapUsedBytes = heap.used,
            heapCommittedBytes = heap.committed,
            heapMaxBytes = heap.max,
            nonHeapUsedBytes = memBean.nonHeapMemoryUsage.used,
            threads = threadBean.threadCount,
            gcCount = gcCountDelta,
            gcTimeMs = gcTimeDelta,
            usersOnline = users,
            roomsLoaded = rooms,
            roomTicks = rt.count,
            roomTickAvgMs = rt.avgMs,
            roomTickP95Ms = rt.p95Ms,
            roomTickMaxMs = rt.maxMs,
            wiredExecutions = w.count,
            wiredAvgMs = w.avgMs,
            wiredP95Ms = w.p95Ms,
            wiredMaxMs = w.maxMs,
            pings = p.count,
            pingAvgMs = p.avgMs,
            pingP95Ms = p.p95Ms,
            packetsIn = packetsIn.sumThenReset(),
            packetsOut = packetsOut.sumThenReset(),
            roomTickMinorCount = rtMinor.count,
            roomTickMinorAvgMs = rtMinor.avgMs,
            roomTickMinorP95Ms = rtMinor.p95Ms,
            roomTickMinorP99Ms = rtMinor.p99Ms,
            roomTickMinorMaxMs = rtMinor.maxMs,
            roomTickMinorOverruns = rtMinor.overBudget,
            roomTickMajorCount = rtMajor.count,
            roomTickMajorAvgMs = rtMajor.avgMs,
            roomTickMajorP95Ms = rtMajor.p95Ms,
            roomTickMajorP99Ms = rtMajor.p99Ms,
            roomTickMajorMaxMs = rtMajor.maxMs,
            roomTickMajorOverruns = rtMajor.overBudget,
            majorIntervalAvgMs = interval.avgMs,
            majorIntervalP95Ms = interval.p95Ms,
            majorIntervalMaxMs = interval.maxMs,
            schedLagAvgMs = lag.avgMs,
            schedLagP95Ms = lag.p95Ms,
            schedLagMaxMs = lag.maxMs,
            handlerCount = handlers.count,
            handlerAvgMs = handlers.avgMs,
            handlerP95Ms = handlers.p95Ms,
            handlerMaxMs = handlers.maxMs,
            handlerOverruns = handlers.overBudget,
            handlerQueueWaitAvgMs = queueWait.avgMs,
            handlerQueueWaitP95Ms = queueWait.p95Ms,
            handlerQueueWaitMaxMs = queueWait.maxMs,
            dbQueries = db.count,
            dbAvgMs = db.avgMs,
            dbP95Ms = db.p95Ms,
            dbMaxMs = db.maxMs,
            dbOverruns = db.overBudget,
            hikariActive = hikari[0],
            hikariIdle = hikari[1],
            hikariTotal = hikari[2],
            hikariWaiting = hikari[3],
            hikariMax = hikari[4],
        )
        history.add(sample)

        // os reservatórios por handler e por quarto precisam ser esvaziados a cada janela, com ou sem ouvintes
        val handlerStats = collectHandlerStats()
        val roomStats = collectRoomStats()

        // junta a janela no resumo por minuto: é ele que responde "os piores dos últimos 5 min / 15 min / 1 h"
        topAggregator.addHandlers(now, handlerStats)
        topAggregator.addRooms(now, roomStats)

        val top = TopStats(
            rooms = roomStats.sortedByDescending { it.tickP95Ms }.take(TOP_ROOMS),
            handlers = handlerStats
                .sortedWith(compareByDescending<TopHandlerStat> { it.p95Ms }.thenByDescending { it.maxMs })
                .take(TOP_HANDLERS),
            roomsByRange = RANGES.mapValues { (_, rangeMs) -> topAggregator.rooms(now, rangeMs, TOP_ROOMS) },
            handlersByRange = RANGES.mapValues { (_, rangeMs) -> topAggregator.handlers(now, rangeMs, TOP_HANDLERS_RANGE) },
        )
        topStats.set(top)

        for (l in listeners) {
            try {
                l(sample, top)
            } catch (e: Throwable) {
                log.debug("Ouvinte de métricas falhou", e)
            }
        }

        return sample
    }

    /** Esvazia e devolve a estatística da janela de TODOS os handlers que tiveram pacote (pelo nome semântico do pacote). */
    private fun collectHandlerStats(): List<TopHandlerStat> {
        if (perHandler.isEmpty()) return emptyList()

        val stats = ArrayList<TopHandlerStat>()
        for ((name, reservoir) in perHandler) {
            val s = reservoir.drain()
            if (s.count == 0L) continue
            stats += TopHandlerStat(name, s.count, s.avgMs, s.p95Ms, s.maxMs)
        }
        return stats
    }

    /** Esvazia e devolve a estatística da janela de TODOS os quartos carregados que tiveram tick. */
    private fun collectRoomStats(): List<RoomTickStat> {
        if (perRoom.isEmpty()) return emptyList()

        val loaded = try {
            HabboServer.habboGame.roomManager.roomTaskManager.rooms.associateBy { it.roomData.id }
        } catch (e: Throwable) {
            emptyMap()
        }

        val stats = ArrayList<RoomTickStat>()
        val iterator = perRoom.entries.iterator()
        while (iterator.hasNext()) {
            val (id, reservoir) = iterator.next()
            val s = reservoir.drain()
            val room = loaded[id]
            if (room == null) {
                // sala descarregada: não há mais o que reportar
                iterator.remove()
                continue
            }
            if (s.count == 0L) continue
            stats += RoomTickStat(
                id,
                room.roomData.name,
                room.userManager.entities.values.count { it is ovh.rwx.habbo.game.room.user.RoomUser },
                s.avgMs, s.p95Ms, s.maxMs,
                s.count,
            )
        }
        return stats
    }
}
