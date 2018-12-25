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

package ovh.rwx.habbo.communication.outgoing.group

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.group.Group

@Suppress("unused", "UNUSED_PARAMETER")
class GroupManageResponse {
    @Response(Outgoing.GROUP_MANAGE)
    fun response(habboResponse: HabboResponse, group: Group) {
        habboResponse.apply {
            writeInt(0)
            writeBoolean(true)
            writeInt(group.groupData.id)
            writeUTF(group.groupData.name)
            writeUTF(group.groupData.description)
            writeInt(group.groupData.roomId)
            writeInt(group.groupData.symbolColor)
            writeInt(group.groupData.backgroundColor)
            writeInt(group.groupData.membershipState.state)
            writeInt(if (group.groupData.onlyAdminCanDecorateRoom) 1 else 0)
            writeBoolean(false)
            writeUTF("")
            writeInt(5)

            val split = group.groupData.badge.split('s')
            val baseCode = split[0].substring(1)
            val symbolsCode = split.drop(1)

            // base
            val baseParts = HabboServer.habboGame.groupManager.getParts(baseCode, false)
            writeInt(baseParts[0].toInt())
            writeInt(baseParts[1].toInt())
            writeInt(baseParts[2].toInt())
            // end base
            // symbols
            symbolsCode.forEach {
                val parts = HabboServer.habboGame.groupManager.getParts(it, true)

                writeInt(parts[0].toInt())
                writeInt(parts[1].toInt())
                writeInt(parts[2].toInt())
            }

            // pad the remaining badges
            if (split.size < 5) {
                (0 until 5 - split.size).forEach { i ->
                    writeInt(0)
                    writeInt(0)
                    writeInt(0)
                }
            }

            writeUTF(group.groupData.badge)
            writeInt(group.members.size)
        }
    }
}