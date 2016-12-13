/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.game.room.user

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.tasks.*
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.util.Rotation
import tk.jomp16.habbo.util.Vector2
import tk.jomp16.habbo.util.Vector3
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class RoomUser(
        val habboSession: HabboSession?, // nullable, for in the future support bots
        val room: Room,
        val virtualID: Int,
        var currentVector3: Vector3,
        var headRotation: Int,
        var bodyRotation: Int
) : IHabboResponseSerialize {
    var updateNeeded: Boolean = false

    val statusMap: MutableMap<String, Pair<LocalDateTime?, String>> = ConcurrentHashMap()

    var objectiveVector2: Vector2? = null
    private var stepSeatedVector3: Vector3? = null

    private val walking: Boolean
        get() = objectiveVector2 != null
    private val stepSeated: Boolean
        get() = stepSeatedVector3 != null

    private var idleCount: Int = 0

    var idle: Boolean = false
        set(newValue) {
            idleCount =
                    if (newValue) (TimeUnit.SECONDS.toMillis(
                            HabboServer.habboConfig.timerConfig.roomIdleSeconds.toLong()) / HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toInt()
                    else 0

            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_IDLE, virtualID, newValue)

            field = newValue
        }

    var typing: Boolean = false
        set(newValue) {
            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_TYPING, virtualID, newValue)

            field = newValue
        }

    var danceId: Int = 0
        set(newValue) {
            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_DANCE, virtualID, newValue)

            field = newValue
        }

    fun addStatus(key: String, value: String = "", milliseconds: Int = -1) {
        statusMap.put(key, Pair(
                if (milliseconds == -1) null
                else LocalDateTime.now(Clock.systemUTC()).plusNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds.toLong())), value)
        )

        updateNeeded = true
    }

    fun removeStatus(key: String) {
        statusMap.remove(key)

        updateNeeded = true
    }

    fun onCycle() {
        statusMap.entries.forEach {
            if (it.value.first != null && LocalDateTime.now(Clock.systemUTC()).isAfter(it.value.first)) removeStatus(it.key)
        }

        if (stepSeated) {
            currentVector3 = stepSeatedVector3!!

            stepSeatedVector3 = null
        }

        if (walking) {
            if (objectiveVector2!! == currentVector3.vector2) {
                stopWalking()
            } else {
                // todo: override, etc
                val path = room.pathfinder.findPath(room.roomGamemap.grid, currentVector3.x, currentVector3.y, objectiveVector2!!.x, objectiveVector2!!.y)

                if (path.isEmpty()) {
                    stopWalking()
                } else {
                    removeUserStatuses()

                    val step = path.first()

                    if (room.roomGamemap.getAbsoluteHeight(step.x, step.y) - room.roomGamemap.getAbsoluteHeight(currentVector3.x, currentVector3.y) > 3) {
                        stopWalking()

                        return
                    }

                    // todo: room item handling

                    val vector2 = Vector2(step.x, step.y)

                    if (vector2 == room.roomModel.doorVector3.vector2) {
                        room.removeUser(this, true, false)

                        return
                    }

                    room.roomGamemap.updateRoomUserMovement(this, currentVector3.vector2, vector2)

                    val z = room.roomGamemap.getAbsoluteHeight(step.x, step.y)

                    addStatus("mv", "${step.x},${step.y},$z")

                    bodyRotation = Rotation.calculate(currentVector3.x, currentVector3.y, step.x, step.y)
                    headRotation = bodyRotation

                    stepSeatedVector3 = Vector3(step.x, step.y, z)
                }
            }
        } else {
            if (!idle) {
                idleCount++

                // check and commit idle state to room
                if (TimeUnit.MILLISECONDS.toSeconds((idleCount * HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toLong()) >= HabboServer.habboConfig.timerConfig.roomIdleSeconds) idle = true
            }
        }
    }

    fun moveTo(x: Int, y: Int) {
        room.roomTask?.addTask(room, UserMoveTask(this, Vector2(x, y)))
    }

    fun chat(virtualID: Int, message: String, bubble: Int, type: ChatType, skipCommands: Boolean) {
        room.roomTask?.addTask(room, UserChatTask(this, virtualID, message, bubble, type, skipCommands))
    }

    fun action(action: Int) {
        room.roomTask?.addTask(room, UserActionTask(this, action))
    }

    fun sign(sign: Int) {
        room.roomTask?.addTask(room, UserSignTask(this, sign))
    }

    fun dance(danceId: Int) {
        room.roomTask?.addTask(room, UserDanceTask(this, danceId))
    }

    private fun stopWalking() {
        objectiveVector2 = null

        removeStatus("mv")

        room.roomGamemap.getHighestItem(currentVector3.vector2)?.let {
            addUserStatuses(it)
        }
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            // todo: bot
            habboSession?.let {
                writeInt(it.userInformation.id)
                writeUTF(it.userInformation.username)
                writeUTF(it.userInformation.motto)
                writeUTF(it.userInformation.figure)
                writeInt(virtualID)
                writeInt(currentVector3.x)
                writeInt(currentVector3.y)
                writeUTF(currentVector3.z.toString())
                writeInt(0)
                writeInt(1) // 1 for user, 2 for pet, 3 for bot.
                writeUTF(it.userInformation.gender.toLowerCase())

                // todo: groups
                writeInt(-1)
                writeInt(0)
                writeUTF("")
                // end group

                writeUTF("")
                writeInt(habboSession.userStats.achievementScore)
                writeBoolean(false) // is member of builder club
            }
        }
    }

    fun removeUserStatuses() {
        removeStatus("sit")
        removeStatus("lay")

        // todo: remove effects

        updateNeeded = true
    }

    fun addUserStatuses(roomItem: RoomItem) {
        if (roomItem.furnishing.canSit || roomItem.furnishing.interactionType == InteractionType.BED) {
            addStatus(if (roomItem.furnishing.canSit) "sit" else "lay", roomItem.height.toString())
            bodyRotation = roomItem.rotation
            headRotation = roomItem.rotation

            // todo: add effects
        }

        updateNeeded = true
    }
}