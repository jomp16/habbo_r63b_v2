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
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.snowwar.bot.BotNameGenerator
import java.util.Locale
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

data class FastFoodGamePlayer(
    val session: FastFoodSession?,
    val id: Int,
    val name: String,
    val figure: String,
    val gender: String,
    val isBot: Boolean = false
) {
    var score = 0
    internal val plate = FastFoodGame.Plate()
    internal var timerTicks = 0 // o temporizador vermelho (§_-BL§): soltar com ticks restantes é largada antecipada
    internal val powerUps = IntArray(3) { FastFoodGame.POWER_UPS_PER_GAME } // 0 paraquedas grande, 1 míssil, 2 escudo
    internal var botDropAt = -1
    internal var botOpenAt = 0.0
    internal var botBigChute = false
}

/**
 * Uma partida de BaseJump (Fast Food): o servidor simula a queda de cada prato tick a tick, com a mesma integração
 * do cliente (§_-HF§.§_-MD§, um passo a cada 16 ms), e decide o que o cliente não decide sozinho: se o prato que
 * tocou a mesa pousou inteiro (NEXT_PLATE estado 3) ou espatifou (estado 4), se um míssil o destruiu (estado 5) e
 * quem venceu. O cliente só mostra: um prato que toca a mesa fica no estado 6 até o veredito chegar.
 *
 * Regras (o próprio cliente e as páginas da época): 3 jogadores; 6 pratos para vencer (Player.§_-LC§), um por curso
 * (sopa, pizza, hambúrguer, bandeja, bolo, bandeja: FallingObjectType); o próximo prato só pode ser solto quando o
 * temporizador vermelho enche, e soltar antes é largada antecipada (o prato cai sem paraquedas e espatifa, e o
 * temporizador não recomeça: §_-A-§.§_-Lp§/§_-A5§); comida pesada pede o paraquedas mais alto; o paraquedas grande
 * freia 10× (abre "no último segundo"); o escudo só protege de mísseis; o míssil vai no adversário que lidera.
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
        const val TICK_MILLIS = 16L // BaseJumpEngine.§_-DG§
        const val COUNTDOWN_TICKS = 188 // BaseJumpEngine.§_-M0§: 3, 2, 1, já! antes do jogo aceitar ações
        const val MISSILE_TICKS = 45 // §_-Hy§.§_-D§: o voo do míssil até o prato
        const val SAFE_LANDING_FACTOR = 1.3 // pousa inteiro tocando a mesa a até 1,3× a velocidade mínima do paraquedas

        // estados do prato (§_-HF§)
        const val IDLE = 0 // §_-DR§: no alto, esperando o soltar
        const val FALLING = 1 // §_-L4§
        const val PARACHUTE = 2 // §_-2q§
        const val LANDED = 3 // §_-0w§: pousou inteiro (ponto)
        const val SMASHED = 4 // §_-DO§: espatifou na mesa
        const val DESTROYED = 5 // §_-CX§: míssil

        private const val DT = TICK_MILLIS / 1000.0

        /**
         * Os seis cursos, na ordem do FallingObjectType (soup, pizza, burger, tray, cake, tray). Gravidade e
         * paraquedas em unidades de locY/s (1 = do alto da janela à mesa); o temporizador em ticks de 16 ms.
         * Os leves aguentam o paraquedas perto da mesa; os pesados pedem abrir lá pelo meio; o bolo demora mais.
         */
        val FOODS = listOf(
            Food(0, gravity = 0.32, decel = 1.6, minFall = 0.15, bigMinFall = 0.08, timerTicks = 150),
            Food(1, gravity = 0.32, decel = 1.5, minFall = 0.15, bigMinFall = 0.08, timerTicks = 170),
            Food(2, gravity = 0.34, decel = 1.0, minFall = 0.16, bigMinFall = 0.08, timerTicks = 200),
            Food(3, gravity = 0.36, decel = 0.65, minFall = 0.17, bigMinFall = 0.09, timerTicks = 240),
            Food(4, gravity = 0.38, decel = 0.55, minFall = 0.18, bigMinFall = 0.09, timerTicks = 290),
            Food(5, gravity = 0.36, decel = 0.65, minFall = 0.17, bigMinFall = 0.09, timerTicks = 240),
        )

        private fun num(v: Double) = String.format(Locale.ROOT, "%.6f", v)

        /** A altura mínima em que o paraquedas normal ainda pousa o prato inteiro (para os bots). */
        internal val SAFE_OPEN_HEIGHT: Map<Int, Double> = FOODS.associate { food ->
            var h = 0.0
            while (h < 1.0 && !landsSafely(food, h)) h += 0.01
            food.id to h
        }

        /** Se o prato, solto do alto, pousa inteiro com o paraquedas aberto em openAt (o grande, se big). */
        internal fun landsSafely(food: Food, openAt: Double, big: Boolean = false): Boolean {
            val p = Plate().apply { reset(food) }
            p.state = FALLING
            while (true) {
                if (p.state == FALLING && p.locY <= openAt) {
                    p.state = PARACHUTE
                    p.bigChute = big
                }
                if (p.step()) return p.state == PARACHUTE && p.speed <= p.minFall() * SAFE_LANDING_FACTOR
            }
        }
    }

    data class Food(
        val id: Int,
        val gravity: Double,
        val decel: Double,
        val minFall: Double,
        val bigMinFall: Double,
        val timerTicks: Int
    ) {
        fun wire() = FastFoodFallingObjectType(id, num(gravity), num(-decel), num(minFall), num(bigMinFall), timerTicks)
    }

    /** O prato corrente de um jogador, com a física de §_-HF§.§_-MD§. */
    class Plate {
        lateinit var food: Food
        var state = IDLE
        var locY = 1.0
        var speed = 0.0
        var falseStarted = false
        var bigChute = false
        var shielded = false
        var serial = 0 // muda a cada prato: um míssil só acerta o prato que mirou

        fun reset(food: Food) {
            this.food = food
            state = IDLE
            locY = 1.0
            speed = 0.0
            falseStarted = false
            bigChute = false
            shielded = false
            serial++
        }

        val inAir get() = state == FALLING || state == PARACHUTE

        fun minFall() = if (bigChute) food.bigMinFall else food.minFall

        /** Um tick; true quando o prato toca a mesa. */
        fun step(): Boolean {
            if (!inAir) return false
            val accel = if (state == FALLING) food.gravity else -food.decel * (if (bigChute) 10 else 1)
            speed += DT * accel
            if (state == PARACHUTE) speed = max(speed, minFall())
            locY -= DT * speed
            return locY < 0
        }
    }

    private class Missile(val target: FastFoodGamePlayer, val plateSerial: Int, val hitTick: Int)

    val players: List<FastFoodGamePlayer>
    val ended = AtomicBoolean(false)
    private var tick = 0
    private var ticker: ScheduledFuture<*>? = null
    private val missiles = mutableListOf<Missile>()

    init {
        val list = humans.mapNotNull { session ->
            val user = session.habboSession ?: return@mapNotNull null
            FastFoodGamePlayer(
                session = session,
                id = user.userInformation.id,
                name = user.userInformation.username,
                figure = user.userInformation.figure,
                gender = user.userInformation.gender
            )
        }.toMutableList()

        // bots completam os 3 lugares (ids negativos para nunca colidir).
        val existingNames = list.map { it.name }.toMutableSet()
        var botId = -1
        while (list.size < FastFoodGameManager.PLAYERS_PER_GAME) {
            val gender = if (ThreadLocalRandom.current().nextBoolean()) "M" else "F"
            val figure = HabboServer.habboGame.figureManager.generateRandomFigure(gender)
            list.add(
                FastFoodGamePlayer(
                    session = null,
                    id = botId--,
                    name = BotNameGenerator.generateNickname(existingNames),
                    figure = figure,
                    gender = gender,
                    isBot = true
                ).also { existingNames.add(it.name) }
            )
        }

        players = list
        players.forEach { it.plate.reset(FOODS[0]) }
    }

    fun start() {
        players.forEach { player ->
            player.session?.sendHabboResponse(
                FFOutgoing.GAME_STARTING,
                player.id,
                false,
                false,
                false,
                FOODS[0].id, // o tipo do primeiro prato (§_-JJ§)
                FOODS.map { it.wire() },
                mapOf(0 to POWER_UPS_PER_GAME, 1 to POWER_UPS_PER_GAME, 2 to POWER_UPS_PER_GAME),
                players
            )
        }
        ticker = FastFoodGameManager.scheduleAtFixedRate(TICK_MILLIS) { onTick() }
    }

    private val playing get() = !ended.get() && tick >= COUNTDOWN_TICKS

    private fun onTick() {
        if (ended.get()) return
        tick++
        for (player in players) {
            if (player.plate.step()) touchdown(player)
            if (ended.get()) return
            // o temporizador corre enquanto o prato não for de largada antecipada (§_-BL§.update)
            if (player.timerTicks > 0 && !player.plate.falseStarted) player.timerTicks--
        }
        val hits = missiles.filter { it.hitTick <= tick }
        missiles.removeAll(hits)
        hits.forEach { missileHit(it) }
        if (playing) players.filter { it.isBot }.forEach { botTick(it) }
    }

    // ---- ações (humanas via EXECUTE_ACTION, bots via botTick) ----

    fun dropFood(player: FastFoodGamePlayer) {
        val plate = player.plate
        if (!playing || plate.state != IDLE) return

        plate.state = FALLING
        plate.falseStarted = player.timerTicks > 0
        sendPlate(player)
    }

    fun parachute(player: FastFoodGamePlayer, big: Boolean) {
        val plate = player.plate
        if (!playing || !plate.inAir || plate.falseStarted) return
        if (big) {
            if (plate.bigChute || !usePowerUp(player, 0)) return
            plate.bigChute = true
            broadcast(FFOutgoing.USE_BIG_PARACHUTE, tick, player.id)
        } else if (plate.state == PARACHUTE) {
            return
        }
        plate.state = PARACHUTE
        sendPlate(player)
    }

    /** O míssil vai no adversário que lidera (no empate, num que tenha prato no ar). */
    fun missile(shooter: FastFoodGamePlayer) {
        if (!playing) return
        val target = players.filter { it.id != shooter.id }
            .sortedWith(compareByDescending<FastFoodGamePlayer> { it.score }.thenByDescending { it.plate.inAir })
            .firstOrNull() ?: return
        if (!usePowerUp(shooter, 1)) return
        broadcast(FFOutgoing.LAUNCH_MISSILE, tick, shooter.id, target.id)
        missiles.add(Missile(target, target.plate.serial, tick + MISSILE_TICKS))
        if (target.isBot) botMaybeShield(target)
    }

    fun shield(player: FastFoodGamePlayer) {
        val plate = player.plate
        if (!playing || !plate.inAir || plate.shielded || !usePowerUp(player, 2)) return
        plate.shielded = true
        broadcast(FFOutgoing.ENABLE_SHIELD, tick, player.id, true)
    }

    private fun usePowerUp(player: FastFoodGamePlayer, index: Int): Boolean {
        if (player.powerUps[index] <= 0) return false
        player.powerUps[index]--
        return true
    }

    private fun sendPlate(player: FastFoodGamePlayer) {
        val p = player.plate
        broadcast(FFOutgoing.DROP_FOOD, DropFoodData(tick, player.id, num(p.locY), num(p.speed), p.state, p.falseStarted))
    }

    // ---- vereditos ----

    private fun touchdown(player: FastFoodGamePlayer) {
        val plate = player.plate
        val safe = plate.state == PARACHUTE && plate.speed <= plate.minFall() * SAFE_LANDING_FACTOR
        if (!safe) {
            endPlate(player, SMASHED)
            return
        }
        player.score++
        if (player.score >= WIN_SCORE) {
            // o END_GAME pousa o último prato do vencedor e soma o ponto (BaseJumpEngine.§_-5c§): sem NEXT_PLATE
            endGame(player)
            return
        }
        endPlate(player, LANDED)
    }

    private fun missileHit(missile: Missile) {
        val plate = missile.target.plate
        if (plate.serial != missile.plateSerial || !plate.inAir || plate.shielded) return
        endPlate(missile.target, DESTROYED)
    }

    /**
     * O prato termina (pousou, espatifou ou foi destruído) e o próximo nasce no alto: o do curso seguinte depois de
     * um ponto, o mesmo curso depois de um prato perdido. O temporizador recomeça como no Player.nextPlate do
     * cliente: depois de um ponto, ou de um prato perdido que não era de largada antecipada; senão continua.
     */
    private fun endPlate(player: FastFoodGamePlayer, how: Int) {
        val lost = player.plate
        val next = FOODS[player.score.coerceAtMost(FOODS.size - 1)]
        if (how == LANDED || !lost.falseStarted) player.timerTicks = next.timerTicks
        broadcast(FFOutgoing.NEXT_PLATE, NextPlateData(tick, player.id, how, next.id, player.score))
        lost.reset(next)
        player.botDropAt = -1
    }

    fun removePlayer(session: FastFoodSession) {
        players.find { it.session == session } ?: return

        // sem humanos restantes, encerra a arena em silêncio
        if (players.none { !it.isBot && it.session != null && it.session != session }) {
            endGame(null, silent = true)
        }
    }

    private fun endGame(winner: FastFoodGamePlayer?, silent: Boolean = false) {
        if (!ended.compareAndSet(false, true)) return
        ticker?.cancel(false)
        missiles.clear()

        if (!silent && winner != null) {
            // o vencedor é o ÚLTIMO entry (o parser §_-FL§ sobrescreve §_-I3§ a cada iteração); o placar dele já
            // conta o prato que o END_GAME pousa
            val scores = (players.filter { it !== winner }.sortedBy { it.score } + winner).map { it.id to it.score }
            broadcast(FFOutgoing.END_GAME, EndGameData(scores))
        }

        players.forEach { it.session?.let { s -> FastFoodGameManager.leave(s) } }
        onEnd(this)
    }

    fun broadcast(outgoing: FFOutgoing, vararg args: Any?) {
        players.forEach { p -> p.session?.sendHabboResponse(outgoing, *args) }
    }

    // ---- bots: as mesmas ações de um humano, com reação e erros ----

    private fun botTick(bot: FastFoodGamePlayer) {
        val rnd = ThreadLocalRandom.current()
        val plate = bot.plate
        when (plate.state) {
            IDLE -> {
                if (bot.botDropAt < 0) {
                    // reação de 0,3 a 1,3 s depois do temporizador encher; às vezes uma largada antecipada
                    val early = rnd.nextInt(100) < 4
                    bot.botDropAt = tick + bot.timerTicks + (if (early) -rnd.nextInt(10, 30) else rnd.nextInt(20, 80))
                    val safeAt = SAFE_OPEN_HEIGHT.getValue(plate.food.id)
                    if (rnd.nextInt(100) < 15) {
                        // atrasado: espatifa, a não ser que salve com o paraquedas grande
                        bot.botOpenAt = safeAt * rnd.nextDouble(0.4, 0.85)
                        bot.botBigChute = bot.powerUps[0] > 0 && rnd.nextBoolean()
                    } else {
                        bot.botOpenAt = (safeAt * rnd.nextDouble(1.05, 1.7)).coerceAtMost(0.95)
                        bot.botBigChute = false
                    }
                }
                if (tick >= bot.botDropAt) dropFood(bot)
            }
            FALLING -> if (plate.locY <= bot.botOpenAt) parachute(bot, bot.botBigChute)
        }
        // às vezes um míssil, quando alguém que já pontuou, à frente ou empatado, tem prato no ar
        if (bot.powerUps[1] > 0 && rnd.nextInt(1000) < 3 &&
            players.any { it.id != bot.id && it.score > 0 && it.score >= bot.score && it.plate.inAir && it.plate.locY > 0.3 }
        ) {
            missile(bot)
        }
    }

    private fun botMaybeShield(bot: FastFoodGamePlayer) {
        val rnd = ThreadLocalRandom.current()
        if (bot.powerUps[2] <= 0 || rnd.nextInt(100) >= 50) return
        FastFoodGameManager.schedule(rnd.nextLong(100, 500)) { if (!ended.get()) shield(bot) }
    }
}
