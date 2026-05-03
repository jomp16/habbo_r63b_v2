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

package ovh.rwx.habbo.game.item.interactors

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemInteractor
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
class WiredItemInteractor : ItemInteractor() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override val interactionType = InteractionType.entries.filter { it.name.startsWith("WIRED") }

    override fun onPlace(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem) {
        super.onPlace(room, roomEntity, roomItem)

        roomItem.extraData = "0"
    }

    override fun onRemove(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem) {
        super.onRemove(room, roomEntity, roomItem)

        roomItem.extraData = "0"
    }

    override fun onTrigger(room: Room, roomEntity: RoomEntity?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomEntity, roomItem, hasRights, request)

        if (!hasRights || roomItem.wiredData == null) return

        roomItem.extraData = "1"
        roomItem.update(updateDb = false, updateClient = true)
        roomItem.requestTicks(1)

        (roomEntity as? RoomUser)?.habboSession?.let { habboSession ->
            val wiredInstance = HabboServer.habboGame.itemManager.getWiredInstance(roomItem.room, roomItem)

            if (wiredInstance != null) {
                val outgoing = if (habboSession.release == "R63A") {
                    when {
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_TRIGGER") -> OutgoingR63A.WIRED_TRIGGER_DIALOG
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT") -> OutgoingR63A.WIRED_EFFECT_DIALOG
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION") -> OutgoingR63A.WIRED_CONDITION_DIALOG
                        else -> {
                            habboSession.sendNotification("WIRED isn't available for your release.")
                            return
                        }
                    }
                } else {
                    when {
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_TRIGGER") -> Outgoing.WIRED_TRIGGER_DIALOG
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT") -> Outgoing.WIRED_EFFECT_DIALOG
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION") -> Outgoing.WIRED_CONDITION_DIALOG
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_EXTRA") -> Outgoing.WIRED_ADDON_DIALOG
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_SELECTOR") -> Outgoing.WIRED_SELECTOR_DIALOG
                        roomItem.furnishing.interactionType.name.startsWith("WIRED_VARIABLE") -> Outgoing.WIRED_VARIABLE_DIALOG
                        else -> return
                    }
                }

                habboSession.sendAnyResponse(outgoing, roomItem, roomItem.wiredData)
            } else {
                val message = "Wired ${roomItem.furnishing.interactionType.name} not found"
                log.error(message)

                if (habboSession.userInformation.ambassador) {
                    habboSession.sendNotification(message)
                }
            }
        }
    }

    override fun processTick(room: Room, roomItem: RoomItem) {
        super.processTick(room, roomItem)

        if (roomItem.extraData == "1") {
            roomItem.extraData = "0"

            roomItem.update(updateDb = false, updateClient = true)
        }
    }
}