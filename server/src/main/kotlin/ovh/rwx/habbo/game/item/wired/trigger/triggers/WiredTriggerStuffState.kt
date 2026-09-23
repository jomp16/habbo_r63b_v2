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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.trigger.StateTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.item.wired.trigger.WiredTriggerType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_TRIGGER_STUFF_STATE)
class WiredTriggerStuffState(room: Room, roomItem: RoomItem) : WiredTrigger<StateTriggerData>(room, roomItem) {
    private var initialized = false
    private val itemSnapshots = mutableMapOf<Int, String>()

    init {
        setData()
    }

    override fun code() = WiredTriggerType.STATE_CHANGE.code
    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let { wiredData ->
            if (!initialized) {
                if (wiredData.extradata.isNotBlank()) {
                    try {
                        val mapper = jacksonObjectMapper()
                        itemSnapshots.putAll(mapper.readValue<Map<Int, String>>(wiredData.extradata))
                    } catch (_: Exception) {
                    }
                }
                initialized = true
            } else {
                itemSnapshots.clear()
                wiredData.items.forEach { itemId ->
                    room.itemManager.items[itemId]?.let { item ->
                        itemSnapshots[itemId] = item.extraData
                    }
                }
                saveSnapshots()
            }
        }
    }

    private fun saveSnapshots() {
        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(itemSnapshots.mapKeys { it.key.toString() })
        roomItem.wiredData?.extradata = json
    }

    override fun onTrigger(wiredContext: WiredContext, data: StateTriggerData): Boolean {
        val changedItem = data.roomItem
        if (changedItem.id == roomItem.id) return false

        val items = wiredContext.getEffectiveFurnis(this)
        val matchByType = roomItem.wiredData?.furniSources?.contains(WiredFurniSource.TRIGGERING_ITEM) == true
        val triggered = if (matchByType) {
            val targetSprites = items.map { it.furnishing.spriteId }.toSet()
            changedItem.furnishing.spriteId in targetSprites
        } else {
            items.contains(changedItem)
        }
        if (!triggered) return false

        val specificStateOnly = (roomItem.wiredData?.options?.getOrNull(0) ?: 0) == 1
        if (specificStateOnly) {
            val targetState = if (matchByType) {
                itemSnapshots[changedItem.id] ?: itemSnapshots.values.firstOrNull()
            } else {
                itemSnapshots[changedItem.id]
            }
            if (targetState != null && changedItem.extraData != targetState) {
                return false
            }
        }

        wiredContext.sourceItem = changedItem
        return true
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
