package tk.jomp16.habbo.game.item.wired.action.actions

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredDelayEvent
import tk.jomp16.habbo.game.item.wired.action.WiredAction
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.tasks.ChatType
import tk.jomp16.habbo.game.room.tasks.WiredDelayTask
import tk.jomp16.habbo.game.room.user.RoomUser

class WiredActionShowMessage(room: Room, roomItem: RoomItem) : WiredAction(room, roomItem) {
    private var message: String = ""
    private var delay: Int = 0

    init {
        if (roomItem.wiredData != null) {
            message = roomItem.wiredData.extra2
            delay = if (roomItem.wiredData.extra5.isEmpty()) 0 else roomItem.wiredData.extra5.toInt()
        }
    }

    override fun handle(roomUser: RoomUser?) {
        if (delay > 0) {
            room.roomTask?.addTask(room, WiredDelayTask(WiredDelayEvent(this, roomUser)))

            return
        }

        if (roomUser != null && !message.isBlank()) roomUser.chat(roomUser.virtualID, message, 1, ChatType.WHISPER, true)
    }

    override fun handle(event: WiredDelayEvent) {
        super.handle(event)

        if (event.counter.incrementAndGet() >= delay) {
            if (event.roomUser != null && !message.isBlank()) event.roomUser.chat(event.roomUser.virtualID, message, 1, ChatType.WHISPER, true)

            event.finished = true
        }
    }

    override fun setData(habboRequest: HabboRequest): Boolean {
        if (roomItem.wiredData == null) return false

        habboRequest.readInt() // useless?

        message = habboRequest.readUTF().trim()

        if (message.length > Byte.MAX_VALUE) message = message.substring(0, Byte.MAX_VALUE.toInt())

        habboRequest.readInt() // useless?

        delay = habboRequest.readInt()

        if (delay < 0) delay = 0
        if (delay > 20) delay = 20

        roomItem.wiredData.extra2 = message
        roomItem.wiredData.extra5 = delay.toString()

        return true
    }
}