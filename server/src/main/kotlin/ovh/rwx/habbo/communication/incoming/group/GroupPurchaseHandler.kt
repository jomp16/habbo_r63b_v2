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

package ovh.rwx.habbo.communication.incoming.group

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class GroupPurchaseHandler {
    private val credits: Int = 10

    @Handler(Incoming.GROUP_PURCHASE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        // [0][4]aaaa[0][4]aaaa[0][0][0][2][0][0][0]�[0][0][0][1][0][0][0][6][0][0][0][8][0][0][0][1][0][0][0][4][0][0][0][26][0][0][0][11][0][0][0][4]

        // how much credits to remove from user?
        if (habboSession.userInformation.credits < credits) return

        val name = habboRequest.readUTF()
        val description = habboRequest.readUTF()
        val roomId = habboRequest.readInt()
        val room = HabboServer.habboGame.roomManager.rooms[roomId] ?: return

        if (room.group != null) {
            return
        }

        val symbolColor = habboRequest.readInt()
        val backgroundColor = habboRequest.readInt()
        val badgePartsSize = habboRequest.readInt()
        val badgeParts = mutableListOf<Int>()

        repeat(badgePartsSize) {
            badgeParts += habboRequest.readInt()
        }

        val groupBadge = HabboServer.habboGame.groupManager.generateBadge(badgeParts)

        val group = HabboServer.habboGame.groupManager.createGroup(name, description, groupBadge, habboSession.userInformation.id, roomId, symbolColor, backgroundColor)

        room.roomData.groupId = group.groupData.id
        habboSession.userStats.favoriteGroupId = group.groupData.id
        habboSession.userInformation.credits -= credits
        habboSession.updateAllCurrencies()

        habboSession.sendHabboResponse(Outgoing.GROUP_PURCHASED, roomId, group.groupData.id)

        // Add new group to room loaded groups
        room.loadedGroups.add(group)

        room.sendHabboResponse(Outgoing.ROOM_INFO, habboSession, room, true, true)
        room.sendHabboResponse(Outgoing.GROUP_BADGES, room.loadedGroups)

        if (room != habboSession.currentRoom) {
            // Forward user to room
            habboSession.sendHabboResponse(Outgoing.ROOM_FORWARD, roomId)
        } else {
            // should update favorite group?
            room.sendHabboResponse(Outgoing.GROUP_UPDATE_FAVORITE, habboSession.roomUser!!.virtualID, group.groupData.id, group.groupData.name, 3)
        }
    }
}
