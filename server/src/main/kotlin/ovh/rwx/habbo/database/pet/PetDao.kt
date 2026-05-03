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

import com.github.andrewoma.kwery.core.Row
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.pet.PetBreed
import ovh.rwx.habbo.game.pet.PetData
import ovh.rwx.habbo.kotlin.insertAndGetGeneratedKey

object PetDao {
    fun getPetBreeds(): Map<Int, List<PetBreed>> = HabboServer.database {
        select("SELECT * FROM `pet_breeds` ORDER BY `pet_type`, `breed_id`, `palette_id`") { row ->
            PetBreed(
                petType = row.int("pet_type"),
                breedId = row.int("breed_id"),
                paletteId = row.int("palette_id"),
                sellable = row.boolean("sellable"),
                rare = row.boolean("rare")
            )
        }.groupBy { it.petType }
    }

    fun insertPet(userId: Int, name: String, type: Int, race: Int, color: String): Int = HabboServer.database {
        insertAndGetGeneratedKey(
            """INSERT INTO `users_pets` (`user_id`, `name`, `type`, `race`, `color`)
               VALUES (:user_id, :name, :type, :race, :color)""",
            mapOf(
                "user_id" to userId,
                "name" to name,
                "type" to type,
                "race" to race,
                "color" to color
            )
        )
    }

    fun getPetById(petId: Int): PetData? = HabboServer.database {
        select(
            "SELECT * FROM `users_pets` WHERE `id` = :id",
            mapOf("id" to petId)
        ) { mapPetData(it) }.firstOrNull()
    }

    fun getPetsByRoomId(roomId: Int): List<PetData> = HabboServer.database {
        select(
            "SELECT * FROM `users_pets` WHERE `room_id` = :room_id",
            mapOf("room_id" to roomId)
        ) { mapPetData(it) }
    }

    fun getPetsByUserId(userId: Int): List<PetData> = HabboServer.database {
        select(
            "SELECT * FROM `users_pets` WHERE `user_id` = :user_id AND `room_id` IS NULL",
            mapOf("user_id" to userId)
        ) { mapPetData(it) }
    }

    fun savePet(pet: PetData) = HabboServer.database {
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

    private fun mapPetData(row: Row): PetData = PetData(
        id = row.int("id"),
        userId = row.int("user_id"),
        roomId = row.intOrNull("room_id") ?: 0,
        name = row.string("name"),
        race = row.int("race"),
        type = row.int("type"),
        color = row.string("color"),
        happiness = row.int("happiness"),
        experience = row.int("experience"),
        energy = row.int("energy"),
        hunger = row.int("hunger"),
        thirst = row.int("thirst"),
        respect = row.int("respect"),
        createdAt = row.timestamp("created_at").time / 1000,
        x = row.int("x"),
        y = row.int("y"),
        z = row.double("z"),
        rot = row.int("rot"),
        extraDataJson = row.string("extra_data")
    )
}
