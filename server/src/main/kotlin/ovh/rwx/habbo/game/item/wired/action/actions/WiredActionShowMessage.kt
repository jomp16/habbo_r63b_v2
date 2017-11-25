/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.wired.action.actions

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredDelayEvent
import ovh.rwx.habbo.game.item.wired.action.WiredAction
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.ChatType
import ovh.rwx.habbo.game.room.tasks.WiredDelayTask
import ovh.rwx.habbo.game.room.user.RoomUser

class WiredActionShowMessage(room: Room, roomItem: RoomItem) : WiredAction(room, roomItem) {
    private var message: String = ""
    private var delay: Int = 0

    init {
        roomItem.wiredData?.let {
            delay = it.delay
            message = it.message
        }
    }

    override fun handle(roomUser: RoomUser?) {
        if (delay > 0) {
            room.roomTask?.addTask(room, WiredDelayTask(WiredDelayEvent(this, roomUser)))

            return
        }

        handleThing(roomUser)
    }

    override fun handle(event: WiredDelayEvent) {
        super.handle(event)

        if (event.counter.incrementAndGet() >= delay) {
            event.finished = true

            handleThing(event.roomUser)
        }
    }

    private fun handleThing(roomUser: RoomUser?) {
        if (roomUser != null && !message.isBlank()) roomUser.chat(roomUser.virtualID, message, 1, ChatType.WHISPER, true)
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        roomItem.wiredData?.let {
            habboRequest.readInt() // useless?
            message = habboRequest.readUTF().trim()

            if (message.length > 100) message = message.substring(0, 100)

            habboRequest.readInt() // useless?
            delay = habboRequest.readInt()

            if (delay < 0) delay = 0
            if (delay > 20) delay = 20

            it.delay = delay
            it.message = message

            return true
        }

        return false
    }
}