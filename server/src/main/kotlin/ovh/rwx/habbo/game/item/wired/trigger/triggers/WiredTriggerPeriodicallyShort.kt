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

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_PERIOD_SHORT)
class WiredTriggerPeriodicallyShort(room: Room, roomItem: RoomItem) : WiredTrigger<PeriodicTriggerData>(room, roomItem) {
    private var delay = 1 // Padrão: 50ms = 1 tick
    private var delayState = 0

    init {
        setData()
    }

    override fun code() = WiredTriggerType.PERIODIC_SHORT.code

    override fun setData() {
        roomItem.wiredData?.let {
            // O client envia em blocos de 0.05s (50ms). 1 passo = 1 tick direto.
            // Travamos entre 1 e 10 para evitar zero-ticks (loop infinito/spam) ou valores absurdos.
            delay = it.options.getOrElse(0) { 1 }.coerceIn(1, 10)
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
