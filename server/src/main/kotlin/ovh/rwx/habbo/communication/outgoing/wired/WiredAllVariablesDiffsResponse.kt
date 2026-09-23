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

package ovh.rwx.habbo.communication.outgoing.wired

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.room.Room

@Suppress("unused", "UNUSED_PARAMETER")
class WiredAllVariablesDiffsResponse {
    @Response(Outgoing.WIRED_ALL_VARIABLES_DIFFS)
    fun response(habboResponse: HabboResponse, room: Room, clientHashes: Map<String, Int> = emptyMap()) {
        val manager = room.wiredVariableManager
        val allVariablesHash = manager.getAllVariablesHash()
        val currentDefinitions = manager.getDefinitions()
        val currentDefMap = currentDefinitions.associateBy { it.variableId }

        // Variáveis que o cliente tem no cache mas não existem mais no quarto
        val removedVariables = clientHashes.keys.filter { it !in currentDefMap }

        // Variáveis novas ou cujo hash mudou em relação ao que o cliente tem
        val addedOrUpdated = currentDefinitions.filter { def ->
            val clientHash = clientHashes[def.variableId]
            val serverVarHash = manager.getVariableIdHash(def.variableId)
            clientHash == null || clientHash != serverVarHash
        }

        habboResponse.apply {
            writeInt(allVariablesHash)
            writeBoolean(true) // isLastChunk

            // Removed variables
            writeInt(removedVariables.size)
            removedVariables.forEach { writeUTF(it) }

            // Added or updated variables
            writeInt(addedOrUpdated.size)
            addedOrUpdated.forEach { def ->
                writeInt(manager.getVariableIdHash(def.variableId))
                serialize(def)
            }
        }
    }
}
