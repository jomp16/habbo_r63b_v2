package tk.jomp16.habbo.communication.outgoing.messenger

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class MessengerChatErrorResponse {
    @Response(Outgoing.MESSENGER_CHAT_ERROR)
    fun handle(habboResponse: HabboResponse, messengerChatError: MessengerChatError, userId: Int, message: String) {
        habboResponse.apply {
            writeInt(messengerChatError.errorCode)
            writeInt(userId)
            writeUTF(message)
        }
    }

    enum class MessengerChatError(val errorCode: Int) {
        FRIEND_MUTED(3),
        SENDER_MUTED(4),
        FRIEND_OFFLINE(5),
        NOT_FRIENDS(6),
        FRIEND_BUSY(7),
        FRIEND_HAS_NO_CHAT(8),
        SENDER_HAS_NO_CHAT(9),
        OFFLINE_MESSAGE_FAILED(10)
    }
}