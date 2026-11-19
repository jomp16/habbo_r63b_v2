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

import ovh.rwx.fastfood.game.bot.FastFoodBot

/** Os power-ups, com o tipo do wire (§_-DQ§). */
enum class FastFoodPowerUp(val wire: Int) {
    BIG_PARACHUTE(0),
    MISSILE(1),
    SHIELD(2),
}

class FastFoodGamePlayer(
    val session: FastFoodSession?,
    val id: Int,
    val name: String,
    val figure: String,
    val gender: String,
    /** O cérebro de um bot; null num humano. */
    val bot: FastFoodBot? = null
) {
    val isBot get() = bot != null

    var score = 0
    internal val plate = FastFoodPlate()

    /** O temporizador vermelho (§_-BL§): soltar com ticks restantes é largada antecipada. */
    internal var timerTicks = 0

    private val powerUps = FastFoodPowerUp.entries.associateWithTo(mutableMapOf()) { FastFoodGame.POWER_UPS_PER_GAME }

    fun hasPowerUp(type: FastFoodPowerUp) = powerUps.getValue(type) > 0

    /** Gasta um power-up; false se não tem. */
    fun usePowerUp(type: FastFoodPowerUp): Boolean {
        if (!hasPowerUp(type)) return false
        powerUps[type] = powerUps.getValue(type) - 1
        return true
    }
}
