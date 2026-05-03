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

package ovh.rwx.habbo.plugin.listeners.room.item

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.wired.WiredRewardNotificationResponse
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.plugin.api.PluginListener
import ovh.rwx.habbo.plugin.event.events.room.annotation.Command

@Suppress("unused", "UNUSED_PARAMETER")
class RoomItemCommandsListener : PluginListener() {
    @Command(["giveitem"], permissionName = "cmd_giveitem")
    fun giveItem(room: Room, roomUser: RoomUser, args: List<String>) {
        if (args.size <= 1) {
            roomUser.chat(
                roomUser.virtualID,
                "Excepted params: item_name or item_name username",
                RoomChatMessageBubbles.ALERT,
                RoomChatType.WHISPER,
                true
            )

            return
        }

        val itemName = args[1]
        val username = if (args.size >= 3) args[2] else null
        val furnishing = HabboServer.habboGame.itemManager.furnishings[itemName]

        if (furnishing == null) {
            roomUser.chat(
                roomUser.virtualID,
                "$itemName not found, try another item",
                RoomChatMessageBubbles.ALERT,
                RoomChatType.WHISPER,
                true
            )

            return
        }

        val extraData =
            HabboServer.habboGame.itemManager.correctExtradataCatalog(roomUser.habboSession, "", furnishing) ?: ""
        val userId =
            if (username == null) roomUser.habboSession.userInformation.id else UserInformationDao.getUserInformationByUsername(
                username
            )?.id

        if (userId == null) {
            roomUser.chat(
                roomUser.virtualID,
                "We couldn't find the user $username!",
                RoomChatMessageBubbles.ALERT,
                RoomChatType.WHISPER,
                true
            )

            return
        }

        val userItems = ItemDao.addItems(
            userId,
            listOf(ItemPurchaseData(furnishing, extraData, limited = false, buildersClub = false)),
        )

        HabboServer.habboSessionManager.getHabboSessionById(userId)?.let { habboSession ->
            habboSession.habboInventory.addItems(userItems)

            if (habboSession.release == "R63A") {
                habboSession.sendNotification("You got a new item, check your inventory.")
            } else {
                habboSession.sendHabboResponse(
                    Outgoing.WIRED_REWARD_NOTIFICATION,
                    WiredRewardNotificationResponse.WiredRewardNotification.ITEM_REWARDED
                )
            }
        }
    }
}