package tk.jomp16.habbo.game.item.wired.trigger.triggers

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.trigger.WiredTrigger
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class WiredTriggerEnterRoom(room: Room, roomItem: RoomItem) : WiredTrigger(room, roomItem) {
    private var username = ""

    init {
        if (roomItem.wiredData != null) username = roomItem.wiredData.message
    }

    override fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean {
        if (roomItem.wiredData == null) return false

        return username.isBlank() || roomUser != null && username == roomUser.habboSession!!.userInformation.username
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        if (roomItem.wiredData == null) return false

        habboRequest.readInt() // useless?

        username = habboRequest.readUTF()
        roomItem.wiredData.message = username

        return true
    }
}