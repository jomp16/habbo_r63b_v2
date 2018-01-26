/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

import ovh.rwx.habbo.database.user.UserInformationDao
import java.time.LocalDateTime

data class GroupMember(
        val id: Int,
        val userId: Int,
        val rank: Int,
        val createdAt: LocalDateTime
) {
    val userName: String by lazy { UserInformationDao.getUserInformationById(userId)?.username ?: "null" }
    val figure: String by lazy { UserInformationDao.getUserInformationById(userId)?.figure ?: "null" }
}