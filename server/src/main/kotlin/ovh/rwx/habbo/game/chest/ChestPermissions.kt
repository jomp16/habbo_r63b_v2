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

package ovh.rwx.habbo.game.chest

import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession

object ChestPermissions {
    fun isOwner(habboSession: HabboSession, chest: ChestData): Boolean =
        chest.userId == habboSession.userInformation.id

    fun canEdit(habboSession: HabboSession, room: Room, chest: ChestData): Boolean =
        isOwner(habboSession, chest)

    fun canView(habboSession: HabboSession, room: Room, chest: ChestData): Boolean {
        if (isOwner(habboSession, chest)) return true
        if (room.userManager.hasRights(habboSession)) return true

        return chest.anyoneCanOpen
    }

    fun canWithdraw(habboSession: HabboSession, room: Room, chest: ChestData): Boolean {
        if (isOwner(habboSession, chest)) return true

        if (chest.locked) {
            // Dono do quarto pode trancar, mas apenas o dono do baú retira de um baú trancado
            return false
        }

        if (chest.isWired && room.userManager.hasRights(habboSession)) return true

        return false
    }

    fun canDonate(habboSession: HabboSession, room: Room, chest: ChestData): Boolean {
        if (isOwner(habboSession, chest)) return true

        if (chest.locked) return false

        if (chest.isWired && room.userManager.hasRights(habboSession)) return true

        return chest.anyoneCanDonate
    }
}
