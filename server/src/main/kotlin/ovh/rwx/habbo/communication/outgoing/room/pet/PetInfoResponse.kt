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

package ovh.rwx.habbo.communication.outgoing.room.pet

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.pet.PetData
import ovh.rwx.habbo.game.pet.PetLevel

@Suppress("unused", "UNUSED_PARAMETER")
class PetInfoResponse {
    @Response(Outgoing.PET_INFO)
    fun response(habboResponse: HabboResponse, petData: PetData, viewingUserId: Int) {
        val horse = petData.horseData

        habboResponse.apply {
            writeInt(petData.id)
            writeUTF(petData.name)
            writeInt(petData.level)
            writeInt(PetLevel.MAX_LEVEL)
            writeInt(petData.experience)
            writeInt(petData.experienceGoal)
            writeInt(petData.energy)
            writeInt(petData.maxEnergy)
            writeInt(petData.happiness)
            writeInt(PetLevel.MAX_HAPPINESS)
            writeInt(petData.respect)
            writeInt(petData.userId)
            writeInt(petData.age)
            writeUTF(petData.ownerName)
            writeInt(1) // rarity
            writeBoolean(horse?.hasSaddle ?: false)
            writeBoolean(false) // isRiding
            writeInt(0) // canBreed (monsterplant)
            writeInt(if (horse?.anyoneCanRide == true) 1 else 0)
            writeBoolean(false) // canBreed (monsterplant)
            writeBoolean(false) // canHarvest (monsterplant)
            writeBoolean(false) // isDead (monsterplant)
            writeInt(0) // rarity (monsterplant)
            writeInt(0) // maxTimeToLive (monsterplant)
            writeInt(0) // remainingTimeToLive (monsterplant)
            writeInt(0) // remainingGrowTime (monsterplant)
            writeBoolean(false) // isPubliclyBreedable (monsterplant)
        }
    }

    @ResponseR63A(OutgoingR63A.PET_INFO)
    fun responseR63A(habboResponse: HabboResponse, petData: PetData, viewingUserId: Int) {
        habboResponse.apply {
            writeInt(petData.id)
            writeUTF(petData.name)
            writeInt(petData.level)
            writeInt(PetLevel.MAX_LEVEL)
            writeInt(petData.experience)
            writeInt(petData.experienceGoal)
            writeInt(petData.energy)
            writeInt(petData.maxEnergy)
            writeInt(petData.happiness)
            writeInt(PetLevel.MAX_HAPPINESS)
            writeInt(petData.respect)
            writeInt(petData.userId)
            writeInt(petData.age)
            writeUTF(petData.ownerName)
        }
    }
}
