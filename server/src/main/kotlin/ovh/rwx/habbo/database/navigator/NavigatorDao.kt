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

import ovh.rwx.habbo.game.navigator.NavigatorEventCategory
import ovh.rwx.habbo.game.navigator.NavigatorRoomCategory
import ovh.rwx.habbo.database.*

object NavigatorDao {
    fun getNavigatorRoomCategories(): List<NavigatorRoomCategory> = db {
        query<NavigatorRoomCategory>(
            "sql/navigator/categories/room/select_room_categories.sql",
            mapOf("enabled" to true)
        )
    }

    fun getNavigatorEventCategories(): List<NavigatorEventCategory> = db {
        query<NavigatorEventCategory>(
            "sql/navigator/categories/event/select_event_categories.sql",
            mapOf("visible" to true)
        )
    }

    fun addFavoriteRoom(userId: Int, roomId: Int): Int {
        return db {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/navigator/favorite/insert_favorite_room.sql").readText(),
                    mapOf(
                            "user_id" to userId,
                            "room_id" to roomId
                    )
            )
        }
    }

    fun removeFavoriteRoom(id: Int) {
        db {
            update(javaClass.classLoader.getResource("sql/navigator/favorite/delete_favorite_room.sql").readText(),
                    mapOf(
                            "id" to id
                    )
            )
        }
    }
}
