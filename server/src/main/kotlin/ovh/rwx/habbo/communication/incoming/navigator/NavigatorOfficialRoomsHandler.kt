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

package ovh.rwx.habbo.communication.incoming.navigator

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.navigator.*
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorOfficialRoomsHandler {
    // Pós-shuffle 2013: aba official recicla o GetGuestRoom (504) — responde o OfficialRoomsEvent 1263
    @Handler(Incoming.NAVIGATOR_OFFICIAL_ROOMS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        // Aba official: encerra o loading e popula a lista do OfficialRoomListCtrl
        habboSession.sendResponse(
            Outgoing.NAVIGATOR_OFFICIAL_ROOMS_RESULT,
            null,
            buildMockOfficialRooms(habboSession)
        )
    }

    // Pré-shuffle: GetOfficialRoomsMessageComposer (380) → OfficialRoomsEvent 450,
    // wire próprio da era (_SafeStr_325, RELEASE34): entries são quartos completos
    @HandlerR63A(IncomingR63A.NAVIGATOR_OFFICIAL_ROOMS)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        val randomRoom = HabboServer.habboGame.roomManager.rooms.values.randomOrNull()
        habboSession.sendResponse(
            null,
            OutgoingR63A.NAVIGATOR_OFFICIAL_ROOMS_RESULT,
            NavigatorOfficialRoomsData(
                entries = listOfNotNull(randomRoom).mapIndexed { index, room ->
                    OfficialRoomEntry(
                        index = index,
                        name = room.roomData.name,
                        description = room.roomData.description,
                        showDetails = true,
                        picText = "",
                        picLink = "",
                        parentIndex = -1,
                        userCount = room.userManager.entities.values.filterIsInstance<RoomUser>().size,
                        type = OfficialRoomEntryType.GUEST_ROOM,
                        guestRoom = room
                    )
                }
            )
        )
    }

    // ponytail: dados mockados cobrindo todos os branches do parser — trocar por fonte real quando existir
    private fun buildMockOfficialRooms(habboSession: HabboSession): NavigatorOfficialRoomsData {
        val randomRoom = HabboServer.habboGame.roomManager.rooms.values.randomOrNull()
        return NavigatorOfficialRoomsData(
            entries = listOf(
                OfficialRoomEntry(
                    index = 0,
                    name = "Welcome Lounge",
                    description = "Dando boas-vindas",
                    showDetails = true,
                    picText = "",
                    picLink = "officialrooms_defaults/hh_room_dustylounge.png", // relativo ao image.library.url (c_images/)
                    parentIndex = -1,
                    userCount = 3,
                    type = OfficialRoomEntryType.TAG,
                    tag = "welcome"
                ),
                OfficialRoomEntry(
                    index = 1,
                    name = "Praça Pública",
                    description = "Quarto público mockado",
                    showDetails = true,
                    picText = "", // legenda sobre a imagem (tooltip text)
                    picLink = "officialrooms_defaults/hh_room_park_general.png",
                    parentIndex = -1,
                    userCount = 5,
                    type = OfficialRoomEntryType.PUBLIC_ROOM,
                    publicRoom = OfficialRoomPublicData(
                        name = "praça_publica",
                        enterBytes = 0,
                        owner = 0,
                        imageNames = listOf("hh_room_park_general"), // pngs em officialrooms_defaults/
                        usersNow = 5,
                        maxUsers = 50
                    )
                ),
                OfficialRoomEntry(
                    index = 2,
                    name = "Quarto Aleatório",
                    description = "GuestRoomData mockado",
                    showDetails = true,
                    parentIndex = -1,
                    userCount = randomRoom?.userManager?.entities?.values?.filterIsInstance<RoomUser>()?.size ?: 0,
                    type = if (randomRoom != null) OfficialRoomEntryType.GUEST_ROOM else OfficialRoomEntryType.NONE,
                    guestRoom = randomRoom
                ),
                OfficialRoomEntry(
                    index = 3,
                    name = "Pasta",
                    description = "Folder mockado",
                    showDetails = false,
                    parentIndex = -1,
                    userCount = 0,
                    type = OfficialRoomEntryType.FOLDER
                ),
                // filhos da pasta 0: só aparecem quando "Entry NONE" está aberto
                OfficialRoomEntry(
                    index = 4,
                    name = "Tag filha",
                    parentIndex = 3,
                    type = OfficialRoomEntryType.TAG,
                    tag = "filha"
                )
            ),
            promoAds = listOf(
                OfficialRoomEntry(
                    index = 0,
                    name = "Promo Ad",
                    description = "Anúncio da header",
                    picLink = "officialrooms_defaults/hh_room_cafe.png",
                    type = OfficialRoomEntryType.NONE // row simples do ad_footer, não pasta
                )
            ),
            promotedRooms = listOf(
                PromotedRoom(
                    code = "majesty", // promotedroomcategory.majesty no external texts
                    figure = HabboServer.habboGame.figureManager.generateRandomFigure(), // avatar do header da categoria
                    room = randomRoom,
                    extraRooms = HabboServer.habboGame.roomManager.rooms.values
                        .filter { it !== randomRoom }
                        .take(10)
                )
            )
        )
    }
}
