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

package ovh.rwx.habbo.game.item.wired.effect.effects

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.*
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableTarget
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_CHANGE_VARIABLE_VALUE)
class WiredEffectChangeVariableValue(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    override fun code() = WiredEffectType.CHANGE_VARIABLE.code

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

    override fun onEffect(wiredContext: WiredContext) {
        val options = roomItem.wiredData?.options ?: return
        val destTarget = WiredVariableTarget.fromCode(options.getOrNull(0) ?: WiredVariableTarget.CONTEXT.code)
            ?: WiredVariableTarget.CONTEXT
        val operator = options.getOrNull(1) ?: 0
        val operandType = options.getOrNull(2) ?: 0
        val high = options.getOrElse(3) { 0 }.toLong()
        val low = options.getOrElse(4) { 0 }.toLong() and 0xFFFFFFFFL
        val numberVal = (high shl 32) or low
        val refTarget = WiredVariableTarget.fromCode(options.getOrNull(5) ?: WiredVariableTarget.CONTEXT.code)
            ?: WiredVariableTarget.CONTEXT

        val destVarId = roomItem.wiredData?.variableIds?.getOrNull(0) ?: ""
        val refVarId = roomItem.wiredData?.variableIds?.getOrNull(1) ?: ""
        if (destVarId.isBlank()) return

        val operandVal = if (operandType == 0) {
            numberVal
        } else {
            wiredContext.resolveVariableValue(refTarget, refVarId, this, 1)
        }

        when (destTarget) {
            WiredVariableTarget.FURNI -> {
                val furnis = wiredContext.getEffectiveFurnis(this, 0)
                furnis.forEach { furni ->
                    val currentVal = (room.wiredVariableManager.getVariableValue(
                        destVarId,
                        furni.id,
                        VariableOwnerType.FURNI
                    )?.value as? Number)?.toLong() ?: 0L
                    val newVal = calculateNewValue(currentVal, operandVal, operator)
                    val oldPos = furni.position.copy()
                    val oldRot = furni.rotation
                    room.wiredVariableManager.setVariableValue(destVarId, newVal, furni.id, VariableOwnerType.FURNI)
                    if (oldPos != furni.position || oldRot != furni.rotation) {
                        wiredContext.batchedMovements.add(
                            WiredFurniMove(
                                furniId = furni.id,
                                sourceX = oldPos.x,
                                sourceY = oldPos.y,
                                sourceZ = oldPos.z,
                                targetX = furni.position.x,
                                targetY = furni.position.y,
                                targetZ = furni.position.z,
                                animationTime = 500,
                                rotation = furni.rotation
                            )
                        )
                    }
                }
            }

            WiredVariableTarget.USER -> {
                val users = wiredContext.getEffectiveUsers(this, 0)
                users.filterIsInstance<RoomUser>().forEach { user ->
                    val userId = user.habboSession.userInformation.id
                    val currentVal = (room.wiredVariableManager.getVariableValue(
                        destVarId,
                        userId,
                        VariableOwnerType.USER
                    )?.value as? Number)?.toLong() ?: 0L
                    val newVal = calculateNewValue(currentVal, operandVal, operator)
                    val oldPos = user.currentVector3.copy()
                    val oldBodyRot = user.bodyRotation
                    val oldHeadRot = user.headRotation
                    room.wiredVariableManager.setVariableValue(destVarId, newVal, userId, VariableOwnerType.USER)
                    if (oldPos != user.currentVector3 || oldBodyRot != user.bodyRotation || oldHeadRot != user.headRotation) {
                        wiredContext.batchedMovements.add(
                            WiredUserMove(
                                userIndex = user.virtualID,
                                sourceX = oldPos.x,
                                sourceY = oldPos.y,
                                sourceZ = oldPos.z,
                                targetX = user.currentVector3.x,
                                targetY = user.currentVector3.y,
                                targetZ = user.currentVector3.z,
                                sliding = true,
                                animationTime = 500,
                                bodyDirection = user.bodyRotation,
                                headDirection = user.headRotation
                            )
                        )
                    }
                }
            }

            WiredVariableTarget.GLOBAL -> {
                val currentVal = (room.wiredVariableManager.getVariableValue(
                    destVarId,
                    room.roomData.id,
                    VariableOwnerType.ROOM
                )?.value as? Number)?.toLong() ?: 0L
                val newVal = calculateNewValue(currentVal, operandVal, operator)
                wiredContext.setVariable(destVarId, newVal, room.roomData.id, VariableOwnerType.ROOM)
            }

            WiredVariableTarget.CONTEXT -> {
                val currentVal = (wiredContext.variables[destVarId] as? Number)?.toLong()
                    ?: (wiredContext.variables[destVarId] as? String)?.toLongOrNull()
                    ?: 0L
                val newVal = calculateNewValue(currentVal, operandVal, operator)
                wiredContext.variables[destVarId] = newVal
                wiredContext.placeholders[destVarId] = newVal.toString()
            }
        }
    }

    private fun calculateNewValue(currentVal: Long, operandVal: Long, operator: Int): Long {
        return when (operator) {
            0 -> operandVal // Set (=)
            1 -> currentVal + operandVal // Add (+)
            2 -> currentVal - operandVal // Sub (-)
            3 -> currentVal * operandVal // Mul (*)
            4 -> if (operandVal != 0L) currentVal / operandVal else 0L // Div (/)
            5 -> if (operandVal != 0L) currentVal % operandVal else 0L // Mod (%)
            6 -> Math.pow(currentVal.toDouble(), operandVal.toDouble()).toLong() // Pow (^)
            40 -> Math.min(currentVal, operandVal) // Min
            41 -> Math.max(currentVal, operandVal) // Max
            50 -> { // Random between
                val min = Math.min(currentVal, operandVal)
                val max = Math.max(currentVal, operandVal)
                if (min < max) (min..max).random() else min
            }

            60 -> -currentVal // Negate
            100 -> Math.abs(currentVal) // Abs
            103 -> currentVal + 1 // Increment
            110 -> currentVal - 1 // Decrement
            else -> operandVal
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
