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

package ovh.rwx.habbo.database.user

import ovh.rwx.habbo.database.db

object UserUniqueIdDao {
    @Suppress("unused")
    fun getUniqueIdsForUser(userId: Int): List<String> = db {
        query<String>(
            "sql/users/unique_ids/select_unique_ids.sql",
            mapOf("user_id" to userId)
        )
    }

    fun addUniqueIdForUser(userId: Int, uniqueID: String, osInformation: String) {
        db {
            update("sql/users/unique_ids/insert_unique_ids.sql",
                mapOf(
                    "user_id" to userId,
                    "unique_id" to uniqueID,
                    "os_information" to osInformation
                )
            )
        }
    }

    fun containsUniqueIdForUser(userId: Int, uniqueID: String): Boolean = db {
        queryOne<Boolean>(
            "sql/users/unique_ids/select_count_unique_id.sql",
            mapOf("user_id" to userId, "unique_id" to uniqueID)
        ) ?: false
    }
}
