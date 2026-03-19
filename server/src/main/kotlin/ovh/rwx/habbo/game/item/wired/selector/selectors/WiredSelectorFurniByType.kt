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

package ovh.rwx.habbo.game.item.wired.selector.selectors

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.selector.WiredSelector
import ovh.rwx.habbo.game.item.wired.selector.WiredSelectorType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_SELECTOR_FURNI_BY_TYPE)
class WiredSelectorFurniByType(room: Room, roomItem: RoomItem) : WiredSelector(room, roomItem) {
    private var checkState = false

    init {
        setData()
    }

    override fun code() = WiredSelectorType.FURNI_BY_TYPE.code
    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let {
            checkState = it.options.getOrElse(0) { 0 } == 1
        }
    }

    override fun onSelect(context: WiredContext) {
        val mySelectedItems = getItemsByType(context)

        refineTargets(
            contextTargets = context.targetFurnis,
            currentSelection = mySelectedItems,
            isFilter = roomItem.wiredData?.filter ?: false,
            isInverse = roomItem.wiredData?.inverse ?: false,
            allPossibleTargets = { room.itemManager.items.values }
        )
    }

    private fun getItemsByType(context: WiredContext): List<RoomItem> {
        val selectedItems = context.getEffectiveFurnis(this)

        if (selectedItems.isEmpty()) return emptyList()

        val result = mutableSetOf<RoomItem>()
        val selectedFurnishingIds = selectedItems.map { it.furnishing.itemName }.toSet()

        for (item in room.itemManager.items.values) {
            if (selectedFurnishingIds.contains(item.furnishing.itemName)) {
                if (!checkState || item.extraData == roomItem.extraData) {
                    result.add(item)
                }
            }
        }

        return result.toList()
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0), "")
        }
    }
}
