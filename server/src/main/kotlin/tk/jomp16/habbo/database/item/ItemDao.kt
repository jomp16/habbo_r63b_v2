/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.database.item

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.item.*
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.user.UserItem
import tk.jomp16.habbo.game.item.xml.FurniXMLInfo
import tk.jomp16.habbo.game.room.dimmer.RoomDimmer
import tk.jomp16.habbo.kotlin.batchInsertAndGetGeneratedKeys
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.util.Vector3

object ItemDao {
    // cache in memory all the information about items fam
    private val limitedItemDatas: MutableMap<Int, LimitedItemData?> = HashMap()
    private val wiredDatas: MutableMap<Int, WiredData?> = HashMap()
    private val roomDimmers: MutableMap<Int, RoomDimmer?> = HashMap()

    fun getFurnishings(furniXMLInfos: Map<String, FurniXMLInfo>): List<Furnishing> {
        return HabboServer.database {
            select(javaClass.classLoader.getResource("sql/furnishings/select_furnishings.sql").readText()) {
                val itemName = it.string("item_name")
                val furniXMLInfo = furniXMLInfos[itemName]!!
                val itemType = ItemType.fromString(it.string("type"))
                val interactionType = InteractionType.fromString(it.string("interaction_type"))

                Furnishing(itemName,
                        furniXMLInfo.spriteId,
                        furniXMLInfo.offerId,
                        itemType,
                        furniXMLInfo.xDim,
                        furniXMLInfo.yDim,
                        it.string("stack_height").split(';').map { it.trim().toDouble() },
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
                        it.int("interaction_modes_count"),
                        it.string("vending_ids").split(',').map { it.trim().toInt() })
            }
        }
    }

    fun getRoomItems(roomId: Int): Map<Int, RoomItem> {
        return HabboServer.database {
            select(javaClass.classLoader.getResource("sql/items/room/select_room_items.sql").readText(),
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
                        it.string("wall_pos")
                )
            }.associateBy { it.id }
        }
    }

    fun getUserItems(userId: Int): Map<Int, UserItem> {
        return HabboServer.database {
            select(javaClass.classLoader.getResource("sql/items/user/select_user_items.sql").readText(),
                    mapOf(
                            "user_id" to userId
                    )
            ) {
                UserItem(
                        it.int("id"),
                        it.int("user_id"),
                        it.string("item_name"),
                        it.string("extra_data"
                        )
                )
            }.associateBy { it.id }
        }
    }

    fun getLimitedData(itemId: Int): LimitedItemData? {
        if (!limitedItemDatas.containsKey(itemId)) {
            val limitedItemData = HabboServer.database {
                select(javaClass.classLoader.getResource("sql/items/limited/select_limited.sql").readText(),
                        mapOf(
                                "item_id" to itemId
                        )
                ) {
                    LimitedItemData(
                            it.int("id"),
                            it.int("item_id"),
                            it.int("limited_num"),
                            it.int("limited_total"
                            )
                    )
                }.firstOrNull()
            }

            limitedItemDatas.put(itemId, limitedItemData)
        }

        return limitedItemDatas[itemId]
    }

    fun getWiredData(itemId: Int): WiredData? {
        if (!wiredDatas.containsKey(itemId)) {
            val wiredData = HabboServer.database {
                select(javaClass.classLoader.getResource("sql/items/wired/select_wired_data.sql").readText(),
                        mapOf(
                                "item_id" to itemId
                        )
                ) {
                    WiredData(
                            it.int("id"),
                            it.int("delay"),
                            it.string("items").split(',').map(String::trim).filter(String::isNotBlank).map(String::toInt),
                            it.string("message"),
                            it.string("options").split(',').map(String::trim).filter(String::isNotBlank).map(String::toInt),
                            it.string("extradata")
                    )
                }.firstOrNull()
            }

            wiredDatas.put(itemId, wiredData)
        }

        return wiredDatas[itemId]
    }

    fun getTeleportLinks(): List<Pair<Int, Int>> = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/items/teleport/select_teleport_links.sql").readText()) {
            it.int("teleport_one_id") to it.int("teleport_two_id")
        }
    }

    fun getLinkedTeleport(teleportId: Int): Int = HabboServer.database {
        select(javaClass.classLoader.getResource("sql/items/teleport/select_room_id_from_linked_teleport.sql").readText(),
                mapOf(
                        "teleport_id" to teleportId
                )
        ) { it.int("room_id") }.first()
    }

    fun deleteItems(itemIds: List<Int>) {
        HabboServer.database {
            batchUpdate(javaClass.classLoader.getResource("sql/items/item/delete_item.sql").readText(),
                    itemIds.map {
                        mapOf(
                                "id" to it
                        )
                    }
            )
        }
    }

    fun removeRoomItems(roomItemsToRemove: Collection<RoomItem>) {
        if (roomItemsToRemove.isEmpty()) return

        HabboServer.database {
            batchUpdate(javaClass.classLoader.getResource("sql/items/room/update_remove_item_from_room.sql").readText(),
                    roomItemsToRemove.map {
                        mapOf(
                                "room_id" to 0,
                                "id" to it.id
                        )
                    }
            )
        }
    }

    fun getRoomDimmer(roomItem: RoomItem): RoomDimmer? {
        if (!roomDimmers.containsKey(roomItem.id)) {
            val roomDimmer = HabboServer.database {
                select(javaClass.classLoader.getResource("sql/items/dimmer/select_dimmer.sql").readText(),
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

            roomDimmers.put(roomItem.id, roomDimmer)
        }
        val roomDimmer = roomDimmers[roomItem.id] ?: return null

        roomDimmer.roomItem = roomItem

        return roomDimmer
    }

    fun saveDimmer(roomDimmer: RoomDimmer) {
        HabboServer.database {
            update(javaClass.classLoader.getResource("sql/items/dimmer/update_dimmer.sql").readText(),
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
        val wiredData = wireds.filter { it.wiredData != null }.map { it.wiredData!! }

        if (wiredData.isEmpty()) return

        HabboServer.database {
            batchUpdate(javaClass.classLoader.getResource("sql/items/wired/update_wired_data.sql").readText(),
                    wiredData.map {
                        mapOf(
                                "delay" to it.delay,
                                "items" to it.items.joinToString(","),
                                "message" to it.message,
                                "options1" to it.options.joinToString(","),
                                "extradata" to it.extradata,
                                "id" to it.id
                        )
                    }
            )
        }
    }

    fun addItems(userId: Int, furnishings: List<Furnishing>, extraDatas: List<String>): List<UserItem> {
        val itemIds = HabboServer.database {
            batchInsertAndGetGeneratedKeys(javaClass.classLoader.getResource("sql/items/item/insert_item.sql").readText(),
                    furnishings.mapIndexed { i, (itemName) ->
                        mapOf(
                                "user_id" to userId,
                                "item_name" to itemName,
                                "extra_data" to extraDatas[i],
                                "wall_pos" to ""
                        )
                    }
            )
        }

        return itemIds.mapIndexed { i, id ->
            UserItem(id, userId, furnishings[i].itemName, extraDatas[i])
        }
    }

    fun addGiftItem(userId: Int, giftFurnishing: Furnishing, amount: Int, giftExtradata: String, furnishing: Furnishing, extraData: String, limitedNumber: Int = 0, limitedTotal: Int = 0): UserItem {
        val giftUserItem = addItems(userId, listOf(giftFurnishing), listOf(giftExtradata)).first()

        if (limitedNumber > 0 && limitedTotal > 0) addLimitedItem(giftUserItem.id, limitedNumber, limitedTotal)

        HabboServer.database {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/items/gift/insert_gift.sql").readText(),
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
            select(javaClass.classLoader.getResource("sql/items/gift/select_gift.sql").readText(),
                    mapOf(
                            "item_id" to itemId
                    )
            ) {
                GiftData(
                        it.int("id"),
                        it.string("item_name"),
                        it.int("amount"),
                        it.string("extradata")
                )
            }
        }.firstOrNull()
    }

    fun addLimitedItem(itemId: Int, limitedNumber: Int, limitedTotal: Int): Int {
        return HabboServer.database {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/items/limited/insert_limited.sql").readText(),
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
            update(javaClass.classLoader.getResource("sql/items/gift/delete_gift.sql").readText(),
                    mapOf(
                            "id" to id
                    )
            )
        }
    }
}