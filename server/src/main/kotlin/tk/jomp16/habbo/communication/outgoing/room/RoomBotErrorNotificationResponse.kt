package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class RoomBotErrorNotificationResponse {
    @Response(Outgoing.ROOM_BOT_ERROR_NOTIFICATION)
    fun handle(habboResponse: HabboResponse, roomBotErrorNotification: RoomBotErrorNotification) {
        habboResponse.apply {
            writeInt(roomBotErrorNotification.errorCode)
        }
    }

    enum class RoomBotErrorNotification(val errorCode: Int) {
        BOTS_FORBIDDEN_IN_HOTEL(0),
        BOTS_FORBIDDEN_IN_ROOM(1),
        BOT_LIMIT_REACHED(2),
        SELECTED_TILE_NOT_FREE_FOR_BOT(3),
        BOT_NAME_NOT_ACCEPTED(4),
    }
}