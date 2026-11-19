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

/** Os turbos, com o tipo do wire e o nome (§_-DQ§) que o cliente usa na loja e o banco guarda. */
enum class FastFoodPowerUp(val wire: Int, val code: String) {
    BIG_PARACHUTE(0, "bigparachute"),
    MISSILE(1, "missile"),
    SHIELD(2, "shield");

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code }
    }
}

/**
 * Um lugar na partida.
 *
 * @param allowance quantos turbos de cada tipo a partida libera (o GAME_STARTING manda isso ao cliente).
 * @param inventory o estoque de onde sai cada turbo usado; null num bot.
 */
class FastFoodGamePlayer(
    val session: FastFoodSession?,
    val id: Int,
    val name: String,
    val figure: String,
    val gender: String,
    /** O cérebro de um bot; null num humano. */
    val bot: FastFoodBot? = null,
    allowance: Map<FastFoodPowerUp, Int> = FastFoodPowerUp.entries.associateWith { FastFoodGame.BOT_POWER_UPS },
    private val inventory: FastFoodPowerUpInventory? = null,
    /** Nível de cada conquista que o cliente lê no GAME_STARTING (nome sem `ACH_` → nível); vazio num bot. */
    val achievementLevels: List<Pair<String, Int>> = emptyList()
) {
    val isBot get() = bot != null

    var score = 0
    internal val plate = FastFoodPlate()

    /** Um humano que saiu no meio da partida: não recebe mais pacotes. */
    internal var left = false

    /** O temporizador vermelho (§_-BL§): soltar com ticks restantes é largada antecipada. */
    internal var timerTicks = 0

    private val powerUps = allowance.toMutableMap()

    /** O que ainda resta liberado nesta partida, no formato do GAME_STARTING (tipo do wire → quantidade). */
    fun powerUpsWire(): Map<Int, Int> = FastFoodPowerUp.entries.associate { it.wire to powerUps.getValue(it) }

    fun hasPowerUp(type: FastFoodPowerUp) = powerUps.getValue(type) > 0

    /** Gasta um turbo: da cota da partida e, num humano, do estoque. False se não tem. */
    fun usePowerUp(type: FastFoodPowerUp): Boolean {
        if (!hasPowerUp(type)) return false
        if (inventory != null && !inventory.take(type)) return false
        powerUps[type] = powerUps.getValue(type) - 1
        return true
    }
}
