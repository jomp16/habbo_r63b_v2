package tk.jomp16.habbo.game.room.wired

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredItem
import tk.jomp16.habbo.game.item.wired.action.WiredAction
import tk.jomp16.habbo.game.item.wired.condition.WiredCondition
import tk.jomp16.habbo.game.item.wired.trigger.WiredTrigger
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.util.Vector2
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class WiredHandler {
    private val wiredStack: MutableMap<Vector2, MutableMap<Int, WiredItem>> = ConcurrentHashMap()

    fun addWiredItem(vector2: Vector2, wiredItem: WiredItem) {
        if (!wiredStack.containsKey(vector2)) wiredStack.put(vector2, mutableMapOf())

        wiredStack[vector2]!!.put(wiredItem.roomItem.id, wiredItem)
    }

    fun removeWiredItem(vector2: Vector2, roomItem: RoomItem): WiredItem? {
        return wiredStack[vector2]?.remove(roomItem.id)
    }

    fun triggerWired(triggerClass: KClass<out WiredTrigger>, roomUser: RoomUser?, data: Any?): Boolean {
        for ((vector2, wiredStackMap) in wiredStack) {
            wiredStackMap.values.forEach { wiredItem ->
                if (wiredItem is WiredTrigger && triggerClass.java.isInstance(wiredItem) && wiredItem.onTrigger(roomUser, data)) {
                    lightWired(wiredItem)

                    if (triggerCondition(vector2, roomUser)) triggerAction(vector2, roomUser)

                    return true
                }
            }
        }

        return false
    }

    private fun triggerCondition(vector2: Vector2, roomUser: RoomUser?): Boolean {
        var canExecute = true

        wiredStack[vector2]!!.values.filter { it is WiredCondition }.forEach {
            if (it is WiredCondition) {
                lightWired(it)

                if (canExecute) canExecute = it.onCondition(roomUser)
            }
        }

        return canExecute
    }

    private fun triggerAction(vector2: Vector2, roomUser: RoomUser?) {
        wiredStack[vector2]!!.values.filter { it is WiredAction }.forEach { wiredItem ->
            if (wiredItem is WiredAction) {
                lightWired(wiredItem)

                wiredItem.handle(roomUser)
            }
        }
    }

    private fun lightWired(wiredItem: WiredItem) {
        if (wiredItem.roomItem.extraData == "1") return

        wiredItem.roomItem.extraData = "1"
        wiredItem.roomItem.update(false, true)
        wiredItem.roomItem.requestCycles(1)
    }

    fun saveWired(roomItem: RoomItem, habboRequest: HabboRequest): Boolean {
        val vector2 = roomItem.position.vector2

        if (!wiredStack.containsKey(vector2) || !wiredStack[vector2]!!.containsKey(roomItem.id)) return false

        val wiredItem = wiredStack[vector2]!![roomItem.id]!!

        if (wiredItem.setData(habboRequest)) {
            roomItem.update(true, false)

            return true
        }

        return false
    }
}