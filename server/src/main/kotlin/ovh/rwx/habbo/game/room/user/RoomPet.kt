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

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.pet.PetAI
import ovh.rwx.habbo.game.pet.PetAction
import ovh.rwx.habbo.game.pet.PetData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.util.Vector3

class RoomPet(
    val petData: PetData,
    room: Room,
    virtualID: Int,
    currentVector3: Vector3,
    headRotation: Int,
    bodyRotation: Int
) : RoomEntity(room, virtualID, currentVector3, headRotation, bodyRotation) {

    val ai = PetAI(this)

    override fun onEffectChanged(effectId: Int) {}

    override fun processEntityTimers() {
        ai.tick().forEach(::executeAction)
    }

    fun executeAction(action: PetAction) {
        when (action) {
            is PetAction.Say -> broadcastPetChat(action.message)
            is PetAction.MoveTo -> moveTo(action.x, action.y)
            is PetAction.SetStatus -> addStatus(action.key, action.value, action.durationMs)
            is PetAction.RemoveStatus -> removeStatus(action.key)
            is PetAction.GainExperience -> addExperience(action.amount)
            is PetAction.Sleep -> enterSleep()
            is PetAction.WakeUp -> exitSleep()
        }
    }

    private fun broadcastPetChat(message: String) {
        room.sendHabboResponse(Outgoing.ROOM_USER_CHAT, virtualID, message, 0, RoomChatMessageBubbles.NORMAL)
        room.sendHabboResponse(OutgoingR63A.ROOM_USER_CHAT, virtualID, message, 0)
    }

    private fun addExperience(amount: Int) {
        val oldLevel = petData.level
        petData.experience += amount

        room.sendHabboResponse(Outgoing.PET_EXPERIENCE, petData.id, virtualID, amount)
        room.sendHabboResponse(OutgoingR63A.PET_EXPERIENCE, petData.id, virtualID, amount)

        if (petData.level > oldLevel) {
            broadcastPetChat("*leveled up to level ${petData.level}!*")
        }
    }

    private fun enterSleep() {
        removeEntityStatuses()
        addStatus("lay")
        addStatus("slp")
        broadcastPetChat("ZzzZzz...")
        updateNeeded = true
    }

    private fun exitSleep() {
        removeStatus("lay")
        removeStatus("slp")
        broadcastPetChat("*wakes up*")
        updateNeeded = true
    }

    fun syncPosition() {
        petData.x = currentVector3.x
        petData.y = currentVector3.y
        petData.z = currentVector3.z
        petData.rot = bodyRotation
    }

    private fun buildLookString(): String {
        val base = petData.look
        val horse = petData.horseData ?: return base

        val parts = mutableListOf<String>()

        // Layer 2: Crina (Mane)
        parts.add("2 ${horse.hairStyle} ${horse.hairColor}")
        // Layer 3: Rabo (Tail)
        parts.add("3 ${horse.hairStyle} ${horse.hairColor}")
        // Layer 4: Sela
        if (horse.hasSaddle) {
            parts.add("4 9 0")
        }

        return "$base ${parts.size} ${parts.joinToString(" ")}"
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        val horse = petData.horseData

        habboResponse.apply {
            writeInt(petData.id)
            writeUTF(petData.name)
            writeUTF("") // motto
            writeUTF(buildLookString())
            writeInt(virtualID)
            writeInt(currentVector3.x)
            writeInt(currentVector3.y)
            writeUTF(currentVector3.z.toString())
            writeInt(0)
            writeInt(2) // entity type: pet
            writeInt(petData.type)
            writeInt(petData.userId)
            writeUTF(petData.ownerName)
            writeInt(1) // rarityLevel
            writeBoolean(horse?.hasSaddle ?: false)
            writeBoolean(false) // isRiding
            writeBoolean(false) // canBreed
            writeBoolean(false) // canHarvest
            writeBoolean(false) // canRevive
            writeBoolean(false) // hasBreedingPermission
            writeInt(petData.level)
            writeUTF("") // petPosture
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(petData.id)
            writeUTF(petData.name)
            writeUTF("") // motto
            writeUTF(buildLookString())
            writeInt(virtualID)
            writeInt(currentVector3.x)
            writeInt(currentVector3.y)
            writeUTF(currentVector3.z.toString())
            writeInt(bodyRotation)
            writeInt(2) // entity type: pet
            writeInt(petData.type)
        }
    }
}
