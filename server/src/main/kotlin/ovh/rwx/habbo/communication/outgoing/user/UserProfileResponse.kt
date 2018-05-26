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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.information.UserInformation
import ovh.rwx.habbo.game.user.information.UserStats
import java.time.Instant
import java.time.ZoneId

@Suppress("unused", "UNUSED_PARAMETER")
class UserProfileResponse {
    @Response(Outgoing.USER_PROFILE)
    fun response(habboResponse: HabboResponse, userInformation: UserInformation, userStats: UserStats, showProfile: Boolean, friends: Int, isFriend: Boolean, isRequest: Boolean, isOnline: Boolean) {
        habboResponse.apply {
            writeInt(userInformation.id)
            writeUTF(userInformation.username)
            writeUTF(userInformation.figure)
            writeUTF(userInformation.motto)
            writeUTF(userInformation.accountCreated.format(HabboServer.DATE_TIME_FORMATTER_ONLY_DAYS))
            writeInt(userStats.achievementScore)
            writeInt(friends)
            writeBoolean(isFriend)
            writeBoolean(isRequest)
            writeBoolean(isOnline)
            // todo: groups
            writeInt(0)

            writeInt(Math.ceil(Instant.now().epochSecond.toDouble() - userStats.lastOnline.atZone(ZoneId.systemDefault()).toEpochSecond().toDouble()).toInt())
            writeBoolean(showProfile)
        }
    }
}