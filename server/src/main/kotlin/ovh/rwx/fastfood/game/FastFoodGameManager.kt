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

/**
 * Física da comida (FallingObjectType AS3): gravidade (px/s²), decel do paraquedas,
 * velocidade mínima de queda (normal / bigparachute) e ticks do timer do prato (16ms cada).
 */
data class FastFoodFallingObjectType(
    val id: Int,
    val gravity: String,
    val parachuteDecel: String,
    val minFallSpeed: String,
    val bigParachuteMinFallSpeed: String,
    val plateTimerTicks: Int
)

/**
 * Uma arena por vez (BaseJump original: 3 jogadores por partida).
 * Humanos entram na hora; bots completam os 3 lugares.
 */
object FastFoodGameManager {
    const val PLAYERS_PER_GAME = 3

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val waiters = CopyOnWriteArrayList<FastFoodSession>()

    @Volatile
    private var game: FastFoodGame? = null

    fun joinGame(session: FastFoodSession) {
        val current = game
        if (current != null && !current.ended.get()) {
            // já tem partida rolando: espera a próxima
            if (!waiters.contains(session)) waiters.add(session)
            return
        }

        startGame(listOf(session))
    }

    fun playerAction(session: FastFoodSession, action: (FastFoodGame, FastFoodGamePlayer) -> Unit) {
        val g = game ?: return
        val player = g.players.find { it.session == session } ?: return

        action(g, player)
    }

    fun leave(session: FastFoodSession) {
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

    fun schedule(delayMillis: Long, task: () -> Unit) {
        scheduler.schedule(task, delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    fun broadcast(outgoing: FFOutgoing, vararg args: Any?) {
        game?.broadcast(outgoing, *args)
    }
}
