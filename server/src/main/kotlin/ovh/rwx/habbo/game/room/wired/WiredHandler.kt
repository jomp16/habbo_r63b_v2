/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

package ovh.rwx.habbo.game.room.wired

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredItem
import ovh.rwx.habbo.game.item.wired.action.WiredAction
import ovh.rwx.habbo.game.item.wired.condition.WiredCondition
import ovh.rwx.habbo.game.item.wired.trigger.WiredTrigger
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector2
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class WiredHandler {
    private val wiredStack: MutableMap<Vector2, MutableMap<Int, WiredItem>> = ConcurrentHashMap()

    fun addWiredItem(vector2: Vector2, wiredItem: WiredItem) {
        if (!wiredStack.containsKey(vector2)) wiredStack.put(vector2, mutableMapOf())

        wiredStack[vector2]!!.put(wiredItem.roomItem.id, wiredItem)
    }

    fun removeWiredItem(vector2: Vector2, roomItem: RoomItem): WiredItem? = wiredStack[vector2]?.remove(roomItem.id)

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