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
import ovh.rwx.habbo.communication.outgoing.navigator.NavigatorOfficialRoomsData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomType
import ovh.rwx.habbo.game.room.navigator.NavigatorGuestRoomsMode
import ovh.rwx.habbo.game.room.navigator.NavigatorListPayload
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Buscas do old navigator (2013 R63B legado, RELEASE63-201305221112) por searchType.
 * Cada aba/dropdown do navigator antigo manda um composer próprio
 * (ver MainViewCtrl._SafeStr_7727 no AS3 2013: searchType -> composer).
 * Todas respondem com o mesmo GuestRoomSearchResult (Outgoing.NAVIGATOR_LIST_ROOMS).
 */
@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorSearchTypeHandler {
    @Handler(
        Incoming.NAVIGATOR_MY_FAVORITES,
        Incoming.NAVIGATOR_MY_HISTORY,
        Incoming.NAVIGATOR_FRIENDS_ROOMS,
        Incoming.NAVIGATOR_FRIENDS_IN_ROOMS,
        Incoming.NAVIGATOR_LATEST_EVENTS
    )
    @HandlerR63A(IncomingR63A.NAVIGATOR_LATEST_EVENTS)
    fun handleSearchByMode(habboSession: HabboSession, habboRequest: HabboRequest) {
        val mode = when {
            !habboRequest.isIncomingInitialized -> NavigatorGuestRoomsMode.LATEST_EVENTS
            habboRequest.incoming == Incoming.NAVIGATOR_MY_FAVORITES -> NavigatorGuestRoomsMode.MY_FAVORITES
            habboRequest.incoming == Incoming.NAVIGATOR_MY_HISTORY -> NavigatorGuestRoomsMode.MY_VISITED_ROOMS
            habboRequest.incoming == Incoming.NAVIGATOR_FRIENDS_ROOMS -> NavigatorGuestRoomsMode.FRIENDS_ROOM
            habboRequest.incoming == Incoming.NAVIGATOR_FRIENDS_IN_ROOMS -> NavigatorGuestRoomsMode.FRIENDS_IN_ROOMS
            else -> NavigatorGuestRoomsMode.LATEST_EVENTS
        }

        habboSession.sendResponse(
            Outgoing.NAVIGATOR_LIST_ROOMS,
            OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            NavigatorListPayload(rooms = roomsForMode(habboSession, mode), mode = mode)
        )
    }

    @Handler(Incoming.NAVIGATOR_HIGHEST_SCORE_ROOMS)
    fun handleHighestScore(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboRequest.readInt() // nodeId (categoria selecionada), ignorado: ranking é global

        val mode = NavigatorGuestRoomsMode.HIGHEST_SCORE

        habboSession.sendResponse(
            Outgoing.NAVIGATOR_LIST_ROOMS,
            OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            NavigatorListPayload(rooms = roomsForMode(habboSession, mode), mode = mode)
        )
    }

    @Handler(Incoming.NAVIGATOR_GUILD_BASE_SEARCH)
    fun handleOfficialRooms(habboSession: HabboSession, habboRequest: HabboRequest) {
        val mode = NavigatorGuestRoomsMode.OFFICIAL

        habboSession.sendResponse(
            Outgoing.NAVIGATOR_LIST_ROOMS,
            OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            NavigatorListPayload(rooms = roomsForMode(habboSession, mode), mode = mode)
        )
    }

    @Handler(Incoming.NAVIGATOR_PROMO_ROOMS)
    fun handlePromoRooms(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboRequest.readInt() // roomId do promo
        habboRequest.readInt() // categoryId

        val mode = NavigatorGuestRoomsMode.PROMO

        habboSession.sendResponse(
            Outgoing.NAVIGATOR_LIST_ROOMS,
            OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            NavigatorListPayload(rooms = roomsForMode(habboSession, mode), mode = mode)
        )
    }

    @Handler(Incoming.NAVIGATOR_TAG_SEARCH)
    fun handleTagSearch(habboSession: HabboSession, habboRequest: HabboRequest) {
        val tag = habboRequest.readUTF()
        val mode = NavigatorGuestRoomsMode.TAG_SEARCH

        habboSession.sendResponse(
            Outgoing.NAVIGATOR_LIST_ROOMS,
            OutgoingR63A.NAVIGATOR_LIST_ROOMS,
            NavigatorListPayload(
                rooms = HabboServer.habboGame.roomManager.rooms.values
                    .filter { it.roomData.roomType == RoomType.PRIVATE && it.roomData.tags.any { t -> t.equals(tag, ignoreCase = true) } }
                    .sortedByDescending { it.userManager.entities.values.count { e -> e is RoomUser } },
                mode = mode,
                query = tag
            )
        )
    }

    companion object {
        /**
         * Resolve os quartos por searchType do old navigator. Compartilhado com o
         * NavigatorListRoomsHandler (composer genérico _-2fh@293 manda o searchType no body).
         *
         * // ponytail: MY_HISTORY/LATEST_EVENTS/OFFICIAL/PROMO retornam vazio (histórico de
         * visitas e sistema de eventos/promos 2013 ainda não modelados); ligar quando existirem.
         */
        fun roomsForMode(habboSession: HabboSession, mode: NavigatorGuestRoomsMode): List<Room> {
            val roomManager = HabboServer.habboGame.roomManager

            return when (mode) {
                NavigatorGuestRoomsMode.MY_ROOMS -> roomManager.getRoomsByOwner(habboSession.userInformation.id)
                NavigatorGuestRoomsMode.MY_FAVORITES -> habboSession.favoritesRooms.mapNotNull { roomManager.rooms[it.second] }
                NavigatorGuestRoomsMode.POPULAR -> roomManager.getPopularRooms()
                NavigatorGuestRoomsMode.HIGHEST_SCORE -> roomManager.rooms.values
                    .filter { it.roomData.roomType == RoomType.PRIVATE && it.running && it.roomData.score > 0 }
                    .sortedByDescending { it.roomData.score }
                    .take(40)
                NavigatorGuestRoomsMode.TEXT_SEARCH, NavigatorGuestRoomsMode.TAG_SEARCH -> emptyList() // têm handler próprio com query
                NavigatorGuestRoomsMode.MY_VISITED_ROOMS,
                NavigatorGuestRoomsMode.FRIENDS_ROOM,
                NavigatorGuestRoomsMode.FRIENDS_IN_ROOMS -> friendRooms(habboSession, mode)
                // Pré-shuffle: a aba official lê a lista do GuestRoomSearchResult (451) — devolve os
            // quartos públicos carregados como aproximação de "official".
            // ponytail: sem conceito real de quartos oficiais; trocar por fonte dedicada quando existir
            NavigatorGuestRoomsMode.OFFICIAL -> roomManager.rooms.values
                .filter { it.roomData.roomType == RoomType.PUBLIC }

            else -> emptyList() // LATEST_EVENTS, PROMO, ROOM_ADS_TOP/NEW: sem sistema de eventos ainda
            }
        }

        private fun friendRooms(habboSession: HabboSession, @Suppress("UNUSED_PARAMETER") mode: NavigatorGuestRoomsMode): List<Room> {
            val friendIds = habboSession.habboMessenger.friends.keys

            return HabboServer.habboGame.roomManager.rooms.values
                .filter { it.roomData.ownerId in friendIds }
                .sortedByDescending { it.userManager.entities.values.count { e -> e is RoomUser } }
        }
    }
}
