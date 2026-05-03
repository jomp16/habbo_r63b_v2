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

package ovh.rwx.habbo.game.pet

data class HorseExtraData(
    var hairStyle: Int = -1,
    var hairColor: Int = 0,
    var hasSaddle: Boolean = false,
    var anyoneCanRide: Boolean = false,
    var saddleItemId: Int = 0
)

data class MonsterPlantExtraData(
    var breedable: Boolean = false,
    var allowBreed: Boolean = false,
    var isDead: Boolean = false,
    var deathTimestamp: Int = 0,
    var rarity: Int = 0,
    var mpType: Int = 0,
    var mpColor: Int = 0,
    var mpNose: Int = 0,
    var mpNoseColor: Int = 0,
    var mpEyes: Int = 0,
    var mpEyesColor: Int = 0,
    var mpMouth: Int = 0,
    var mpMouthColor: Int = 0
)
