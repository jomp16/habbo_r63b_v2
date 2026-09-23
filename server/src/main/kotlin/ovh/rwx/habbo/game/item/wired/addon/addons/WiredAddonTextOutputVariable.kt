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

package ovh.rwx.habbo.game.item.wired.addon.addons

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.WiredUserSource
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableTarget
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_TEXT_OUTPUT_VARIABLE)
class WiredAddonTextOutputVariable(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.VARIABLE_PLACEHOLDER

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

    override fun onAddon(wiredContext: WiredContext) {
        val messageParts = (roomItem.wiredData?.message ?: "").split("\t")
        val placeholder = messageParts.firstOrNull()?.trim() ?: ""
        if (placeholder.isBlank()) return

        val delimiter = if (messageParts.size > 1) messageParts[1] else ", "
        val options = roomItem.wiredData?.options ?: emptyList()
        val isShowMultiple = options.getOrNull(0) == 1
        val target = WiredVariableTarget.fromCode(options.getOrNull(1) ?: WiredVariableTarget.CONTEXT.code)
            ?: WiredVariableTarget.CONTEXT
        val isTextMode = options.getOrNull(2) == 1
        val variableId = roomItem.wiredData?.variableIds?.firstOrNull() ?: placeholder

        val def = room.wiredVariableManager.getDefinition(variableId)

        fun formatVal(num: Long): String {
            return if (isTextMode || (def != null && def.textConnectors.containsKey(num.toInt()))) {
                def?.textConnectors?.get(num.toInt()) ?: num.toString()
            } else {
                num.toString()
            }
        }

        when (target) {
            WiredVariableTarget.FURNI -> {
                val furnis = wiredContext.getEffectiveFurnis(this, 0)
                val strVal = if (!isShowMultiple) {
                    val first = furnis.firstOrNull()
                    val num = if (first != null) {
                        (room.wiredVariableManager.getVariableValue(
                            variableId,
                            first.id,
                            VariableOwnerType.FURNI
                        )?.value as? Number)?.toLong() ?: 0L
                    } else 0L
                    formatVal(num)
                } else {
                    furnis.joinToString(delimiter) { furni ->
                        val num = (room.wiredVariableManager.getVariableValue(
                            variableId,
                            furni.id,
                            VariableOwnerType.FURNI
                        )?.value as? Number)?.toLong() ?: 0L
                        formatVal(num)
                    }
                }
                wiredContext.placeholders[placeholder] = strVal
                wiredContext.placeholders[variableId] = strVal
            }

            WiredVariableTarget.USER -> {
                val users = wiredContext.getEffectiveUsers(this, 0).filterIsInstance<RoomUser>()
                val strVal = if (!isShowMultiple) {
                    val first = users.firstOrNull()
                    val num = if (first != null) {
                        (room.wiredVariableManager.getVariableValue(
                            variableId,
                            first.habboSession.userInformation.id,
                            VariableOwnerType.USER
                        )?.value as? Number)?.toLong() ?: 0L
                    } else 0L
                    formatVal(num)
                } else {
                    users.joinToString(delimiter) { user ->
                        val num = (room.wiredVariableManager.getVariableValue(
                            variableId,
                            user.habboSession.userInformation.id,
                            VariableOwnerType.USER
                        )?.value as? Number)?.toLong() ?: 0L
                        formatVal(num)
                    }
                }
                wiredContext.placeholders[placeholder] = strVal
                wiredContext.placeholders[variableId] = strVal
            }

            WiredVariableTarget.GLOBAL -> {
                val num = (room.wiredVariableManager.getVariableValue(
                    variableId,
                    room.roomData.id,
                    VariableOwnerType.ROOM
                )?.value as? Number)?.toLong() ?: 0L
                val strVal = formatVal(num)
                wiredContext.placeholders[placeholder] = strVal
                wiredContext.placeholders[variableId] = strVal
            }

            WiredVariableTarget.CONTEXT -> {
                val num = (wiredContext.variables[variableId] as? Number)?.toLong()
                    ?: (wiredContext.variables[variableId] as? String)?.toLongOrNull()
                    ?: 0L
                val strVal = formatVal(num)
                wiredContext.placeholders[placeholder] = strVal
                wiredContext.placeholders[variableId] = strVal
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
