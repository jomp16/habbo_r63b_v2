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
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.condition.WiredConditionType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(
    InteractionType.WIRED_CONDITION_TRIGGER_ON_FURNI,
    InteractionType.WIRED_CONDITION_NOT_TRIGGER_ON_FURNI
)
class WiredConditionTriggerOnFurni(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_NOT_TRIGGER_ON_FURNI

    override fun code() =
        if (isNegative) WiredConditionType.NOT_TRIGGERER_IS_ON_FURNI.code else WiredConditionType.TRIGGERER_IS_ON_FURNI.code

    override val requiresItems = true
    override val allowedFurniSources = listOf(
        WiredFurniSource.SELECTED_ITEMS,
        WiredFurniSource.TRIGGERING_ITEM,
        WiredFurniSource.SELECTOR_ITEMS
    )
    override val allowedUserSources = listOf(
        WiredUserSource.TRIGGERING_USER,
        WiredUserSource.SELECTOR_USERS
    )

    override fun onCondition(wiredContext: WiredContext): Boolean {
        // Wired 2.0: Pega os usuários E os mobis usando o resolvedor!
        val targetUsers = wiredContext.getEffectiveUsers(this)
        val targetFurnis = wiredContext.getEffectiveFurnis(this)

        if (targetUsers.isEmpty() || targetFurnis.isEmpty()) return false

        // A condição passa se TODOS os usuários alvo estiverem em cima de ALGUM dos mobis alvo
        val isOnFurni = targetUsers.all { user ->
            targetFurnis.any { furni ->
                furni.affectedTiles.contains(user.currentVector3.vector2)
            }
        }

        return if (isNegative) !isOnFurni else isOnFurni
    }
}