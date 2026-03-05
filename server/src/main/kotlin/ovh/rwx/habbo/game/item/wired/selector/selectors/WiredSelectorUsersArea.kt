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
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector2

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_SELECTOR_USERS_AREA)
class WiredSelectorUsersArea(room: Room, roomItem: RoomItem) : WiredSelector(room, roomItem) {
    private var areaX = 0
    private var areaY = 0
    private var areaWidth = 1
    private var areaLength = 1

    init {
        setData()
    }

    override fun code() = WiredSelectorType.USERS_IN_AREA.code

    override fun setData() {
        roomItem.wiredData?.let {
            areaX = it.options.getOrElse(0) { 0 }
            areaY = it.options.getOrElse(1) { 0 }
            areaWidth = it.options.getOrElse(2) { 1 }
            areaLength = it.options.getOrElse(3) { 1 }
        }
    }

    override fun onSelect(context: WiredContext) {
        val mySelectedUsers = getUsersFromArea()

        refineTargets(
            contextTargets = context.targetUsers,
            currentSelection = mySelectedUsers,
            isFilter = roomItem.wiredData?.filter ?: false,
            isInverse = roomItem.wiredData?.inverse ?: false,
            allPossibleTargets = { room.userManager.users.values }
        )
    }

    private fun getUsersFromArea(): List<RoomUser> {
        val endX = areaX + areaWidth
        val endY = areaY + areaLength

        val users = mutableSetOf<RoomUser>()

        for (x in areaX until endX) {
            for (y in areaY until endY) {
                val vector2 = Vector2(x, y)
                val usersOnTile = room.roomGamemap.getUsersFromVector2(vector2)
                users.addAll(usersOnTile)
            }
        }

        return users.toList()
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
