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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastFoodPhysicsTest {
    private fun fallTicks(food: FastFoodFood): Int {
        val p = FastFoodPlate().apply { reset(food) }
        p.state = PlateState.FALLING
        var ticks = 1
        while (!p.step()) ticks++
        return ticks
    }

    @Test
    fun freeFallTakesAboutTwoAndAHalfSeconds() {
        // sqrt(2 / g): 2,5 s para a sopa (g 0,32), menos para o bolo, mais pesado
        assertEquals(2.5, fallTicks(FastFoodPhysics.FOODS[0]) * 0.016, 0.05)
        assertTrue(fallTicks(FastFoodPhysics.FOODS[4]) < fallTicks(FastFoodPhysics.FOODS[0]))
    }

    @Test
    fun heavyFoodNeedsTheParachuteHigher() {
        val h = FastFoodPhysics.SAFE_OPEN_HEIGHT
        // leves perto da mesa, pesados lá pelo meio
        assertTrue("soup ${h[0]}", h.getValue(0) in 0.1..0.3)
        assertTrue("cake ${h[4]}", h.getValue(4) in 0.35..0.6)
        assertTrue(h.getValue(4) > h.getValue(2) && h.getValue(2) > h.getValue(0))
    }

    @Test
    fun aLateNormalParachuteSmashesWhereTheBigOneSaves() {
        val cake = FastFoodPhysics.FOODS[4]
        val late = FastFoodPhysics.SAFE_OPEN_HEIGHT.getValue(4) / 3
        assertFalse(FastFoodPhysics.landsSafely(cake, late))
        assertTrue(FastFoodPhysics.landsSafely(cake, late, big = true))
        // sem paraquedas nenhum, espatifa
        assertFalse(FastFoodPhysics.landsSafely(cake, -1.0))
    }

    @Test
    fun theWinnerIsTheFirstEndGameEntry() {
        fun player(id: Int, score: Int) = FastFoodGamePlayer(null, id, "p$id", "", "M").also { it.score = score }
        val human = player(1, 2)
        val winner = player(-1, 6)
        val second = player(-2, 4)

        // o parser §_-FL§ toma a entrada 0 como vencedor: nunca o humano com menos pontos
        assertEquals(listOf(-1 to 6, -2 to 4, 1 to 2), FastFoodGame.endGameScores(listOf(human, winner, second), winner))
    }
}
