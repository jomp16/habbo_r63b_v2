/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.interactors.banzai

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.user.RoomUserEffect

@Suppress("unused")
class BanzaiGateItemInteractor() : ItemInteractor() {
    override val interactionType = InteractionType.values().filter { it.name.startsWith("BATTLE_BANZAI_GATE") }

    private val banzaiEffectId = 32

    private fun getColor(interactionType: InteractionType): Int {
        return when (interactionType) {
            InteractionType.BATTLE_BANZAI_GATE_RED -> 1
            InteractionType.BATTLE_BANZAI_GATE_GREEN -> 2
            InteractionType.BATTLE_BANZAI_GATE_BLUE -> 3
            InteractionType.BATTLE_BANZAI_GATE_YELLOW -> 4
            else -> 0
        }
    }

    override fun onUserWalksOn(room: Room, roomUser: RoomUser, roomItem: RoomItem) {
        super.onUserWalksOn(room, roomUser, roomItem)

        val effectId = banzaiEffectId + getColor(roomItem.furnishing.interactionType)

        if (roomUser.effect?.effectId != effectId) {
            roomUser.effect = RoomUserEffect(effectId, Integer.MAX_VALUE)
        } else {
            roomUser.effect = null
        }
    }
}