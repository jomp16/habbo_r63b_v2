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

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.metrics.Sample
import ovh.rwx.habbo.metrics.ServerMetrics
import java.util.Locale

/**
 * Linha periódica do log de stress (logger `ovh.rwx.habbo.stress`, mesmo do tick das salas) com a saúde da
 * persistência: write-behind (fila, espera, execução, ciclos) e o banco (queries, pool Hikari). Só roda com
 * `stress_test: true`. Usa o sampler do [ServerMetrics] (1 s) e fecha uma janela a cada [WINDOW_SECONDS] amostras.
 *
 * Como ler:
 *  - `espera` de ~0 a 2000 ms é NORMAL: o worker só drena a fila a cada 2 s (intervalo do write-behind), então a
 *    escrita fica esperando o próximo ciclo; `espera` acima do intervalo do worker é que indica atraso;
 *  - `pend` que não volta a ~0 entre janelas, ou o pico crescendo a cada janela = escritas chegando mais rápido do
 *    que o banco absorve;
 *  - `ciclos` com avg/max perto de 2000 ms = o ciclo não termina antes do próximo (o worker não dá conta);
 *  - `exec` = custo real de cada escrita (inclui a espera por conexão do pool);
 *  - `hikari esperando-max>0` = handlers esperando conexão (o write-behind disputa o pool com eles).
 */
object WriteBehindStressLog {
    private const val WINDOW_SECONDS = 10
    private val stressLog = LoggerFactory.getLogger("ovh.rwx.habbo.stress")

    // acumuladores da janela do banco (as amostras do ServerMetrics já vêm agregadas por segundo)
    private var samples = 0
    private var dbQueries = 0L
    private var dbWeightedAvgMs = 0.0
    private var dbP95MaxMs = 0.0
    private var dbMaxMs = 0.0
    private var dbOverruns = 0L
    private var hikariActiveMax = 0
    private var hikariWaitingMax = 0
    private var hikariTotal = 0
    private var hikariMax = 0

    @Volatile
    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        ServerMetrics.addListener { sample, _ -> onSample(sample) }
    }

    private fun onSample(s: Sample) {
        samples++
        dbQueries += s.dbQueries
        dbWeightedAvgMs += s.dbAvgMs * s.dbQueries
        if (s.dbP95Ms > dbP95MaxMs) dbP95MaxMs = s.dbP95Ms
        if (s.dbMaxMs > dbMaxMs) dbMaxMs = s.dbMaxMs
        dbOverruns += s.dbOverruns
        if (s.hikariActive > hikariActiveMax) hikariActiveMax = s.hikariActive
        if (s.hikariWaiting > hikariWaitingMax) hikariWaitingMax = s.hikariWaiting
        hikariTotal = s.hikariTotal
        hikariMax = s.hikariMax

        if (samples < WINDOW_SECONDS) return
        report()
        samples = 0
        dbQueries = 0; dbWeightedAvgMs = 0.0; dbP95MaxMs = 0.0; dbMaxMs = 0.0; dbOverruns = 0
        hikariActiveMax = 0; hikariWaitingMax = 0
    }

    private fun report() {
        val w = WriteBehindManager.drainStats()
        val dbAvg = if (dbQueries > 0) dbWeightedAvgMs / dbQueries else 0.0
        val message = String.format(
            Locale.ROOT,
            "[write-behind] %ds | escritas: +%d feitas=%d falhas=%d pend=%d(pico %d) simult.max=%d | " +
                "espera: avg=%.1fms p95=%.0fms max=%.0fms | exec: avg=%.1fms p95=%.0fms max=%.0fms | " +
                "ciclos=%d avg=%.0fms max=%.0fms sujos(pico)=%d flush=%d | " +
                "db: queries=%d avg=%.1fms p95max=%.0fms max=%.0fms >orcamento=%d | hikari ativos-max=%d esperando-max=%d (%d/%d)",
            WINDOW_SECONDS * ServerMetrics.sampleIntervalMs / 1000, w.submitted, w.completed, w.failed, w.pendingNow, w.pendingPeak, w.inFlightPeak,
            w.wait.avgMs, w.wait.p95Ms, w.wait.maxMs, w.exec.avgMs, w.exec.p95Ms, w.exec.maxMs,
            w.cycle.count, w.cycle.avgMs, w.cycle.maxMs, w.dirtyPeak, w.dirtyFlushed,
            dbQueries, dbAvg, dbP95MaxMs, dbMaxMs, dbOverruns,
            hikariActiveMax, hikariWaitingMax, hikariTotal, hikariMax,
        )
        stressLog.info(message)
    }
}
