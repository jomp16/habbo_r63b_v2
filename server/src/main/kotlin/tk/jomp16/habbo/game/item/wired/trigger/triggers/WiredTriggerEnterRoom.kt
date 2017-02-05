package tk.jomp16.habbo.game.item.wired.trigger.triggers

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.trigger.WiredTrigger
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class WiredTriggerEnterRoom(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean {
        if (roomItem.wiredData == null) return false

        return roomItem.wiredData.extra2.isBlank() || roomUser != null && roomItem.wiredData.extra2 == roomUser.habboSession!!.userInformation.username
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        if (roomItem.wiredData == null) return false

        habboRequest.readInt() // useless?

        roomItem.wiredData.extra2 = habboRequest.readUTF()

        return true
    }
}