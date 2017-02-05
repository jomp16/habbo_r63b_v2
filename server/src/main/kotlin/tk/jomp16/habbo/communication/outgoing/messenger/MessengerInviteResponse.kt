package tk.jomp16.habbo.communication.outgoing.messenger

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerInviteResponse {
    @Response(Outgoing.MESSENGER_INVITE)
    fun handle(habboResponse: HabboResponse, userId: Int, message: String) {
        habboResponse.apply {
            writeInt(userId)
            writeUTF(message)
        }
    }
}