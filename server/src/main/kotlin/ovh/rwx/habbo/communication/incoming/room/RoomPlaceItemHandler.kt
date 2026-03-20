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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector2
import kotlin.math.abs

@Suppress("unused", "UNUSED_PARAMETER")
class RoomPlaceItemHandler {
    @Handler(Incoming.ROOM_PLACE_ITEM, Incoming.ROOM_PLACE_POST_IT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
        // floor = [0][7]3 8 4 2
        // wall  = [0][19]2 :w=2,11 l=11,36 l
        // postit = [0][0][0]2[0][16]:w=4,7 l=11,11 l
        if (!habboSession.currentRoom?.userManager?.hasRights(habboSession)!!) {
            habboSession.sendSuperNotification(
                MiscSuperNotificationResponse.MiscSuperNotificationKeys.FURNITURE_PLACEMENT_ERROR,
                mapOf("message" to $$"${room.error.cant_set_not_owner}")
            )

            return
        }
        val rawDataSplit: List<String>
        val itemId: Int
        var postIt = false

        if (habboRequest.incoming == Incoming.ROOM_PLACE_POST_IT) {
            itemId = abs(habboRequest.readInt())
            val extraData = habboRequest.readUTF().trim().split(' ')

            rawDataSplit = listOf("", extraData[0], extraData[1], extraData[2])

            postIt = true
        } else {
            rawDataSplit = habboRequest.readUTF().trim().split(' ')
            itemId = abs(rawDataSplit[0].toInt())
        }

        finishStuff(itemId, postIt, rawDataSplit, habboSession)
    }

    @HandlerR63A(IncomingR63A.ROOM_PLACE_ITEM, IncomingR63A.ROOM_PLACE_POST_IT)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
        // floor = [0][7]3 8 4 2
        // wall  = [0][19]2 :w=2,11 l=11,36 l
        // postit = [0][0][0]2[0][16]:w=4,7 l=11,11 l
        if (!habboSession.currentRoom?.userManager?.hasRights(habboSession)!!) {
            habboSession.sendNotification($$"${room.error.cant_set_not_owner}")

            return
        }
        val rawDataSplit: List<String>
        val itemId: Int
        var postIt = false

        if (habboRequest.incomingR63A == IncomingR63A.ROOM_PLACE_POST_IT) {
            itemId = abs(habboRequest.readInt())
            val extraData = habboRequest.readUTF().trim().split(' ')

            rawDataSplit = listOf("", extraData[0], extraData[1], extraData[2])

            postIt = true
        } else {
            rawDataSplit = habboRequest.readUTF().trim().split(' ')
            itemId = abs(rawDataSplit[0].toInt())
        }

        finishStuff(itemId, postIt, rawDataSplit, habboSession)
    }

    private fun finishStuff(itemId: Int, postIt: Boolean, rawDataSplit: List<String>, habboSession: HabboSession) {
        if (postIt) {
            // Check if room has more or equals than 50 post it
            if (habboSession.currentRoom!!.itemManager.wallItems.values.count { it.furnishing.interactionType == InteractionType.POST_IT } >= 50) {
                habboSession.sendNotification($$"${room.error.max_stickies}")

                return
            }
        }

        val userItem = habboSession.habboInventory.items[itemId]

        if (userItem == null) {
            habboSession.sendNotification($$"${room.error.cant_set_item}")
            return
        }

        val success: Boolean

        if (userItem.furnishing.type == ItemType.WALL) {
            // parse wall data
            val correctedWallData = rawDataSplit.drop(1)

            if (correctedWallData.size < 3) return
            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(
                habboSession.currentRoom!!.roomData.id,
                userItem
            )

            success =
                habboSession.currentRoom!!.itemManager.setWallItem(roomItem, correctedWallData, habboSession.roomUser)
        } else {
            // parse floor data
            if (rawDataSplit.size < 4) return
            val x = rawDataSplit[1].toInt()
            val y = rawDataSplit[2].toInt()
            val rot = rawDataSplit[3].toInt()
            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(
                habboSession.currentRoom!!.roomData.id,
                userItem
            )

            success =
                habboSession.currentRoom!!.itemManager.setFloorItem(roomItem, Vector2(x, y), rot, habboSession.roomUser)
        }

        if (success) {
            habboSession.habboInventory.removeItems(listOf(itemId))

            // ACH_PlaceCreditValue: colocar moedas de câmbio no quarto
            if (userItem.itemName.startsWith("CF_") || userItem.itemName.startsWith("CFC_")) {
                val split = userItem.itemName.split('_')
                val creditValue = if (split.size > 2 && split[1] == "diamond") {
                    split[2].toIntOrNull() ?: 0
                } else if (split.size > 1) {
                    split[1].toIntOrNull() ?: 0
                } else {
                    0
                }
                if (creditValue > 0) {
                    HabboServer.habboGame.achievementManager.progress(
                        habboSession,
                        "ACH_PlaceCreditValue",
                        creditValue,
                        accumulate = true
                    )
                }
            }

            // ACH_RoomDecoFurniCount: quantidade total de mobis no quarto
            habboSession.currentRoom?.let { room ->
                val totalFurni = room.itemManager.items.size
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_RoomDecoFurniCount",
                    totalFurni,
                    accumulate = false
                )

                // ACH_RoomDecoFurniTypeCount: quantidade de tipos diferentes de mobis
                val uniqueFurniTypes = room.itemManager.items.values.map { it.furnishing.itemName }.toSet().size
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_RoomDecoFurniTypeCount",
                    uniqueFurniTypes,
                    accumulate = false
                )
            }

            // Por mover, girar, escolher ou colocar Mobis nos seus quartos.
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_Tutorial5", 1, false)
        } else {
            habboSession.sendNotification($$"${room.error.cant_set_item}")
        }
    }
}