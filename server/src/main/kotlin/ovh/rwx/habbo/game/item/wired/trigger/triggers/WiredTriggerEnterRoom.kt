/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

class WiredTriggerEnterRoom(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var username = ""

    init {
        roomItem.wiredData?.let {
            username = it.message
        }
    }

    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean {
        if (roomItem.wiredData == null) return false

        return username.isBlank() || roomUser != null && username == roomUser.habboSession!!.userInformation.username
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        roomItem.wiredData?.let {
            habboRequest.readInt() // useless?
            username = habboRequest.readUTF()
            it.message = username

            return true
        }

        return false
    }
}