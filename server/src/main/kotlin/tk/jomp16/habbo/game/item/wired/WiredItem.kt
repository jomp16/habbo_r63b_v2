package tk.jomp16.habbo.game.item.wired

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room

abstract class WiredItem(protected val room: Room, val roomItem: RoomItem) {
    abstract fun setData(habboRequest: HabboRequest): Boolean
}