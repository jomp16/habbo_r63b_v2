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

interface ISnowWarGameEvent {
    val eventTypeId: Int
    fun serialize(response: HabboResponse)
}

data class HumanLeftGameEvent(
    val humanGameObjectId: Int
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 1
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
    }
}

data class NewMoveTargetGameEvent(
    val humanGameObjectId: Int,
    val x: Int,
    val y: Int
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 2
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(x)
        response.writeInt(y)
    }
}

data class HumanThrowsSnowballAtHumanGameEvent(
    val humanGameObjectId: Int,
    val targetHumanGameObjectId: Int,
    val trajectory: Int
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 3
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(targetHumanGameObjectId)
        response.writeInt(trajectory)
    }
}

data class HumanThrowsSnowballAtPositionGameEvent(
    val humanGameObjectId: Int,
    val targetX: Int,
    val targetY: Int,
    val trajectory: Int
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 4
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(targetX)
        response.writeInt(targetY)
        response.writeInt(trajectory)
    }
}

data class HumanStartsToMakeASnowballGameEvent(
    val humanGameObjectId: Int
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 7
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
    }
}

data class CreateSnowballGameEvent(
    val humanGameObjectId: Int,
    val snowBallMachineReference: Int = 0
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 8
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(snowBallMachineReference)
    }
}

data class MachineCreatesSnowballGameEvent(
    val snowBallMachineReference: Int
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 11
    override fun serialize(response: HabboResponse) {
        response.writeInt(snowBallMachineReference)
    }
}

data class HumanGetsSnowballsFromMachineGameEvent(
    val humanGameObjectId: Int,
    val snowBallMachineReference: Int
) : ISnowWarGameEvent {
    override val eventTypeId: Int get() = 12
    override fun serialize(response: HabboResponse) {
        response.writeInt(humanGameObjectId)
        response.writeInt(snowBallMachineReference)
    }
}
