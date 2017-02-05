package tk.jomp16.habbo.game.item.wired.condition

import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

abstract class WiredCondition(room: Room, roomItem: RoomItem) : WiredItem(room, roomItem) {
    abstract fun onCondition(roomUser: RoomUser?): Boolean
}