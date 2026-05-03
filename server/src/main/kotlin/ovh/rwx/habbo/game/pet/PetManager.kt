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

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.pet.PetDao

class PetManager {
    private val log = LoggerFactory.getLogger(javaClass)

    var breeds: Map<Int, List<PetBreed>> = emptyMap()
        private set

    fun load() {
        breeds = PetDao.getPetBreeds()

        log.info("Loaded {} pet breed types with {} total breeds", breeds.size, breeds.values.sumOf { it.size })
    }

    fun getBreedsForType(productCode: String): List<PetBreed>? {
        val petType = extractPetType(productCode) ?: return null
        return breeds[petType]
    }

    fun extractPetType(productCode: String): Int? {
        // "a0 pet0" → 0, "a0 pet15" → 15
        val idx = productCode.indexOf("pet")
        if (idx == -1) return null
        return productCode.substring(idx + 3).toIntOrNull()
    }
}
