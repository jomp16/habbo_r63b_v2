/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.room.user

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.util.Rotation
import tk.jomp16.habbo.util.Vector2
import tk.jomp16.habbo.util.Vector3
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

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

    private var objectiveVector2: Vector2? = null
    private var stepSeatedVector3: Vector3? = null

    private val walking: Boolean
        get() = objectiveVector2 != null
    private val stepSeated: Boolean
        get() = stepSeatedVector3 != null

    fun addStatus(key: String, value: String = "", seconds: Int = -1) {
        statusMap.put(key, Pair(if (seconds == -1) null else LocalDateTime.now().plusSeconds(seconds.toLong()), value))

        updateNeeded = true
    }

    fun removeStatus(key: String) {
        statusMap.remove(key)

        updateNeeded = true
    }

    fun onCycle() {
        statusMap.entries.forEach {
            if (it.value.first != null && LocalDateTime.now().isAfter(it.value.first)) removeStatus(it.key)
        }

        if (stepSeated) {
            currentVector3 = stepSeatedVector3!!

            stepSeatedVector3 = null
        }

        if (walking) {
            if (objectiveVector2!!.equals(currentVector3.vector2)) {
                stopWalking()
            } else {
                // todo: override, etc
                val path = room.pathfinder.findPath(room.roomGamemap.grid, currentVector3.x, currentVector3.y, objectiveVector2!!.x, objectiveVector2!!.y)

                if (path.isEmpty()) {
                    stopWalking()
                } else {
                    val step = path.first()

                    if (room.roomGamemap.getAbsoluteHeight(step.x, step.y)
                            - room.roomGamemap.getAbsoluteHeight(currentVector3.x, currentVector3.y) > 3) {
                        stopWalking()

                        return
                    }

                    // todo: room item handling

                    val vector2 = Vector2(step.x, step.y)

                    if (vector2.equals(room.roomModel.doorVector3.vector2)) {
                        room.removeUser(this, true, false)

                        return
                    }

                    room.roomGamemap.updateRoomUserMovement(this, currentVector3.vector2, vector2)

                    val z = room.roomGamemap.getAbsoluteHeight(step.x, step.y)

                    addStatus("mv", "${step.x},${step.y},${z.toString()}")

                    bodyRotation = Rotation.calculate(currentVector3.x, currentVector3.y, step.x, step.y)
                    headRotation = bodyRotation

                    stepSeatedVector3 = Vector3(step.x, step.y, z)
                }
            }
        }
    }

    fun moveTo(x: Int, y: Int) {
        objectiveVector2 = Vector2(x, y)
    }

    private fun stopWalking() {
        objectiveVector2 = null

        removeStatus("mv")
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
}