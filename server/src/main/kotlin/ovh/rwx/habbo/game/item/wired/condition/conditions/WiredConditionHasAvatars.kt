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

package ovh.rwx.habbo.game.item.wired.condition.conditions

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.condition.WiredConditionType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_CONDITION_HAS_AVATARS, InteractionType.WIRED_CONDITION_HAS_NO_AVATARS)
class WiredConditionHasAvatars(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_HAS_NO_AVATARS

    override fun code() =
        if (isNegative) WiredConditionType.NOT_FURNIS_HAVE_AVATARS.code else WiredConditionType.FURNIS_HAVE_AVATARS.code

    override val requiresItems = true
    override val allowedFurniSources = listOf(
        WiredFurniSource.SELECTED_ITEMS,
        WiredFurniSource.TRIGGERING_ITEM,
        WiredFurniSource.SELECTOR_ITEMS
    )

    override fun onCondition(wiredContext: WiredContext): Boolean {
        val targetFurnis = wiredContext.getEffectiveFurnis(this)

        if (targetFurnis.isEmpty()) return false

        // A regra do Habbo: TODOS os mobis alvos precisam ter um avatar em cima
        val hasAvatars = targetFurnis.all { item ->
            item.affectedTiles.any { tile ->
                // OTIMIZAÇÃO: Usar o GameMap é muito mais rápido do que iterar todos os usuários do quarto (roomUsers.values.any)
                room.roomGamemap.getEntitiesFromVector2(tile).isNotEmpty()
            }
        }

        return if (isNegative) !hasAvatars else hasAvatars
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}