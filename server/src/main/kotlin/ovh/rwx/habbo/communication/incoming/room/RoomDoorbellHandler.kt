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
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomDoorbellHandler {
    @Handler(Incoming.ROOM_DOORBELL)
    @HandlerR63A(IncomingR63A.ROOM_DOORBELL)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        this.parse(habboSession, habboRequest)
    }

    @Handler(Incoming.ROOM_DOORBELL)
    fun handleWithRoomId(habboSession: HabboSession, habboRequest: HabboRequest) {
        this.parse(habboSession, habboRequest)
    }

    private fun parse(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
        val username = habboRequest.readUTF()
        val accept = habboRequest.readBoolean()
        val requestHabboSession = HabboServer.habboSessionManager.getHabboSessionByUsername(username)

        if (requestHabboSession == null || requestHabboSession.currentRoom != habboSession.currentRoom) return

        val isR63A = requestHabboSession.release == "R63A"

        if (accept) {
            if (isR63A) {
                requestHabboSession.sendHabboResponse(OutgoingR63A.ROOM_DOORBELL_ACCEPT, "")
            } else {
                val methodName = HabboServer.habboHandler.getOverrideMethodForHeader(
                    Outgoing.ROOM_DOORBELL_ACCEPT,
                    requestHabboSession.release
                )
                if (methodName == "response") {
                    requestHabboSession.sendHabboResponse(Outgoing.ROOM_DOORBELL_ACCEPT, "")
                } else if (methodName == "responseWithRoomId") {
                    requestHabboSession.sendHabboResponse(
                        Outgoing.ROOM_DOORBELL_ACCEPT,
                        habboSession.currentRoom!!.roomData.id,
                        ""
                    )
                    requestHabboSession.enterRoom(habboSession.currentRoom!!, "", true)
                }
            }

            habboSession.currentRoom?.userManager?.usersWithRights?.forEach {
                (it as? RoomUser)?.habboSession?.let { rightsHabboSession ->
                    val isR63ARights = rightsHabboSession.release == "R63A"
                    val methodName = HabboServer.habboHandler.getOverrideMethodForHeader(
                        Outgoing.ROOM_DOORBELL_ACCEPT,
                        rightsHabboSession.release
                    )

                    when {
                        isR63ARights -> {
                            rightsHabboSession.sendHabboResponse(
                                OutgoingR63A.ROOM_DOORBELL_ACCEPT,
                                habboSession.userInformation.username
                            )
                        }
                        methodName == "response" -> {
                            rightsHabboSession.sendHabboResponse(
                                Outgoing.ROOM_DOORBELL_ACCEPT,
                                habboSession.userInformation.username
                            )
                        }
                        methodName == "responseWithRoomId" -> {
                            rightsHabboSession.sendHabboResponse(
                                Outgoing.ROOM_DOORBELL_ACCEPT,
                                habboSession.currentRoom!!.roomData.id,
                                habboSession.userInformation.username
                            )
                        }
                    }
                }
            }
        } else {
            requestHabboSession.currentRoom = null

            if (isR63A) {
                requestHabboSession.sendHabboResponse(OutgoingR63A.ROOM_DOORBELL_DENIED, "")
            } else {
                val methodName = HabboServer.habboHandler.getOverrideMethodForHeader(
                    Outgoing.ROOM_DOORBELL_ACCEPT,
                    requestHabboSession.release
                )

                if (methodName == "response") {
                    requestHabboSession.sendHabboResponse(Outgoing.ROOM_DOORBELL_DENIED, "")
                } else if (methodName == "responseWithRoomId") {
                    requestHabboSession.sendHabboResponse(
                        Outgoing.ROOM_DOORBELL_DENIED,
                        habboSession.currentRoom!!.roomData.id,
                        ""
                    )
                }
            }

            habboSession.currentRoom?.userManager?.usersWithRights?.forEach {
                (it as? RoomUser)?.habboSession?.let { rightsHabboSession ->
                    val isR63ARights = rightsHabboSession.release == "R63A"
                    val methodName = HabboServer.habboHandler.getOverrideMethodForHeader(
                        Outgoing.ROOM_DOORBELL_DENIED,
                        rightsHabboSession.release
                    )

                    when {
                        isR63ARights -> {
                            rightsHabboSession.sendHabboResponse(
                                OutgoingR63A.ROOM_DOORBELL_DENIED,
                                habboSession.userInformation.username
                            )
                        }
                        methodName == "response" -> {
                            rightsHabboSession.sendHabboResponse(
                                Outgoing.ROOM_DOORBELL_DENIED,
                                habboSession.userInformation.username
                            )
                        }
                        methodName == "responseWithRoomId" -> {
                            rightsHabboSession.sendHabboResponse(
                                Outgoing.ROOM_DOORBELL_DENIED,
                                habboSession.currentRoom!!.roomData.id,
                                habboSession.userInformation.username
                            )
                        }
                    }
                }
            }
        }
    }
}