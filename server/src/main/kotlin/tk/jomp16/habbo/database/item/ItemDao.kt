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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.*
import tk.jomp16.habbo.game.item.*
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.user.UserItem
import tk.jomp16.habbo.game.item.xml.FurniXMLInfo
import tk.jomp16.habbo.game.room.dimmer.RoomDimmer
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.util.ICacheable
import tk.jomp16.habbo.util.Vector3
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
object ItemDao : ICacheable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val limitedItemDataCache: MutableMap<Int, LimitedItemData?> = mutableMapOf()
    private val roomDimmerCache: MutableMap<Int, RoomDimmer?> = mutableMapOf()
    private val wiredDataCache: MutableMap<Int, WiredData?> = mutableMapOf()
    private val roomItemsCache: MutableMap<Int, MutableMap<Int, RoomItem>> = mutableMapOf()
    private val userItemsCache: MutableMap<Int, MutableMap<Int, UserItem>> = mutableMapOf()
    private val teleportLinksCache: MutableList<Pair<Int, Int>> = mutableListOf()

    override fun cacheAll() {
        cacheLimitedItems()
        cacheWiredItems()
        cacheTeleportLinks()
        cacheRoomItems()
        cacheUserItems()
    }

    override fun saveCache() {
        HabboServer.saveCache(LIMITED_ITEMS_KEY_CACHE, limitedItemDataCache)
        HabboServer.saveCache(WIREDS_DATA_KEY_CACHE, wiredDataCache)
        HabboServer.saveCache(TELEPORT_LINKS_KEY_CACHE, teleportLinksCache)
        HabboServer.saveCache(ROOM_ITEMS_KEY_CACHE, roomItemsCache)
        HabboServer.saveCache(USER_ITEMS_KEY_CACHE, userItemsCache)
    }

    fun getFurnishings(furniXMLInfos: Map<String, FurniXMLInfo>): List<Furnishing> {
        val cache = HabboServer.loadCache(FURNISHING_KEY_CACHE) as List<Furnishing>?

        if (cache != null) return cache
        else {
            val furnishings = HabboServer.database {
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

            HabboServer.saveCache(FURNISHING_KEY_CACHE, furnishings)

            return furnishings
        }
    }

    private fun cacheRoomItems() {
        val cache = HabboServer.loadCache(ROOM_ITEMS_KEY_CACHE) as MutableMap<Int, MutableMap<Int, RoomItem>>?

        if (cache != null) {
            roomItemsCache.putAll(cache)
        } else {
            log.info("Caching room items...")

            HabboServer.database {
                select("SELECT id, room_id, item_name, extra_data, x, y, z, rot, wall_pos, user_id FROM items WHERE room_id > 0 ORDER BY id DESC") {
                    RoomItem(
                            it.int("id"),
                            it.int("user_id"),
                            it.int("room_id"),
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
                }.let { data ->
                    data.map { it.roomId }.forEach { roomId ->
                        roomItemsCache.put(roomId, ConcurrentHashMap<Int, RoomItem>().apply { putAll(data.filter { it.roomId == roomId }.associateBy { it.id }) })
                    }
                }
            }

            log.info("Done!")
        }
    }

    fun getRoomItems(roomId: Int): MutableMap<Int, RoomItem> {
        if (!roomItemsCache.containsKey(roomId)) roomItemsCache.put(roomId, ConcurrentHashMap())

        return roomItemsCache[roomId]!!
    }

    private fun cacheUserItems() {
        val cache = HabboServer.loadCache(USER_ITEMS_KEY_CACHE) as MutableMap<Int, MutableMap<Int, UserItem>>?

        if (cache != null) {
            userItemsCache.putAll(cache)
        } else {
            log.info("Caching user items...")

            HabboServer.database {
                select("SELECT id, user_id, item_name, extra_data FROM items WHERE room_id = 0") {
                    UserItem(
                            it.int("id"),
                            it.int("user_id"),
                            it.string("item_name"),
                            it.string("extra_data")
                    )
                }.let { data ->
                    data.map { it.userId }.forEach { userId ->
                        userItemsCache.put(userId, data.filter { it.userId == userId }.associateBy { it.id }.toMutableMap())
                    }
                }
            }

            log.info("Done!")
        }
    }

    fun getUserItems(userId: Int): MutableMap<Int, UserItem> {
        if (!userItemsCache.containsKey(userId)) userItemsCache.put(userId, mutableMapOf())

        return userItemsCache[userId]!!
    }

    private fun cacheLimitedItems() {
        val cache = HabboServer.loadCache(LIMITED_ITEMS_KEY_CACHE) as MutableMap<Int, LimitedItemData?>?

        if (cache != null) {
            limitedItemDataCache.putAll(cache)
        } else {
            log.info("Caching limited items...")

            HabboServer.database {
                select("SELECT * FROM items_limited") {
                    LimitedItemData(
                            it.int("id"),
                            it.int("item_id"),
                            it.int("limited_num"),
                            it.int("limited_total")
                    )
                }.map { it.itemId to it }.forEach {
                    limitedItemDataCache.put(it.first, it.second)
                }
            }

            log.info("Done!")
        }
    }

    fun getLimitedData(itemId: Int): LimitedItemData? = limitedItemDataCache[itemId]

    private fun cacheWiredItems() {
        val cache = HabboServer.loadCache(WIREDS_DATA_KEY_CACHE) as MutableMap<Int, WiredData?>?

        if (cache != null) {
            wiredDataCache.putAll(cache)
        } else {
            log.info("Caching wired items...")

            wiredDataCache.putAll(HabboServer.database {
                select("SELECT * FROM items_wired") {
                    it.int("item_id") to WiredData(
                            it.int("id"),
                            it.int("delay"),
                            it.string("items").split(',').map(String::trim).filter(String::isNotBlank).map(String::toInt),
                            it.string("message"),
                            it.string("options").split(',').map(String::trim).filter(String::isNotBlank).map(String::toInt),
                            it.string("extradata")
                    )
                }
            })

            log.info("Done!")
        }
    }

    fun getWiredData(itemId: Int): WiredData? = wiredDataCache[itemId]

    internal fun cacheTeleportLinks() {
        val cache = HabboServer.loadCache(TELEPORT_LINKS_KEY_CACHE) as MutableList<Pair<Int, Int>>?

        if (cache != null) {
            teleportLinksCache.addAll(cache)
        } else {
            val links = HabboServer.database {
                select("SELECT * FROM items_teleport") {
                    it.int("teleport_one_id") to it.int("teleport_two_id")
                }
            }

            teleportLinksCache.addAll(links)
        }
    }

    fun getTeleportLinks(): MutableList<Pair<Int, Int>> = teleportLinksCache

    fun getLinkedTeleport(teleportId: Int): Int = HabboServer.database {
        select("SELECT room_id FROM items WHERE id = :teleport_id LIMIT 1",
                mapOf(
                        "teleport_id" to teleportId
                )
        ) {
            it.int("room_id")
        }.first()
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

    fun getRoomDimmer(roomItem: RoomItem, cache: Boolean = true): RoomDimmer? {
        if (!cache || !roomDimmerCache.containsKey(roomItem.id)) {
            val roomDimmer = HabboServer.database {
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

            roomDimmerCache.put(roomItem.id, roomDimmer)
        }

        return roomDimmerCache[roomItem.id]
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

    fun saveWireds(wireds: List<RoomItem>) {
        val wiredData = wireds.filter { it.wiredData != null }.map { it.wiredData!! }

        if (wiredData.isEmpty()) return

        HabboServer.database {
            batchUpdate("UPDATE items_wired SET delay = :delay, items = :items, message = :message, options = :options, extradata = :extradata WHERE id = :id",
                    wiredData.map {
                        mapOf(
                                "delay" to it.delay,
                                "items" to it.items.joinToString(","),
                                "message" to it.message,
                                "options" to it.options.joinToString(","),
                                "extradata" to it.extradata,
                                "id" to it.id
                        )
                    }
            )
        }
    }

    fun addItem(userId: Int, furnishing: Furnishing, extraData: String): UserItem {
        val itemId = HabboServer.database {
            insertAndGetGeneratedKey("INSERT INTO items (user_id, item_name, extra_data, wall_pos) VALUES (:user_id, :item_name, :extra_data, :wall_pos)",
                    mapOf(
                            "user_id" to userId,
                            "item_name" to furnishing.itemName,
                            "extra_data" to extraData,
                            "wall_pos" to ""
                    )
            )
        }

        return UserItem(itemId, userId, furnishing.itemName, extraData)
    }
}