package tk.jomp16.habbo.game.room.tasks

import tk.jomp16.habbo.game.item.wired.WiredDelayEvent
import tk.jomp16.habbo.game.room.IRoomTask
import tk.jomp16.habbo.game.room.Room

class WiredDelayTask(private val wiredDelayEvent: WiredDelayEvent) : IRoomTask {
    override fun executeTask(room: Room) {
        wiredDelayEvent.wiredAction.handle(wiredDelayEvent)

        if (!wiredDelayEvent.finished) room.roomTask?.addTask(room, this)
    }
}