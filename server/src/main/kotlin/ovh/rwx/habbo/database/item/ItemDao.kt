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

package ovh.rwx.habbo.database.item

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.db
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.game.item.*
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.xml.FurniXMLInfo
import ovh.rwx.habbo.game.room.dimmer.RoomDimmer
import ovh.rwx.habbo.kotlin.toIntList
import ovh.rwx.habbo.util.Vector3

object ItemDao {
    private val log = LoggerFactory.getLogger(javaClass)

    // cache in memory all the information about items fam
    private val limitedItemDatas: MutableMap<Int, LimitedItemData?> = mutableMapOf()
    private val wiredDatas: MutableMap<Int, WiredData?> = mutableMapOf()
    private val roomDimmers: MutableMap<Int, RoomDimmer?> = mutableMapOf()

    fun getFurnishings(furniXMLInfos: Map<String, FurniXMLInfo>): List<Furnishing> = db {
        query<FurnishingRowDto>(javaClass.classLoader.getResource("sql/furnishings/select_furnishings.sql").readText())
            .map { it.toDomain(furniXMLInfos) }
    }

    fun getRoomItems(roomId: Int): Map<Int, RoomItem> = db {
        query<RoomItemRowDto>(
            javaClass.classLoader.getResource("sql/items/room/select_room_items.sql").readText(),
            mapOf("room_id" to roomId)
        ).associateBy({ it.id }, { it.toDomain() })
    }

    fun getUserItems(userId: Int): Map<Int, UserItem> = db {
        query<UserItemRowDto>(
            javaClass.classLoader.getResource("sql/items/user/select_user_items.sql").readText(),
            mapOf("user_id" to userId)
        ).associateBy({ it.id }, { it.toDomain() })
    }

    fun getAllUserItems(userId: Int): List<UserItem> = db {
        query<UserItemRowDto>(
            javaClass.classLoader.getResource("sql/items/user/select_all_user_items.sql").readText(),
            mapOf("user_id" to userId)
        ).map { it.toDomain() }
    }

    data class UserItemWithRoom(val userItem: UserItem, val roomId: Int?)

    fun getAllUserItemsWithRoom(userId: Int): List<UserItemWithRoom> = db {
        query<UserItemRowDto>(
            javaClass.classLoader.getResource("sql/items/user/select_all_user_items.sql").readText(),
            mapOf("user_id" to userId)
        ).map { UserItemWithRoom(it.toDomain(), it.roomId) }
    }

    fun getLimitedData(itemId: Int): LimitedItemData? {
        if (!limitedItemDatas.containsKey(itemId)) {
            val limitedItemData = db {
                queryOne<LimitedItemData>(
                    "sql/items/limited/select_limited.sql",
                    mapOf("item_id" to itemId)
                )
            }

            limitedItemDatas[itemId] = limitedItemData
        }

        return limitedItemDatas[itemId]
    }

    fun getWiredData(itemId: Int): WiredData? {
        if (!wiredDatas.containsKey(itemId)) {
            val wiredData = db {
                queryOne<WiredRowDto>(
                    javaClass.classLoader.getResource("sql/items/wired/select_wired_data.sql").readText(),
                    mapOf("item_id" to itemId)
                )?.toDomain()
            }

            if (wiredData != null) {
                wiredDatas[itemId] = wiredData
            }
        }

        return wiredDatas[itemId]
    }

    fun getTeleportLinks(): List<Pair<Int, Int>> = db {
        query<TeleportLinkDto>(javaClass.classLoader.getResource("sql/items/teleport/select_teleport_links.sql").readText())
            .map { it.teleportOneId to it.teleportTwoId }
    }

    fun getLinkedTeleport(teleportIds: Set<Int>): List<Pair<Int, Int>> = db {
        if (teleportIds.isEmpty()) return@db emptyList()
        query<LinkedTeleportDto>(
            "sql/items/teleport/select_room_id_from_linked_teleport.sql",
            mapOf("teleport_ids" to teleportIds)
        ).map { it.teleportId to (it.roomId ?: 0) }
    }

    fun deleteItems(itemIds: List<Int>) {
        db {
            batchUpdate(
                javaClass.classLoader.getResource("sql/items/item/delete_item.sql").readText(),
                itemIds.map {
                    mapOf(
                        "id" to it
                    )
                }
            )
        }
    }

    private fun removeRoomItems(roomItemsToRemove: Collection<RoomItem>) {
        if (roomItemsToRemove.isEmpty()) return

        db {
            batchUpdate(
                javaClass.classLoader.getResource("sql/items/item/update_item_room.sql").readText(),
                roomItemsToRemove.map {
                    val extraData = HabboServer.habboGame.itemManager
                        .getFurnitureLogic(it.furnishing)
                        .sanitizeForDatabase(it.extraData)
                    mapOf(
                        "room_id" to null,
                        "extra_data" to extraData,
                        "id" to it.id
                    )
                }
            )
        }
    }

    fun updateItemsOwner(itemIds: List<Int>, userId: Int) {
        if (itemIds.isEmpty()) return

        db {
            batchUpdate(
                javaClass.classLoader.getResource("sql/items/item/update_item_owner.sql").readText(),
                itemIds.map {
                    mapOf(
                        "user_id" to userId,
                        "id" to it
                    )
                }
            )
        }
    }

    fun getRoomDimmer(roomItem: RoomItem): RoomDimmer? {
        if (!roomDimmers.containsKey(roomItem.id)) {
            val roomDimmer = db {
                queryOne<RoomDimmerRowDto>(
                    javaClass.classLoader.getResource("sql/items/dimmer/select_dimmer.sql").readText(),
                    mapOf("item_id" to roomItem.id)
                )?.toDomain(roomItem)
            }

            roomDimmers[roomItem.id] = roomDimmer
        }
        val roomDimmer = roomDimmers[roomItem.id] ?: return null

        roomDimmer.roomItem = roomItem

        return roomDimmer
    }

    fun saveDimmer(roomDimmer: RoomDimmer) {
        db {
            update(
                javaClass.classLoader.getResource("sql/items/dimmer/update_dimmer.sql").readText(),
                mapOf(
                    "enabled" to roomDimmer.enabled,
                    "current_preset" to roomDimmer.currentPreset,
                    "preset_one" to roomDimmer.presets[0].toString(),
                    "preset_two" to roomDimmer.presets[1].toString(),
                    "preset_three" to roomDimmer.presets[2].toString(),
                    "id" to roomDimmer.id
                )
            )
        }
    }

    fun saveWireds(wireds: List<RoomItem>) {
        val wiredData = wireds.mapNotNull { it.wiredData }

        if (wiredData.isEmpty()) return

        db {
            batchUpdate(
                javaClass.classLoader.getResource("sql/items/wired/update_wired_data.sql").readText(),
                wiredData.map {
                    mapOf(
                        "delay" to it.delay,
                        "items" to it.items.joinToString(","),
                        "message" to it.message,
                        "options" to it.options.joinToString(","),
                        "extradata" to it.extradata,
                        "is_filter" to it.filter,
                        "is_inverse" to it.inverse,
                        "furni_sources" to it.furniSources.map { source -> source.code }.joinToString(","),
                        "user_sources" to it.userSources.map { source -> source.code }.joinToString(","),
                        "stuff_ids2" to it.stuffIds2.joinToString(","),
                        "variable_ids" to it.variableIds.joinToString(","),
                        "id" to it.id
                    )
                }
            )
        }
    }

    fun addItems(
        userId: Int,
        itemPurchaseDatas: List<ItemPurchaseData>,
        ignoreSpecialHandling: Boolean = false
    ): List<UserItem> {
        val sanitizedPurchaseDatas = itemPurchaseDatas.map { data ->
            val sanitizedExtraData = HabboServer.habboGame.itemManager
                .getFurnitureLogic(data.furnishing)
                .sanitizeForDatabase(data.extraData)
            if (sanitizedExtraData != data.extraData) data.copy(extraData = sanitizedExtraData) else data
        }

        val userItems = sanitizedPurchaseDatas.map { data ->
            val newId = HabboServer.habboGame.itemManager.itemSequence.nextId()
            UserItem(
                newId,
                userId,
                data.furnishing.itemName,
                data.extraData,
                data.limited,
                data.buildersClub
            )
        }

        WriteBehindManager.queue {
            batchInsertItems(userItems)
        }

        if (!ignoreSpecialHandling) {
            userItems.forEach { userItem ->
                addSpecialItemData(userItem)
            }
        }

        return userItems
    }

    fun addGiftItem(
        userId: Int,
        giftFurnishing: Furnishing,
        amount: Int,
        giftExtradata: String,
        furnishing: Furnishing,
        extraData: String,
        limitedNumber: Int = 0,
        limitedTotal: Int = 0
    ): UserItem {
        val giftUserItem = addItems(
            userId,
            listOf(ItemPurchaseData(giftFurnishing, giftExtradata, limitedNumber > 0, buildersClub = false)),
        ).first()

        if (limitedNumber > 0 && limitedTotal > 0) addLimitedItem(giftUserItem.id, limitedNumber, limitedTotal)

        WriteBehindManager.queue {
            db {
                update(
                    "INSERT INTO `items_gift` (`item_id`, `item_name`, `amount`, `extradata`) VALUES (:item_id, :item_name, :amount, :extradata)",
                    mapOf("item_id" to giftUserItem.id, "item_name" to furnishing.itemName, "amount" to amount, "extradata" to extraData)
                )
            }
        }

        return giftUserItem
    }

    fun getGiftData(itemId: Int): GiftData? = db {
        queryOne<GiftDataRowDto>(
            javaClass.classLoader.getResource("sql/items/gift/select_gift.sql").readText(),
            mapOf("item_id" to itemId)
        )?.toDomain()
    }

    fun addLimitedItem(itemId: Int, limitedNumber: Int, limitedTotal: Int): Int {
        WriteBehindManager.queue {
            db {
                update(
                    "INSERT INTO `items_limited` (`item_id`, `limited_num`, `limited_total`) VALUES (:item_id, :limited_num, :limited_total)",
                    mapOf("item_id" to itemId, "limited_num" to limitedNumber, "limited_total" to limitedTotal)
                )
            }
        }
        return itemId
    }

    fun deleteGiftData(id: Int) {
        db {
            update(
                javaClass.classLoader.getResource("sql/items/gift/delete_gift.sql").readText(),
                mapOf(
                    "id" to id
                )
            )
        }
    }

    fun addRoomItemInventory(roomItems: List<RoomItem>) {
        val roomItemsGrouped = roomItems.groupBy { it.userId }

        roomItemsGrouped.keys.forEach { userId ->
            val habboSession = HabboServer.habboSessionManager.getHabboSessionById(userId)

            habboSession?.habboInventory?.addItems(
                (roomItemsGrouped[userId]
                    ?: error("Can't find the room items!")).map {
                    UserItem(
                        it.id,
                        it.userId,
                        it.itemName,
                        HabboServer.habboGame.itemManager
                            .getFurnitureLogic(it.furnishing)
                            .sanitizeForDatabase(it.extraData),
                        it.limited,
                        it.buildersClub
                    )
                })
        }

        removeRoomItems(roomItems)
    }

    private fun addSpecialItemData(userItem: UserItem) {
        if (userItem.furnishing.interactionType.name.startsWith("WIRED_")) {
            val defaultWiredData =
                HabboServer.habboGame.itemManager.getWiredDefaultData(userItem.furnishing.interactionType)
                    ?: WiredData(0, 0, emptyList(), "", emptyList(), "")

            WriteBehindManager.queue {
                db {
                    update(
                        "INSERT INTO `items_wired` (`item_id`, `delay`, `items`, `message`, `options`, `extradata`) VALUES (:item_id, :delay, :items, :message, :options, :extradata)",
                        mapOf(
                            "item_id" to userItem.id,
                            "delay" to defaultWiredData.delay,
                            "items" to defaultWiredData.items.joinToString(","),
                            "message" to defaultWiredData.message,
                            "options" to defaultWiredData.options.joinToString(","),
                            "extradata" to defaultWiredData.extradata
                        )
                    )
                }
            }
            return
        }

        when (userItem.furnishing.interactionType) {
            InteractionType.TELEPORT -> {
                val teleporterItem = addItems(
                    userItem.userId,
                    listOf(
                        ItemPurchaseData(
                            userItem.furnishing,
                            userItem.extraData,
                            limited = false,
                            buildersClub = false
                        )
                    ),
                    ignoreSpecialHandling = true
                ).first()

                WriteBehindManager.queue {
                    db {
                        batchUpdate(
                            "INSERT INTO `items_teleport` (`teleport_one_id`, `teleport_two_id`) VALUES (:teleport_one_id, :teleport_two_id)",
                            listOf(
                                mapOf("teleport_one_id" to userItem.id, "teleport_two_id" to teleporterItem.id),
                                mapOf("teleport_one_id" to teleporterItem.id, "teleport_two_id" to userItem.id)
                            )
                        )
                    }
                }

                HabboServer.habboGame.itemManager.teleportLinks[userItem.id] = teleporterItem.id
                HabboServer.habboGame.itemManager.roomTeleportLinks[userItem.id] = 0
                HabboServer.habboGame.itemManager.teleportLinks[teleporterItem.id] = userItem.id
                HabboServer.habboGame.itemManager.roomTeleportLinks[teleporterItem.id] = 0
            }

            InteractionType.DIMMER -> {
                WriteBehindManager.queue {
                    db {
                        update(
                            "INSERT INTO `items_dimmer` (`item_id`, `enabled`, `current_preset`, `preset_one`, `preset_two`, `preset_three`) VALUES (:item_id, :enabled, :current_preset, :preset_one, :preset_two, :preset_three)",
                            mapOf(
                                "item_id" to userItem.id,
                                "enabled" to false,
                                "current_preset" to 1,
                                "preset_one" to "#000000,255,0",
                                "preset_two" to "#000000,255,0",
                                "preset_three" to "#000000,255,0"
                            )
                        )
                    }
                }
            }

            else -> {}
        }
    }


    /**
     * Transfere itens entre dois usuários em uma transação atômica.
     * 
     * GARANTIAS DE SEGURANÇA:
     * - Todos os itens são transferidos ou nenhum é (atomicidade via transação SQL)
     * - Valida que o usuário atual é o dono do item antes de transferir
     * - Previne race conditions através de transação SQL
     *
     * @param user1Id ID do usuário 1
     * @param user2Id ID do usuário 2
     * @param user1Items IDs dos itens que user1 está dando (vão para user2)
     * @param user2Items IDs dos itens que user2 está dando (vão para user1)
     * @throws Exception se algum item não for encontrado ou não pertencer ao usuário
     */
    fun transferTradeItems(
        user1Id: Int,
        user2Id: Int,
        user1Items: List<Int>,
        user2Items: List<Int>
    ) {
        if (user1Items.isEmpty() && user2Items.isEmpty()) return

        db {
            // Prepara os parâmetros para batchUpdate
            // Cada item terá seu próprio UPDATE com validação de proprietário
            val allUpdates = mutableListOf<Map<String, Any>>()

            // Adiciona atualizações para itens do user1 (vão para user2)
            user1Items.forEach { itemId ->
                allUpdates.add(
                    mapOf(
                        "new_user_id" to user2Id,
                        "item_id" to itemId,
                        "old_user_id" to user1Id
                    )
                )
            }

            // Adiciona atualizações para itens do user2 (vão para user1)
            user2Items.forEach { itemId ->
                allUpdates.add(
                    mapOf(
                        "new_user_id" to user1Id,
                        "item_id" to itemId,
                        "old_user_id" to user2Id
                    )
                )
            }

            if (allUpdates.isEmpty()) return@db

            // Executa todas as atualizações em batch
            // A query valida que o item pertence ao usuário antes de atualizar
            val updateSql = javaClass.classLoader
                .getResource("sql/items/trade/transfer_trade_items.sql")
                ?.readText()!!

            val rowsAffectedList = batchUpdate(updateSql, allUpdates)
            val totalRowsAffected = rowsAffectedList.sum()

            // Verifica se todos os itens foram atualizados
            if (totalRowsAffected != allUpdates.size) {
                throw IllegalStateException(
                    "Esperado atualizar ${allUpdates.size} itens, mas apenas $totalRowsAffected foram afetados. " +
                            "Possível tentativa de exploit ou item não encontrado."
                )
            }
        }
    }

    fun batchInsertItems(items: List<UserItem>) {
        if (items.isEmpty()) return
        try {
            db {
                batchInsert(
                    "INSERT INTO `items` (`id`, `user_id`, `item_name`, `extra_data`, `wall_pos`, `is_builders_club`) VALUES (:id, :userId, :itemName, :extraData, '', :buildersClub)",
                    items
                )
            }
        } catch (e: Exception) {
            log.error("Write-Behind error bulk inserting items (count: ${items.size})", e)
        }
    }
}


data class ItemPurchaseData(
    val furnishing: Furnishing,
    val extraData: String,
    val limited: Boolean,
    val buildersClub: Boolean,
)

data class FurnishingRowDto(
    val itemName: String,
    val type: String,
    val interactionType: String,
    val stackHeight: String,
    val canStack: Boolean,
    val allowRecycle: Boolean,
    val allowTrade: Boolean,
    val allowMarketplaceSell: Boolean,
    val allowGift: Boolean,
    val allowInventoryStack: Boolean,
    val vendingIds: String
) {
    fun toDomain(furniXMLInfos: Map<String, FurniXMLInfo>): Furnishing {
        val xml = furniXMLInfos[itemName] ?: error("Can't find the XML info for the furni!")
        val itemType = ItemType.fromString(type)
        val interaction = InteractionType.fromString(interactionType)
        return Furnishing(
            itemName,
            xml.spriteId,
            xml.offerId,
            itemType,
            stackHeight.split(';').map { s -> s.trim().toDouble() },
            canStack,
            xml.canSitOn,
            xml.canLayOn,
            interaction != InteractionType.GATE && interaction != InteractionType.TELEPORT && xml.canStandOn,
            allowRecycle,
            allowTrade,
            allowMarketplaceSell,
            allowGift,
            allowInventoryStack,
            interaction,
            vendingIds.split(',').filter { it.isNotBlank() }.map { s -> s.trim().toInt() }
        )
    }
}

data class RoomItemRowDto(
    val id: Int,
    val userId: Int,
    val roomId: Int,
    val itemName: String,
    val extraData: String,
    val x: Int,
    val y: Int,
    val z: Double,
    val rot: Int,
    val wallPos: String,
    val isLimited: Boolean,
    val isBuildersClub: Boolean
) {
    fun toDomain() = RoomItem(
        id, userId, roomId, itemName, extraData,
        Vector3(x, y, z), rot, wallPos, isLimited, isBuildersClub
    )
}

data class UserItemRowDto(
    val id: Int,
    val userId: Int,
    val itemName: String,
    val extraData: String,
    val isLimited: Boolean,
    val isBuildersClub: Boolean,
    val roomId: Int? = null
) {
    fun toDomain() = UserItem(id, userId, itemName, extraData, isLimited, isBuildersClub)
}

data class WiredRowDto(
    val id: Int,
    val delay: Int,
    val items: String,
    val message: String,
    val options: String,
    val extradata: String,
    val isFilter: Boolean,
    val isInverse: Boolean,
    val furniSources: String = "",
    val userSources: String = "",
    val stuffIds2: String = "",
    val variableIds: String? = null
) {
    fun toDomain() = WiredData(
        id = id,
        delay = delay,
        items = items.toIntList(),
        message = message,
        options = options.toIntList(),
        extradata = extradata,
        filter = isFilter,
        inverse = isInverse,
        furniSources = furniSources.toIntList().mapNotNull { code -> WiredFurniSource.fromCode(code) },
        userSources = userSources.toIntList().mapNotNull { code -> WiredUserSource.fromCode(code) },
        stuffIds2 = stuffIds2.toIntList(),
        variableIds = variableIds?.split(",")?.filter { s -> s.isNotEmpty() } ?: emptyList()
    )
}

data class TeleportLinkDto(val teleportOneId: Int, val teleportTwoId: Int)
data class LinkedTeleportDto(val teleportId: Int, val roomId: Int? = 0)

data class RoomDimmerRowDto(
    val id: Int,
    val enabled: Boolean,
    val currentPreset: Int,
    val presetOne: String,
    val presetTwo: String,
    val presetThree: String
) {
    fun toDomain(roomItem: RoomItem) = RoomDimmer(
        id,
        roomItem,
        enabled,
        currentPreset,
        mutableListOf(
            RoomDimmer.generatePreset(presetOne),
            RoomDimmer.generatePreset(presetTwo),
            RoomDimmer.generatePreset(presetThree)
        )
    )
}

data class GiftDataRowDto(
    val id: Int,
    val itemName: String,
    val amount: Int,
    val extradata: String,
    val isLimited: Boolean
) {
    fun toDomain() = GiftData(id, itemName, amount, extradata, isLimited)
}
