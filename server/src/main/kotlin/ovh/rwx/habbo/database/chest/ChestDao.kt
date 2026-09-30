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
import ovh.rwx.habbo.game.chest.ChestData
import ovh.rwx.habbo.game.chest.ChestEntry
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.database.*
import java.time.LocalDateTime

private val jsonMapper = jacksonObjectMapper()

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

    private fun sql(name: String): String =
        javaClass.classLoader.getResource("sql/chest/$name.sql").readText()

    fun getChest(itemId: Int): ChestData? = db {
        queryOne<ChestData>(sql("select_chest"), mapOf("item_id" to itemId))
    }

    fun createChest(itemId: Int, userId: Int, capacity: Int) {
        db {
            update(sql("insert_chest"), mapOf("item_id" to itemId, "user_id" to userId, "capacity" to capacity))
        }
    }

    fun getChestEntries(itemId: Int): List<ChestEntry> = db {
        query<ChestEntryDto>(sql("select_chest_items"), mapOf("item_id" to itemId))
            .map { it.toDomain() }
    }

    fun insertChestItems(chestItemId: Int, itemIds: List<Int>) {
        if (itemIds.isEmpty()) return

        db {
            batchUpdate(
                sql("insert_chest_item"),
                itemIds.map { mapOf("chest_item_id" to chestItemId, "item_id" to it) }
            )
        }
    }

    fun deleteChestItems(chestItemId: Int, itemIds: List<Int>) {
        if (itemIds.isEmpty()) return

        db {
            batchUpdate(
                sql("delete_chest_items"),
                itemIds.map { mapOf("chest_item_id" to chestItemId, "item_id" to it) }
            )
        }
    }

    fun updateChest(chest: ChestData) {
        db {
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

    fun deleteChest(chestItemId: Int) {
        db {
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
    ): Int = db {
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

    fun getChestLogs(itemId: Int, limit: Int, page: Int): Pair<Int, List<ChestLog>> = db {
        val total = queryOne<Int>(sql("count_chest_logs"), mapOf("item_id" to itemId)) ?: 0
        val logs = query<ChestLogDto>(
            sql("select_chest_logs"),
            mapOf("item_id" to itemId, "limit" to limit, "offset" to ((page - 1).coerceAtLeast(0) * limit))
        ).map { it.toDomain() }

        total to logs
    }

    fun getRoomLogs(roomId: Int, limit: Int, page: Int): Pair<Int, List<ChestLog>> = db {
        val total = queryOne<Int>(
            "SELECT COUNT(*) AS `total` FROM `chest_logs` WHERE `room_id` = :room_id",
            mapOf("room_id" to roomId)
        ) ?: 0
        val logs = query<ChestLogDto>(
            "SELECT * FROM `chest_logs` WHERE `room_id` = :room_id ORDER BY `id` DESC LIMIT :limit OFFSET :offset",
            mapOf("room_id" to roomId, "limit" to limit, "offset" to ((page - 1).coerceAtLeast(0) * limit))
        ).map { it.toDomain() }

        total to logs
    }

    fun getChestLog(logId: Long): ChestLog? = db {
        queryOne<ChestLogDto>(
            "SELECT * FROM `chest_logs` WHERE `id` = :log_id LIMIT 1",
            mapOf("log_id" to logId)
        )?.toDomain()
    }
}

data class ChestLogDto(
    val id: Int,
    val chestItemId: Int,
    val roomId: Int? = 0,
    val userId: Int,
    val username: String,
    val withdrawFurniCount: Int = 0,
    val depositFurniCount: Int = 0,
    val withdrawCoinsCount: Int = 0,
    val depositCoinsCount: Int = 0,
    val itemsData: String? = null,
    val createdAt: LocalDateTime
) {
    fun toDomain(): ChestDao.ChestLog {
        val parsed = itemsData?.let { json ->
            runCatching { jsonMapper.readValue<ChestLogItemsData>(json) }.getOrNull()
        }
        return ChestDao.ChestLog(
            id, chestItemId, roomId ?: 0, userId, username,
            withdrawFurniCount, depositFurniCount, withdrawCoinsCount, depositCoinsCount,
            parsed, createdAt
        )
    }
}

data class ChestEntryDto(
    val chestEntryId: Int,
    val id: Int,
    val userId: Int,
    val itemName: String,
    val extraData: String,
    val isLimited: Boolean,
    val isBuildersClub: Boolean,
    val lockState: Int,
    val transactionId: Long
) {
    fun toDomain() = ChestEntry(
        chestItemId = chestEntryId,
        item = UserItem(id, userId, itemName, extraData, isLimited, isBuildersClub),
        lockState = lockState,
        transactionId = transactionId
    )
}
