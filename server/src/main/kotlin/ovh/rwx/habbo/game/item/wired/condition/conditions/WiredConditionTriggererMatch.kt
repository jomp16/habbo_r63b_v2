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
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.condition.WiredConditionType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomBot
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(
    InteractionType.WIRED_CONDITION_TRIGGERER_MATCH,
    InteractionType.WIRED_CONDITION_NOT_TRIGGERER_MATCH
)
class WiredConditionTriggererMatch(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_NOT_TRIGGERER_MATCH

    override fun code() =
        if (isNegative) WiredConditionType.NOT_TRIGGERER_MATCHES.code else WiredConditionType.TRIGGERER_MATCHES.code

    override val requiresUsers = true

    override val allowedUserSourceGroups: List<List<WiredUserSource>> = listOf(
        listOf(
            WiredUserSource.TRIGGERING_USER,
            WiredUserSource.SELECTOR_USERS,
            WiredUserSource.SIGNAL_USERS,
            WiredUserSource.USER_BY_NAME,
            WiredUserSource.ALL_ROOM_USERS
        ),
        listOf(
            WiredUserSource.TRIGGERING_USER,
            WiredUserSource.SELECTOR_USERS,
            WiredUserSource.SIGNAL_USERS,
            WiredUserSource.USER_BY_NAME,
            WiredUserSource.ALL_ROOM_USERS
        )
    )

    override val defaultUserSourceGroups: List<WiredUserSource> = listOf(
        WiredUserSource.TRIGGERING_USER,
        WiredUserSource.SELECTOR_USERS
    )

    override fun onCondition(wiredContext: WiredContext): Boolean {
        val options = roomItem.wiredData?.options ?: emptyList()
        val userType = options.getOrNull(0) ?: 1 // 1: Habbo, 2: Bot, 4: Pet
        val specificName = roomItem.wiredData?.message?.trim() ?: ""

        val usersToMatch = wiredContext.getEffectiveUsers(this, 0)
        if (usersToMatch.isEmpty()) return isNegative

        val configuredSources = roomItem.wiredData?.userSources ?: emptyList()
        val hasComparisonGroup = configuredSources.size > 1

        val matches = if (hasComparisonGroup) {
            val usersToCompare = wiredContext.getEffectiveUsers(this, 1)
            usersToMatch.all { matchUser ->
                usersToCompare.any { it.virtualID == matchUser.virtualID }
            }
        } else {
            usersToMatch.all { entity ->
                matchesTypeAndName(entity, userType, specificName)
            }
        }

        return if (isNegative) !matches else matches
    }

    private fun matchesTypeAndName(entity: RoomEntity, userType: Int, specificName: String): Boolean {
        val matchesType = when (userType) {
            1 -> entity is RoomUser
            2 -> entity is RoomBot
            4 -> entity is RoomPet
            else -> true
        }
        if (!matchesType) return false

        if (specificName.isNotBlank()) {
            val entityName = when (entity) {
                is RoomUser -> entity.habboSession.userInformation.username
                is RoomBot -> entity.botName
                is RoomPet -> entity.petData.name
                else -> ""
            }
            return entityName.equals(specificName, ignoreCase = true)
        }

        return true
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1), "")
        }
    }
}
