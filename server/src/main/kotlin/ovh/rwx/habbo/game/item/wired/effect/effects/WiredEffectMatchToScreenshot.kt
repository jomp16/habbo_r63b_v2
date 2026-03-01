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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredFurniMove
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.util.Vector2

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_MATCH_TO_SCREENSHOT)
class WiredEffectMatchToScreenshot(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    private var setState = false
    private var setDirection = false
    private var setPosition = false
    private var setHeight = false
    private var initialized = false
    private val itemSnapshots = mutableMapOf<Int, ItemSnapshot>()

    data class ItemSnapshot(val x: Int, val y: Int, val z: Double, val rotation: Int, val extraData: String)

    init {
        setData()
    }

    override fun code() = WiredEffectType.MATCH_SSHOT.code
    override val requiresItems = true

    override fun setData() {
        roomItem.wiredData?.let { wiredData ->
            setState = wiredData.options.getOrElse(0) { 0 } == 1
            setDirection = wiredData.options.getOrElse(1) { 0 } == 1
            setPosition = wiredData.options.getOrElse(2) { 0 } == 1
            setHeight = wiredData.options.getOrElse(3) { 0 } == 1

            // Load snapshots from extradata JSON
            if (!initialized) {
                if (wiredData.extradata.isNotBlank()) {
                    try {
                        val mapper = jacksonObjectMapper()
                        itemSnapshots.putAll(mapper.readValue(wiredData.extradata))
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }

                initialized = true
            } else {
                itemSnapshots.clear()

                wiredData.items.forEach { itemId ->
                    room.roomItems[itemId]?.let { item ->
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

    override fun onEffect(wiredContext: WiredContext) {
        roomItem.wiredData?.items?.forEach { itemId ->
            val item = room.roomItems[itemId] ?: return@forEach
            val snapshot = itemSnapshots[itemId] ?: return@forEach

            // Check if item needs to be restored
            val needsPositionChange = setPosition && (item.position.x != snapshot.x || item.position.y != snapshot.y)
            val needsRotationChange = setDirection && item.rotation != snapshot.rotation
            val needsHeightChange = setHeight && item.position.z != snapshot.z
            val needsStateChange = setState && item.extraData != snapshot.extraData

            if (needsPositionChange || needsRotationChange || needsHeightChange) {
                val newX = if (setPosition) snapshot.x else item.position.x
                val newY = if (setPosition) snapshot.y else item.position.y
                val newZ = if (setHeight) snapshot.z else item.position.z
                val newRotation = if (setDirection) snapshot.rotation else item.rotation

                val oldPos = item.position.copy() // Salva posição original

                if (room.setFloorItem(item, Vector2(newX, newY), newRotation, null, newZ)) {
                    // Adiciona ao acumulador do ciclo
                    wiredContext.batchedMovements.add(
                        WiredFurniMove(
                            furniId = item.id,
                            sourceX = oldPos.x,
                            sourceY = oldPos.y,
                            sourceZ = oldPos.z,
                            targetX = item.position.x,
                            targetY = item.position.y,
                            targetZ = item.position.z,
                            rotation = item.rotation
                        )
                    )
                }
            }

            if (needsStateChange) {
                item.extraData = snapshot.extraData
                item.update(updateDb = true, updateClient = true)
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(0, 0, 0, 0), "")
        }
    }
}