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
 * Amostra de métricas de uma janela (fim da janela em [timestamp]).
 * -1 em CPU/pacotes = indisponível.
 */
data class Sample(
    val timestamp: Long,
    val processCpu: Double,
    val systemCpu: Double,
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long,
    val nonHeapUsedBytes: Long,
    val threads: Int,
    val gcCount: Long,
    val gcTimeMs: Long,
    val usersOnline: Int,
    val roomsLoaded: Int,
    val roomTicks: Long,
    val roomTickAvgMs: Double,
    val roomTickP95Ms: Double,
    val roomTickMaxMs: Double,
    val wiredExecutions: Long,
    val wiredAvgMs: Double,
    val wiredP95Ms: Double,
    val wiredMaxMs: Double,
    val pings: Long,
    val pingAvgMs: Double,
    val pingP95Ms: Double,
    val packetsIn: Long,
    val packetsOut: Long,

    // ---- Tick separado por cadência (os campos roomTick* acima misturam as duas) ----
    // Minor (a cada 50 ms): gatilhos periódicos do wired. Orçamento: 50 ms. Um minor lento atrasa o wired.
    val roomTickMinorCount: Long = 0,
    val roomTickMinorAvgMs: Double = 0.0,
    val roomTickMinorP95Ms: Double = 0.0,
    val roomTickMinorP99Ms: Double = 0.0,
    val roomTickMinorMaxMs: Double = 0.0,
    /** Ticks minor da janela que passaram de 50 ms. */
    val roomTickMinorOverruns: Long = 0,

    // Major (a cada 500 ms): física, animação, entidades, itens. Orçamento: 500 ms (um ciclo inteiro).
    val roomTickMajorCount: Long = 0,
    val roomTickMajorAvgMs: Double = 0.0,
    val roomTickMajorP95Ms: Double = 0.0,
    val roomTickMajorP99Ms: Double = 0.0,
    val roomTickMajorMaxMs: Double = 0.0,
    /** Ticks major da janela que passaram de 500 ms. */
    val roomTickMajorOverruns: Long = 0,

    // Cadência: período real entre dois major ticks da mesma sala (esperado 500 ms). Cresce quando um tick
    // demora e também quando a thread demora a ser agendada, então enxerga o que o tempo de processamento não vê.
    val majorIntervalAvgMs: Double = 0.0,
    val majorIntervalP95Ms: Double = 0.0,
    val majorIntervalMaxMs: Double = 0.0,

    // Atraso de agendamento: quanto o loop da sala acordou depois do que pediu ao delay (pool ocupado, GC, safepoint).
    val schedLagAvgMs: Double = 0.0,
    val schedLagP95Ms: Double = 0.0,
    val schedLagMaxMs: Double = 0.0,

    // ---- Handlers dos pacotes que chegam do cliente ----
    // Tempo que cada handler leva rodando (a maioria é dominada por idas ao banco). Orçamento: 500 ms.
    val handlerCount: Long = 0,
    val handlerAvgMs: Double = 0.0,
    val handlerP95Ms: Double = 0.0,
    val handlerMaxMs: Double = 0.0,
    /** Handlers da janela que passaram de 500 ms. */
    val handlerOverruns: Long = 0,

    // Espera entre o pacote nascer no decoder e o handler começar: fila do dispatcher, GC, CPU saturada.
    // O tempo do handler não enxerga isso.
    val handlerQueueWaitAvgMs: Double = 0.0,
    val handlerQueueWaitP95Ms: Double = 0.0,
    val handlerQueueWaitMaxMs: Double = 0.0,

    // ---- Banco de dados ----
    // Latência de cada db { } / dbTransaction { }, incluindo a espera por uma conexão do pool. Orçamento: 250 ms.
    val dbQueries: Long = 0,
    val dbAvgMs: Double = 0.0,
    val dbP95Ms: Double = 0.0,
    val dbMaxMs: Double = 0.0,
    /** Chamadas da janela que passaram de 250 ms. */
    val dbOverruns: Long = 0,

    // Pool de conexões (HikariCP) no instante da amostra; -1 = indisponível.
    val hikariActive: Int = -1,
    val hikariIdle: Int = -1,
    val hikariTotal: Int = -1,
    /** Threads esperando uma conexão: diferente de zero = o pool está esgotado. */
    val hikariWaiting: Int = -1,
    val hikariMax: Int = -1,
)

/** Estatística de tick de um quarto (numa janela de 1 s, ou agregada num recorte de 5 min / 15 min / 1 h). */
data class RoomTickStat(
    val roomId: Int,
    val name: String,
    val users: Int,
    val tickAvgMs: Double,
    val tickP95Ms: Double,
    val tickMaxMs: Double,
    /** Quantos ticks entraram nesta estatística (pesa a média quando os recortes são juntados). */
    val ticks: Long = 0,
)

/** Estatística de um handler (um tipo de pacote) na última janela. */
data class TopHandlerStat(
    val name: String,
    val calls: Long,
    val avgMs: Double,
    val p95Ms: Double,
    val maxMs: Double,
)

/**
 * Quartos e handlers mais lentos que o amostrador entrega junto de cada amostra: os da última janela de 1 s e os dos
 * recortes (chaves de [ServerMetrics.RANGES]: "5m", "15m", "1h"), com resolução de 1 minuto.
 */
data class TopStats(
    val rooms: List<RoomTickStat> = emptyList(),
    val handlers: List<TopHandlerStat> = emptyList(),
    val roomsByRange: Map<String, List<RoomTickStat>> = emptyMap(),
    val handlersByRange: Map<String, List<TopHandlerStat>> = emptyMap(),
)

/** Instantâneo servido pela API admin. */
data class MetricsSnapshot(
    val sampleIntervalMs: Long,
    val current: Sample?,
    val history: List<Sample>,
    val topRooms: List<RoomTickStat>,
    val topHandlers: List<TopHandlerStat> = emptyList(),
    val topRoomsByRange: Map<String, List<RoomTickStat>> = emptyMap(),
    val topHandlersByRange: Map<String, List<TopHandlerStat>> = emptyMap(),
)
