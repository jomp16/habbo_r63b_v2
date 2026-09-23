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
import ovh.rwx.habbo.game.item.wired.trigger.SignalTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerReceiveSignal
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_SEND_SIGNAL, InteractionType.WIRED_EFFECT_NEGATIVE_SEND_SIGNAL)
class WiredEffectSendSignal(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_EFFECT_NEGATIVE_SEND_SIGNAL

    override fun code() =
        if (isNegative) WiredEffectType.NEG_SEND_SIGNAL.code else WiredEffectType.SEND_SIGNAL.code

    override val requiresItems = true
    override val requiresUsers = true

    // Grupo 0 = Antenas (SELECTED_ITEMS = 100)
    // Grupo 1 = Mobis para enviar (TRIGGERING_ITEM, SELECTED_ITEMS, SELECTOR_ITEMS, SIGNAL_ITEMS, ALL_ROOM_ITEMS)
    override val allowedFurniSourceGroups: List<List<WiredFurniSource>> = listOf(
        listOf(WiredFurniSource.SELECTED_ITEMS),
        listOf(
            WiredFurniSource.TRIGGERING_ITEM,
            WiredFurniSource.SELECTED_ITEMS,
            WiredFurniSource.SELECTOR_ITEMS,
            WiredFurniSource.SIGNAL_ITEMS,
            WiredFurniSource.ALL_ROOM_ITEMS
        )
    )

    override val defaultFurniSourceGroups: List<WiredFurniSource> = listOf(
        WiredFurniSource.SELECTED_ITEMS,
        WiredFurniSource.TRIGGERING_ITEM
    )

    // Grupo 0 = Habbos para enviar (TRIGGERING_USER, SELECTOR_USERS, SIGNAL_USERS, USER_BY_NAME, ALL_ROOM_USERS)
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
        val selectedAntennas = roomItem.wiredData?.items ?: emptyList()
        val options = roomItem.wiredData?.options ?: emptyList()
        val splitFurni = options.getOrNull(0) == 1
        val splitUsers = options.getOrNull(1) == 1

        val effectiveUsers = wiredContext.getEffectiveUsers(this)
        val effectiveFurnis = wiredContext.getEffectiveFurnis(this)

        val targetAntennas = if (selectedAntennas.isEmpty()) listOf(0) else selectedAntennas

        targetAntennas.forEach { antennaId ->
            if (antennaId > 0) {
                room.itemManager.items[antennaId]?.let { room.itemManager.wiredHandler.lightItem(it) }
            }

            if (splitUsers && effectiveUsers.isNotEmpty()) {
                effectiveUsers.forEach { user ->
                    room.itemManager.wiredHandler.triggerWired(
                        WiredTriggerReceiveSignal::class,
                        user,
                        SignalTriggerData(
                            antennaId = antennaId,
                            signalUsers = listOf(user),
                            signalFurnis = effectiveFurnis,
                            context = wiredContext
                        )
                    )
                }
            } else if (splitFurni && effectiveFurnis.isNotEmpty()) {
                effectiveFurnis.forEach { furni ->
                    room.itemManager.wiredHandler.triggerWired(
                        WiredTriggerReceiveSignal::class,
                        wiredContext.triggererUser,
                        SignalTriggerData(
                            antennaId = antennaId,
                            signalUsers = effectiveUsers,
                            signalFurnis = listOf(furni),
                            context = wiredContext
                        )
                    )
                }
            } else {
                room.itemManager.wiredHandler.triggerWired(
                    WiredTriggerReceiveSignal::class,
                    wiredContext.triggererUser,
                    SignalTriggerData(
                        antennaId = antennaId,
                        signalUsers = effectiveUsers,
                        signalFurnis = effectiveFurnis,
                        context = wiredContext
                    )
                )
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
