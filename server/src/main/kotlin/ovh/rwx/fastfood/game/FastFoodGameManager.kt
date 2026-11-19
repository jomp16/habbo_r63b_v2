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

package ovh.rwx.fastfood.game

import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Uma arena por vez (BaseJump original: 3 jogadores por partida).
 * Humanos entram na hora; bots completam os 3 lugares.
 */
object FastFoodGameManager {
    const val PLAYERS_PER_GAME = 3
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val waiters = CopyOnWriteArrayList<FastFoodSession>()

    @Volatile
    private var game: FastFoodGame? = null

    fun joinGame(session: FastFoodSession) = run {
        val current = game
        if (current != null && !current.ended.get()) {
            // já tem partida rolando: espera a próxima
            if (!waiters.contains(session)) waiters.add(session)
            return@run
        }

        startGame(listOf(session))
    }

    fun playerAction(session: FastFoodSession, action: (FastFoodGame, FastFoodGamePlayer) -> Unit) = run {
        val g = game ?: return@run
        val player = g.players.find { it.session == session } ?: return@run

        action(g, player)
    }

    fun leave(session: FastFoodSession) = run {
        waiters.remove(session)
        game?.removePlayer(session)
    }

    private fun startGame(humans: List<FastFoodSession>) {
        val newGame = FastFoodGame(humans, this::gameEnded)
        game = newGame
        newGame.start()
    }

    private fun gameEnded(endedGame: FastFoodGame) {
        if (game === endedGame) game = null

        // próximos na fila viram a próxima partida
        val next = waiters.toList()
        if (next.isNotEmpty()) {
            waiters.clear()
            startGame(next)
        }
    }

    // Tudo o que mexe numa partida (ações das sessões Netty, o tick, os bots) roda nesta thread única: o estado
    // da partida não tem sincronização. Uma exceção é registrada e não derruba o executor (nem o tick periódico).
    fun run(task: () -> Unit) {
        scheduler.execute(guarded(task))
    }

    fun schedule(delayMillis: Long, task: () -> Unit) {
        scheduler.schedule(guarded(task), delayMillis, TimeUnit.MILLISECONDS)
    }

    fun scheduleAtFixedRate(periodMillis: Long, task: () -> Unit): ScheduledFuture<*> =
        scheduler.scheduleAtFixedRate(guarded(task), periodMillis, periodMillis, TimeUnit.MILLISECONDS)

    private fun guarded(task: () -> Unit) = Runnable {
        try {
            task()
        } catch (e: Exception) {
            log.error("FastFood: erro na partida", e)
        }
    }

    fun broadcast(outgoing: FFOutgoing, vararg args: Any?) {
        game?.broadcast(outgoing, *args)
    }
}
