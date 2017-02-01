package tk.jomp16.habbo.communication.incoming.room

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomUserSitHandler {
    @Handler(Incoming.ROOM_USER_SIT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || habboSession.roomUser == null || habboSession.roomUser!!.walking
                || habboSession.roomUser!!.statusMap.containsKey("sit") || habboSession.roomUser!!.statusMap.containsKey("lay")) return

        val sit = habboRequest.readInt() == 1

        if (sit) {
            if (habboSession.roomUser!!.bodyRotation % 2 != 0) {
                habboSession.roomUser!!.headRotation = habboSession.roomUser!!.headRotation - 1
                habboSession.roomUser!!.bodyRotation = habboSession.roomUser!!.bodyRotation - 1
            }

            habboSession.roomUser!!.addStatus("sit", "0.55")
        } else {
            habboSession.roomUser!!.removeStatus("sit")
        }

        habboSession.roomUser!!.updateNeeded = true
    }
}