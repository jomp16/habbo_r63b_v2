package tk.jomp16.habbo.game.item.wired

import tk.jomp16.habbo.game.item.wired.action.WiredAction
import tk.jomp16.habbo.game.room.user.RoomUser
import java.util.concurrent.atomic.AtomicInteger

data class WiredDelayEvent(
        val wiredAction: WiredAction,
        val roomUser: RoomUser?,
        val counter: AtomicInteger = AtomicInteger(0),
        var finished: Boolean = false
)