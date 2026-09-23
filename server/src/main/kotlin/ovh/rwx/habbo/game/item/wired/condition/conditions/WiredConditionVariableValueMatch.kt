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
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.condition.WiredConditionType
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableTarget
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_CONDITION_VARIABLE_VALUE_MATCH)
class WiredConditionVariableValueMatch(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    override fun code() = WiredConditionType.VARIABLE_VALUE.code

    override val requiresItems = true
    override val requiresUsers = true

    override val allowedFurniSourceGroups: List<List<WiredFurniSource>> = listOf(
        listOf(
            WiredFurniSource.SELECTED_ITEMS,
            WiredFurniSource.TRIGGERING_ITEM,
            WiredFurniSource.SELECTOR_ITEMS,
            WiredFurniSource.SIGNAL_ITEMS,
            WiredFurniSource.ALL_ROOM_ITEMS
        ),
        listOf(
            WiredFurniSource.SELECTED_ITEMS,
            WiredFurniSource.TRIGGERING_ITEM,
            WiredFurniSource.SELECTOR_ITEMS,
            WiredFurniSource.SIGNAL_ITEMS,
            WiredFurniSource.ALL_ROOM_ITEMS
        )
    )

    override val defaultFurniSourceGroups: List<WiredFurniSource> = listOf(
        WiredFurniSource.TRIGGERING_ITEM,
        WiredFurniSource.TRIGGERING_ITEM
    )

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
        WiredUserSource.TRIGGERING_USER
    )

    override fun onCondition(wiredContext: WiredContext): Boolean {
        val options = roomItem.wiredData?.options ?: return true
        val leftTarget = WiredVariableTarget.fromCode(options.getOrNull(0) ?: WiredVariableTarget.CONTEXT.code)
            ?: WiredVariableTarget.CONTEXT
        val operator = options.getOrNull(1) ?: 1
        val operandType = options.getOrNull(2) ?: 0
        val high = options.getOrElse(3) { 0 }.toLong()
        val low = options.getOrElse(4) { 0 }.toLong() and 0xFFFFFFFFL
        val numberVal = (high shl 32) or low
        val rightTarget = WiredVariableTarget.fromCode(options.getOrNull(5) ?: WiredVariableTarget.CONTEXT.code)
            ?: WiredVariableTarget.CONTEXT

        val leftVarId = roomItem.wiredData?.variableIds?.getOrNull(0) ?: ""
        val rightVarId = roomItem.wiredData?.variableIds?.getOrNull(1) ?: ""
        if (leftVarId.isBlank()) return true

        val rightValue = if (operandType == 0) {
            numberVal
        } else {
            wiredContext.resolveVariableValue(rightTarget, rightVarId, this, 1)
        }

        return when (leftTarget) {
            WiredVariableTarget.FURNI -> {
                val furnis = wiredContext.getEffectiveFurnis(this, 0)
                furnis.isNotEmpty() && furnis.all { furni ->
                    val leftVal = (room.wiredVariableManager.getVariableValue(
                        leftVarId,
                        furni.id,
                        VariableOwnerType.FURNI
                    )?.value as? Number)?.toLong() ?: 0L
                    compare(leftVal, rightValue, operator)
                }
            }

            WiredVariableTarget.USER -> {
                val users = wiredContext.getEffectiveUsers(this, 0).filterIsInstance<RoomUser>()
                users.isNotEmpty() && users.all { user ->
                    val leftVal = (room.wiredVariableManager.getVariableValue(
                        leftVarId,
                        user.habboSession.userInformation.id,
                        VariableOwnerType.USER
                    )?.value as? Number)?.toLong() ?: 0L
                    compare(leftVal, rightValue, operator)
                }
            }

            WiredVariableTarget.GLOBAL, WiredVariableTarget.CONTEXT -> {
                val leftVal = wiredContext.resolveVariableValue(leftTarget, leftVarId, this, 0)
                compare(leftVal, rightValue, operator)
            }
        }
    }

    private fun compare(left: Long, right: Long, operator: Int): Boolean {
        return when (operator) {
            0 -> left < right
            1 -> left == right
            2 -> left > right
            3 -> left <= right
            4 -> left != right
            5 -> left >= right
            else -> true
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
