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

package ovh.rwx.habbo.game.room.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.room.tasks.UserChatTask
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector3
import java.util.*

class RoomUser(
    val habboSession: HabboSession,
    room: Room,
    virtualID: Int,
    currentVector3: Vector3,
    headRotation: Int,
    bodyRotation: Int
) : RoomHumanoid(room, virtualID, currentVector3, headRotation, bodyRotation) {

    override fun onMovementStepCommitted() {
        HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_LegDay", 1, accumulate = true)
    }

    fun chat(
        virtualID: Int,
        message: String,
        bubble: RoomChatMessageBubbles,
        type: RoomChatType,
        skipCommands: Boolean
    ) {
        room.addTask(UserChatTask(this, virtualID, message, bubble, type, skipCommands))
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(habboSession.userInformation.id)
            writeUTF(habboSession.userInformation.username)
            writeUTF(habboSession.userInformation.motto)
            writeUTF(habboSession.userInformation.figure)
            writeInt(virtualID)
            writeInt(currentVector3.x)
            writeInt(currentVector3.y)
            writeUTF(currentVector3.z.toString())
            writeInt(0) // 4 or 2 ?
            writeInt(1) // 1 for user, 2 for pet, 3 for bot.
            writeUTF(habboSession.userInformation.gender.lowercase(Locale.getDefault()))

            val group = habboSession.userStats.favoriteGroup

            if (group == null) {
                writeInt(-1)
                writeInt(0)
                writeUTF("")
            } else {
                writeInt(group.groupData.id)
                writeInt(0)
                writeUTF(group.groupData.name)
            }

            writeUTF("")
            writeInt(habboSession.userStats.achievementScore)
            writeBoolean(habboSession.habboSubscription.hasBuildersClub) // is member of builder club

            if (isVersionAtLeast(2026, 8, 6)) {
                writeInt(0) // todo: badgesRank
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(habboSession.userInformation.id)
            writeUTF(habboSession.userInformation.username)
            writeUTF(habboSession.userInformation.motto)
            writeUTF(habboSession.userInformation.figure)
            writeInt(virtualID)
            writeInt(currentVector3.x)
            writeInt(currentVector3.y)
            writeUTF(currentVector3.z.toString())
            writeInt(bodyRotation)
            writeInt(1) // 1 for user, 2 for pet, 3 for bot.
            writeUTF(habboSession.userInformation.gender.lowercase(Locale.getDefault()))

//                val group = habboSession.userStats.favoriteGroup

            writeInt(-1) // xp
//                    if (group == null) {
            writeInt(-1)
            writeInt(-1)
            writeUTF("")
                // bugged as fuck
//                    } else {
//                        writeInt(group.groupData.id)
//                        writeInt(-1)
//                        writeUTF(group.groupData.name)
//                    }

            if (isVersionAtLeast(2010, 12, 3)) {
                writeInt(habboSession.userStats.achievementScore)
            }
        }
    }
}
