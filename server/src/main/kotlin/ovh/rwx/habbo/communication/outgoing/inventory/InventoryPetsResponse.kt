/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.inventory

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

@Suppress("unused", "UNUSED_PARAMETER")
class InventoryPetsResponse {
    @Response(Outgoing.INVENTORY_PETS)
    fun response(habboResponse: HabboResponse, pets: List<Nothing>) {
        habboResponse.apply {
            writeInt(1)
            writeInt(1)
            writeInt(pets.size)

            @Suppress("ForEachParameterNotUsed")
            pets.forEach {
                // Todo: when adding support to pets, rewrite this part
                writeInt(0) // id
                writeUTF("") // name
                // start - PetFigureData
                writeInt(0) // type
                writeInt(0) // pallete id
                writeUTF("") // color
                writeInt(0) // ?
                writeInt(0) // qtd figureString
                // start - figureString
//                writeInt(0)
//                writeInt(0)
//                writeInt(0)
                // end - qtd figureString
                // end - PetFigureData
                writeInt(0) // level
            }
        }
    }

    @ResponseR63A(OutgoingR63A.INVENTORY_PETS)
    fun responseR63A(habboResponse: HabboResponse, pets: List<Nothing>) {
        habboResponse.apply {
            writeInt(1)
            writeInt(pets.size)

            @Suppress("ForEachParameterNotUsed")
            pets.forEach {
                // Todo: when adding support to pets, rewrite this part
//                writeInt(0) // id
//                writeUTF("") // name
//                writeInt(0) // type
//                writeInt(0) // breed
//                writeUTF("") // color
            }
        }
    }
}