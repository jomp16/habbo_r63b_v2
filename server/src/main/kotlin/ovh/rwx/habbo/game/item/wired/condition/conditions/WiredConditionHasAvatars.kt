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

package ovh.rwx.habbo.game.item.wired.condition.conditions

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.condition.WiredConditionType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@WiredItemInteractor(InteractionType.WIRED_CONDITION_HAS_AVATARS, InteractionType.WIRED_CONDITION_HAS_NO_AVATARS)
class WiredConditionHasAvatars(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_HAS_NO_AVATARS

    init {
        setData()
    }

    override fun setData() {
        // No additional data to load
    }

    override fun onCondition(roomUser: RoomUser?): Boolean {
        val items = roomItem.wiredData?.items ?: return false
        if (items.isEmpty()) return false

        val hasAvatars = items.all { itemId ->
            val roomItem = room.roomItems[itemId] ?: return false
            roomItem.affectedTiles.any { tile ->
                room.roomUsers.values.any { user -> user.currentVector3.vector2 == tile }
            }
        }

        return if (isNegative) !hasAvatars else hasAvatars
    }


    override fun writeDialog(habboResponse: HabboResponse, wiredData: WiredData) {
        habboResponse.apply {
            writeItems(wiredData)
            writeItemInfo(roomItem)
            writeEmptySettings()
            writeInt(if (isNegative) WiredConditionType.NOT_FURNI_HAVE_HABBO.code else WiredConditionType.FURNI_HAVE_HABBO.code)
        }
    }
}