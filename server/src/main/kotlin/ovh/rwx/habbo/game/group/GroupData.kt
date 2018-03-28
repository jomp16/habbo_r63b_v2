/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.group

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.game.room.Room
import java.time.LocalDateTime

data class GroupData(
        val id: Int,
        var name: String,
        var description: String,
        var badge: String,
        val ownerId: Int,
        val roomId: Int,
        var membershipState: GroupMembershipState,
        var symbolColor: Int,
        var backgroundColor: Int,
        var onlyAdminCanDecorateRoom: Boolean,
        val createdAt: LocalDateTime
) {
    val ownerName: String by lazy { UserInformationDao.getUserInformationById(ownerId)?.username ?: "null" }
    val room: Room by lazy { HabboServer.habboGame.roomManager.rooms[roomId]!! }
}