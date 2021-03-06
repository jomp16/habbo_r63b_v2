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

package ovh.rwx.habbo.database.wardrobe

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.wardrobe.Wardrobe
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey

object WardrobeDao {
    fun getWardrobes(userId: Int): List<Wardrobe> = HabboServer.database {
        select("SELECT * FROM `users_wardrobe` WHERE `user_id` = :user_id",
                mapOf(
                        "user_id" to userId
                )
        ) {
            Wardrobe(
                    it.int("id"),
                    it.int("slot_id"),
                    it.string("figure"),
                    it.string("gender")
            )
        }
    }

    fun updateWardrobe(id: Int, slotId: Int, figure: String, gender: String): Wardrobe = HabboServer.database {
        update("UPDATE `users_wardrobe` SET `figure` = :figure, `gender` = :gender, `slot_id` = :slot_id WHERE `id` = :id",
                mapOf(
                        "figure" to figure,
                        "gender" to gender,
                        "slot_id" to slotId,
                        "id" to id
                )
        )

        Wardrobe(id, slotId, figure, gender)
    }

    fun createWardrobe(userId: Int, slotId: Int, figure: String, gender: String): Wardrobe = HabboServer.database {
        val id = insertAndGetGeneratedKey("INSERT INTO `users_wardrobe` (`user_id`, `slot_id`, `figure`, `gender`) VALUES (:user_id, :slot_id, :figure, :gender)",
                mapOf(
                        "user_id" to userId,
                        "slot_id" to slotId,
                        "figure" to figure,
                        "gender" to gender
                )
        )

        Wardrobe(id, slotId, figure, gender)
    }
}