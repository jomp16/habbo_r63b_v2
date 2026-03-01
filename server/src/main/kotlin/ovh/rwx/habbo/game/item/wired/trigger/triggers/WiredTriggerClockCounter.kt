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
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room
import java.time.Duration

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_CLOCK_COUNTER)
class WiredTriggerClockCounter(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var duration = Duration.ZERO

    init {
        setData()
    }

    // todo
    override fun code() = WiredTriggerType.CLOCK_REACH_TIME.code

    override fun setData() {
        roomItem.wiredData?.let {
            val seconds = it.options.getOrElse(0) { 0 }
            val minutes = it.options.getOrElse(1) { 0 }
            val fractionalSeconds = it.options.getOrElse(2) { 0 }

            duration = Duration
                .ofMinutes(minutes.toLong())
                .plusSeconds(seconds.toLong())
                .plusMillis(if (fractionalSeconds > 0) fractionalSeconds * 500.toLong() else 0)
        }
    }

    override fun onTrigger(wiredContext: WiredContext, data: Any?): Boolean {
        // todo
        return false
        /*val clockTime = data as? Int ?: 0
        return clockTime >= targetTime*/
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }
}
