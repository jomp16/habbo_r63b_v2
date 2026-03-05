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

package ovh.rwx.habbo.game.room.tasks

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerUserPerformsAction
import ovh.rwx.habbo.game.room.IRoomTask
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

enum class UserAction(val action: Int) {
    NONE(0),
    WAVE(1),
    BLOW_KISS(2),
    LAUGH(3),
    UNKNOWN(4),
    IDLE(5),
    JUMP(6),
    THUMB_UP(7);

    companion object {
        fun fromValue(value: Int): UserAction {
            for (action in UserAction.entries) {
                if (action.action == value) {
                    return action
                }
            }

            return NONE
        }
    }
}

class UserActionTask(private val roomUser: RoomUser, private val action: UserAction) : IRoomTask {
    override fun executeTask(room: Room) {
        if (action == UserAction.IDLE) {
            roomUser.idle = true

            return
        }

        roomUser.idle = false
        roomUser.handItem = 0
        roomUser.danceId = 0

        room.sendHabboResponse(Outgoing.ROOM_USER_ACTION, roomUser.virtualID, action.action)

        // Now for R63A
        when (action) {
            UserAction.WAVE -> {
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_WAVE, roomUser.virtualID, action.action)
            }

            else -> {}
        }

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerUserPerformsAction::class,
            roomUser,
            WiredTriggerUserPerformsAction.WiredUserAction.fromUserAction(action)
        )
    }
}