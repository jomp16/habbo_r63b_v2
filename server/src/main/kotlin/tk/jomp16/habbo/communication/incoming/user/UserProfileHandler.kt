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

package tk.jomp16.habbo.communication.incoming.user

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.user.UserInformationDao
import tk.jomp16.habbo.database.user.UserStatsDao
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class UserProfileHandler {
    @Handler(Incoming.USER_PROFILE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return
        val userId = habboRequest.readInt()
        val showProfile = habboRequest.readBoolean()
        val userInformation = UserInformationDao.getUserInformationById(userId) ?: return
        val userStats = UserStatsDao.getUserStats(userId)
        val friends = HabboServer.database {
            if (userId != UserInformationDao.serverConsoleUserInformation.id) {
                select("SELECT COUNT(*) AS `friends_count` FROM `messenger_friendships` WHERE `user_one_id` = :user_one_id",
                        mapOf(
                                "user_one_id" to userId
                        )
                ) { it.int("friends_count") }.first()
            } else {
                select("SELECT COUNT(*) AS `friends_count` FROM `users` WHERE `rank` IN (SELECT `rank` FROM `permissions_ranks` WHERE `acc_server_console` = :acc_server_console) OR `id` IN (SELECT `user_id` FROM `permissions_users` WHERE `acc_server_console` = :acc_server_console)",
                        mapOf(
                                "acc_server_console" to true
                        )
                ) { it.int("friends_count") }.first()
            }
        }
        val isFriend = habboSession.habboMessenger.friends.containsKey(userId)
        val isRequest = habboSession.habboMessenger.requests.containsKey(userId)
        val isOnline = userId == UserInformationDao.serverConsoleUserInformation.id || HabboServer.habboSessionManager.containsHabboSessionById(userId)

        habboSession.sendHabboResponse(Outgoing.USER_PROFILE, userInformation, userStats, showProfile, friends, isFriend, isRequest, isOnline)
    }
}