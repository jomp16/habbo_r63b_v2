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

package ovh.rwx.habbo.game.item.wired.condition.conditions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.condition.WiredConditionType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_CONDITION_MATCH_SNAPSHOT, InteractionType.WIRED_CONDITION_NOT_MATCH_SNAPSHOT)
class WiredConditionMatchSnapshot(room: Room, roomItem: RoomItem) : WiredCondition(room, roomItem) {
    private val isNegative = roomItem.furnishing.interactionType == InteractionType.WIRED_CONDITION_NOT_MATCH_SNAPSHOT

    private var setState = false
    private var setDirection = false
    private var setPosition = false
    private var setAltitude = false
    private var initialized = false
    private val itemSnapshots = mutableMapOf<Int, ItemSnapshot>()

    data class ItemSnapshot(val x: Int, val y: Int, val z: Double, val rotation: Int, val extraData: String)

    init {
        setData()
    }

    override fun code() =
        if (isNegative) WiredConditionType.NOT_STATES_MATCH.code else WiredConditionType.STATES_MATCH.code

    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let { wiredData ->
            setState = wiredData.options.getOrElse(0) { 0 } == 1
            setDirection = wiredData.options.getOrElse(1) { 0 } == 1
            setPosition = wiredData.options.getOrElse(2) { 0 } == 1
            setAltitude = wiredData.options.getOrElse(3) { 0 } == 1

            if (!initialized) {
                if (wiredData.extradata.isNotBlank()) {
                    try {
                        val mapper = jacksonObjectMapper()
                        itemSnapshots.putAll(mapper.readValue<Map<Int, ItemSnapshot>>(wiredData.extradata))
                    } catch (_: Exception) {
                    }
                }
                initialized = true
            } else {
                itemSnapshots.clear()
                wiredData.items.forEach { itemId ->
                    room.itemManager.items[itemId]?.let { item ->
                        itemSnapshots[itemId] = ItemSnapshot(
                            item.position.x,
                            item.position.y,
                            item.position.z,
                            item.rotation,
                            item.extraData
                        )
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

    override fun onCondition(wiredContext: WiredContext): Boolean {
        val items = wiredContext.getEffectiveFurnis(this)
        if (items.isEmpty()) return false

        val allMatch = items.all { item ->
            val snapshot = itemSnapshots[item.id] ?: return@all false
            val stateMatch = !setState || item.extraData == snapshot.extraData
            val dirMatch = !setDirection || item.rotation == snapshot.rotation
            val posMatch = !setPosition || (item.position.x == snapshot.x && item.position.y == snapshot.y)
            val altMatch = !setAltitude || item.position.z == snapshot.z
            stateMatch && dirMatch && posMatch && altMatch
        }

        return if (isNegative) !allMatch else allMatch
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
