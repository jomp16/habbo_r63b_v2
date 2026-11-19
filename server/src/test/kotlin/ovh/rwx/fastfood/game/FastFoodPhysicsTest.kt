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
    private fun fallTicks(food: FastFoodGame.Food): Int {
        val p = FastFoodGame.Plate().apply { reset(food) }
        p.state = FastFoodGame.FALLING
        var ticks = 1
        while (!p.step()) ticks++
        return ticks
    }

    @Test
    fun freeFallTakesAboutTwoAndAHalfSeconds() {
        // sqrt(2 / g): 2,5 s para a sopa (g 0,32), menos para o bolo, mais pesado
        assertEquals(2.5, fallTicks(FastFoodGame.FOODS[0]) * 0.016, 0.05)
        assertTrue(fallTicks(FastFoodGame.FOODS[4]) < fallTicks(FastFoodGame.FOODS[0]))
    }

    @Test
    fun heavyFoodNeedsTheParachuteHigher() {
        val h = FastFoodGame.SAFE_OPEN_HEIGHT
        // leves perto da mesa, pesados lá pelo meio
        assertTrue("soup ${h[0]}", h.getValue(0) in 0.1..0.3)
        assertTrue("cake ${h[4]}", h.getValue(4) in 0.35..0.6)
        assertTrue(h.getValue(4) > h.getValue(2) && h.getValue(2) > h.getValue(0))
    }

    @Test
    fun aLateNormalParachuteSmashesWhereTheBigOneSaves() {
        val cake = FastFoodGame.FOODS[4]
        val late = FastFoodGame.SAFE_OPEN_HEIGHT.getValue(4) / 3
        assertFalse(FastFoodGame.landsSafely(cake, late))
        assertTrue(FastFoodGame.landsSafely(cake, late, big = true))
        // sem paraquedas nenhum, espatifa
        assertFalse(FastFoodGame.landsSafely(cake, -1.0))
    }
}
