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
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_TOGGLE_TO_RANDOM)
class WiredEffectToggleToRandom(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredEffectType.TOGGLE_TO_RANDOM_STATE.code
    override val requiresItems = true

    override fun onEffect(wiredContext: WiredContext) {
        val targets = wiredContext.getEffectiveFurnis(this)
        targets.forEach { item ->
            val currentExtra = item.extraData
            val states = item.furnishing.interactionModesCount
            if (states > 0) {
                var newState = (0..states).random()
                if (newState == currentExtra.toIntOrNull()) {
                    newState = (newState + 1) % (states + 1)
                }
                item.extraData = newState.toString()
                item.update(updateDb = true, updateClient = true)
                room.itemManager.wiredHandler.triggerWired(
                    ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerStuffState::class,
                    wiredContext.triggererUser,
                    ovh.rwx.habbo.game.item.wired.trigger.StateTriggerData(item)
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
