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

package ovh.rwx.habbo.game.item.wired.trigger.triggers

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.PeriodicTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_PERIODICALLY_LONG)
class WiredTriggerPeriodicallyLong(room: Room, roomItem: RoomItem) : WiredTrigger<PeriodicTriggerData>(room, roomItem) {
    private var delay = 100 // Padrão: 5s = 100 ticks
    private var delayState = 0

    init {
        setData()
    }

    override fun code() = WiredTriggerType.PERIODIC_LONG.code

    override fun setData() {
        roomItem.wiredData?.let {
            // O client envia em blocos de 5s. Multiplicamos por 100 para converter em ticks de 50ms.
            val rawDelay = it.options.getOrElse(0) { 1 }
            delay = rawDelay * 100
        }
    }

    override fun onTrigger(wiredContext: WiredContext, data: PeriodicTriggerData): Boolean {
        if (++delayState >= delay) {
            delayState = 0

            return true
        }

        return false
    }

    override fun resetTriggered() {
        delayState = 0
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData() = WiredData(0, 0, emptyList(), "", listOf(1), "")
    }
}