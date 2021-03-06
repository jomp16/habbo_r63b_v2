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

package ovh.rwx.habbo.database.navigator

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.navigator.NavigatorEventCategory
import ovh.rwx.habbo.game.navigator.NavigatorRoomCategory
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey

object NavigatorDao {
    fun getNavigatorRoomCategories(): List<NavigatorRoomCategory> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/navigator/categories/room/select_room_categories.sql").readText(),
                mapOf(
                        "enabled" to true
                )
        ) {
            NavigatorRoomCategory(
                    it.int("id"),
                    it.string("caption"),
                    it.int("min_rank")
            )
        }
    }

    fun getNavigatorEventCategories(): List<NavigatorEventCategory> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/navigator/categories/event/select_event_categories.sql").readText(),
                mapOf(
                        "visible" to true
                )
        ) {
            NavigatorEventCategory(
                    it.int("id"),
                    it.string("caption"),
                    it.int("min_rank")
            )
        }
    }

    fun addFavoriteRoom(userId: Int, roomId: Int): Int {
        return HabboServer.database {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/navigator/favorite/insert_favorite_room.sql").readText(),
                    mapOf(
                            "user_id" to userId,
                            "room_id" to roomId
                    )
            )
        }
    }

    fun removeFavoriteRoom(id: Int) {
        HabboServer.database {
            update(javaClass.classLoader.getResource("sql/navigator/favorite/delete_favorite_room.sql").readText(),
                    mapOf(
                            "id" to id
                    )
            )
        }
    }
}