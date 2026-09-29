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

package ovh.rwx.habbo.game.snowwar

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.snowwar.enums.SnowWarEventType
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTrajectory
import ovh.rwx.habbo.game.snowwar.objects.SnowWarSnowball

interface ISnowWarGameEvent {
    val type: SnowWarEventType
    val eventTypeId: Int get() = type.id
    fun serialize(response: HabboResponse)
    fun apply(game: SnowWarGame) {}
}

data class HumanLeftGameEvent(
    val humanGameObjectId: Int
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.HUMAN_LEFT
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
    }

    override fun apply(game: SnowWarGame) {
        val user = game.users.values.find { it.objectId == humanGameObjectId } ?: return
        game.users.remove(user.userId)
    }
}

data class NewMoveTargetGameEvent(
    val humanGameObjectId: Int,
    val x: Int,
    val y: Int
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.NEW_MOVE_TARGET
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(x)
        response.writeInt(y)
    }

    override fun apply(game: SnowWarGame) {
        val user = game.users.values.find { it.objectId == humanGameObjectId } ?: return
        user.changeMoveTarget(x, y)
    }
}

data class HumanThrowsSnowballAtHumanGameEvent(
    val humanGameObjectId: Int,
    val targetHumanGameObjectId: Int,
    val trajectory: SnowWarTrajectory
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.HUMAN_THROWS_SNOWBALL_AT_HUMAN
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(targetHumanGameObjectId)
        response.writeInt(trajectory.id)
    }

    override fun apply(game: SnowWarGame) {
        val user = game.users.values.find { it.objectId == humanGameObjectId } ?: return
        val targetUser = game.users.values.find { it.objectId == targetHumanGameObjectId }
        val targetX = targetUser?.currentLocationX ?: user.currentLocationX
        val targetY = targetUser?.currentLocationY ?: user.currentLocationY
        user.throwSnowball(targetX, targetY)
    }
}

data class HumanThrowsSnowballAtPositionGameEvent(
    val humanGameObjectId: Int,
    val targetX: Int,
    val targetY: Int,
    val trajectory: SnowWarTrajectory
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.HUMAN_THROWS_SNOWBALL_AT_POSITION
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(targetX)
        response.writeInt(targetY)
        response.writeInt(trajectory.id)
    }

    override fun apply(game: SnowWarGame) {
        val user = game.users.values.find { it.objectId == humanGameObjectId } ?: return
        user.throwSnowball(targetX, targetY)
    }
}

data class HumanStartsToMakeASnowballGameEvent(
    val humanGameObjectId: Int
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.HUMAN_STARTS_TO_MAKE_A_SNOWBALL
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
    }

    override fun apply(game: SnowWarGame) {
        val user = game.users.values.find { it.objectId == humanGameObjectId } ?: return
        user.startMakingSnowball()
    }
}

// id=8: cria a bola física na arena do cliente (AS3 CreateSnowballEventData)
// Wire: snowBallGameObjectId, humanGameObjectId, targetX, targetY, trajectory
data class CreateSnowballGameEvent(
    val snowBallGameObjectId: Int,
    val humanGameObjectId: Int,
    val targetX: Int,
    val targetY: Int,
    val trajectory: SnowWarTrajectory
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.CREATE_SNOWBALL
    override fun serialize(response: HabboResponse) {
        response.writeInt(snowBallGameObjectId)
        response.writeInt(humanGameObjectId)
        response.writeInt(targetX)
        response.writeInt(targetY)
        response.writeInt(trajectory.id)
    }

    override fun apply(game: SnowWarGame) {
        val user = game.users.values.find { it.objectId == humanGameObjectId } ?: return
        val ball = SnowWarSnowball(
            objectId = snowBallGameObjectId,
            thrower = user,
            startWorldX = user.currentLocationX,
            startWorldY = user.currentLocationY,
            targetWorldX = targetX,
            targetWorldY = targetY,
            trajectoryRequested = trajectory
        )
        game.snowballs.add(ball)
    }
}

data class MachineCreatesSnowballGameEvent(
    val snowBallMachineReference: Int
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.MACHINE_CREATES_SNOWBALL
    override fun serialize(response: HabboResponse) {
        response.writeInt(snowBallMachineReference)
    }

    override fun apply(game: SnowWarGame) {
        val machine = game.machines.find { it.objectId == snowBallMachineReference }
        machine?.addSnowball()
    }
}

data class HumanGetsSnowballsFromMachineGameEvent(
    val humanGameObjectId: Int,
    val snowBallMachineReference: Int
) : ISnowWarGameEvent {
    override val type: SnowWarEventType get() = SnowWarEventType.HUMAN_GETS_SNOWBALLS_FROM_MACHINE
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(snowBallMachineReference)
    }

    override fun apply(game: SnowWarGame) {
        val user = game.users.values.find { it.objectId == humanGameObjectId } ?: return
        val machine = game.machines.find { it.objectId == snowBallMachineReference }
        if (machine != null) {
            machine.transferReservedSnowballTo(user)
        } else {
            val pile = game.piles.find { it.objectId == snowBallMachineReference }
            pile?.transferReservedSnowballTo(user)
        }
    }
}
