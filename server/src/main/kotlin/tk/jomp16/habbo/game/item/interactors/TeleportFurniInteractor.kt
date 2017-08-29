/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.game.item.interactors

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.trigger.triggers.WiredTriggerStateChanged
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.util.Vector3

class TeleportFurniInteractor : ItemInteractor() {
    override fun onPlace(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onPlace(room, roomUser, roomItem)

        roomItem.extraData = "0"

        HabboServer.habboGame.itemManager.roomTeleportLinks.put(roomItem.id, room.roomData.id)
    }

    override fun onRemove(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onRemove(room, roomUser, roomItem)

        roomItem.extraData = "0"

        HabboServer.habboGame.itemManager.roomTeleportLinks.remove(roomItem.id)
    }

    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (roomUser == null) return

        if (!roomItem.isTouching(roomUser.currentVector3, roomUser.bodyRotation, roomItem.position.z)) {
            roomUser.moveTo(roomItem.getFrontPosition(), roomItem.getFrontRotation(), actingItem = roomItem)

            return
        }

        if (roomItem.extraData == "0" && roomItem.interactingUsers.isEmpty()) {
            roomItem.interactingUsers.put(1, roomUser)

            roomUser.walkingBlocked = true
            roomUser.moveTo(roomItem.position.vector2, ignoreBlocking = true)

            roomItem.extraData = "1"
            roomItem.update(false, true)
            roomItem.requestCycles(2)
        }

        room.wiredHandler.triggerWired(WiredTriggerStateChanged::class, roomUser, roomItem)
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)
        val outgoingUser = roomItem.interactingUsers[1]
        val incomingUser = roomItem.interactingUsers[2]
        var extraData = "0"

        if (outgoingUser?.habboSession != null) {
            if (roomItem.extraData == "2") {
                if (outgoingUser.habboSession.teleporting) outgoingUser.habboSession.enterRoom(outgoingUser.habboSession.teleportRoom!!, bypassAuth = true)

                roomItem.interactingUsers.remove(1)
            } else {
                val teleportTwoId = HabboServer.habboGame.itemManager.teleportLinks[roomItem.id] ?: return
                var targetRoomItem: RoomItem? = null
                var showUpdate = false

                if (room.roomItems.containsKey(teleportTwoId)) {
                    showUpdate = true

                    targetRoomItem = room.roomItems[teleportTwoId] ?: return
                } else {
                    HabboServer.habboGame.roomManager.rooms[HabboServer.habboGame.itemManager.roomTeleportLinks[teleportTwoId]]?.let {
                        showUpdate = true

                        outgoingUser.habboSession.targetTeleporterId = teleportTwoId
                        outgoingUser.habboSession.teleportRoom = it
                    }
                }

                when {
                    outgoingUser.currentVector3.vector2 != roomItem.position.vector2 -> {
                        outgoingUser.walkingBlocked = false

                        roomItem.interactingUsers.remove(1)
                    }
                    showUpdate -> {
                        extraData = "2"

                        if (targetRoomItem != null) {
                            val newVector3 = Vector3(targetRoomItem.position.vector2, room.roomGamemap.getAbsoluteHeight(targetRoomItem.position.vector2))

                            room.roomGamemap.updateRoomUserMovement(outgoingUser, outgoingUser.currentVector3.vector2, newVector3.vector2)

                            outgoingUser.currentVector3 = newVector3
                            outgoingUser.headRotation = targetRoomItem.rotation
                            outgoingUser.bodyRotation = targetRoomItem.rotation
                            outgoingUser.updateNeeded = true

                            if (targetRoomItem.extraData != "2") {
                                targetRoomItem.extraData = "2"
                                targetRoomItem.update(false, true)
                                targetRoomItem.requestCycles(2)
                            }

                            targetRoomItem.interactingUsers.put(2, outgoingUser)
                            roomItem.interactingUsers.remove(1)
                        }

                        roomItem.requestCycles(1)
                    }
                    else -> {
                        extraData = "1"

                        outgoingUser.moveTo(roomItem.getFrontPosition(), ignoreBlocking = true)
                        outgoingUser.walkingBlocked = false

                        roomItem.interactingUsers.remove(1)
                        roomItem.requestCycles(2)
                    }
                }
            }
        }

        if (incomingUser?.habboSession != null) {
            if (incomingUser.currentVector3.vector2 != roomItem.position.vector2) {
                incomingUser.walkingBlocked = false
                roomItem.interactingUsers.remove(2)
            } else {
                extraData = "1"

                incomingUser.moveTo(roomItem.getFrontPosition(), ignoreBlocking = true)
                incomingUser.walkingBlocked = false

                roomItem.interactingUsers.remove(2)
                roomItem.requestCycles(1)
            }
        }

        if (roomItem.extraData != extraData) {
            roomItem.extraData = extraData
            roomItem.update(false, true)
        }
    }
}