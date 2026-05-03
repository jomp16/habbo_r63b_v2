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

package ovh.rwx.habbo.communication.outgoing.catalog

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.pet.PetBreed

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPetBreedsResponse {
    @Response(Outgoing.CATALOG_PET_BREEDS)
    @ResponseR63A(OutgoingR63A.CATALOG_PET_BREEDS)
    fun response(habboResponse: HabboResponse, productCode: String, breeds: List<PetBreed>) {
        habboResponse.apply {
            if (isVersionBefore(2011, 2, 18)) {
                writeInt(breeds.first().petType)
                writeInt(breeds.size) // size

                breeds.forEach {
                    writeInt(it.paletteId)
                }
            } else {
                writeUTF(productCode)
                writeInt(breeds.size)

                breeds.forEach {
                    writeInt(it.petType)
                    writeInt(it.breedId)
                    if (isVersionAtLeast(2011, 9, 14)) {
                        writeInt(it.paletteId)
                    }
                    writeBoolean(it.sellable)
                    writeBoolean(it.rare)
                }
            }
        }
    }
}
