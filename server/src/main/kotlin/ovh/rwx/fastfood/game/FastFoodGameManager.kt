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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * As partidas de Fast Food, várias ao mesmo tempo, de 3 jogadores cada (BaseJump original). Quem entra espera no
 * lobby (FastFoodLobby) até a mesa encher ou a espera vencer; os bots completam os lugares vazios.
 *
 * Uma thread só, daemon, roda o lobby e TODAS as partidas num tick de 16 ms: o tick de uma partida é aritmética de
 * microssegundos (3 pratos), sem I/O, então uma thread aguenta dezenas de partidas, e o estado fica confinado a
 * ela, sem locks. As ações das sessões (event loop do Netty) entram por [run]. Sem ninguém no lobby nem jogando, o
 * tick para.
 */
object FastFoodGameManager {
    const val PLAYERS_PER_GAME = 3
    const val LOBBY_WAIT_MILLIS = 8_000L

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("fastfood-games").daemon().factory())
    private val lobby = FastFoodLobby<FastFoodSession>(PLAYERS_PER_GAME, LOBBY_WAIT_MILLIS / FastFoodPhysics.TICK_MILLIS)
    private val games = mutableListOf<FastFoodGame>()
    private val gameBySession = HashMap<FastFoodSession, FastFoodGame>()
    private var ticker: ScheduledFuture<*>? = null
    private var now = 0L

    fun joinGame(session: FastFoodSession) = run {
        if (session in gameBySession || !lobby.join(session, now)) return@run
        startReadyGames() // a mesa encheu: sai na hora
        if (ticker == null) {
            ticker = scheduler.scheduleAtFixedRate(guarded(::onTick), FastFoodPhysics.TICK_MILLIS, FastFoodPhysics.TICK_MILLIS, TimeUnit.MILLISECONDS)
        }
    }

    fun playerAction(session: FastFoodSession, action: (FastFoodGame, FastFoodGamePlayer) -> Unit) = run {
        val game = gameBySession[session] ?: return@run
        val player = game.playerOf(session) ?: return@run
        action(game, player)
    }

    fun leave(session: FastFoodSession) = run {
        lobby.leave(session)
        gameBySession.remove(session)?.removePlayer(session)
    }

    private fun onTick() {
        now++
        startReadyGames()
        for (game in games.toList()) {
            try {
                game.tick()
            } catch (e: Exception) {
                // uma partida quebrada não derruba as outras nem repete o erro a cada 16 ms
                log.error("FastFood: erro na partida, encerrada", e)
                game.abort()
            }
        }
        if (games.isEmpty() && lobby.isEmpty()) {
            ticker?.cancel(false)
            ticker = null
        }
    }

    private fun startReadyGames() {
        while (true) {
            val table = lobby.takeReady(now) ?: return
            val humans = table.filter { it.authenticated && it.channel.isActive }
            if (humans.isEmpty()) continue
            val game = FastFoodGame(humans, ::gameEnded)
            games += game
            humans.forEach { gameBySession[it] = game }
            game.start()
        }
    }

    private fun gameEnded(game: FastFoodGame) {
        games -= game
        gameBySession.values.removeIf { it === game }
    }

    /** Roda [task] na thread das partidas. Uma exceção é registrada e não derruba a thread. */
    fun run(task: () -> Unit) {
        scheduler.execute(guarded(task))
    }

    private fun guarded(task: () -> Unit) = Runnable {
        try {
            task()
        } catch (e: Exception) {
            log.error("FastFood: erro na partida", e)
        }
    }
}
