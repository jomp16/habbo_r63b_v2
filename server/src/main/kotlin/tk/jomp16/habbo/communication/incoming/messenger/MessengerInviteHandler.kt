package tk.jomp16.habbo.communication.incoming.messenger

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerInviteHandler {
    @Handler(Incoming.MESSENGER_INVITE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || !habboSession.habboMessenger.initialized) return

        val size = habboRequest.readInt()
        val friendsId = mutableListOf<Int>()

        repeat(size) { friendsId += habboRequest.readInt() }

        var message = habboRequest.readUTF().trim()

        if (message.isBlank()) return
        if (message.length > Byte.MAX_VALUE) message = message.substring(0, Byte.MAX_VALUE.toInt())

        habboSession.habboMessenger.friends.filterKeys { friendsId.contains(it) }.values.filter { it.online && it.habboSession != null }.forEach {
            it.habboSession?.sendHabboResponse(Outgoing.MESSENGER_INVITE, habboSession.userInformation.id, message)
        }
    }
}