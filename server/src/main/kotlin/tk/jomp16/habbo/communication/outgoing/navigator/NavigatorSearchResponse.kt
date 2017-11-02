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

package tk.jomp16.habbo.communication.outgoing.navigator

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.navigator.NavigatorEventCategory
import tk.jomp16.habbo.game.navigator.NavigatorRoomCategory
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.RoomType
import tk.jomp16.habbo.game.user.HabboSession
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorSearchResponse {
    // todo: rewrite it to be more generic enough
    @Response(Outgoing.NAVIGATOR_SEARCH)
    fun response(habboResponse: HabboResponse, habboSession: HabboSession, category: String, searchTerm: String) {
        habboResponse.apply {
            writeUTF(category)
            writeUTF(searchTerm)
            writeInt(if (!searchTerm.isBlank()) 1 else getNewNavigatorLength(category))
        }

        if (!searchTerm.isBlank()) serializeSearches(searchTerm, habboResponse)
        else serializeSearchResultList(category, true, habboResponse, habboSession)
    }

    private fun serializeSearchResultList(category: String, direct: Boolean, habboResponse: HabboResponse, habboSession: HabboSession) {
        habboResponse.apply {
            val staticId: String = category

            if (staticId != "hotel_view" && staticId != "roomads_view" && staticId != "myworld_view" && staticId != "official_view") {
                writeUTF(staticId) // code to be appended to navigator.searchcode.title.%s
                writeUTF("") // title if nothing found ^
                writeInt(1) // 0 : no button - 1 : Show More - 2 : Show Back button
                writeBoolean(staticId != "my" && staticId != "popular" && staticId != "official") // collapsed or not
                writeInt(if (staticId == "official" || staticId == "popular") 1 else 0) // 0 : list - 1 : thumbnail
            }

            when (staticId) {
                "hotel_view" -> {
                    serializeSearchResultList("popular", false, habboResponse, habboSession)
                    serializeSearchResultList("recommended", false, habboResponse, habboSession)

                    HabboServer.habboGame.navigatorManager.navigatorRoomCategories.values.forEach {
                        serializeRoomCategories(it, direct, habboResponse)
                    }
                }
                "myworld_view" -> {
                    serializeSearchResultList("my", false, habboResponse, habboSession)
                    serializeSearchResultList("favorites", false, habboResponse, habboSession)
                    serializeSearchResultList("history", false, habboResponse, habboSession)
                    serializeSearchResultList("friends_rooms", false, habboResponse, habboSession)
                    serializeSearchResultList("history_freq", false, habboResponse, habboSession)
                }
                "roomads_view" -> {
                    serializeSearchResultList("top_promotions", false, habboResponse, habboSession)

                    HabboServer.habboGame.navigatorManager.navigatorEventCategories.values.forEach {
                        serializeEvents(it, direct, habboResponse)
                    }
                }
                "official_view" -> {
                    serializeSearchResultList("official", false, habboResponse, habboSession)
                    serializeSearchResultList("recommended", false, habboResponse, habboSession)
                }
                "my" -> {
                    val rooms: MutableList<Room> = ArrayList()

                    habboSession.rooms.forEachIndexed { i, room ->
                        rooms.add(room)

                        if (!direct && i + 1 == 8) return@forEachIndexed
                    }

                    writeInt(rooms.size)
                    rooms.sortedByDescending { it.roomData.id }.forEach { serialize(it, false, false) }
                }
                "popular" -> {
                    HabboServer.habboGame.roomManager.rooms.values.filter { it.roomTask != null && it.roomUsers.isNotEmpty() }
                            .sortedBy { it.roomUsers.size }.take(8).let {
                        writeInt(it.size)

                        it.forEach { habboResponse.serialize(it, false, false) }
                    }
                }
                "favorites" -> {
                    val rooms: MutableList<Room> = ArrayList()

                    habboSession.favoritesRooms.map { it.second }.forEachIndexed { i, roomId ->
                        rooms.add(HabboServer.habboGame.roomManager.rooms[roomId]!!)

                        if (!direct && i + 1 == 8) return@forEachIndexed
                    }

                    writeInt(rooms.size)
                    rooms.sortedByDescending { it.roomData.id }.forEach { serialize(it, false, false) }
                }
                else -> writeInt(0)
            }
        }
    }

    private fun serializeSearches(searchTerm: String, habboResponse: HabboResponse) {
        habboResponse.apply {
            // actually, official Habbo search isn't like this, but whatever
            writeUTF("")
            writeUTF(searchTerm)
            writeInt(2)
            writeBoolean(false)
            writeInt(0)
            val rooms: List<Room> = HabboServer.habboGame.roomManager.rooms.values.filter {
                when {
                    it.roomData.roomType == RoomType.PUBLIC -> false
                    searchTerm.startsWith("owner:") -> it.roomData.ownerName == searchTerm.substring(6)
                    searchTerm.startsWith("tag:") -> it.roomData.tags.any {
                        it == searchTerm.substring(4)
                    }
                    searchTerm.startsWith("roomname:") -> it.roomData.name == searchTerm.substring(9)
                    it.roomData.ownerName.matches("(?i:.*$searchTerm.*)".toRegex()) -> true
                    it.roomData.name.matches("(?i:.*$searchTerm.*)".toRegex()) -> true
                    it.roomData.description.matches("(?i:.*$searchTerm.*)".toRegex()) -> true
                    else -> it.roomData.tags.any {
                        it.toLowerCase().matches("(?i:.*$searchTerm.*)".toRegex())
                    }
                }
            }.sortedByDescending { it.roomUsers.size }

            writeInt(rooms.size)

            rooms.forEach { serialize(it, false, false) }
        }
    }

    private fun serializeRoomCategories(navigatorRoomCategory: NavigatorRoomCategory, direct: Boolean, habboResponse: HabboResponse) {
        habboResponse.apply {
            writeUTF("")
            writeUTF(navigatorRoomCategory.caption)
            writeInt(0)
            writeBoolean(true)
            writeInt(0)

            HabboServer.habboGame.roomManager.rooms.values.filter { it.roomTask != null && it.roomUsers.isNotEmpty() }
                    .filter { it.roomData.category == navigatorRoomCategory.id }
                    .sortedBy { it.roomUsers.size }.take(8).let {
                writeInt(it.size)

                it.forEach { habboResponse.serialize(it, false, false) }
            }
        }
    }

    private fun serializeEvents(navigatorEventCategory: NavigatorEventCategory, direct: Boolean, habboResponse: HabboResponse) {
        habboResponse.apply {
            writeUTF("")
            writeUTF(navigatorEventCategory.caption)
            writeInt(0)
            writeBoolean(true)
            writeInt(0)
            writeInt(0) // todo: rooms
        }
    }

    private fun getNewNavigatorLength(category: String): Int = when (category) {
        "official_view" -> 2
        "myworld_view" -> 4
        "hotel_view" -> HabboServer.habboGame.navigatorManager.navigatorRoomCategories.size + 2
        "roomads_view" -> HabboServer.habboGame.navigatorManager.navigatorEventCategories.size + 1
        else -> 1
    }
}