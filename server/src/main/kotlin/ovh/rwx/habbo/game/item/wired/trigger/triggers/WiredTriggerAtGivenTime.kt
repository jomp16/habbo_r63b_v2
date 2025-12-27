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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_AT_GIVEN_TIME)
class WiredTriggerAtGivenTime(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var targetTime = 5
    private var hasTriggered = false

    init {
        setData()
    }

    override fun setData() {
        roomItem.wiredData?.let {
            targetTime = it.options.getOrElse(0) { targetTime }
        }
    }

    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean {
        val currentTime = room.roomTimer.get()

        if (currentTime >= targetTime && !hasTriggered) {
            hasTriggered = true
            return true
        }

        if (currentTime < targetTime) {
            hasTriggered = false
        }

        return false
    }


    fun resetTimer() {
        hasTriggered = false
    }

    override fun writeDialog(habboResponse: HabboResponse, wiredData: WiredData) {
        habboResponse.apply {
            writeEmptyItems()
            writeItemInfo(roomItem)
            writeSettings(wiredData.message, wiredData.options, 1)
            writeInt(WiredTriggerType.AT_GIVEN_TIME.code)
            writeBlockedActions(wiredData)
        }
    }

    companion object {
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(5), "")
        }
    }
}