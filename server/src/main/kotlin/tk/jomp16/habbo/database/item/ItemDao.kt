/*
 * Copyright (C) 2016 jomp16
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
import tk.jomp16.habbo.game.item.Furnishing
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.LimitedItemData
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.user.UserItem
import tk.jomp16.habbo.game.item.xml.FurniXMLInfo
import tk.jomp16.habbo.game.room.dimmer.RoomDimmer
import tk.jomp16.habbo.util.Vector3

object ItemDao {
    private val limitedItemDatas: MutableMap<Int, LimitedItemData?> = mutableMapOf()

    fun getFurnishings(furniXMLInfos: Map<String, FurniXMLInfo>) = HabboServer.database {
        select("SELECT * FROM furnishings") {
            val itemName = it.string("item_name")
            val furniXMLInfo = furniXMLInfos[itemName]!!
            val itemType = ItemType.fromString(it.string("type"))
            val interactionType = InteractionType.fromString(it.string("interaction_type"))

            Furnishing(
                    itemName,
                    furniXMLInfo.spriteId,
                    itemType,
                    furniXMLInfo.xDim,
                    furniXMLInfo.yDim,
                    it.string("stack_height").split(';').map { it.trim().toDouble() },
                    it.boolean("can_stack"),
                    furniXMLInfo.canSitOn,
                    furniXMLInfo.canLayOn,
                    interactionType != InteractionType.GATE && furniXMLInfo.canStandOn,
                    it.boolean("allow_recycle"),
                    it.boolean("allow_trade"),
                    it.boolean("allow_marketplace_sell"),
                    it.boolean("allow_gift"),
                    it.boolean("allow_inventory_stack"),
                    interactionType,
                    it.int("interaction_modes_count"),
                    it.string("vending_ids").split(',').map { it.trim().toInt() }
            )
        }
    }

    fun getRoomItems(roomId: Int) = HabboServer.database {
        select("SELECT id, item_name, extra_data, x, y, z, rot, wall_pos, user_id FROM items WHERE room_id = :room_id ORDER BY id DESC",
                mapOf(
                        "room_id" to roomId
                )
        ) {
            RoomItem(
                    it.int("id"),
                    it.int("user_id"),
                    roomId,
                    it.string("item_name"),
                    it.string("extra_data"),
                    Vector3(
                            it.int("x"),
                            it.int("y"),
                            it.double("z")
                    ),
                    it.int("rot"),
                    it.string("wall_pos")
            )
        }
    }

    fun getUserItems(userId: Int) = HabboServer.database {
        select("SELECT id, item_name, extra_data FROM items WHERE room_id = 0 AND user_id = :user_id",
                mapOf(
                        "user_id" to userId
                )
        ) {
            UserItem(
                    it.int("id"),
                    userId,
                    it.string("item_name"),
                    it.string("extra_data")
            )
        }
    }

    fun getLimitedData(itemId: Int): LimitedItemData? {
        if (!limitedItemDatas.containsKey(itemId)) {
            val limitedItemData = HabboServer.database {
                select("SELECT * FROM items_limited WHERE item_id = :id",
                        mapOf(
                                "id" to itemId
                        )
                ) {
                    LimitedItemData(
                            it.int("id"),
                            itemId,
                            it.int("limited_num"),
                            it.int("limited_total")
                    )
                }.firstOrNull()
            }

            limitedItemDatas.put(itemId, limitedItemData)
        }

        return limitedItemDatas[itemId]
    }

    fun deleteItem(itemId: Int) {
        HabboServer.database {
            update("DELETE FROM items WHERE id = :id",
                    mapOf(
                            "id" to itemId
                    )
            )
        }
    }

    fun removeRoomItems(roomItemsToRemove: Collection<RoomItem>) {
        if (roomItemsToRemove.isEmpty()) return

        HabboServer.database {
            batchUpdate("UPDATE items SET room_id = :room_id WHERE id = :id",
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
        return HabboServer.database {
            select("SELECT * FROM items_dimmer WHERE item_id = :item_id LIMIT 1",
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
    }

    fun saveDimmer(roomDimmer: RoomDimmer) {
        HabboServer.database {
            update("UPDATE items_dimmer SET enabled = :enabled, current_preset = :current_preset, preset_one = :preset_one, preset_two = :preset_two, preset_three = :preset_three WHERE id = :id",
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
}