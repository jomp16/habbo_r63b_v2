package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class RoomPetErrorNotificationResponse {
    @Response(Outgoing.ROOM_PET_ERROR_NOTIFICATION)
    fun handle(habboResponse: HabboResponse, roomPetErrorNotification: RoomPetErrorNotification) {
        habboResponse.apply {
            writeInt(roomPetErrorNotification.errorCode)
        }
    }

    enum class RoomPetErrorNotification(val errorCode: Int) {
        PETS_FORBIDDEN_IN_HOTEL(0),
        PETS_FORBIDDEN_IN_ROOM(1),
        MAX_PETS(2),
        NO_FREE_TILES_FOR_PET(3),
        SELECTED_TILE_NOT_FREE_FOR_PET(4),
        MAX_NUMBER_OF_OWN_PETS(5)
    }
}