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

            writeInt(Math.ceil(Instant.now(Clock.systemUTC()).epochSecond.toDouble() - userStats.lastOnline.toEpochSecond(ZoneOffset.UTC).toDouble()).toInt())
            writeBoolean(showProfile)
        }
    }
}