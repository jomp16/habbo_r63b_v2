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
@WiredItemInteractor(InteractionType.WIRED_CONDITION_TIME_MORE_THAN, InteractionType.WIRED_CONDITION_TIME_LESS_THAN)
class WiredConditionTime(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private var targetTicks: Int = 21
    private val isLessThan = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_TIME_LESS_THAN

    init {
        setData()
    }

    override fun code() =
        if (isLessThan) WiredConditionType.TIME_ELAPSED_LESS.code else WiredConditionType.TIME_ELAPSED_MORE.code

    override fun setData() {
        roomItem.wiredData?.let {
            targetTicks = it.options.getOrElse(0) { targetTicks }
        }
    }

    override fun onCondition(wiredContext: WiredContext): Boolean {
        val currentTick = room.roomTimer.get()

        return if (isLessThan) {
            currentTick < targetTicks
        } else {
            currentTick > targetTicks
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(21), "")
        }
    }
}