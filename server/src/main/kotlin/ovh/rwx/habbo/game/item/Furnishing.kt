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

package ovh.rwx.habbo.game.item

import ovh.rwx.habbo.HabboServer

data class Furnishing(
        val itemName: String,
        val spriteId: Int,
        val offerId: Int,
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