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

import java.util.Locale
import kotlin.math.max

/** Estados do prato (§_-HF§), com o int que vai no DROP_FOOD e no NEXT_PLATE. */
enum class PlateState(val wire: Int) {
    IDLE(0), // §_-DR§: no alto, esperando o soltar
    FALLING(1), // §_-L4§
    PARACHUTE(2), // §_-2q§
    LANDED(3), // §_-0w§: pousou inteiro (ponto)
    SMASHED(4), // §_-DO§: espatifou na mesa
    DESTROYED(5), // §_-CX§: míssil
}

/**
 * Um curso (FallingObjectType): gravidade e paraquedas em unidades de locY/s (1 = do alto da janela à mesa); o
 * temporizador em ticks de 16 ms.
 */
data class FastFoodFood(
    val id: Int,
    val gravity: Double,
    val decel: Double,
    val minFall: Double,
    val bigMinFall: Double,
    val timerTicks: Int
) {
    fun wire() = FastFoodFallingObjectType(
        id,
        FastFoodPhysics.num(gravity),
        FastFoodPhysics.num(-decel),
        FastFoodPhysics.num(minFall),
        FastFoodPhysics.num(bigMinFall),
        timerTicks
    )
}

/** O prato corrente de um jogador, com a física de §_-HF§.§_-MD§. */
class FastFoodPlate {
    lateinit var food: FastFoodFood
    var state = PlateState.IDLE
    var locY = 1.0
    var speed = 0.0
    var falseStarted = false
    var bigChute = false
    var shielded = false
    var serial = 0 // muda a cada prato: um míssil só acerta o prato que mirou

    fun reset(food: FastFoodFood) {
        this.food = food
        state = PlateState.IDLE
        locY = 1.0
        speed = 0.0
        falseStarted = false
        bigChute = false
        shielded = false
        serial++
    }

    val inAir get() = state == PlateState.FALLING || state == PlateState.PARACHUTE

    fun minFall() = if (bigChute) food.bigMinFall else food.minFall

    /** Um tick; true quando o prato toca a mesa. */
    fun step(): Boolean {
        if (!inAir) return false
        val accel = if (state == PlateState.FALLING) food.gravity else -food.decel * (if (bigChute) 10 else 1)
        speed += FastFoodPhysics.DT * accel
        if (state == PlateState.PARACHUTE) speed = max(speed, minFall())
        locY -= FastFoodPhysics.DT * speed
        return locY < 0
    }

    /** Ao tocar a mesa: pousa inteiro se veio de paraquedas a até 1,3× a velocidade mínima dele. */
    fun touchedDownSafely() = state == PlateState.PARACHUTE && speed <= minFall() * FastFoodPhysics.SAFE_LANDING_FACTOR
}

object FastFoodPhysics {
    const val TICK_MILLIS = 16L // BaseJumpEngine.§_-DG§
    const val SAFE_LANDING_FACTOR = 1.3
    internal const val DT = TICK_MILLIS / 1000.0

    /**
     * Os seis cursos, na ordem do FallingObjectType (soup, pizza, burger, tray, cake, tray). Os leves aguentam o
     * paraquedas perto da mesa; os pesados pedem abrir lá pelo meio; o bolo demora mais.
     */
    val FOODS = listOf(
        FastFoodFood(0, gravity = 0.32, decel = 1.6, minFall = 0.15, bigMinFall = 0.08, timerTicks = 150),
        FastFoodFood(1, gravity = 0.32, decel = 1.5, minFall = 0.15, bigMinFall = 0.08, timerTicks = 170),
        FastFoodFood(2, gravity = 0.34, decel = 1.0, minFall = 0.16, bigMinFall = 0.08, timerTicks = 200),
        FastFoodFood(3, gravity = 0.36, decel = 0.65, minFall = 0.17, bigMinFall = 0.09, timerTicks = 240),
        FastFoodFood(4, gravity = 0.38, decel = 0.55, minFall = 0.18, bigMinFall = 0.09, timerTicks = 290),
        FastFoodFood(5, gravity = 0.36, decel = 0.65, minFall = 0.17, bigMinFall = 0.09, timerTicks = 240),
    )

    /** O curso de quem tem `score` pontos. */
    fun foodFor(score: Int) = FOODS[score.coerceAtMost(FOODS.size - 1)]

    /** A altura mínima em que o paraquedas normal ainda pousa o prato inteiro. */
    val SAFE_OPEN_HEIGHT: Map<Int, Double> = FOODS.associate { food ->
        var h = 0.0
        while (h < 1.0 && !landsSafely(food, h)) h += 0.01
        food.id to h
    }

    /** Se o prato, solto do alto, pousa inteiro com o paraquedas aberto em openAt (o grande, se big). */
    fun landsSafely(food: FastFoodFood, openAt: Double, big: Boolean = false): Boolean {
        val p = FastFoodPlate().apply { reset(food) }
        p.state = PlateState.FALLING
        while (true) {
            if (p.state == PlateState.FALLING && p.locY <= openAt) {
                p.state = PlateState.PARACHUTE
                p.bigChute = big
            }
            if (p.step()) return p.touchedDownSafely()
        }
    }

    internal fun num(v: Double) = String.format(Locale.ROOT, "%.6f", v)
}
