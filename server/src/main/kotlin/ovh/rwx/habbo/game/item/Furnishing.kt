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

package ovh.rwx.habbo.game.item

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.camera.Dimensions
import ovh.rwx.habbo.camera.SwfInfo
import kotlin.math.max

data class Furnishing(
    val itemName: String,
    val spriteId: Int,
    val offerId: Int,
    val type: ItemType,
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
    val vendingIds: List<Int>
) {
    val stackMultiple: Boolean = stackHeight.size > 1

    val interactor: ItemInteractor?
        get() = HabboServer.habboGame.itemManager.furniInteractor[interactionType]

    val swfInfo: SwfInfo? by lazy {
        HabboServer.habboGame.cameraManager.getSwfInfo(itemName)
    }

    val interactionModesCount: Int by lazy {
        max(1, swfInfo?.states?.size ?: 1)
    }

    val width: Int by lazy {
        val swfX = swfInfo?.dimensions?.x?.toInt() ?: 1
        // Se for item de parede (W), permitimos 0. Se for piso (S), mínimo 1.
        if (type == ItemType.WALL) swfX else max(1, swfX)
    }

    val height: Int by lazy {
        val swfY = swfInfo?.dimensions?.y?.toInt() ?: 1
        // Se for item de parede (W), permitimos 0. Se for piso (S), mínimo 1.
        if (type == ItemType.WALL) swfY else max(1, swfY)
    }

    val swfDimensions: Dimensions? by lazy {
        swfInfo?.dimensions
    }

    val allowedDirections: List<Int> by lazy {
        swfInfo?.directions?.ifEmpty { listOf(0) } ?: listOf(0)
    }
}