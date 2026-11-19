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
import ovh.rwx.fastfood.game.bot.FastFoodBot
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Uma partida de BaseJump (Fast Food): o servidor simula a queda de cada prato tick a tick, com a mesma integração
 * do cliente (FastFoodPlate, um passo a cada 16 ms), e decide o que o cliente não decide sozinho: se o prato que
 * tocou a mesa pousou inteiro (NEXT_PLATE estado 3) ou espatifou (estado 4), se um míssil o destruiu (estado 5) e
 * quem venceu. O cliente só mostra: um prato que toca a mesa fica no estado 6 até o veredito chegar.
 *
 * Regras (o próprio cliente e as páginas da época): 3 jogadores; 6 pratos para vencer (Player.§_-LC§), um por curso
 * (FastFoodPhysics.FOODS); o próximo prato só pode ser solto quando o temporizador vermelho enche, e soltar antes é
 * largada antecipada (o prato cai sem paraquedas e espatifa, e o temporizador não recomeça: §_-A-§.§_-Lp§/§_-A5§);
 * comida pesada pede o paraquedas mais alto; o paraquedas grande freia 10× (abre "no último segundo"); o escudo só
 * protege de mísseis; o míssil vai no adversário que lidera. Os bots (FastFoodBot) usam as mesmas ações.
 *
 * Tudo roda na thread única do FastFoodGameManager: ações das sessões, o tick e os bots.
 */
class FastFoodGame(
    humans: List<FastFoodSession>,
    private val onEnd: (FastFoodGame) -> Unit
) {
    companion object {
        const val WIN_SCORE = 6 // Player.§_-LC§
        const val POWER_UPS_PER_GAME = 1
        const val COUNTDOWN_TICKS = 188 // BaseJumpEngine.§_-M0§: 3, 2, 1, já! antes do jogo aceitar ações
        const val MISSILE_TICKS = 45 // §_-Hy§.§_-D§: o voo do míssil até o prato

        /**
         * O placar do END_GAME: o vencedor é a PRIMEIRA entrada (o parser §_-FL§ só guarda §_-I3§ quando o índice
         * é 0), depois os demais do maior placar ao menor. O placar do vencedor já conta o prato que o END_GAME
         * pousa.
         */
        internal fun endGameScores(players: List<FastFoodGamePlayer>, winner: FastFoodGamePlayer): List<Pair<Int, Int>> =
            (listOf(winner) + players.filter { it !== winner }.sortedByDescending { it.score }).map { it.id to it.score }
    }

    private class Missile(val target: FastFoodGamePlayer, val plateSerial: Int, val hitTick: Int)

    val players: List<FastFoodGamePlayer>
    val ended = AtomicBoolean(false)
    var currentTick = 0
        private set
    private var ticker: ScheduledFuture<*>? = null
    private val missiles = mutableListOf<Missile>()

    init {
        val humanPlayers = humans.mapNotNull { session ->
            val user = session.habboSession ?: return@mapNotNull null
            FastFoodGamePlayer(
                session = session,
                id = user.userInformation.id,
                name = user.userInformation.username,
                figure = user.userInformation.figure,
                gender = user.userInformation.gender
            )
        }
        players = humanPlayers + FastFoodBot.fill(humanPlayers, FastFoodGameManager.PLAYERS_PER_GAME)
        players.forEach { it.plate.reset(FastFoodPhysics.FOODS[0]) }
    }

    fun start() {
        val foods = FastFoodPhysics.FOODS
        players.forEach { player ->
            player.session?.sendHabboResponse(
                FFOutgoing.GAME_STARTING,
                player.id,
                false,
                false,
                false,
                foods[0].id, // o tipo do primeiro prato (§_-JJ§)
                foods.map { it.wire() },
                FastFoodPowerUp.entries.associate { it.wire to POWER_UPS_PER_GAME },
                players
            )
        }
        ticker = FastFoodGameManager.scheduleAtFixedRate(FastFoodPhysics.TICK_MILLIS) { onTick() }
    }

    private val playing get() = !ended.get() && currentTick >= COUNTDOWN_TICKS

    private fun onTick() {
        if (ended.get()) return
        currentTick++
        for (player in players) {
            if (player.plate.step()) touchdown(player)
            if (ended.get()) return
            // o temporizador corre enquanto o prato não for de largada antecipada (§_-BL§.update)
            if (player.timerTicks > 0 && !player.plate.falseStarted) player.timerTicks--
        }
        val hits = missiles.filter { it.hitTick <= currentTick }
        missiles.removeAll(hits)
        hits.forEach { missileHit(it) }
        if (playing) players.forEach { player -> player.bot?.tick(this, player) }
    }

    // ---- ações (humanas via EXECUTE_ACTION, bots via FastFoodBot.tick) ----

    fun dropFood(player: FastFoodGamePlayer) {
        val plate = player.plate
        if (!playing || plate.state != PlateState.IDLE) return

        plate.state = PlateState.FALLING
        plate.falseStarted = player.timerTicks > 0
        sendPlate(player)
    }

    fun parachute(player: FastFoodGamePlayer, big: Boolean) {
        val plate = player.plate
        if (!playing || !plate.inAir || plate.falseStarted) return
        if (big) {
            if (plate.bigChute || !player.usePowerUp(FastFoodPowerUp.BIG_PARACHUTE)) return
            plate.bigChute = true
            broadcast(FFOutgoing.USE_BIG_PARACHUTE, currentTick, player.id)
        } else if (plate.state == PlateState.PARACHUTE) {
            return
        }
        plate.state = PlateState.PARACHUTE
        sendPlate(player)
    }

    /** O míssil vai no adversário que lidera (no empate, num que tenha prato no ar). */
    fun missile(shooter: FastFoodGamePlayer) {
        if (!playing) return
        val target = players.filter { it !== shooter }
            .sortedWith(compareByDescending<FastFoodGamePlayer> { it.score }.thenByDescending { it.plate.inAir })
            .firstOrNull() ?: return
        if (!shooter.usePowerUp(FastFoodPowerUp.MISSILE)) return
        broadcast(FFOutgoing.LAUNCH_MISSILE, currentTick, shooter.id, target.id)
        missiles.add(Missile(target, target.plate.serial, currentTick + MISSILE_TICKS))
        target.bot?.onMissileIncoming(this, target)
    }

    fun shield(player: FastFoodGamePlayer) {
        val plate = player.plate
        if (!playing || !plate.inAir || plate.shielded || !player.usePowerUp(FastFoodPowerUp.SHIELD)) return
        plate.shielded = true
        broadcast(FFOutgoing.ENABLE_SHIELD, currentTick, player.id, true)
    }

    private fun sendPlate(player: FastFoodGamePlayer) {
        val p = player.plate
        broadcast(
            FFOutgoing.DROP_FOOD,
            DropFoodData(currentTick, player.id, FastFoodPhysics.num(p.locY), FastFoodPhysics.num(p.speed), p.state.wire, p.falseStarted)
        )
    }

    // ---- vereditos ----

    private fun touchdown(player: FastFoodGamePlayer) {
        if (!player.plate.touchedDownSafely()) {
            endPlate(player, PlateState.SMASHED)
            return
        }
        player.score++
        if (player.score >= WIN_SCORE) {
            // o END_GAME pousa o último prato do vencedor e soma o ponto (BaseJumpEngine.§_-5c§): sem NEXT_PLATE
            endGame(player)
            return
        }
        endPlate(player, PlateState.LANDED)
    }

    private fun missileHit(missile: Missile) {
        val plate = missile.target.plate
        if (plate.serial != missile.plateSerial || !plate.inAir || plate.shielded) return
        endPlate(missile.target, PlateState.DESTROYED)
    }

    /**
     * O prato termina (pousou, espatifou ou foi destruído) e o próximo nasce no alto: o do curso seguinte depois de
     * um ponto, o mesmo curso depois de um prato perdido. O temporizador recomeça como no Player.nextPlate do
     * cliente: depois de um ponto, ou de um prato perdido que não era de largada antecipada; senão continua.
     */
    private fun endPlate(player: FastFoodGamePlayer, how: PlateState) {
        val lost = player.plate
        val next = FastFoodPhysics.foodFor(player.score)
        if (how == PlateState.LANDED || !lost.falseStarted) player.timerTicks = next.timerTicks
        broadcast(FFOutgoing.NEXT_PLATE, NextPlateData(currentTick, player.id, how.wire, next.id, player.score))
        lost.reset(next)
        player.bot?.onNewPlate()
    }

    fun removePlayer(session: FastFoodSession) {
        players.find { it.session == session } ?: return

        // sem humanos restantes, encerra a arena em silêncio
        if (players.none { !it.isBot && it.session != null && it.session != session }) {
            endGame(null)
        }
    }

    /** Encerra a partida; sem vencedor (todos os humanos saíram), sem END_GAME. */
    private fun endGame(winner: FastFoodGamePlayer?) {
        if (!ended.compareAndSet(false, true)) return
        ticker?.cancel(false)
        missiles.clear()

        if (winner != null) {
            broadcast(FFOutgoing.END_GAME, EndGameData(endGameScores(players, winner)))
        }

        players.forEach { it.session?.let { s -> FastFoodGameManager.leave(s) } }
        onEnd(this)
    }

    fun broadcast(outgoing: FFOutgoing, vararg args: Any?) {
        players.forEach { p -> p.session?.sendHabboResponse(outgoing, *args) }
    }
}
