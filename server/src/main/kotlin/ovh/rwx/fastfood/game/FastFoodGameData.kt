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

/**
 * DTOs dos eventos in-game do BaseJump (wire confirmado nos parsers AS3).
 * O seq SEMPRE vem antes do playerId no wire (getters §_-Jq§/§_-1q§ dos parsers).
 */

data class DropFoodData(
    val seq: Int,
    val playerId: Int,
    val locY: String,
    val speed: String,
    val state: Int,
    val falseStarted: Boolean
)

/**
 * nextPlate do engine consome só o playerId; os 3 ints finais são cosméticos
 * (estados de prato §_-HF§ 0..6, tipo de comida, posição).
 */
data class NextPlateData(
    val seq: Int,
    val playerId: Int,
    val plateState: Int,
    val foodTypeId: Int,
    val platePos: Int
)

/** POWER_UP_GAINED: count vem ANTES do type no wire (parser §_-9Y§). */
data class PowerUpEventData(
    val count: Int,
    val type: Int
)

/** END_GAME: vencedor é o ÚLTIMO entry (parser §_-FL§ sobrescreve §_-I3§ a cada iteração). */
data class EndGameData(
    val scores: List<Pair<Int, Int>>
)
