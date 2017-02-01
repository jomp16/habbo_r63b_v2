package tk.jomp16.habbo.communication.incoming.misc

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")
class MiscEventTrackerHandler {
    @Handler(Incoming.EVENT_TRACKER)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val type = habboRequest.readUTF()
        val location = habboRequest.readUTF()
        val action = habboRequest.readUTF()
        val unknownUTF = habboRequest.readUTF()
        val unknownInt = habboRequest.readInt()
    }
}