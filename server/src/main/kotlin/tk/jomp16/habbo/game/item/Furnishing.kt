/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.item

import tk.jomp16.habbo.HabboServer

data class Furnishing(
        val itemName: String,
        val spriteId: Int,
        val type: ItemType,
        val width: Int,
        val height: Int,
        val stackHeight: List<Double>,
        val canStack: Boolean,
        val canSit: Boolean,
        val canLay: Boolean,
        val walkable: Boolean,
        val allowRecycle: Boolean,
        val allowTrade: Boolean,
        val allowMarketplaceSell: Boolean,
        val canGift: Boolean,
        val allowInventoryStack: Boolean,
        val interactionType: InteractionType,
        val interactionModesCount: Int,
        val vendingIds: List<Int>
) {
    val stackMultiple: Boolean = stackHeight.size > 1
    val interactor: ItemInteractor?
        get() = HabboServer.habboGame.itemManager.furniInteractor[interactionType]
}