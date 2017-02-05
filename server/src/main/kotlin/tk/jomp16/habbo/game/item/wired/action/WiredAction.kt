package tk.jomp16.habbo.game.item.wired.action

import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredDelayEvent
import tk.jomp16.habbo.game.item.wired.WiredItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

abstract class WiredAction(room: Room, roomItem: RoomItem) : WiredItem(room, roomItem) {
    abstract fun handle(roomUser: RoomUser?)

    open fun handle(event: WiredDelayEvent) {
    }
}