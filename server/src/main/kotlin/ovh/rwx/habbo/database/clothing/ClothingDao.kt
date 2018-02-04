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

package ovh.rwx.habbo.database.clothing

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey

object ClothingDao {
    fun getClothings(userId: Int): List<String> = HabboServer.database {
        select("SELECT `item_name` FROM `users_clothing` WHERE `user_id` = :user_id",
                mapOf(
                        "user_id" to userId
                )
        ) {
            it.string("item_name")
        }
    }

    fun addClothing(userId: Int, itemName: String) {
        HabboServer.database {
            insertAndGetGeneratedKey("INSERT INTO `users_clothing` (`user_id`, `item_name`) VALUES (:user_id, :item_name)",
                    mapOf(
                            "user_id" to userId,
                            "item_name" to itemName
                    )
            )
        }
    }
}