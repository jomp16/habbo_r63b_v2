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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.habbicon.Habbicon
import ovh.rwx.habbo.game.habbicon.HabbiconCollection
import ovh.rwx.habbo.game.habbicon.UserHabbicon
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey

object HabbiconDao {
    fun getOrCreateCollection(name: String): Int = HabboServer.database {
        select(
            "SELECT id FROM habbicon_collections WHERE name = :name LIMIT 1",
            mapOf("name" to name)
        ) { it.int("id") }.firstOrNull() ?: insertAndGetGeneratedKey(
            "INSERT INTO habbicon_collections (name, enabled, price_credits, price_activity_points, activity_point_type) VALUES (:name, 1, 1, 0, 0)",
            mapOf("name" to name)
        )
    }

    fun upsertHabbicon(id: Int, collectionId: Int, name: String, purchasable: Boolean) {
        HabboServer.database {
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

    fun getCollectionRewardId(collectionId: Int): Int? = HabboServer.database {
        select(
            "SELECT reward_habbicon_id FROM habbicon_collections WHERE id = :id",
            mapOf("id" to collectionId)
        ) { it.intOrNull("reward_habbicon_id") }.firstOrNull()
    }

    fun setCollectionReward(collectionId: Int, habbiconId: Int) {
        HabboServer.database {
            update(
                "UPDATE habbicon_collections SET reward_habbicon_id = :habbicon_id WHERE id = :collection_id",
                mapOf("habbicon_id" to habbiconId, "collection_id" to collectionId)
            )
        }
    }

    fun getCollections(): List<HabbiconCollection> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/habbicon/select_collections.sql").readText(),
                emptyMap<String, Any>()
            ) {
                HabbiconCollection(
                    id = it.int("id"),
                    name = it.string("name"),
                    enabled = it.boolean("enabled"),
                    priceCredits = it.int("price_credits"),
                    priceActivityPoints = it.int("price_activity_points"),
                    activityPointType = it.int("activity_point_type"),
                    rewardHabbiconId = it.intOrNull("reward_habbicon_id"),
                    rewardState = it.int("reward_state")
                )
            }
        }
    }

    fun getHabbicons(): List<Habbicon> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/habbicon/select_habbicons.sql").readText(),
                emptyMap<String, Any>()
            ) {
                Habbicon(
                    id = it.int("id"),
                    collectionId = it.int("collection_id"),
                    name = it.string("name"),
                    enabled = it.boolean("enabled"),
                    purchasable = it.boolean("purchasable"),
                    priceCredits = it.int("price_credits"),
                    priceActivityPoints = it.int("price_activity_points"),
                    activityPointType = it.int("activity_point_type")
                )
            }
        }
    }

    fun getUserHabbicons(userId: Int): List<UserHabbicon> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/habbicon/select_user_habbicons.sql").readText(),
                mapOf("user_id" to userId)
            ) {
                UserHabbicon(
                    id = it.int("id"),
                    userId = it.int("user_id"),
                    habbiconId = it.int("habbicon_id"),
                    state = it.int("state")
                )
            }
        }
    }

    fun addUserHabbicon(userId: Int, habbiconId: Int, state: Int): Int {
        return HabboServer.database {
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
        HabboServer.database {
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
        HabboServer.database {
            update("DELETE FROM users_habbicon_recent WHERE user_id = :user_id", mapOf("user_id" to userId))
            recentIds.forEachIndexed { position, recentId ->
                update(
                    "INSERT INTO users_habbicon_recent (user_id, position, habbicon_id) VALUES (:user_id, :position, :habbicon_id)",
                    mapOf("user_id" to userId, "position" to position, "habbicon_id" to recentId)
                )
            }
        }
    }

    fun getRecentHabbiconIds(userId: Int): List<Int> = HabboServer.database {
        select(
            javaClass.classLoader.getResource("sql/habbicon/select_user_habbicon_recent.sql").readText(),
            mapOf("user_id" to userId)
        ) { it.int("habbicon_id") }
    }

    fun getUserOwnedHabbiconIds(userId: Int): Set<Int> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/habbicon/select_user_owned_habbicon_ids.sql").readText(),
                mapOf("user_id" to userId)
            ) {
                it.int("habbicon_id")
            }.toSet()
        }
    }
}
