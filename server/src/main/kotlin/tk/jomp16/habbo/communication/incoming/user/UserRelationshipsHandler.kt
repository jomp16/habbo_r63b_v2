package tk.jomp16.habbo.communication.incoming.user

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.information.UserInformationDao
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.information.UserInformation

@Suppress("unused", "UNUSED_PARAMETER")
class UserRelationshipsHandler {
    @Handler(Incoming.USER_RELATIONSHIPS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val userId = habboRequest.readInt()

        // todo: relationships

        @Suppress("UNUSED_VARIABLE")
        val userInformation = UserInformationDao.getUserInformationById(userId) ?: return

        habboSession.sendHabboResponse(Outgoing.USER_RELATIONSHIPS, userId, listOf<UserInformation>(), 0, 0, 0)
    }
}