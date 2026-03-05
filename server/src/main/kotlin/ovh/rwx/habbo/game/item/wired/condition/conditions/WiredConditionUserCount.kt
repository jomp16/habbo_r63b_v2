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

package ovh.rwx.habbo.game.item.wired.condition.conditions

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.condition.WiredConditionType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_CONDITION_USER_COUNT_IN, InteractionType.WIRED_CONDITION_NOT_USER_COUNT)
class WiredConditionUserCount(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private var minUsers: Int = 1
    private var maxUsers: Int = 50
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_NOT_USER_COUNT

    init {
        setData()
    }

    override fun code() =
        if (isNegative) WiredConditionType.NOT_USER_COUNT_IN.code else WiredConditionType.USER_COUNT_IN.code

    override fun setData() {
        roomItem.wiredData?.let {
            minUsers = it.options.getOrElse(0) { minUsers }
            maxUsers = it.options.getOrElse(1) { maxUsers }
        }
    }

    override fun onCondition(wiredContext: WiredContext): Boolean {
        val userCount = room.userManager.users.size
        val inRange = userCount in minUsers..maxUsers
        return if (isNegative) !inRange else inRange
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1, 50), "")
        }
    }
}