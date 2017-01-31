package tk.jomp16.habbo.game.room.tasks

import tk.jomp16.habbo.game.room.IRoomTask
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class UserHandItemTask(private val roomUser: RoomUser, private val handItem: Int) : IRoomTask {
    override fun executeTask(room: Room) {
        roomUser.idle = false
        roomUser.danceId = 0
        roomUser.handItem = handItem
    }
}