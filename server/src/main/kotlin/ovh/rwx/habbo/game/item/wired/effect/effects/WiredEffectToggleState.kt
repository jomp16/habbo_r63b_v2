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
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.item.wired.trigger.StateTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStuffState
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_TOGGLE_STATE)
class WiredEffectToggleState(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredEffectType.TOGGLE_STATE.code
    override val requiresItems = true

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveFurnis(this)
        targets.forEach { item ->
            val interactor = item.furnishing.interactor
            if (interactor != null) {
                interactor.onTrigger(item.room, null, item, true, 0)
                item.room.itemManager.wiredHandler.triggerWired(
                    WiredTriggerStuffState::class,
                    wiredContext.triggererUser,
                    StateTriggerData(item)
                )
            } else {
                val modes = item.furnishing.interactionModesCount - 1
                if (modes > 0) {
                    var currentMode = item.extraData.toIntOrNull() ?: 0
                    if (++currentMode > modes) currentMode = 0
                    item.extraData = currentMode.toString()
                    item.update(updateDb = true, updateClient = true)
                    item.room.itemManager.wiredHandler.triggerWired(
                        WiredTriggerStuffState::class,
                        wiredContext.triggererUser,
                        StateTriggerData(item)
                    )
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
