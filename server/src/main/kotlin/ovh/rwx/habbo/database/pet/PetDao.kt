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

package ovh.rwx.habbo.database.pet

import ovh.rwx.habbo.game.pet.PetBreed
import ovh.rwx.habbo.game.pet.PetData

import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.database.*
import org.slf4j.LoggerFactory

object PetDao {
    private val log = LoggerFactory.getLogger(javaClass)
    val petSequence = HiLoSequence("users_pets", blockSize = 100)

    fun getPetBreeds(): Map<Int, List<PetBreed>> = db {
        query<PetBreed>("SELECT * FROM `pet_breeds` ORDER BY `pet_type`, `breed_id`, `palette_id`")
            .groupBy { it.petType }
    }

    fun insertPet(userId: Int, name: String, type: Int, race: Int, color: String): Int {
        val petId = petSequence.nextId()
        WriteBehindManager.queue {
            db {
                update(
                    "INSERT INTO `users_pets` (`id`, `user_id`, `name`, `type`, `race`, `color`) VALUES (:id, :user_id, :name, :type, :race, :color)",
                    mapOf("id" to petId, "user_id" to userId, "name" to name, "type" to type, "race" to race, "color" to color)
                )
            }
        }
        return petId
    }

    fun getPetById(petId: Int): PetData? = db {
        queryOne<PetDataDto>(
            "SELECT * FROM `users_pets` WHERE `id` = :id",
            mapOf("id" to petId)
        )?.toDomain()
    }

    fun getPetsByRoomId(roomId: Int): List<PetData> = db {
        query<PetDataDto>(
            "SELECT * FROM `users_pets` WHERE `room_id` = :room_id",
            mapOf("room_id" to roomId)
        ).map { it.toDomain() }
    }

    fun getPetsByUserId(userId: Int): List<PetData> = db {
        query<PetDataDto>(
            "SELECT * FROM `users_pets` WHERE `user_id` = :user_id AND `room_id` IS NULL",
            mapOf("user_id" to userId)
        ).map { it.toDomain() }
    }

    fun savePet(pet: PetData) = db {
        update(
            """UPDATE `users_pets` SET 
                `room_id` = :room_id, `experience` = :experience, `energy` = :energy,
                `happiness` = :happiness, `hunger` = :hunger, `thirst` = :thirst,
                `respect` = :respect, `x` = :x, `y` = :y, `z` = :z, `rot` = :rot,
                `extra_data` = :extra_data
                WHERE `id` = :id""",
            mapOf(
                "id" to pet.id,
                "room_id" to if (pet.roomId == 0) null else pet.roomId,
                "experience" to pet.experience,
                "energy" to pet.energy,
                "happiness" to pet.happiness,
                "hunger" to pet.hunger,
                "thirst" to pet.thirst,
                "respect" to pet.respect,
                "x" to pet.x,
                "y" to pet.y,
                "z" to pet.z,
                "rot" to pet.rot,
                "extra_data" to pet.serializeExtraData()
            )
        )
    }
}


data class PetDataDto(
    val id: Int,
    val userId: Int,
    val roomId: Int? = 0,
    val name: String,
    val race: Int,
    val type: Int,
    val color: String,
    val happiness: Int = 100,
    val experience: Int = 0,
    val energy: Int = 100,
    val hunger: Int = 0,
    val thirst: Int = 0,
    val respect: Int = 0,
    val createdAt: java.sql.Timestamp,
    val x: Int = 0,
    val y: Int = 0,
    val z: Double = 0.0,
    val rot: Int = 0,
    val extraData: String = ""
) {
    fun toDomain() = PetData(
        id = id,
        userId = userId,
        roomId = roomId ?: 0,
        name = name,
        race = race,
        type = type,
        color = color,
        happiness = happiness,
        experience = experience,
        energy = energy,
        hunger = hunger,
        thirst = thirst,
        respect = respect,
        createdAt = createdAt.time / 1000,
        x = x,
        y = y,
        z = z,
        rot = rot,
        extraDataJson = extraData
    )
}

