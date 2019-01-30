/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_PERIODICALLY_LONG)
class WiredTriggerPeriodicallyLong(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var delay = 0
    private var delayState = 0

    init {
        roomItem.wiredData?.let {
            delay = if (it.options.size == 1) it.options[0] * 10 else 10
        }
    }

    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean {
        if (++delayState >= delay) {
            delayState = 0

            return true
        }

        return false
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        roomItem.wiredData?.let {
            habboRequest.readInt() // useless?
            delay = habboRequest.readInt()

            if (delay < 0) delay = 1
            else if (delay > 120) delay = 120

            it.options = listOf(delay)

            delayState = 0
            delay *= 10

            return true
        }

        return false
    }
}