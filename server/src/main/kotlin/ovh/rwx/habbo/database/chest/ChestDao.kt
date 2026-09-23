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

package ovh.rwx.habbo.database.chest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.andrewoma.kwery.core.Row
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.chest.ChestData
import ovh.rwx.habbo.game.chest.ChestEntry
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey
import ovh.rwx.habbo.kotlin.localDateTime
import java.time.LocalDateTime

data class ChestLogItemEntry(
    val isWallItem: Boolean,
    val typeId: Int,
    val legacyPosterId: String = "",
    val count: Int,
)

data class ChestLogItemsData(
    val deposited: List<ChestLogItemEntry> = emptyList(),
    val withdrawn: List<ChestLogItemEntry> = emptyList(),
)

object ChestDao {
    private val jsonMapper = jacksonObjectMapper()

    private fun sql(name: String): String =
        javaClass.classLoader.getResource("sql/chest/$name.sql").readText()

    fun getChest(itemId: Int): ChestData? = HabboServer.database {
        select(sql("select_chest"), mapOf("item_id" to itemId)) {
            ChestData(
                itemId = it.int("item_id"),
                userId = it.int("user_id"),
                name = it.string("name"),
                description = it.string("description"),
                capacity = it.int("capacity"),
                coins = it.int("coins"),
                isWired = it.boolean("is_wired"),
                locked = it.boolean("locked"),
                autoLock = it.boolean("auto_lock"),
                stateMode = it.int("state_mode"),
                previewMode = it.int("preview_mode"),
                previewAmount = it.int("preview_amount"),
                anyoneCanOpen = it.boolean("anyone_can_open"),
                anyoneCanDonate = it.boolean("anyone_can_donate"),
                notificationMode = it.int("notification_mode"),
                notifyFull = it.boolean("notify_full"),
                notifyDonation = it.boolean("notify_donation"),
                notifyWithdraw = it.boolean("notify_withdraw"),
                notifyEmpty = it.boolean("notify_empty"),
                notifyTransaction = it.boolean("notify_transaction"),
            )
        }.firstOrNull()
    }

    fun createChest(itemId: Int, userId: Int, capacity: Int) {
        HabboServer.database {
            update(sql("insert_chest"), mapOf("item_id" to itemId, "user_id" to userId, "capacity" to capacity))
        }
    }

    fun getChestEntries(itemId: Int): List<ChestEntry> = HabboServer.database {
        select(sql("select_chest_items"), mapOf("item_id" to itemId)) {
            val userItem = UserItem(
                id = it.int("id"),
                userId = it.int("user_id"),
                itemName = it.string("item_name"),
                extraData = it.string("extra_data"),
                limited = it.boolean("is_limited"),
                buildersClub = it.boolean("is_builders_club"),
            )

            ChestEntry(
                chestItemId = it.int("chest_entry_id"),
                item = userItem,
                lockState = it.int("lock_state"),
                transactionId = it.long("transaction_id"),
            )
        }
    }

    fun insertChestItems(chestItemId: Int, itemIds: List<Int>) {
        if (itemIds.isEmpty()) return

        HabboServer.database {
            batchUpdate(
                sql("insert_chest_item"),
                itemIds.map { mapOf("chest_item_id" to chestItemId, "item_id" to it) }
            )
        }
    }

    fun deleteChestItems(chestItemId: Int, itemIds: List<Int>) {
        if (itemIds.isEmpty()) return

        HabboServer.database {
            batchUpdate(
                sql("delete_chest_items"),
                itemIds.map { mapOf("chest_item_id" to chestItemId, "item_id" to it) }
            )
        }
    }

    fun updateChest(chest: ChestData) {
        HabboServer.database {
            update(
                sql("update_chest"),
                mapOf(
                    "item_id" to chest.itemId,
                    "name" to chest.name,
                    "description" to chest.description,
                    "capacity" to chest.capacity,
                    "coins" to chest.coins,
                    "is_wired" to chest.isWired,
                    "locked" to chest.locked,
                    "auto_lock" to chest.autoLock,
                    "state_mode" to chest.stateMode,
                    "preview_mode" to chest.previewMode,
                    "preview_amount" to chest.previewAmount,
                    "anyone_can_open" to chest.anyoneCanOpen,
                    "anyone_can_donate" to chest.anyoneCanDonate,
                    "notification_mode" to chest.notificationMode,
                    "notify_full" to chest.notifyFull,
                    "notify_donation" to chest.notifyDonation,
                    "notify_withdraw" to chest.notifyWithdraw,
                    "notify_empty" to chest.notifyEmpty,
                    "notify_transaction" to chest.notifyTransaction,
                )
            )
        }
    }

    fun updateChestOwner(chestItemId: Int, userId: Int) {
        HabboServer.database {
            update(
                "UPDATE `chests` SET `user_id` = :user_id WHERE `item_id` = :item_id",
                mapOf("user_id" to userId, "item_id" to chestItemId)
            )
        }
    }

    fun deleteChest(chestItemId: Int) {
        HabboServer.database {
            update("DELETE FROM `chest_items` WHERE `chest_item_id` = :item_id", mapOf("item_id" to chestItemId))
            update("DELETE FROM `chest_logs` WHERE `chest_item_id` = :item_id", mapOf("item_id" to chestItemId))
            update("DELETE FROM `chests` WHERE `item_id` = :item_id", mapOf("item_id" to chestItemId))
        }
    }

    fun insertChestLog(
        chestItemId: Int,
        roomId: Int,
        userId: Int,
        username: String,
        withdrawFurniCount: Int,
        depositFurniCount: Int,
        withdrawCoinsCount: Int,
        depositCoinsCount: Int,
        itemsData: ChestLogItemsData? = null,
    ): Int = HabboServer.database {
        val itemsDataJson = itemsData?.let { jsonMapper.writeValueAsString(it) }

        insertAndGetGeneratedKey(
            sql("insert_chest_log"),
            mapOf(
                "chest_item_id" to chestItemId,
                "room_id" to roomId,
                "user_id" to userId,
                "username" to username,
                "withdraw_furni_count" to withdrawFurniCount,
                "deposit_furni_count" to depositFurniCount,
                "withdraw_coins_count" to withdrawCoinsCount,
                "deposit_coins_count" to depositCoinsCount,
                "items_data" to itemsDataJson,
            )
        )
    }

    data class ChestLog(
        val id: Int,
        val chestItemId: Int,
        val roomId: Int,
        val userId: Int,
        val username: String,
        val withdrawFurniCount: Int,
        val depositFurniCount: Int,
        val withdrawCoinsCount: Int,
        val depositCoinsCount: Int,
        val itemsData: ChestLogItemsData? = null,
        val createdAt: LocalDateTime,
    )

    fun getChestLogs(itemId: Int, limit: Int, page: Int): Pair<Int, List<ChestLog>> = HabboServer.database {
        val total = select(sql("count_chest_logs"), mapOf("item_id" to itemId)) { it.int("total") }.firstOrNull() ?: 0
        val logs = select(
            sql("select_chest_logs"),
            mapOf("item_id" to itemId, "limit" to limit, "offset" to ((page - 1).coerceAtLeast(0) * limit))
        ) { mapLog(it) }

        total to logs
    }

    fun getRoomLogs(roomId: Int, limit: Int, page: Int): Pair<Int, List<ChestLog>> = HabboServer.database {
        val total = select(
            "SELECT COUNT(*) AS `total` FROM `chest_logs` WHERE `room_id` = :room_id",
            mapOf("room_id" to roomId)
        ) { it.int("total") }.firstOrNull() ?: 0
        val logs = select(
            "SELECT * FROM `chest_logs` WHERE `room_id` = :room_id ORDER BY `id` DESC LIMIT :limit OFFSET :offset",
            mapOf("room_id" to roomId, "limit" to limit, "offset" to ((page - 1).coerceAtLeast(0) * limit))
        ) { mapLog(it) }

        total to logs
    }

    fun getChestLog(logId: Long): ChestLog? = HabboServer.database {
        select(
            "SELECT * FROM `chest_logs` WHERE `id` = :log_id LIMIT 1",
            mapOf("log_id" to logId)
        ) { mapLog(it) }.firstOrNull()
    }

    private fun mapLog(it: Row): ChestLog {
        val itemsDataJson = it.stringOrNull("items_data")
        val itemsData = itemsDataJson?.let { json ->
            runCatching { jsonMapper.readValue<ChestLogItemsData>(json) }.getOrNull()
        }

        return ChestLog(
            id = it.int("id"),
            chestItemId = it.int("chest_item_id"),
            roomId = it.intOrNull("room_id") ?: 0,
            userId = it.int("user_id"),
            username = it.string("username"),
            withdrawFurniCount = it.int("withdraw_furni_count"),
            depositFurniCount = it.int("deposit_furni_count"),
            withdrawCoinsCount = it.int("withdraw_coins_count"),
            depositCoinsCount = it.int("deposit_coins_count"),
            itemsData = itemsData,
            createdAt = it.localDateTime("created_at"),
        )
    }
}
