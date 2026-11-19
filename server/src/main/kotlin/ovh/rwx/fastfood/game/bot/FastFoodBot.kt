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

package ovh.rwx.fastfood.game.bot

import ovh.rwx.fastfood.game.FastFoodFood
import ovh.rwx.fastfood.game.FastFoodGame
import ovh.rwx.fastfood.game.FastFoodGameManager
import ovh.rwx.fastfood.game.FastFoodGamePlayer
import ovh.rwx.fastfood.game.FastFoodPhysics
import ovh.rwx.fastfood.game.FastFoodPowerUp
import ovh.rwx.fastfood.game.PlateState
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.snowwar.bot.BotNameGenerator
import java.util.concurrent.ThreadLocalRandom
import java.util.random.RandomGenerator

/**
 * Como um bot joga (FastFoodBot.plan). Cada bot da partida tem um estilo diferente.
 *
 * @param reactionTicks ticks de 16 ms entre o temporizador encher e o soltar.
 * @param earlyDropPercent chance de soltar antes da hora (largada antecipada: o prato espatifa).
 * @param bigChuteRescuePercent chance de, abrindo tarde, gastar o paraquedas grande para salvar o prato.
 */
enum class BotStyle(val reactionTicks: IntRange, val earlyDropPercent: Int, val bigChuteRescuePercent: Int) {
    CAUTIOUS(60..150, 2, 0), // medroso: abre cedo demais e demora a soltar
    AVERAGE(35..100, 5, 30), // mediano: abre pela metade
    DARING(20..55, 10, 50), // afoito: abre rente ao limite, solta rápido e às vezes antes da hora
}

/** O que um bot fará com o próximo prato: ticks depois do temporizador encher (negativo = largada antecipada). */
data class BotPlan(val dropDelay: Int, val openAt: Double, val bigChute: Boolean)

/**
 * O cérebro de um bot: joga com as mesmas ações de um humano (FastFoodGame.dropFood/parachute/missile/shield), a
 * cada tick da partida, na thread do FastFoodGameManager.
 */
class FastFoodBot(val style: BotStyle) {
    companion object {
        /**
         * Os bots que completam a partida até `seats` lugares: ids negativos (nunca colidem com usuários), nomes
         * únicos, visual aleatório e um estilo diferente para cada um.
         */
        fun fill(humans: List<FastFoodGamePlayer>, seats: Int): List<FastFoodGamePlayer> {
            val rnd = ThreadLocalRandom.current()
            val names = humans.mapTo(mutableSetOf()) { it.name }
            val styles = BotStyle.entries.shuffled().iterator()
            return (1..(seats - humans.size).coerceAtLeast(0)).map { n ->
                val gender = if (rnd.nextBoolean()) "M" else "F"
                FastFoodGamePlayer(
                    session = null,
                    id = -n,
                    name = BotNameGenerator.generateNickname(names).also { names.add(it) },
                    figure = HabboServer.habboGame.figureManager.generateRandomFigure(gender),
                    gender = gender,
                    bot = FastFoodBot(if (styles.hasNext()) styles.next() else BotStyle.AVERAGE)
                )
            }
        }

        /**
         * O plano de um bot para o próximo prato, conforme o estilo. Dois pontos à frente do melhor humano, o bot se
         * distrai (reage 0,6 a 1,6 s mais devagar); dois atrás, se apressa um pouco.
         *
         * @param lead pontos do bot menos os do melhor humano.
         */
        fun plan(food: FastFoodFood, style: BotStyle, lead: Int, hasBigChute: Boolean, rnd: RandomGenerator): BotPlan {
            val reaction = rnd.nextInt(style.reactionTicks.first, style.reactionTicks.last + 1) + when {
                lead >= 2 -> rnd.nextInt(40, 100)
                lead <= -2 -> -15
                else -> 0
            }
            val dropDelay = if (rnd.nextInt(100) < style.earlyDropPercent) -rnd.nextInt(10, 40) else reaction.coerceAtLeast(10)
            val safeAt = FastFoodPhysics.SAFE_OPEN_HEIGHT.getValue(food.id)
            val openAt = when (style) {
                // lá no alto: nunca espatifa, mas desce devagar
                BotStyle.CAUTIOUS -> rnd.nextDouble(0.75, 0.95)
                // "pela metade", sem olhar o prato: folgado nos leves, às vezes tarde nos pesados
                BotStyle.AVERAGE -> rnd.nextDouble(0.25, 0.55)
                // rente ao limite do prato: rápido, e às vezes passa do ponto
                BotStyle.DARING -> safeAt * rnd.nextDouble(0.9, 1.35)
            }
            val late = openAt < safeAt
            return BotPlan(dropDelay, openAt, late && hasBigChute && rnd.nextInt(100) < style.bigChuteRescuePercent)
        }
    }

    private var dropAt = -1
    private var current: BotPlan? = null

    /** Um prato novo nasceu no alto: o plano é refeito no próximo tick. */
    fun onNewPlate() {
        dropAt = -1
        current = null
    }

    fun tick(game: FastFoodGame, self: FastFoodGamePlayer) {
        val rnd = ThreadLocalRandom.current()
        val plate = self.plate
        when (plate.state) {
            PlateState.IDLE -> {
                if (current == null) {
                    // a vantagem sobre o melhor humano: quem dispara na frente se distrai
                    val bestHuman = game.players.filter { !it.isBot }.maxOfOrNull { it.score } ?: 0
                    val next = plan(plate.food, style, self.score - bestHuman, self.hasPowerUp(FastFoodPowerUp.BIG_PARACHUTE), rnd)
                    current = next
                    dropAt = game.currentTick + self.timerTicks + next.dropDelay
                }
                if (game.currentTick >= dropAt) game.dropFood(self)
            }

            PlateState.FALLING -> current?.let { if (plate.locY <= it.openAt) game.parachute(self, it.bigChute) }
            else -> Unit
        }
        // às vezes um míssil, quando alguém que já pontuou, à frente ou empatado, tem prato no ar
        if (self.hasPowerUp(FastFoodPowerUp.MISSILE) && rnd.nextInt(1000) < 3 &&
            game.players.any { it !== self && it.score > 0 && it.score >= self.score && it.plate.inAir && it.plate.locY > 0.3 }
        ) {
            game.missile(self)
        }
    }

    /** Um míssil vem neste bot: metade das vezes ergue o escudo, depois de 0,1 a 0,5 s. */
    fun onMissileIncoming(game: FastFoodGame, self: FastFoodGamePlayer) {
        val rnd = ThreadLocalRandom.current()
        if (!self.hasPowerUp(FastFoodPowerUp.SHIELD) || rnd.nextInt(100) >= 50) return
        FastFoodGameManager.schedule(rnd.nextLong(100, 500)) { if (!game.ended.get()) game.shield(self) }
    }
}
