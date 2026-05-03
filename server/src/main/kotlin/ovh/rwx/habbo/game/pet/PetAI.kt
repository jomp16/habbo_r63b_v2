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

package ovh.rwx.habbo.game.pet

import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector2

class PetAI(private val roomPet: RoomPet) {
    var state: PetState = PetState.Idle
        private set

    private var ticksInState: Int = 0
    private var decayCounter: Int = 0
    private var nextIdleDuration: Int = randomIdleDuration()
    private var nextRoamDuration: Int = randomRoamDuration()

    private val petData get() = roomPet.petData

    fun tick(): List<PetAction> {
        ticksInState++
        decayCounter++

        val actions = mutableListOf<PetAction>()

        // Decay needs every ~30s (60 ticks × 500ms)
        if (decayCounter >= 60) {
            decayCounter = 0
            actions += decayNeeds()
        }

        // Evaluate state transition
        evaluateStateTransition()?.let { newState ->
            actions += transitionTo(newState)
        }

        // Execute current state behavior
        actions += executeState()

        return actions
    }

    private fun decayNeeds(): List<PetAction> {
        if (state is PetState.Sleeping) {
            // Sleeping recovers energy
            petData.energy = (petData.energy + 5).coerceAtMost(petData.maxEnergy)
            return emptyList()
        }

        petData.hunger = (petData.hunger + 2).coerceAtMost(PetLevel.MAX_NUTRITION)
        petData.thirst = (petData.thirst + 2).coerceAtMost(PetLevel.MAX_NUTRITION)
        petData.happiness = (petData.happiness - 1).coerceAtLeast(0)
        petData.energy = (petData.energy - 1).coerceAtLeast(0)

        return emptyList()
    }

    private fun evaluateStateTransition(): PetState? {
        // Priority-based evaluation
        if (petData.energy <= 10 && state !is PetState.Sleeping) return PetState.Sleeping
        if (state is PetState.Sleeping && petData.energy >= 80) return PetState.Idle
        if (state is PetState.PerformingTrick || state is PetState.FollowingOwner) return null

        if (petData.hunger >= 80 && state !is PetState.SeekingFood) return PetState.SeekingFood
        if (petData.thirst >= 80 && state !is PetState.SeekingWater) return PetState.SeekingWater
        if (petData.happiness < 20 && state !is PetState.Begging) return PetState.Begging

        return when (state) {
            is PetState.Idle -> if (ticksInState > nextIdleDuration) PetState.Roaming else null
            is PetState.Roaming -> if (ticksInState > nextRoamDuration) PetState.Idle else null
            is PetState.Begging -> if (ticksInState > 12 || petData.happiness >= 40) PetState.Idle else null
            is PetState.SeekingFood -> if (ticksInState > 20) PetState.Idle else null
            is PetState.SeekingWater -> if (ticksInState > 20) PetState.Idle else null
            else -> null
        }
    }

    private fun transitionTo(newState: PetState): List<PetAction> {
        val actions = mutableListOf<PetAction>()

        // Exit current state
        when (state) {
            is PetState.Sleeping -> actions += PetAction.WakeUp
            is PetState.PerformingTrick -> {
                val trick = (state as PetState.PerformingTrick).trick
                if (trick.statusKey.isNotEmpty()) actions += PetAction.RemoveStatus(trick.statusKey)
            }

            is PetState.Begging -> actions += PetAction.RemoveStatus("beg")
            else -> {}
        }

        state = newState
        ticksInState = 0

        // Enter new state
        when (newState) {
            is PetState.Sleeping -> actions += PetAction.Sleep
            is PetState.Begging -> {
                actions += PetAction.SetStatus("beg")
                actions += PetAction.Say("*begs*")
            }

            is PetState.Idle -> {
                nextIdleDuration = randomIdleDuration()
                actions += PetAction.RemoveStatus("mv")
            }

            is PetState.Roaming -> nextRoamDuration = randomRoamDuration()
            else -> {}
        }

        return actions
    }

    private fun executeState(): List<PetAction> = when (state) {
        is PetState.Roaming -> executeRoaming()
        is PetState.PerformingTrick -> executeTrick()
        is PetState.FollowingOwner -> executeFollow()
        is PetState.SeekingFood -> executeSeekItem("pet_food")
        is PetState.SeekingWater -> executeSeekItem("pet_water")
        else -> emptyList()
    }

    private fun executeRoaming(): List<PetAction> {
        if (roomPet.walking) return emptyList()

        // Pick a random walkable tile
        findRandomWalkableTile()?.let { tile ->
            return listOf(PetAction.MoveTo(tile.x, tile.y))
        }

        return emptyList()
    }

    private fun executeTrick(): List<PetAction> {
        val performing = state as PetState.PerformingTrick
        val remaining = performing.ticksRemaining - 1

        if (remaining <= 0) {
            val actions = mutableListOf<PetAction>()
            if (performing.trick.statusKey.isNotEmpty()) {
                actions += PetAction.RemoveStatus(performing.trick.statusKey)
            }
            actions += PetAction.GainExperience(performing.trick.experienceReward)
            state = PetState.Idle
            ticksInState = 0
            nextIdleDuration = randomIdleDuration()
            return actions
        }

        state = PetState.PerformingTrick(performing.trick, remaining)
        return emptyList()
    }

    private fun executeFollow(): List<PetAction> {
        val following = state as PetState.FollowingOwner
        val owner = roomPet.room.userManager.entities[following.ownerVirtualId] ?: run {
            state = PetState.Idle
            ticksInState = 0
            return emptyList()
        }

        if (roomPet.walking) return emptyList()

        val dist = roomPet.room.roomGamemap.tileDistance(
            roomPet.currentVector3.x, roomPet.currentVector3.y,
            owner.currentVector3.x, owner.currentVector3.y
        )

        if (dist > 2) {
            return listOf(PetAction.MoveTo(owner.currentVector3.x, owner.currentVector3.y))
        }

        return emptyList()
    }

    private fun executeSeekItem(@Suppress("UNUSED_PARAMETER") itemType: String): List<PetAction> {
        // For now, just roam — item interaction will be added when pet food items are implemented
        if (roomPet.walking) return emptyList()

        findRandomWalkableTile()?.let { tile ->
            return listOf(PetAction.MoveTo(tile.x, tile.y))
        }

        return emptyList()
    }

    // --- External event handlers ---

    fun handleCommand(trick: PetTrick): List<PetAction> {
        if (state is PetState.Sleeping) return listOf(PetAction.Say("ZzzZzz..."))
        if (petData.energy < trick.energyCost) return listOf(PetAction.Say("*yawns*"))
        if (petData.level < trick.levelRequired) return listOf(PetAction.Say("*tilts head*"))

        val actions = mutableListOf<PetAction>()

        // Exit current state cleanly
        actions += transitionTo(
            when (trick) {
            PetTrick.FREE, PetTrick.STAND -> PetState.Idle
            PetTrick.FOLLOW -> {
                val owner = roomPet.room.userManager.entities.values.filterIsInstance<RoomUser>().find {
                    it.habboSession.userInformation.id == petData.userId
                }
                if (owner != null) PetState.FollowingOwner(owner.virtualID) else PetState.Idle
            }

            else -> PetState.PerformingTrick(trick, trick.durationTicks)
        })

        petData.energy = (petData.energy - trick.energyCost).coerceAtLeast(0)

        if (trick.statusKey.isNotEmpty()) {
            actions += PetAction.SetStatus(trick.statusKey)
        }

        if (trick == PetTrick.SPEAK) {
            actions += PetAction.Say("*bark bark!*")
        }

        // Success chance based on happiness
        val successChance = 0.5 + (petData.happiness / 200.0)
        if (Math.random() > successChance) {
            actions += PetAction.Say("*ignores*")
            state = PetState.Idle
            ticksInState = 0
            return actions
        }

        return actions
    }

    fun handleScratch(): List<PetAction> {
        petData.happiness = (petData.happiness + 10).coerceAtMost(PetLevel.MAX_HAPPINESS)

        val actions = mutableListOf<PetAction>()
        actions += PetAction.Say("*purrs*")
        actions += PetAction.GainExperience(10)

        return actions
    }

    // --- Helpers ---

    private fun findRandomWalkableTile(): Vector2? {
        val gamemap = roomPet.room.roomGamemap
        val model = roomPet.room.roomModel

        repeat(10) {
            val x = (0 until model.mapSizeX).random()
            val y = (0 until model.mapSizeY).random()
            val vec = Vector2(x, y)

            if (!gamemap.isBlocked(vec, ignoreUsers = true)) return vec
        }

        return null
    }

    companion object {
        private fun randomIdleDuration() = (6..20).random()
        private fun randomRoamDuration() = (4..12).random()
    }
}
