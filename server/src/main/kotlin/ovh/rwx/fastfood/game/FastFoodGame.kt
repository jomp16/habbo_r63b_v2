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
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.sqrt

data class FastFoodGamePlayer(
    val session: FastFoodSession?,
    val id: Int,
    val name: String,
    val figure: String,
    val gender: String,
    val isBot: Boolean = false
) {
    val score = AtomicInteger(0)
}

/**
 * Loop do BaseJump: o cliente simula a física da comida (queda, paraquedas);
 * o servidor é autoritativo no que importa — quem derrubou o quê (lockstep
 * seq compartilhado), prato entregue (NEXT_PLATE), mísseis e o placar final.
 */
class FastFoodGame(
    humans: List<FastFoodSession>,
    private val onEnd: (FastFoodGame) -> Unit
) {
    companion object {
        const val WIN_SCORE = 5
        const val FOOD_TYPE_ID = 0

        // escala: §_-BT§.update faz y = 200 + 620*(1-locY), ou seja, 1 unidade de locY = 620px de tela.
        // Queda visível: locY 1.0 (y=200px, topo da janela) → 0 (y=820, mesa). Física do §_-MD§ em unidades/s.
        const val FOOD_START_LOC_Y = 1.0
        const val FOOD_GRAVITY = 0.22 // queda livre ~3s: sqrt(2*1.0/0.22)
        const val FOOD_PARACHUTE_DECEL = 0.5
        const val FOOD_BIG_PARACHUTE_DECEL = 5.0 // §_-KL§: decel ×10 no bigparachute
        const val FOOD_MIN_FALL = 0.3 // ≈186 px/s de tela: pouso suave
        const val FOOD_BIG_PARACHUTE_MIN_FALL = 0.15
        const val FOOD_FREE_FALL_MILLIS = 3020L // sqrt(2*1.0/0.22) ≈ 3.02s: espatifa (state 6 no cliente)
        const val PLATE_NEXT_MILLIS = 70L * 16 // plateTimerTicks (§_-C-§=70 subturns): prato assenta antes do próximo nascer
        const val FOOD_STATE_FALLING = 1 // §_-L4§
        const val FOOD_STATE_PARACHUTE = 2 // §_-2q§: clamp em minFallSpeed, decel em vez de g
        const val FOOD_STATE_ON_PLATE = 3 // §_-0w§: §_-0L§ zera locY/speed (comida pousa) e inicia o timer do prato
        const val FOOD_STATE_DELIVERED = 4 // §_-DO§: state inicial do prato no GAME_STARTING

        private val FALLING_TYPES = listOf(
            // gravidade, decel do paraquedas, velocidade mínima (bigparachute) e timer do prato em ticks de 16ms
            FastFoodFallingObjectType(
                0, "${FOOD_GRAVITY}", "-${FOOD_PARACHUTE_DECEL}", "${FOOD_MIN_FALL}", "${FOOD_BIG_PARACHUTE_MIN_FALL}", 70
            )
        )

    }

    val players: List<FastFoodGamePlayer>
    val ended = AtomicBoolean(false)
    private val foodToken = AtomicInteger(0)
    private val falls = mutableMapOf<Int, Fall>()
    private var startTime = 0L

    /** seq = nº do subturn (clock de 16ms do engine, §_-4n§), não índice de evento:
     *  o cliente faz fast-forward/catchup da física em subturns via (counter - event.seq). */
    private fun nextSeq() = ((System.currentTimeMillis() - startTime) / 16L).toInt() + 2

    private class Fall(val token: Int, val dropTime: Long) {
        var chuteDecel: Double? = null // null = queda livre; aberto = paraquedas
        var minFall = 0.0
    }

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
    }

    fun start() {
        startTime = System.currentTimeMillis()
        players.forEach { player ->
            player.session?.sendHabboResponse(
                FFOutgoing.GAME_STARTING,
                player.id,
                false,
                false,
                false,
                FALLING_TYPES,
                mapOf(0 to 1, 1 to 1, 2 to 1), // powerups iniciais: 0=bigparachute, 1=missile, 2=shield
                players
            )
        }

        players.filter { it.isBot }.forEach { scheduleBot(it) }
    }

    // ---- ações (humanas via EXECUTE_ACTION, bots via scheduleBot) ----

    fun dropFood(player: FastFoodGamePlayer) {
        if (ended.get() || player.id in falls) return

        // o prato já foi armado (NEXT_PLATE(4,type) do ciclo anterior ou do §_-64§ do GAME_STARTING);
        // o drop é só o DROP_FOOD de queda — NEXT_PLATE aqui resetaria o widget do jogador local
        // (engine.nextPlate põe §_-7D§ em §_-0C§) e mataria o botão-vermelho de paraquedas.
        val token = foodToken.incrementAndGet()
        falls[player.id] = Fall(token, System.currentTimeMillis())
        broadcast(
            FFOutgoing.DROP_FOOD,
            DropFoodData(nextSeq(), player.id, "$FOOD_START_LOC_Y", "0", FOOD_STATE_FALLING, false)
        )

        // sem paraquedas até o fim da queda livre: espatifa na mesa (state 6 anima sozinho no cliente)
        FastFoodGameManager.schedule(FOOD_FREE_FALL_MILLIS) { resolveFall(player, token) }
    }

    /**
     * Abre o paraquedas em plena queda: DROP_FOOD state 2 com locY/speed atuais
     * (o cliente troca a aceleração g pela decel e clampa em minFallSpeed).
     */
    fun parachute(player: FastFoodGamePlayer, big: Boolean) {
        val fall = falls[player.id] ?: return
        if (fall.chuteDecel != null) return

        val t = (System.currentTimeMillis() - fall.dropTime) / 1000.0
        val locY = FOOD_START_LOC_Y - 0.5 * FOOD_GRAVITY * t * t
        if (locY <= 0) return // já bateu na mesa

        val decel = if (big) FOOD_BIG_PARACHUTE_DECEL else FOOD_PARACHUTE_DECEL
        val minFall = if (big) FOOD_BIG_PARACHUTE_MIN_FALL else FOOD_MIN_FALL
        fall.chuteDecel = decel
        fall.minFall = minFall

        if (big) broadcast(FFOutgoing.USE_BIG_PARACHUTE, nextSeq(), player.id)
        val speed = max(FOOD_GRAVITY * t, minFall)
        broadcast(
            FFOutgoing.DROP_FOOD,
            DropFoodData(nextSeq(), player.id, locY.toString(), speed.toString(), FOOD_STATE_PARACHUTE, false)
        )

        // pouso suave: desacelera até minFall, depois desce a minFall px/s
        val t1 = (speed - minFall) / decel
        val d1 = speed * t1 - 0.5 * decel * t1 * t1
        val landSec = if (locY <= d1) {
            (speed - sqrt(speed * speed - 2 * decel * locY)) / decel
        } else {
            t1 + (locY - d1) / minFall
        }
        FastFoodGameManager.schedule((landSec * 1000).toLong()) { resolveFall(player, fall.token) }
    }

    private fun resolveFall(player: FastFoodGamePlayer, token: Int) {
        if (ended.get()) return
        val fall = falls[player.id] ?: return
        if (fall.token != token) return
        falls.remove(player.id)

        val score: Int
        if (fall.chuteDecel == null) {
            // espatifou: state 6 (§_-6E§) o cliente anima sozinho, sem ponto
            score = player.score.get()
        } else {
            score = player.score.incrementAndGet()
            // type -1 → Map.getValue(-1) = null no AS3: só aplica §_-0L§(3) no prato corrente (pousa a comida), sem criar prato novo
            broadcast(FFOutgoing.NEXT_PLATE, NextPlateData(nextSeq(), player.id, FOOD_STATE_ON_PLATE, -1, score))
        }

        if (score < WIN_SCORE) {
            // o próximo prato só nasce depois do atual assentar na mesa (inteiro ou espatifado):
            // plateTimerTicks do §_-6k§ — drop antes disso é "false start" no cliente (§_-Lp§)
            FastFoodGameManager.schedule(PLATE_NEXT_MILLIS) {
                if (ended.get()) return@schedule
                broadcast(FFOutgoing.NEXT_PLATE, NextPlateData(nextSeq(), player.id, FOOD_STATE_DELIVERED, FOOD_TYPE_ID, player.score.get()))
            }
        } else {
            endGame()
        }
    }

    fun bigParachute(player: FastFoodGamePlayer) {
        parachute(player, big = true)
    }

    fun missile(shooter: FastFoodGamePlayer, target: FastFoodGamePlayer) {
        if (ended.get() || shooter.id == target.id) return

        broadcast(FFOutgoing.LAUNCH_MISSILE, nextSeq(), shooter.id, target.id)

        // míssil destrói a comida em queda do alvo
        if (falls.remove(target.id) != null) {

            // alvo bot às vezes reage com escudo (visual)
            if (target.isBot && ThreadLocalRandom.current().nextInt(100) < 30) {
                shield(target, true)
            }
        }
    }

    fun shield(player: FastFoodGamePlayer, active: Boolean) {
        if (ended.get()) return

        broadcast(FFOutgoing.ENABLE_SHIELD, nextSeq(), player.id, active)
    }

    fun removePlayer(session: FastFoodSession) {
        val player = players.find { it.session == session } ?: return

        falls.remove(player.id)

        // sem humanos restantes, encerra a arena em silêncio
        if (players.none { !it.isBot && it.session != null }) {
            endGame(silent = true)
        }
    }

    private fun endGame(silent: Boolean = false) {
        if (!ended.compareAndSet(false, true)) return

        falls.clear()

        if (!silent) {
            // vencedor é o ÚLTIMO entry (parser §_-I3§ sobrescrito a cada iteração)
            val scores = players.sortedBy { it.score.get() }
                .map { it.id to it.score.get() }
            broadcast(FFOutgoing.END_GAME, EndGameData(scores))
        }

        players.forEach { it.session?.let { s -> FastFoodGameManager.leave(s) } }
        onEnd(this)
    }

    fun broadcast(outgoing: FFOutgoing, vararg args: Any?) {
        players.forEach { p -> p.session?.sendHabboResponse(outgoing, *args) }
    }

    // ---- bot AI: loop simples com cadência aleatória ----

    private fun scheduleBot(bot: FastFoodGamePlayer) {
        val rnd = ThreadLocalRandom.current()
        FastFoodGameManager.schedule(rnd.nextLong(3000, 6000)) {
            if (ended.get()) return@schedule

            dropFood(bot)

            // bot abre o paraquedas (85%): sem isso, o prato espatifa e ele nunca pontua
            if (rnd.nextInt(100) < 85) {
                FastFoodGameManager.schedule(rnd.nextLong(300, 900)) {
                    parachute(bot, big = rnd.nextInt(100) < 30)
                }
            }

            // às vezes um míssil num humano
            if (rnd.nextInt(100) < 15) {
                players.filter { !it.isBot }.randomOrNull()?.let { target ->
                    missile(bot, target)
                }
            }

            if (!ended.get()) scheduleBot(bot)
        }
    }
}
