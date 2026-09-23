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

package ovh.rwx.habbo.game.item.wired.selector.selectors

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.selector.WiredSelector
import ovh.rwx.habbo.game.item.wired.selector.WiredSelectorType
import ovh.rwx.habbo.game.room.Room

import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.util.Vector2

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_SELECTOR_USERS_NEIGHBORHOOD)
class WiredSelectorUsersNeighborhood(room: Room, roomItem: RoomItem) : WiredSelector(room, roomItem) {
    override fun code() = WiredSelectorType.USERS_IN_NEIGHBORHOOD.code

    override fun onSelect(context: WiredContext) {
        val wiredData = roomItem.wiredData ?: return
        val rootX = wiredData.options.getOrElse(1) { context.triggererUser?.currentVector3?.x ?: roomItem.position.x }
        val rootY = wiredData.options.getOrElse(2) { context.triggererUser?.currentVector3?.y ?: roomItem.position.y }
        val offsets = getNeighborhoodOffsets(wiredData.options)

        val selectedUsers = mutableSetOf<RoomEntity>()
        for (offset in offsets) {
            val targetTile = Vector2(rootX + offset.x, rootY + offset.y)
            selectedUsers.addAll(room.roomGamemap.getEntitiesFromVector2(targetTile))
        }

        refineTargets(
            contextTargets = context.targetUsers,
            currentSelection = selectedUsers.toList(),
            isFilter = wiredData.filter,
            isInverse = wiredData.inverse,
            allPossibleTargets = { room.userManager.entities.values }
        )
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
