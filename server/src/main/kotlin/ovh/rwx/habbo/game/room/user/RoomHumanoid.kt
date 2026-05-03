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
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.*
import ovh.rwx.habbo.util.Vector3
import java.util.concurrent.TimeUnit

abstract class RoomHumanoid(
    room: Room,
    virtualID: Int,
    currentVector3: Vector3,
    headRotation: Int,
    bodyRotation: Int
) : RoomEntity(room, virtualID, currentVector3, headRotation, bodyRotation) {

    private var idleCount: Int = 0
    private var ticks: Int = 0
    private var currentTick: Int = 0
    private var handItemTicks: Int = 0
    private var handItemCurrentTick: Int = 0
    internal var headResetTick: Int = 0
    var handleVendingId: Int = -1

    var idle: Boolean = false
        set(newValue) {
            idleCount = if (newValue) {
                (TimeUnit.SECONDS.toMillis(HabboServer.habboConfig.timerConfig.roomIdleSeconds.toLong()) / HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toInt()
            } else 0

            if (field != newValue) {
                room.sendHabboResponse(Outgoing.ROOM_USER_IDLE, virtualID, newValue)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_IDLE, virtualID, newValue)
            }

            field = newValue
        }

    var typing: Boolean = false
        set(newValue) {
            if (field != newValue) {
                val state = if (newValue) 1 else 0
                room.sendHabboResponse(Outgoing.ROOM_USER_TYPING, virtualID, state)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_TYPING, virtualID, state)
            }

            field = newValue
        }

    var danceId: Int = 0
        set(newValue) {
            if (field != newValue) {
                room.sendHabboResponse(Outgoing.ROOM_USER_DANCE, virtualID, newValue)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_DANCE, virtualID, newValue)
            }

            field = newValue
        }

    var handItem: Int = 0
        set(newValue) {
            if (field != newValue) {
                room.sendHabboResponse(Outgoing.ROOM_USER_HANDITEM, virtualID, newValue)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_HANDITEM, virtualID, newValue)
            }

            field = newValue
        }

    override fun onEffectChanged(effectId: Int) {
        room.sendHabboResponse(Outgoing.ROOM_USER_EFFECT, virtualID, effectId)
        room.sendHabboResponse(OutgoingR63A.ROOM_USER_EFFECT, virtualID, effectId)
    }

    override fun onIdleExpired() {
        idle = true
    }

    override fun processEntityTimers() {
        if (handItemTicks > 0 && ++handItemCurrentTick >= handItemTicks) {
            if (handItem > 0) {
                handItemCurrentTick = 0
                handItemTicks = 0
                carryHandItem(0)
            }
        }

        if (headResetTick > 0 && --headResetTick == 0) {
            if (!walking && !idle) {
                headRotation = bodyRotation
                updateNeeded = true
            }
        }

        if (ticks > 0 && ++currentTick >= ticks) {
            if (handleVendingId > 0) {
                handItemTicks = 240
                carryHandItem(handleVendingId)
                handleVendingId = 0
            }

            walkingBlocked = false
            ticks = 0
            currentTick = 0
        }
    }

    override fun handleIdleCounter() {
        if (idle) return

        idleCount++
        val secondsIdle =
            TimeUnit.MILLISECONDS.toSeconds((idleCount * HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toLong())

        if (secondsIdle >= HabboServer.habboConfig.timerConfig.roomIdleSeconds) {
            idle = true
        }
    }

    fun action(action: UserAction) {
        room.addTask(UserActionTask(this, action))
    }

    fun sign(sign: Int) {
        room.addTask(UserSignTask(this, sign))
    }

    fun dance(danceId: Int) {
        room.addTask(UserDanceTask(this, danceId))
    }

    fun vendingMachine(handItem: Int) {
        room.addTask(UserVendingMachineTask(this, handItem))
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun carryHandItem(handItem: Int) {
        room.addTask(UserHandItemTask(this, handItem))
    }

    fun requestTicks(ticks1: Int) {
        if (currentTick == 0 || ticks1 == 0) {
            ticks = ticks1
            currentTick = 0
        }
    }
}
