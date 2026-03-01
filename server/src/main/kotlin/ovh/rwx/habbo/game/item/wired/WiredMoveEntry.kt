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

package ovh.rwx.habbo.game.item.wired

sealed interface WiredMoveEntry

data class WiredUserMove(
    val userIndex: Int,
    val sourceX: Int,
    val sourceY: Int,
    val sourceZ: Double,
    val targetX: Int,
    val targetY: Int,
    val targetZ: Double,
    val sliding: Boolean, // 0 = mv, 1 = sld
    val animationTime: Int,
    val bodyDirection: Int,
    val headDirection: Int,
) : WiredMoveEntry

data class WiredFurniMove(
    val furniId: Int,
    val sourceX: Int,
    val sourceY: Int,
    val sourceZ: Double,
    val targetX: Int,
    val targetY: Int,
    val targetZ: Double,
    val animationTime: Int = 500,
    val rotation: Int
) : WiredMoveEntry

data class WiredWallItemMove(
    val itemId: Int,
    val isDirectionRight: Boolean,
    val oldWallX: Int,
    val oldWallY: Int,
    val oldOffsetX: Int,
    val oldOffsetY: Int,
    val newWallX: Int,
    val newWallY: Int,
    val newOffsetX: Int,
    val newOffsetY: Int,
    val animationTime: Int = 500
) : WiredMoveEntry

data class WiredUserDirection(
    val userIndex: Int,
    val bodyDirection: Int,
    val headDirection: Int
) : WiredMoveEntry