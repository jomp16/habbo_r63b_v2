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

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_SELECTOR_USERS_BY_TYPE)
class WiredSelectorUsersByType(room: Room, roomItem: RoomItem) : WiredSelector(room, roomItem) {
    private var userType = UserType.HABBO

    init {
        setData()
    }

    override fun code() = WiredSelectorType.USERS_BY_TYPE.code

    override fun setData() {
        roomItem.wiredData?.let {
            userType = UserType.fromCode(it.options.getOrElse(0) { 1 })
        }
    }

    override fun onSelect(context: WiredContext) {
        val mySelectedUsers = getUsersByType()

        refineTargets(
            contextTargets = context.targetUsers,
            currentSelection = mySelectedUsers,
            isFilter = roomItem.wiredData?.filter ?: false,
            isInverse = roomItem.wiredData?.inverse ?: false,
            allPossibleTargets = { room.userManager.users.values }
        )
    }

    private fun getUsersByType(): List<RoomUser> {
        return when (userType) {
            UserType.HABBO -> room.userManager.users.values.filter { it.habboSession != null }
            UserType.PET -> {
                // TODO: implementar mascotes/pets
                emptyList()
            }

            UserType.BOT -> {
                // TODO: implementar bots
                emptyList()
            }
        }
    }

    private enum class UserType(val code: Int) {
        HABBO(1),
        PET(2),
        BOT(4);

        companion object {
            fun fromCode(code: Int) = entries.find { it.code == code } ?: HABBO
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", listOf(1), "")
        }
    }
}
