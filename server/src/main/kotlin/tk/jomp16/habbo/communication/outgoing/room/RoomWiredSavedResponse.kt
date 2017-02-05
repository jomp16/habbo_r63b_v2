package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class RoomWiredSavedResponse {
    @Response(Outgoing.ROOM_WIRED_SAVED)
    fun handle(habboResponse: HabboResponse) {
    }
}