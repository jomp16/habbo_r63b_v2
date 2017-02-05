package tk.jomp16.habbo.communication.incoming.room

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomWiredSaveHandler {
    @Handler(Incoming.ROOM_WIRED_SAVE_TRIGGER, Incoming.ROOM_WIRED_SAVE_EFFECT, Incoming.ROOM_WIRED_SAVE_CONDITION)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return

        val itemId = habboRequest.readInt()

        val roomItem = habboSession.currentRoom!!.roomItems[itemId] ?: return

        if (habboSession.currentRoom!!.wiredHandler.saveWired(roomItem, habboRequest)) habboSession.sendHabboResponse(Outgoing.ROOM_WIRED_SAVED)
    }
}