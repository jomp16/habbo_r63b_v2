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

@WiredItemInteractor(InteractionType.WIRED_CONDITION_WEARING_BADGE, InteractionType.WIRED_CONDITION_NOT_WEARING_BADGE)
class WiredConditionWearingBadge(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private var badgeCode: String = ""
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_NOT_WEARING_BADGE

    init {
        setData()
    }

    override fun setData() {
        roomItem.wiredData?.let {
            badgeCode = it.message
        }
    }

    override fun onCondition(roomUser: RoomUser?): Boolean {
        if (roomUser == null || badgeCode.isBlank()) return false

        val equippedBadges = roomUser.habboSession?.habboBadge?.badges?.values?.filter { it.slot > 0 } ?: emptyList()
        val hasBadge = equippedBadges.any { it.code == badgeCode }

        return if (isNegative) !hasBadge else hasBadge
    }

    override fun writeDialog(habboResponse: HabboResponse, wiredData: WiredData) {
        habboResponse.apply {
            writeEmptyItems()
            writeItemInfo(roomItem)
            writeSettings(wiredData.message, emptyList(), 0)
            writeInt(if (isNegative) WiredConditionType.NOT_ACTOR_WEARS_BADGE.code else WiredConditionType.ACTOR_WEARS_BADGE.code)
        }
    }

    companion object {
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}