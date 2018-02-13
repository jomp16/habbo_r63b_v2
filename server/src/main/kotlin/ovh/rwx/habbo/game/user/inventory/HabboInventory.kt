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

package ovh.rwx.habbo.game.user.inventory

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.user.HabboSession
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
class HabboInventory(private val habboSession: HabboSession) {
    val items: MutableMap<Int, UserItem> = ConcurrentHashMap()
    private var initialized: Boolean = false
    private val inventoryCachePath: File = File(HabboServer.cachePath, "inventory/${habboSession.userInformation.username}").apply {
        if (!exists()) mkdirs()
    }
    private val itemsCachePath: File = File(inventoryCachePath, "items.bin")

    fun load() {
        if (!initialized) {
            if (itemsCachePath.exists()) {
                items.putAll(HabboServer.fstConfiguration.asObject(itemsCachePath.readBytes()) as MutableMap<Int, UserItem>)
            } else {
                items.putAll(ItemDao.getUserItems(habboSession.userInformation.id))

                itemsCachePath.writeBytes(HabboServer.fstConfiguration.asByteArray(items))
            }

            initialized = true
        }
    }

    fun addItems(userItems: List<UserItem>) {
        items += userItems.associateBy { it.id }

        habboSession.sendHabboResponse(Outgoing.INVENTORY_NEW_OBJECTS, true, 1, userItems.map { it.id })
        habboSession.sendHabboResponse(Outgoing.INVENTORY_UPDATE)
    }

    fun removeItems(itemIds: List<Int>, delete: Boolean = false) {
        if (!items.keys.any { itemId -> itemIds.any { it == itemId } }) return

        itemIds.forEach {
            items.remove(it)

            habboSession.sendHabboResponse(Outgoing.INVENTORY_REMOVE_OBJECT, it)
        }

        if (delete) ItemDao.deleteItems(itemIds)
    }

    fun saveCache() {
        itemsCachePath.writeBytes(HabboServer.fstConfiguration.asByteArray(items))
    }
}