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

package ovh.rwx.habbo.database.habbicon

import ovh.rwx.habbo.game.habbicon.Habbicon
import ovh.rwx.habbo.game.habbicon.HabbiconCollection
import ovh.rwx.habbo.game.habbicon.UserHabbicon
import ovh.rwx.habbo.database.*

object HabbiconDao {
    fun getOrCreateCollection(name: String): Int = db {
        queryOne<Int>(
            "SELECT id FROM habbicon_collections WHERE name = :name LIMIT 1",
            mapOf("name" to name)
        ) ?: insertAndGetGeneratedKey(
            "INSERT INTO habbicon_collections (name, enabled, price_credits, price_activity_points, activity_point_type) VALUES (:name, 1, 1, 0, 0)",
            mapOf("name" to name)
        )
    }

    fun upsertHabbicon(id: Int, collectionId: Int, name: String, purchasable: Boolean) {
        db {
            update(
                """
                INSERT INTO habbicons
                    (id, collection_id, name, enabled, purchasable, price_credits, price_activity_points, activity_point_type)
                VALUES (:id, :collection_id, :name, 1, :purchasable, 1, 0, 0)
                ON DUPLICATE KEY UPDATE
                    collection_id = VALUES(collection_id),
                    name = VALUES(name),
                    enabled = 1,
                    purchasable = VALUES(purchasable)
                """.trimIndent(),
                mapOf(
                    "id" to id,
                    "collection_id" to collectionId,
                    "name" to name,
                    "purchasable" to purchasable
                )
            )
        }
    }

    fun getCollectionRewardId(collectionId: Int): Int? = db {
        queryOne<Int>(
            "SELECT reward_habbicon_id FROM habbicon_collections WHERE id = :id",
            mapOf("id" to collectionId)
        )
    }

    fun setCollectionReward(collectionId: Int, habbiconId: Int) {
        db {
            update(
                "UPDATE habbicon_collections SET reward_habbicon_id = :habbicon_id WHERE id = :collection_id",
                mapOf("habbicon_id" to habbiconId, "collection_id" to collectionId)
            )
        }
    }

    fun getCollections(): List<HabbiconCollection> = db {
        query<HabbiconCollection>("sql/habbicon/select_collections.sql")
    }

    fun getHabbicons(): List<Habbicon> = db {
        query<Habbicon>("sql/habbicon/select_habbicons.sql")
    }

    fun getUserHabbicons(userId: Int): List<UserHabbicon> = db {
        query<UserHabbicon>(
            "sql/habbicon/select_user_habbicons.sql",
            mapOf("user_id" to userId)
        )
    }

    fun addUserHabbicon(userId: Int, habbiconId: Int, state: Int): Int {
        return db {
            insertAndGetGeneratedKey(
                javaClass.classLoader.getResource("sql/habbicon/insert_user_habbicon.sql").readText(),
                mapOf(
                    "user_id" to userId,
                    "habbicon_id" to habbiconId,
                    "state" to state
                )
            )
        }
    }

    fun updateUserHabbiconState(userId: Int, habbiconId: Int, state: Int) {
        db {
            update(
                javaClass.classLoader.getResource("sql/habbicon/update_user_habbicon_state.sql").readText(),
                mapOf(
                    "user_id" to userId,
                    "habbicon_id" to habbiconId,
                    "state" to state
                )
            )
        }
    }

    fun updateUserHabbiconRecentIds(userId: Int, habbiconId: Int, recentIds: List<Int>) {
        db {
            update("DELETE FROM users_habbicon_recent WHERE user_id = :user_id", mapOf("user_id" to userId))
            recentIds.forEachIndexed { position, recentId ->
                update(
                    "INSERT INTO users_habbicon_recent (user_id, position, habbicon_id) VALUES (:user_id, :position, :habbicon_id)",
                    mapOf("user_id" to userId, "position" to position, "habbicon_id" to recentId)
                )
            }
        }
    }

    fun getRecentHabbiconIds(userId: Int): List<Int> = db {
        query<Int>(
            "sql/habbicon/select_user_habbicon_recent.sql",
            mapOf("user_id" to userId)
        )
    }
}
