package tk.jomp16.habbo.game.item.wired.trigger

import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

abstract class WiredTrigger(room: Room, roomItem: RoomItem) : WiredItem(room, roomItem) {
    abstract fun onTrigger(roomUser: RoomUser?, data: Any?): Boolean
}