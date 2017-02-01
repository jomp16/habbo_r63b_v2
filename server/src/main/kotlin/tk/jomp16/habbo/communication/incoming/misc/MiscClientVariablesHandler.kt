package tk.jomp16.habbo.communication.incoming.misc

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")
class MiscClientVariablesHandler {
    @Handler(Incoming.CLIENT_VARIABLES)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        val gordonUrl = habboRequest.readUTF()
        val externalVariablesUrl = habboRequest.readUTF()
    }
}