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
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableTarget
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_GIVE_VARIABLE)
class WiredEffectGiveVariable(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    override fun code() = WiredEffectType.GIVE_VARIABLE.code

    override val requiresItems = true
    override val requiresUsers = true

    override val allowedFurniSourceGroups: List<List<WiredFurniSource>> = listOf(
        listOf(
            WiredFurniSource.SELECTED_ITEMS,
            WiredFurniSource.TRIGGERING_ITEM,
            WiredFurniSource.SELECTOR_ITEMS,
            WiredFurniSource.SIGNAL_ITEMS,
            WiredFurniSource.ALL_ROOM_ITEMS
        )
    )

    override val defaultFurniSourceGroups: List<WiredFurniSource> = listOf(
        WiredFurniSource.TRIGGERING_ITEM
    )

    override val allowedUserSourceGroups: List<List<WiredUserSource>> = listOf(
        listOf(
            WiredUserSource.TRIGGERING_USER,
            WiredUserSource.SELECTOR_USERS,
            WiredUserSource.SIGNAL_USERS,
            WiredUserSource.USER_BY_NAME,
            WiredUserSource.ALL_ROOM_USERS
        )
    )

    override val defaultUserSourceGroups: List<WiredUserSource> = listOf(
        WiredUserSource.TRIGGERING_USER
    )

    override fun onEffect(wiredContext: WiredContext) {
        val options = roomItem.wiredData?.options ?: return
        val targetType = WiredVariableTarget.fromCode(options.getOrNull(0) ?: WiredVariableTarget.CONTEXT.code)
            ?: WiredVariableTarget.CONTEXT
        val high = options.getOrElse(1) { 0 }.toLong()
        val low = options.getOrElse(2) { 0 }.toLong() and 0xFFFFFFFFL
        val initialValue = (high shl 32) or low
        val overrideExisting = options.getOrNull(3) == 1

        val variableId = roomItem.wiredData?.variableIds?.firstOrNull() ?: ""
        if (variableId.isBlank()) return

        when (targetType) {
            WiredVariableTarget.FURNI -> {
                val furnis = wiredContext.getEffectiveFurnis(this, 0)
                furnis.forEach { furni ->
                    val existing =
                        room.wiredVariableManager.getVariableValue(variableId, furni.id, VariableOwnerType.FURNI)
                    if (existing == null || overrideExisting) {
                        room.wiredVariableManager.setVariableValue(
                            variableId,
                            initialValue,
                            furni.id,
                            VariableOwnerType.FURNI
                        )
                    }
                }
            }

            WiredVariableTarget.USER -> {
                val users = wiredContext.getEffectiveUsers(this, 0)
                users.filterIsInstance<RoomUser>().forEach { user ->
                    val userId = user.habboSession.userInformation.id
                    val existing =
                        room.wiredVariableManager.getVariableValue(variableId, userId, VariableOwnerType.USER)
                    if (existing == null || overrideExisting) {
                        room.wiredVariableManager.setVariableValue(
                            variableId,
                            initialValue,
                            userId,
                            VariableOwnerType.USER
                        )
                    }
                }
            }

            WiredVariableTarget.GLOBAL -> {
                val existing =
                    room.wiredVariableManager.getVariableValue(variableId, room.roomData.id, VariableOwnerType.ROOM)
                if (existing == null || overrideExisting) {
                    wiredContext.setVariable(variableId, initialValue, room.roomData.id, VariableOwnerType.ROOM)
                }
            }

            WiredVariableTarget.CONTEXT -> {
                if (!wiredContext.variables.containsKey(variableId) || overrideExisting) {
                    wiredContext.variables[variableId] = initialValue
                    wiredContext.placeholders[variableId] = initialValue.toString()
                }
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
