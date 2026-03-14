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
import ovh.rwx.habbo.game.item.wired.trigger.RoomEventTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_ENTER_ROOM)
class WiredTriggerEnterRoom(room: Room, roomItem: RoomItem) : WiredTrigger<RoomEventTriggerData>(room, roomItem) {
    private var username = ""

    init {
        setData()
    }

    override fun code() = WiredTriggerType.AVATAR_ENTERS_ROOM.code

    override fun setData() {
        username = roomItem.wiredData?.message ?: ""
    }

    override fun onTrigger(wiredContext: WiredContext, data: RoomEventTriggerData): Boolean {
        if (roomItem.wiredData == null) return false

        val user = wiredContext.triggererUser // Já vem preenchido pelo WiredHandler!
        val triggered = username.isBlank() || (user != null && username == user.habboSession!!.userInformation.username)

        // Não precisa fazer mais nada!
        // O WiredHandler já colocou o RoomUser no `wiredContext.triggererUser`.
        // Qualquer Efeito com Fonte 0 (Usuário Acionador) já vai conseguir puxar ele!

        return triggered
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
