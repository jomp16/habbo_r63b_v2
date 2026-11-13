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

package ovh.rwx.habbo.communication.outgoing.inventory

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.user.UserItem

enum class UnseenItemCategory(val code: Int) {
    OWNED_FURNI(1),
    RENTED_FURNI(2),
    PET(3),
    BADGE(4),
    BOT(5),
    GAMES(6),
    COLLECTIBLES(7),
    HABBICON(8);

    companion object {
        private val BY_CODE = entries.associateBy { it.code }

        fun fromCode(code: Int): UnseenItemCategory = BY_CODE[code] ?: OWNED_FURNI
    }
}

data class UnseenItemsData(
    val items: Map<UnseenItemCategory, Collection<Int>>,
    val userItems: Collection<UserItem>? = null,
) {
    companion object {
        fun single(category: UnseenItemCategory, ids: Collection<Int>): UnseenItemsData {
            return if (ids.isEmpty()) {
                UnseenItemsData(emptyMap())
            } else {
                UnseenItemsData(mapOf(category to ids))
            }
        }

        fun fromUserItems(userItems: Collection<UserItem>): UnseenItemsData {
            if (userItems.isEmpty()) return UnseenItemsData(emptyMap())

            return UnseenItemsData(emptyMap(), userItems = userItems)
        }
    }
}

@Suppress("unused", "UNUSED_PARAMETER")
class InventoryUnseenItemsResponse {
    @Response(Outgoing.INVENTORY_UNSEEN_ITEMS)
    @ResponseR63A(OutgoingR63A.INVENTORY_UNSEEN_ITEMS)
    fun response(habboResponse: HabboResponse, data: UnseenItemsData) {
        habboResponse.apply {
            val categoriesToSend: Map<Int, Collection<Int>> = if (data.userItems != null) {
                if (isVersionBefore(2013, 5, 22)) {
                    // Pré-2013-05-22 (R63A e R63B antigo): abas Floor (1) e Wall (2) separadas
                    val floorIds = data.userItems.filter { it.furnishing.type == ItemType.FLOOR }.map { it.id }
                    val wallIds = data.userItems.filter { it.furnishing.type != ItemType.FLOOR }.map { it.id }
                    buildMap {
                        if (floorIds.isNotEmpty()) put(1, floorIds)
                        if (wallIds.isNotEmpty()) put(2, wallIds)
                    }
                } else {
                    // Pós-2013-05-22 (Moderno/AIR): aba única de mobis (OWNED_FURNI = 1).
                    // O client moderno indexa o inventário e o grid (GroupItem.getFurniIds)
                    // pelo stripId NEGATIVO (-id). Para o slot ficar verde (isUnseen), o id trafega negativo.
                    mapOf(UnseenItemCategory.OWNED_FURNI.code to data.userItems.map { -it.id })
                }
            } else {
                data.items.mapKeys { it.key.code }
            }

            writeInt(categoriesToSend.size)

            categoriesToSend.forEach { (categoryCode, itemIds) ->
                writeInt(categoryCode)
                writeInt(itemIds.size)
                itemIds.forEach { id ->
                    writeInt(id)
                }
            }
        }
    }
}