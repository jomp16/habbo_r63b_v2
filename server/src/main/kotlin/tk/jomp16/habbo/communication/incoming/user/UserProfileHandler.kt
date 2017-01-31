package tk.jomp16.habbo.communication.incoming.user

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.database.information.UserStatsDao
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
            if (userId != Int.MAX_VALUE) {
                select("SELECT COUNT(*) as friends_count FROM messenger_friendships WHERE user_one_id = :user_one_id",
                        mapOf(
                                "user_one_id" to userId
                        )
                ) { it.int("friends_count") }.first()
            } else {
                select("SELECT COUNT(*) as friends_count FROM users WHERE rank IN (SELECT rank FROM permissions_ranks WHERE acc_server_console = :acc_server_console) OR id IN (SELECT user_id FROM permissions_users WHERE acc_server_console = :acc_server_console)",
                        mapOf(
                                "acc_server_console" to true
                        )
                ) { it.int("friends_count") }.first()
            }
        }

        val isFriend = habboSession.habboMessenger.friends.containsKey(userId)
        val isRequest = habboSession.habboMessenger.requests.containsKey(userId)
        val isOnline = userId == Int.MAX_VALUE || HabboServer.habboSessionManager.containsHabboSessionById(userId)

        habboSession.sendHabboResponse(Outgoing.USER_PROFILE, userInformation, userStats, showProfile, friends, isFriend, isRequest, isOnline)
    }
}