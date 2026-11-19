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
import ovh.rwx.fastfood.game.FastFoodPowerUp.BIG_PARACHUTE
import ovh.rwx.fastfood.game.FastFoodPowerUp.MISSILE
import ovh.rwx.fastfood.game.FastFoodPowerUp.SHIELD

class FastFoodPowerUpInventoryTest {
    private val saved = mutableListOf<Pair<FastFoodPowerUp, Int>>()

    private fun inventory(vararg amounts: Pair<FastFoodPowerUp, Int>) =
        FastFoodPowerUpInventory(FastFoodPowerUp.entries.associateWith { 0 }.toMutableMap().apply { putAll(amounts) }) { t, n ->
            saved += t to n
        }

    @Test
    fun aFirstTimePlayerGetsTheStarterPackAndItIsSaved() {
        val (amounts, firstTime) = FastFoodPowerUpInventory.fromRows(emptyMap())
        assertTrue(firstTime)
        assertEquals(mapOf(BIG_PARACHUTE to 0, MISSILE to 3, SHIELD to 5), amounts)

        // quem já jogou não ganha de novo, mesmo zerado
        val (again, firstAgain) = FastFoodPowerUpInventory.fromRows(mapOf("missile" to 0, "shield" to 0, "bigparachute" to 0))
        assertFalse(firstAgain)
        assertEquals(0, again.getValue(MISSILE))
    }

    @Test
    fun aGameAllowsAtMostThreeOfEachFromTheStock() {
        val inv = inventory(MISSILE to 25, SHIELD to 2)
        assertEquals(mapOf(BIG_PARACHUTE to 0, MISSILE to 3, SHIELD to 2), inv.allowance())
    }

    @Test
    fun usingAPowerUpSpendsTheGameAllowanceAndTheStock() {
        val inv = inventory(MISSILE to 4)
        val player = FastFoodGamePlayer(null, 1, "ana", "", "F", allowance = inv.allowance(), inventory = inv)

        repeat(3) { assertTrue(player.usePowerUp(MISSILE)) }
        assertFalse("a partida libera só 3", player.usePowerUp(MISSILE))
        assertEquals(1, inv.amount(MISSILE))
        assertEquals(listOf(MISSILE to 3, MISSILE to 2, MISSILE to 1), saved)
        assertFalse(player.usePowerUp(SHIELD))
    }

    @Test
    fun aPurchaseAddsToTheStockAndIsSaved() {
        val inv = inventory(MISSILE to 1)
        inv.add(MISSILE, 25)
        assertEquals(26, inv.amount(MISSILE))
        assertEquals(listOf(MISSILE to 26), saved)
        assertEquals(mapOf(0 to 0, 1 to 26, 2 to 0), inv.snapshot())
    }
}
