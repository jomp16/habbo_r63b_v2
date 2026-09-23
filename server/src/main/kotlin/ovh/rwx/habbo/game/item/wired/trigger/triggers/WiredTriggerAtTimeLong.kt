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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.EmptyTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_TRIGGER_AT_TIME_LONG)
class WiredTriggerAtTimeLong(room: Room, roomItem: RoomItem) : WiredTrigger<EmptyTriggerData>(room, roomItem) {
    private var targetTime = 10
    private var hasTriggered = false

    override val requiresItems = true

    init {
        setData()
    }

    override fun code() = WiredTriggerType.TRIGGER_ONCE.code

    override fun setData() {
        roomItem.wiredData?.let {
            val rawTime = it.options.getOrElse(0) { 1 }.coerceIn(1, 1200)
            targetTime = rawTime * 10
        }
    }

    override fun onTrigger(wiredContext: WiredContext, data: EmptyTriggerData): Boolean {
        val currentTime = room.roomTimer.get()

        if (currentTime < targetTime) {
            hasTriggered = false
            return false
        }

        if (!hasTriggered) {
            hasTriggered = true
            return true
        }

        return false
    }

    override fun resetTriggered() {
        hasTriggered = false
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1), "")
        }
    }
}
