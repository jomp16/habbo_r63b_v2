/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredDelayEvent
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.WiredDelayTask
import ovh.rwx.habbo.game.room.user.RoomUser

@WiredItemInteractor(InteractionType.WIRED_EFFECT_RESET_TIMERS)
class WiredEffectResetTimers(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {

    init {
        setData()
    }

    override fun setData() {
        // No additional data to load
    }

    override fun handle(roomUser: RoomUser?) {
        val delay = roomItem.wiredData?.delay ?: 0
        if (delay > 0) {
            room.roomTask?.addTask(room, WiredDelayTask(WiredDelayEvent(this, roomUser)))
            return
        }
        handleThing(roomUser)
    }

    override fun handle(event: WiredDelayEvent) {
        super.handle(event)
        val delay = roomItem.wiredData?.delay ?: 0
        if (event.counter.incrementAndGet() >= delay) {
            event.finished = true
            handleThing(event.roomUser)
        }
    }

    private fun handleThing(roomUser: RoomUser?) {
        // Reset room timer
        room.roomTimer.set(0)

        // Reset all wired triggers with internal timers
        room.wiredHandler.resetTimers()
    }


    override fun writeDialog(habboResponse: HabboResponse, wiredData: WiredData) {
        habboResponse.apply {
            writeEmptyItems()
            writeItemInfo(roomItem)
            writeEmptySettings()
            writeInt(WiredEffectType.RESET_TIMERS.code)
            writeDelay(wiredData)
            writeBlockedTriggers(wiredData)
        }
    }
}