/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.room.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.*
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Rotation
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import ovh.rwx.utils.pathfinding.core.Path
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
    private var oldCurrentVector3: Vector3? = null
    var objectiveVector2: Vector2? = null
    var objectiveRotation: Int = 0
    var objectiveItem: RoomItem? = null
    private var stepSeatedVector3: Vector3? = null
    val walking: Boolean
        get() = objectiveVector2 != null || ignoreBlocking && overrideBlocking && !walkingBlocked
    private val stepSeated: Boolean
        get() = stepSeatedVector3 != null
    private var idleCount: Int = 0
    private var cycles: Int = 0
    private var currentCycles: Int = 0
    private var handItemCycle: Int = 0
    private var handItemCurrentCycles: Int = 0
    var walkingBlocked: Boolean = false
    var ignoreBlocking: Boolean = false
    private var overrideBlocking: Boolean = false
    var rollerId: Int = -1
    var handleVendingId: Int = -1
    internal var path: MutableList<Path> = mutableListOf()
    var idle: Boolean = false
        set(newValue) {
            idleCount =
                    if (newValue) (TimeUnit.SECONDS.toMillis(HabboServer.habboConfig.timerConfig.roomIdleSeconds.toLong()) / HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toInt()
                    else 0

            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_IDLE, virtualID, newValue)

            field = newValue
        }
    var typing: Boolean = false
        set(newValue) {
            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_TYPING, virtualID, if (newValue) 1 else 0)

            field = newValue
        }
    var danceId: Int = 0
        set(newValue) {
            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_DANCE, virtualID, newValue)

            field = newValue
        }
    var handItem: Int = 0
        set(newValue) {
            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_HANDITEM, virtualID, newValue)

            field = newValue
        }
    var effect: RoomUserEffect? = null
        set(newValue) {
            if (field != newValue) room.sendHabboResponse(Outgoing.ROOM_USER_EFFECT, virtualID, newValue?.effectId ?: 0)

            field = newValue
            lastEffect = newValue
        }
    private var lastEffect: RoomUserEffect? = null

    fun addStatus(key: String, value: String = "", milliseconds: Int = -1) {
        statusMap[key] = Pair(
                if (milliseconds == -1) null
                else LocalDateTime.now().plusNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds.toLong())), value)

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
            oldCurrentVector3 = currentVector3
            currentVector3 = stepSeatedVector3!!

            stepSeatedVector3 = null
        }

        if (handItemCycle > 0) {
            if (handItemCurrentCycles++ >= handItemCycle) {
                if (handItem > 0) {
                    handItemCurrentCycles = 0
                    handItemCycle = 0

                    carryHandItem(0)
                }
            }
        }

        effect?.let {
            if (it.hasEffect && it.duration-- <= 0) effect = null
        }

        if (cycles > 0) {
            if (currentCycles++ >= cycles) {
                if (handleVendingId > 0) {
                    handItemCycle = 240

                    carryHandItem(handleVendingId)

                    handleVendingId = 0
                }

                walkingBlocked = false

                cycles = 0
                currentCycles = 0
            }
        }

        if (walking) {
            if (objectiveVector2 == currentVector3.vector2) {
                stopWalking()
            } else {
                if (path.isEmpty()) calculatePath()

                if (path.isEmpty()) {
                    stopWalking()
                } else {
                    var step = path.removeAt(0)

                    if (room.roomGamemap.roomUserMap[Vector2(step.x, step.y)]?.isNotEmpty() == true && !ignoreBlocking && !overrideBlocking) {
                        calculatePath()

                        if (path.isEmpty()) stopWalking()
                        else step = path.removeAt(0)
                    }

                    if (!ignoreBlocking && !overrideBlocking && room.roomGamemap.getAbsoluteHeight(step.x, step.y) - room.roomGamemap.getAbsoluteHeight(currentVector3.x, currentVector3.y) > 3) {
                        stopWalking()

                        return
                    }
                    val vector2 = Vector2(step.x, step.y)
                    val roomItem = room.roomGamemap.getHighestItem(currentVector3.vector2)
                    val roomItem1 = room.roomGamemap.getHighestItem(vector2)

                    if (roomItem != roomItem1) {
                        roomItem?.onUserWalksOff(this, true)
                        roomItem1?.onUserWalksOn(this, true)
                    }

                    if (vector2 == room.roomModel.doorVector3.vector2) {
                        room.removeUser(this, true, false)

                        return
                    }

                    room.roomGamemap.updateRoomUserMovement(this, currentVector3.vector2, vector2)
                    val z = room.roomGamemap.getAbsoluteHeight(vector2)

                    stepSeatedVector3 = Vector3(vector2, z)

                    if (rollerId == -1) {
                        removeUserStatuses()

                        bodyRotation = Rotation.calculate(currentVector3.x, currentVector3.y, step.x, step.y)
                        headRotation = bodyRotation

                        addStatus("mv", "${step.x},${step.y},$z")
                    }
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

    fun moveTo(vector2: Vector2, rotation: Int = -1, rollerId: Int = -1, ignoreBlocking: Boolean = false, actingItem: RoomItem? = null) = moveTo(vector2.x, vector2.y, rotation, rollerId, ignoreBlocking, actingItem)

    fun moveTo(x: Int, y: Int, rotation: Int = -1, rollerId: Int = -1, ignoreBlocking: Boolean = false, actingItem: RoomItem? = null): Boolean {
        if (!ignoreBlocking && !overrideBlocking && walkingBlocked) return false

        room.roomTask?.addTask(room, UserMoveTask(this, Vector2(x, y), rotation, actingItem, ignoreBlocking, rollerId))

        return true
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

    fun vendingMachine(handItem: Int) {
        room.roomTask?.addTask(room, UserVendingMachineTask(this, handItem))
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun carryHandItem(handItem: Int) {
        room.roomTask?.addTask(room, UserHandItemTask(this, handItem))
    }

    fun requestCycles(cycles1: Int) {
        if (currentCycles == 0 || cycles1 == 0) {
            cycles = cycles1
            currentCycles = 0
        }
    }

    fun stopWalking() {
        path = mutableListOf()
        stepSeatedVector3 = null
        objectiveVector2 = null
        ignoreBlocking = false

        removeStatus("mv")

        if (objectiveRotation != -1) {
            headRotation = objectiveRotation
            bodyRotation = objectiveRotation

            objectiveRotation = -1
        }

        if (objectiveItem != null) {
            objectiveItem!!.furnishing.interactor?.onTrigger(room, this, objectiveItem!!, room.hasRights(habboSession, false), 0)
            objectiveItem = null
        }

        room.roomGamemap.getHighestItem(currentVector3.vector2)?.let { addUserStatuses(it) }
    }

    private fun calculatePath() {
        if (objectiveVector2 == null) return

        path = room.pathfinder.findPath(room.roomGamemap.grid, currentVector3.x, currentVector3.y, objectiveVector2!!.x, objectiveVector2!!.y, ignoreBlocking || overrideBlocking).toMutableList()
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

                val group = habboSession.userStats.favoriteGroup

                if (group == null) {
                    writeInt(-1)
                    writeInt(0)
                    writeUTF("")
                } else {
                    writeInt(group.groupData.id)
                    writeInt(0)
                    writeUTF(group.groupData.name)
                }

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