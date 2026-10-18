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

package ovh.rwx.habbo.game.user.inventory

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.inventory.UnseenItemsData
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.pet.PetData
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap

class HabboInventory(private val habboSession: HabboSession) {
    val items: MutableMap<Int, UserItem> = ConcurrentHashMap()
    val pets: MutableMap<Int, PetData> = ConcurrentHashMap()
    private var initialized: Boolean = false

    fun load() {
        if (!initialized) {
            items.putAll(ItemDao.getUserItems(habboSession.userInformation.id))
            pets.putAll(PetDao.getPetsByUserId(habboSession.userInformation.id).associateBy { it.id })

            initialized = true
        }
    }

    fun addItems(userItems: List<UserItem>) {
        items += userItems.associateBy { it.id }

        habboSession.sendResponse(
            Outgoing.INVENTORY_UNSEEN_ITEMS,
            OutgoingR63A.INVENTORY_UNSEEN_ITEMS,
            UnseenItemsData.fromUserItems(userItems)
        )

        if (habboSession.isPreShuffle) {
            habboSession.sendHabboResponse(OutgoingR63A.INVENTORY_UPDATE)
        } else {
            if (habboSession.isVersionAtLeast(2025, 4, 17)) {
                habboSession.sendHabboResponse(Outgoing.INVENTORY_FURNI_ADD_OR_UPDATE, userItems)
            } else {
                // Parser antigo: 1 item por pacote, SEM count no wire
                userItems.forEach { habboSession.sendHabboResponse(Outgoing.INVENTORY_FURNI_ADD_OR_UPDATE, listOf(it)) }
            }
        }
    }

    fun addPet(petData: PetData, openInventory: Boolean = false) {
        pets[petData.id] = petData

        habboSession.sendResponse(
            Outgoing.PET_ADDED_TO_INVENTORY,
            OutgoingR63A.PET_ADDED_TO_INVENTORY,
            petData,
            openInventory
        )
    }

    fun removeItems(itemIds: List<Int>, delete: Boolean = false) {
        if (!items.keys.any { itemId -> itemIds.any { it == itemId } }) return

        itemIds.forEach {
            items.remove(it)

            // O stripId trafega NEGATIVO em ambas as eras (convenção oficial desde R62/2010-11):
            // as listas de inventário mandam -id (UserItem), o cliente indexa por essa chave e
            // faz abs/Math.abs ao colocar no quarto — o remove manda o mesmo -id da lista.
            habboSession.sendResponse(Outgoing.INVENTORY_REMOVE_OBJECT, OutgoingR63A.INVENTORY_REMOVE_OBJECT, -it)
        }

        if (delete) ItemDao.deleteItems(itemIds)
    }

    fun removePet(petId: Int): PetData? {
        val petData = pets.remove(petId) ?: return null

        habboSession.sendResponse(Outgoing.PET_REMOVED_FROM_INVENTORY, OutgoingR63A.PET_REMOVED_FROM_INVENTORY, petData)

        return petData
    }
}