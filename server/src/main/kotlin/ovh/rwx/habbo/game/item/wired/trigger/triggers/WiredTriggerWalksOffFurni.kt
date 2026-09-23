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
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.FurniTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@WiredItemInteractor(InteractionType.WIRED_TRIGGER_WALKS_OFF_FURNI)
class WiredTriggerWalksOffFurni(room: Room, roomItem: RoomItem) : WiredTrigger<FurniTriggerData>(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredTriggerType.WALKS_OFF_FURNI.code
    override val requiresItems = true

    override fun onTrigger(wiredContext: WiredContext, data: FurniTriggerData): Boolean {
        val walkedItem = data.roomItem
        val items = wiredContext.getEffectiveFurnis(this)
        val matchByType = roomItem.wiredData?.furniSources?.contains(WiredFurniSource.TRIGGERING_ITEM) == true
        val triggered = if (matchByType) {
            val targetSprites = items.map { it.furnishing.spriteId }.toSet()
            walkedItem.furnishing.spriteId in targetSprites
        } else {
            items.contains(walkedItem)
        }

        if (triggered) {
            wiredContext.sourceItem = walkedItem
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