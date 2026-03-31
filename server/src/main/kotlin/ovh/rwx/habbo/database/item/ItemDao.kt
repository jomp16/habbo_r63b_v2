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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.*
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.xml.FurniXMLInfo
import ovh.rwx.habbo.game.room.dimmer.RoomDimmer
import ovh.rwx.habbo.kotlin.batchInsertAndGetGeneratedKeys
import ovh.rwx.habbo.kotlin.batchUpdate
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey
import ovh.rwx.habbo.kotlin.toIntList
import ovh.rwx.habbo.util.Vector3

object ItemDao {
    // cache in memory all the information about items fam
    private val limitedItemDatas: MutableMap<Int, LimitedItemData?> = mutableMapOf()
    private val wiredDatas: MutableMap<Int, WiredData?> = mutableMapOf()
    private val roomDimmers: MutableMap<Int, RoomDimmer?> = mutableMapOf()

    fun getFurnishings(furniXMLInfos: Map<String, FurniXMLInfo>): List<Furnishing> {
        return HabboServer.database {
            select(javaClass.classLoader.getResource("sql/furnishings/select_furnishings.sql").readText()) {
                val itemName = it.string("item_name")
                val furniXMLInfo = furniXMLInfos[itemName] ?: error("Can't find the XML info for the furni!")
                val itemType = ItemType.fromString(it.string("type"))
                val interactionType = InteractionType.fromString(it.string("interaction_type"))

                Furnishing(
                    itemName,
                    furniXMLInfo.spriteId,
                    furniXMLInfo.offerId,
                    itemType,
                    it.string("stack_height").split(';').map { s -> s.trim().toDouble() },
                    it.boolean("can_stack"),
                    furniXMLInfo.canSitOn,
                    furniXMLInfo.canLayOn,
                    interactionType != InteractionType.GATE && interactionType != InteractionType.TELEPORT && furniXMLInfo.canStandOn,
                    it.boolean("allow_recycle"),
                    it.boolean("allow_trade"),
                    it.boolean("allow_marketplace_sell"),
                    it.boolean("allow_gift"),
                    it.boolean("allow_inventory_stack"),
                    interactionType,
                    it.string("vending_ids").split(',').map { s -> s.trim().toInt() })
            }
        }
    }

    fun getRoomItems(roomId: Int): Map<Int, RoomItem> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/items/room/select_room_items.sql").readText(),
                mapOf(
                    "room_id" to roomId
                )
            ) {
                RoomItem(
                    it.int("id"),
                    it.int("user_id"),
                    it.int("room_id"),
                    it.string("item_name"),
                    it.string("extra_data"),
                    Vector3(it.int("x"), it.int("y"), it.double("z")),
                    it.int("rot"),
                    it.string("wall_pos"),
                    it.boolean("is_limited"),
                    it.boolean("is_builders_club")
                )
            }.associateBy { it.id }
        }
    }

    fun getUserItems(userId: Int): Map<Int, UserItem> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/items/user/select_user_items.sql").readText(),
                mapOf(
                    "user_id" to userId
                )
            ) {
                UserItem(
                    it.int("id"),
                    it.int("user_id"),
                    it.string("item_name"),
                    it.string("extra_data"),
                    it.boolean("is_limited"),
                    it.boolean("is_builders_club")
                )
            }.associateBy { it.id }
        }
    }

    fun getAllUserItems(userId: Int): List<UserItem> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/items/user/select_all_user_items.sql").readText(),
                mapOf(
                    "user_id" to userId
                )
            ) {
                UserItem(
                    it.int("id"),
                    it.int("user_id"),
                    it.string("item_name"),
                    it.string("extra_data"),
                    it.boolean("is_limited"),
                    it.boolean("is_builders_club")
                )
            }
        }
    }

    data class UserItemWithRoom(val userItem: UserItem, val roomId: Int?)

    fun getAllUserItemsWithRoom(userId: Int): List<UserItemWithRoom> {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/items/user/select_all_user_items.sql").readText(),
                mapOf(
                    "user_id" to userId
                )
            ) {
                UserItemWithRoom(
                    UserItem(
                        it.int("id"),
                        it.int("user_id"),
                        it.string("item_name"),
                        it.string("extra_data"),
                        it.boolean("is_limited"),
                        it.boolean("is_builders_club")
                    ),
                    it.intOrNull("room_id")
                )
            }
        }
    }

    fun getLimitedData(itemId: Int): LimitedItemData? {
        if (!limitedItemDatas.containsKey(itemId)) {
            val limitedItemData = HabboServer.database {
                select(
                    javaClass.classLoader.getResource("sql/items/limited/select_limited.sql").readText(),
                    mapOf(
                        "item_id" to itemId
                    )
                ) {
                    LimitedItemData(
                        it.int("id"),
                        it.int("item_id"),
                        it.int("limited_num"),
                        it.int("limited_total")
                    )
                }.firstOrNull()
            }

            limitedItemDatas[itemId] = limitedItemData
        }

        return limitedItemDatas[itemId]
    }

    fun getWiredData(itemId: Int): WiredData? {
        if (!wiredDatas.containsKey(itemId)) {
            val wiredData = HabboServer.database {
                select(
                    javaClass.classLoader.getResource("sql/items/wired/select_wired_data.sql").readText(),
                    mapOf(
                        "item_id" to itemId
                    )
                ) {
                    WiredData(
                        id = it.int("id"),
                        delay = it.int("delay"),
                        items = it.string("items").toIntList(),
                        message = it.string("message"),
                        options = it.string("options").toIntList(),
                        extradata = it.string("extradata"),
                        filter = it.boolean("is_filter"),
                        inverse = it.boolean("is_inverse"),

                        // --- NOVOS CAMPOS ---
                        furniSources = it.string("furni_sources").toIntList()
                            .mapNotNull { code -> WiredFurniSource.fromCode(code) },
                        userSources = it.string("user_sources").toIntList()
                            .mapNotNull { code -> WiredUserSource.fromCode(code) },
                        stuffIds2 = it.string("stuff_ids2").toIntList()
                    )
                }.firstOrNull()
            }

            if (wiredData != null) {
                wiredDatas[itemId] = wiredData
            }
        }

        return wiredDatas[itemId]
    }

    fun getTeleportLinks(): List<Pair<Int, Int>> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/items/teleport/select_teleport_links.sql").readText()) {
            it.int("teleport_one_id") to it.int("teleport_two_id")
        }
    }

    fun getLinkedTeleport(teleportIds: Set<Int>): List<Pair<Int, Int>> = HabboServer.database {
        select(
            javaClass.classLoader.getResource("sql/items/teleport/select_room_id_from_linked_teleport.sql").readText(),
            mapOf(
                "teleport_ids" to teleportIds
            )
        ) { it.int("teleport_id") to (it.intOrNull("room_id") ?: 0) }
    }

    fun deleteItems(itemIds: List<Int>) {
        HabboServer.database {
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

        HabboServer.database {
            batchUpdate(
                javaClass.classLoader.getResource("sql/items/item/update_item_room.sql").readText(),
                roomItemsToRemove.map {
                    mapOf(
                        "room_id" to null,
                        "id" to it.id
                    )
                }
            )
        }
    }

    fun getRoomDimmer(roomItem: RoomItem): RoomDimmer? {
        if (!roomDimmers.containsKey(roomItem.id)) {
            val roomDimmer = HabboServer.database {
                select(
                    javaClass.classLoader.getResource("sql/items/dimmer/select_dimmer.sql").readText(),
                    mapOf(
                        "item_id" to roomItem.id
                    )
                ) {
                    RoomDimmer(
                        it.int("id"),
                        roomItem,
                        it.boolean("enabled"),
                        it.int("current_preset"),
                        mutableListOf(
                            RoomDimmer.generatePreset(it.string("preset_one")),
                            RoomDimmer.generatePreset(it.string("preset_two")),
                            RoomDimmer.generatePreset(it.string("preset_three"))
                        )
                    )
                }.firstOrNull()
            }

            roomDimmers[roomItem.id] = roomDimmer
        }
        val roomDimmer = roomDimmers[roomItem.id] ?: return null

        roomDimmer.roomItem = roomItem

        return roomDimmer
    }

    fun saveDimmer(roomDimmer: RoomDimmer) {
        HabboServer.database {
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

        HabboServer.database {
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
        val itemIds = HabboServer.database {
            batchInsertAndGetGeneratedKeys(
                javaClass.classLoader.getResource("sql/items/item/insert_item.sql").readText(),
                itemPurchaseDatas.map { itemPurchaseData ->
                    mapOf(
                        "user_id" to userId,
                        "item_name" to itemPurchaseData.furnishing.itemName,
                        "extra_data" to itemPurchaseData.extraData,
                        "wall_pos" to "",
                        "is_builders_club" to itemPurchaseData.buildersClub
                    )
                }
            )
        }

        val userItems = itemIds.mapIndexed { i, id ->
            UserItem(
                id,
                userId,
                itemPurchaseDatas[i].furnishing.itemName,
                itemPurchaseDatas[i].extraData,
                itemPurchaseDatas[i].limited,
                itemPurchaseDatas[i].buildersClub
            )
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

        HabboServer.database {
            insertAndGetGeneratedKey(
                javaClass.classLoader.getResource("sql/items/gift/insert_gift.sql").readText(),
                mapOf(
                    "item_id" to giftUserItem.id,
                    "item_name" to furnishing.itemName,
                    "amount" to amount,
                    "extradata" to extraData
                )
            )
        }

        return giftUserItem
    }

    fun getGiftData(itemId: Int): GiftData? {
        return HabboServer.database {
            select(
                javaClass.classLoader.getResource("sql/items/gift/select_gift.sql").readText(),
                mapOf(
                    "item_id" to itemId
                )
            ) {
                GiftData(
                    it.int("id"),
                    it.string("item_name"),
                    it.int("amount"),
                    it.string("extradata"),
                    it.boolean("is_limited")
                )
            }
        }.firstOrNull()
    }

    fun addLimitedItem(itemId: Int, limitedNumber: Int, limitedTotal: Int): Int {
        return HabboServer.database {
            insertAndGetGeneratedKey(
                javaClass.classLoader.getResource("sql/items/limited/insert_limited.sql").readText(),
                mapOf(
                    "item_id" to itemId,
                    "limited_num" to limitedNumber,
                    "limited_total" to limitedTotal
                )
            )
        }
    }

    fun deleteGiftData(id: Int) {
        HabboServer.database {
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
                        it.extraData,
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

            HabboServer.database {
                insertAndGetGeneratedKey(
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
                    ignoreSpecialHandling = true,
                ).first()

                HabboServer.database {
                    batchInsertAndGetGeneratedKeys(
                        "INSERT INTO `items_teleport` (`teleport_one_id`, `teleport_two_id`) VALUES (:teleport_one_id, :teleport_two_id)",
                        listOf(
                            mapOf("teleport_one_id" to userItem.id, "teleport_two_id" to teleporterItem.id),
                            mapOf("teleport_two_id" to userItem.id, "teleport_one_id" to teleporterItem.id)
                        )
                    )
                }

                HabboServer.habboGame.itemManager.teleportLinks[userItem.id] = teleporterItem.id
                HabboServer.habboGame.itemManager.roomTeleportLinks[userItem.id] = 0
                HabboServer.habboGame.itemManager.teleportLinks[teleporterItem.id] = userItem.id
                HabboServer.habboGame.itemManager.roomTeleportLinks[teleporterItem.id] = 0
            }

            InteractionType.DIMMER -> {
                HabboServer.database {
                    insertAndGetGeneratedKey(
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

        HabboServer.database {
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

            if (allUpdates.isEmpty()) return@database

            // Executa todas as atualizações em batch
            // A query valida que o item pertence ao usuário antes de atualizar
            val updateSql = javaClass.classLoader
                .getResource("sql/items/trade/transfer_trade_items.sql")
                ?.readText()!!

            val rowsAffectedList = batchUpdate(this, updateSql, allUpdates)
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
}

data class ItemPurchaseData(
    val furnishing: Furnishing,
    val extraData: String,
    val limited: Boolean,
    val buildersClub: Boolean,
)