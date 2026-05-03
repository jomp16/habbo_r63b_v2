/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
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
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.UserActionTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerUserPerformsAction
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.UserMoveTask
import ovh.rwx.habbo.pathfinding.core.Path
import ovh.rwx.habbo.util.Direction
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

abstract class RoomEntity(
    val room: Room,
    val virtualID: Int,
    var currentVector3: Vector3,
    var headRotation: Int,
    var bodyRotation: Int
) : IHabboResponseSerialize {

    var updateNeeded: Boolean = false
    val statusMap: MutableMap<String, Pair<LocalDateTime?, String>> = ConcurrentHashMap()
    var pendingJoin: Boolean = true

    var objectiveVector2: Vector2? = null
    var objectiveRotation: Int = 0
    var objectiveItem: RoomItem? = null

    var nextStepVector: Vector3? = null
    private val hasPendingStep: Boolean
        get() = nextStepVector != null

    val walking: Boolean
        get() = objectiveVector2 != null || ignoreBlocking && overrideBlocking && !walkingBlocked

    var walkingBlocked: Boolean = false
    var frozen: Boolean = false
    var kicked: Boolean = false
    var ignoreBlocking: Boolean = false
    protected var overrideBlocking: Boolean = false
    var rollerId: Int = -1
    internal var path: MutableList<Path> = mutableListOf()

    private var pendingTeleport: Vector3? = null
    private var pendingTeleportRotation: Int = -1

    var effect: RoomUserEffect? = null
        set(newValue) {
            if (field != newValue) {
                onEffectChanged(newValue?.effectId ?: 0)
            }
            field = newValue
            lastEffect = newValue
        }

    protected var lastEffect: RoomUserEffect? = null

    // --- Abstract / open hooks for subclasses ---

    protected abstract fun onEffectChanged(effectId: Int)

    /** Called after a movement step is committed. Subclasses can track achievements, etc. */
    protected open fun onMovementStepCommitted() {}

    /** Called when idle counter expires. Subclasses handle idle broadcasting. */
    protected open fun onIdleExpired() {}

    /** Called during processTimers for subclass-specific timer logic. */
    protected open fun processEntityTimers() {}

    fun addStatus(key: String, value: String = "", milliseconds: Int = -1) {
        val hasStatus = statusMap.containsKey(key)

        val expiryTime = if (milliseconds == -1) null else {
            LocalDateTime.now().plusNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds.toLong()))
        }

        statusMap[key] = Pair(expiryTime, value)

        if (!hasStatus) {
            updateNeeded = true

            when (key) {
                "sit" -> triggerWiredAction(WiredTriggerUserPerformsAction.WiredUserAction.SIT)
                "lay" -> triggerWiredAction(WiredTriggerUserPerformsAction.WiredUserAction.LAY)
            }
        }
    }

    fun removeStatus(key: String) {
        val removed = statusMap.remove(key)

        updateNeeded = true

        if (removed != null && (key == "sit" || key == "lay")) {
            triggerWiredAction(WiredTriggerUserPerformsAction.WiredUserAction.STAND)
        }
    }

    private fun triggerWiredAction(action: WiredTriggerUserPerformsAction.WiredUserAction) {
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerUserPerformsAction::class, this,
            UserActionTriggerData(action)
        )
    }

    fun processTick() {
        processExpiredStatuses()
        processPendingTeleport()
        commitPendingMovementStep()
        processTimers()
        processEntityTimers()

        if (walking) {
            processWalking()
        } else {
            handleIdleCounter()
        }
    }

    private fun processExpiredStatuses() {
        val now = LocalDateTime.now()
        statusMap.entries.forEach { (key, value) ->
            if (value.first != null && now.isAfter(value.first)) {
                removeStatus(key)
            }
        }
    }

    private fun commitPendingMovementStep() {
        if (!hasPendingStep) return

        val oldPos = currentVector3.copy()
        val nextPos = nextStepVector!!

        this.currentVector3 = nextPos
        this.nextStepVector = null
        room.roomGamemap.updateRoomEntityMovement(this, oldPos.vector2, currentVector3.vector2)

        val oldItem = room.roomGamemap.getHighestItem(oldPos.vector2)
        val newItem = room.roomGamemap.getHighestItem(currentVector3.vector2)

        if (oldItem != newItem) {
            oldItem?.onEntityWalksOff(this, true)
            newItem?.onEntityWalksOn(this, true)
        }

        onMovementStepCommitted()
        this.updateNeeded = true
    }

    private fun processTimers() {
        effect?.let {
            if (it.hasEffect && it.duration-- <= 0) effect = null
        }
    }

    private fun processWalking() {
        if (frozen) {
            stopWalking()
            updateNeeded = true
            return
        }

        if (objectiveVector2 == currentVector3.vector2) {
            stopWalking()
            return
        }

        if (path.isEmpty()) calculatePath()

        if (path.isEmpty()) {
            stopWalking()
            return
        }

        var step = path.removeAt(0)
        var stepVector2 = Vector2(step.x, step.y)

        if (room.roomGamemap.isBlocked(
                stepVector2,
                ignoreUsers = ignoreBlocking,
                overrideBlocking = overrideBlocking
            )
        ) {
            calculatePath()

            if (path.isEmpty()) {
                stopWalking()
                return
            }

            step = path.removeAt(0)
            stepVector2 = Vector2(step.x, step.y)
        }

        if (!ignoreBlocking && !overrideBlocking) {
            val currentHeight = room.roomGamemap.getAbsoluteHeight(currentVector3.x, currentVector3.y)
            val stepHeight = room.roomGamemap.getAbsoluteHeight(step.x, step.y)

            if (stepHeight - currentHeight > 3) {
                stopWalking()
                return
            }
        }

        handleTileTransition(stepVector2)
    }

    protected open fun handleTileTransition(stepVector2: Vector2) {
        if (rollerId == -1) {
            bodyRotation = Direction.calculate(currentVector3.x, currentVector3.y, stepVector2.x, stepVector2.y)
            headRotation = bodyRotation
        }

        if (stepVector2 == room.roomModel.doorVector3.vector2) {
            room.userManager.removeEntity(this, notifyClient = true, kickNotification = kicked)
            return
        }

        val z = room.roomGamemap.getAbsoluteHeight(stepVector2)

        nextStepVector = Vector3(stepVector2, z)

        if (rollerId == -1 && walking) {
            removeEntityStatuses()
            addStatus("mv", "${stepVector2.x},${stepVector2.y},$z")
        }
    }

    protected open fun handleIdleCounter() {}

    fun moveTo(
        vector2: Vector2,
        rotation: Int = -1,
        rollerId: Int = -1,
        ignoreBlocking: Boolean = false,
        actingItem: RoomItem? = null
    ) = moveTo(vector2.x, vector2.y, rotation, rollerId, ignoreBlocking, actingItem)

    fun moveTo(
        x: Int,
        y: Int,
        rotation: Int = -1,
        rollerId: Int = -1,
        ignoreBlocking: Boolean = false,
        actingItem: RoomItem? = null
    ): Boolean {
        if (frozen || (!ignoreBlocking && !overrideBlocking && walkingBlocked)) return false

        val currentUserItem = room.roomGamemap.getHighestItem(currentVector3.vector2)
        var destinationVector2 = Vector2(x, y)

        if (currentUserItem != null) {
            val affectedTiles = HabboServer.habboGame.itemManager.getAffectedTiles(
                currentUserItem.position.x,
                currentUserItem.position.y,
                currentUserItem.rotation,
                currentUserItem.furnishing.width,
                currentUserItem.furnishing.length
            )

            if (affectedTiles.any { it == destinationVector2 }) {
                val xAxisChanged = currentVector3.x != x
                val yAxisChanged = currentVector3.y != y

                val shouldBlock = when (currentUserItem.rotation) {
                    0, 4 -> !xAxisChanged && yAxisChanged
                    2, 6 -> xAxisChanged && !yAxisChanged
                    else -> false
                }

                if (shouldBlock) return false
            }
        }

        val destinationItem = room.roomGamemap.getHighestItem(destinationVector2)
        if (destinationItem != null && destinationItem.furnishing.interactionType == InteractionType.BED) {
            destinationVector2 = when (destinationItem.rotation) {
                0, 4 -> Vector2(x, destinationItem.position.y)
                2, 6 -> Vector2(destinationItem.position.x, y)
                else -> Vector2(destinationItem.position.x, destinationItem.position.y)
            }
        }

        room.addTask(UserMoveTask(this, destinationVector2, rotation, actingItem, ignoreBlocking, rollerId))

        return true
    }

    fun stopWalking() {
        path.clear()
        nextStepVector = null
        objectiveVector2 = null
        ignoreBlocking = false

        removeStatus("mv")

        if (objectiveRotation != -1) {
            headRotation = objectiveRotation
            bodyRotation = objectiveRotation
            objectiveRotation = -1
        }

        objectiveItem?.let { item ->
            item.furnishing.interactor?.onTrigger(room, this, item, room.userManager.hasRights(this), 0)
            objectiveItem = null
        }

        room.roomGamemap.getHighestItem(currentVector3.vector2)?.let { addEntityStatuses(it) }
    }

    private fun calculatePath() {
        if (objectiveVector2 == null) return

        path = room.pathfinder.findPath(
            room.roomGamemap.grid,
            currentVector3.x,
            currentVector3.y,
            objectiveVector2!!.x,
            objectiveVector2!!.y,
            ignoreBlocking || overrideBlocking
        ).toMutableList()
    }

    fun removeEntityStatuses() {
        removeStatus("sit")
        removeStatus("lay")
        updateNeeded = true
    }

    fun addEntityStatuses(roomItem: RoomItem) {
        if (roomItem.furnishing.canSit || roomItem.furnishing.interactionType == InteractionType.BED) {
            addStatus(if (roomItem.furnishing.canSit) "sit" else "lay", roomItem.height.toString())
            bodyRotation = roomItem.rotation
            headRotation = roomItem.rotation

            if (roomItem.furnishing.interactionType == InteractionType.BED) {
                val oldVector3 = currentVector3
                val userPosition = oldVector3.vector2

                val finalX: Int
                val finalY: Int

                when (roomItem.rotation) {
                    0, 4 -> {
                        finalX = userPosition.x
                        finalY = roomItem.position.y
                    }

                    2, 6 -> {
                        finalX = roomItem.position.x
                        finalY = userPosition.y
                    }

                    else -> {
                        finalX = roomItem.position.x
                        finalY = roomItem.position.y
                    }
                }

                currentVector3 = Vector3(finalX, finalY, roomItem.position.z)

                if (oldVector3.x != finalX || oldVector3.y != finalY) {
                    room.roomGamemap.updateRoomEntityMovement(this, oldVector3.vector2, Vector2(finalX, finalY))
                }
            }
        }

        updateNeeded = true
    }

    fun teleportTo(vector2: Vector2, rotation: Int = -1, showSlide: Boolean = false) {
        this.stopWalking()

        val z = room.roomGamemap.getAbsoluteHeight(vector2.x, vector2.y)
        this.pendingTeleport = Vector3(vector2, z)
        this.pendingTeleportRotation = rotation
        this.effect = RoomUserEffect(4, 5)
        this.updateNeeded = true
    }

    private fun processPendingTeleport() {
        val target = pendingTeleport ?: return
        pendingTeleport = null

        val oldPos = currentVector3.copy()
        val oldItem = room.roomGamemap.getHighestItem(oldPos.vector2)

        oldItem?.onEntityWalksOff(this, true)
        this.removeEntityStatuses()

        room.roomGamemap.updateRoomEntityMovement(this, oldPos.vector2, target.vector2)
        this.currentVector3 = target

        val newItem = room.roomGamemap.getHighestItem(target.vector2)
        if (newItem != null) {
            this.addEntityStatuses(newItem)
            newItem.onEntityWalksOn(this, true)
        }

        this.updateNeeded = true
    }
}
