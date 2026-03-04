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
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_STATE_CHANGED)
class WiredTriggerStateChanged(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredTriggerType.STATE_CHANGE.code
    override val requiresItems = true

    override fun onTrigger(wiredContext: WiredContext, data: Any?): Boolean {
        val changedItem = data as? RoomItem ?: return false

        val items = wiredContext.getEffectiveFurnis(this)

        // Verifica se o item que mudou está na lista de monitoramento deste Wired
        val triggered = items.contains(changedItem)

        if (triggered) {
            // todo: Verifica se o usuário ativador está próximo do mobi
            if (wiredContext.triggererUser != null) {

            }

            // Define a fonte de origem para os efeitos (Fonte 0)
            wiredContext.sourceItem = changedItem
        }

        return triggered
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}