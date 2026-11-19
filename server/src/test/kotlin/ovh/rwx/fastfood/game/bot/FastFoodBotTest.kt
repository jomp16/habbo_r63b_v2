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

import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.fastfood.game.FastFoodFood
import ovh.rwx.fastfood.game.FastFoodGame
import ovh.rwx.fastfood.game.FastFoodPhysics
import ovh.rwx.fastfood.game.FastFoodPlate
import ovh.rwx.fastfood.game.PlateState
import java.util.Random

class FastFoodBotTest {
    /** Ticks que o prato leva no ar e se pousou inteiro, abrindo o paraquedas como o bot planejou. */
    private fun flight(food: FastFoodFood, plan: BotPlan): Pair<Int, Boolean> {
        val falseStart = plan.dropDelay < 0 // largada antecipada: cai sem paraquedas
        val p = FastFoodPlate().apply { reset(food) }
        p.state = PlateState.FALLING
        var ticks = 1
        while (true) {
            if (!falseStart && p.state == PlateState.FALLING && p.locY <= plan.openAt) {
                p.state = PlateState.PARACHUTE
                p.bigChute = plan.bigChute
            }
            if (p.step()) return ticks to p.touchedDownSafely()
            ticks++
        }
    }

    private class Stats(val seconds: Double, val landedRate: Double)

    /**
     * Um jogador sozinho até os 6 pontos, várias vezes: duração média da partida e taxa de pratos inteiros. O
     * temporizador recomeça a cada prato, menos depois de uma largada antecipada (aí só resta o que faltava).
     */
    private fun simulate(games: Int, plan: (FastFoodFood, Boolean) -> BotPlan): Stats {
        var totalTicks = 0L
        var plates = 0
        var landed = 0
        repeat(games) {
            var score = 0
            var timer = FastFoodPhysics.FOODS[0].timerTicks
            var bigChutes = FastFoodGame.BOT_POWER_UPS
            while (score < FastFoodGame.WIN_SCORE) {
                val food = FastFoodPhysics.foodFor(score)
                val p = plan(food, bigChutes > 0)
                if (p.bigChute) bigChutes--
                val (air, ok) = flight(food, p)
                totalTicks += timer + p.dropDelay + air
                plates++
                if (ok) {
                    landed++
                    score++
                }
                timer = if (p.dropDelay < 0) -p.dropDelay else FastFoodPhysics.foodFor(score).timerTicks
            }
        }
        return Stats(totalTicks * 0.016 / games, landed.toDouble() / plates)
    }

    private fun simulate(style: BotStyle): Stats {
        val rnd = Random(42)
        return simulate(400) { food, big -> FastFoodBot.plan(food, style, 0, big, rnd) }
    }

    @Test
    fun eachStyleHasItsOwnFlawAndNoneIsPerfect() {
        // referência: reage em 0,4 s e abre logo acima do limite de cada prato, sem nunca errar
        val perfect = simulate(1) { food, _ -> BotPlan(25, FastFoodPhysics.SAFE_OPEN_HEIGHT.getValue(food.id) + 0.02, false) }
        val cautious = simulate(BotStyle.CAUTIOUS)
        val average = simulate(BotStyle.AVERAGE)
        val daring = simulate(BotStyle.DARING)
        println("perfeito %.1f s".format(perfect.seconds))
        listOf("medroso" to cautious, "mediano" to average, "afoito" to daring).forEach { (name, s) ->
            println("%-8s %.1f s, pratos inteiros %.0f%%".format(name, s.seconds, s.landedRate * 100))
        }

        // nenhum bot chega perto do jogador perfeito: há folga para um humano vencer
        listOf(cautious, average, daring).forEach { assertTrue(it.seconds > perfect.seconds * 1.15) }
        // o medroso quase nunca espatifa, mas é o mais lento
        assertTrue(cautious.landedRate > 0.9 && cautious.seconds > average.seconds && cautious.seconds > daring.seconds)
        // o afoito é o mais rápido, e o que mais espatifa
        assertTrue(daring.seconds < average.seconds && daring.landedRate < average.landedRate)
        assertTrue(daring.landedRate < 0.9 && average.landedRate < 0.9)
    }

    @Test
    fun aBotFarAheadOfTheHumansSlowsDown() {
        val food = FastFoodPhysics.FOODS[0]
        fun meanDelay(lead: Int): Double {
            val rnd = Random(7)
            return (1..2000).map { FastFoodBot.plan(food, BotStyle.DARING, lead, false, rnd).dropDelay }
                .filter { it > 0 }.average()
        }
        assertTrue(meanDelay(2) > meanDelay(0) + 40)
    }
}
