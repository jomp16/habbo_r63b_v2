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
import ovh.rwx.habbo.game.item.wired.*
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.item.wired.effect.WiredEffectType
import ovh.rwx.habbo.game.room.Room

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EFFECT_FURNI_TO_USER)
class WiredEffectFurniToUser(room: Room, roomItem: RoomItem) : WiredEffect(room, roomItem) {
    init {
        setData()
    }

    override fun code() = WiredEffectType.MOVE_FURNI_TO_USER.code
    override val requiresItems = true
    override val requiresUsers = true

    override val allowedFurniSources = listOf(
        WiredFurniSource.SELECTED_ITEMS,
        WiredFurniSource.SELECTOR_ITEMS,
        WiredFurniSource.SIGNAL_ITEMS,
        WiredFurniSource.TRIGGERING_ITEM,
        WiredFurniSource.ALL_ROOM_ITEMS
    )
    override val defaultFurniSource = WiredFurniSource.SELECTED_ITEMS

    override val allowedUserSources = listOf(
        WiredUserSource.TRIGGERING_USER,
        WiredUserSource.SELECTOR_USERS,
        WiredUserSource.SIGNAL_USERS,
        WiredUserSource.USER_BY_NAME,
        WiredUserSource.ALL_ROOM_USERS
    )
    override val defaultUserSource = WiredUserSource.TRIGGERING_USER

    override fun onEffect(wiredContext: WiredContext) {
        val furnis = wiredContext.getEffectiveFurnis(this)
        val targets = wiredContext.getEffectiveUsers(this)

        if (furnis.isEmpty() || targets.isEmpty()) return

        targets.forEach { targetUser ->
            val userPos = targetUser.currentVector3.vector2

            furnis.forEach { item ->
                val oldPos = item.position.copy()

                if (room.itemManager.setFloorItem(item, userPos, item.rotation, null)) {
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
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
