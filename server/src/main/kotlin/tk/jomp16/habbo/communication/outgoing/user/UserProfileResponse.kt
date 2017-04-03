/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.communication.outgoing.user

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.information.UserInformation
import tk.jomp16.habbo.game.user.information.UserStats
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("unused", "UNUSED_PARAMETER")
class UserProfileResponse {
    @Response(Outgoing.USER_PROFILE)
    fun handle(habboResponse: HabboResponse, userInformation: UserInformation, userStats: UserStats, showProfile: Boolean, friends: Int, isFriend: Boolean, isRequest: Boolean, isOnline: Boolean) {
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

            writeInt(if (isOnline) 0 else Math.ceil(Instant.now(Clock.systemUTC()).epochSecond.toDouble() - userStats.lastOnline.toEpochSecond(ZoneOffset.UTC).toDouble()).toInt())
            writeBoolean(showProfile)
        }
    }
}