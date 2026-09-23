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
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing

data class WiredVariablesForObjectData(
    val type: Int,
    val objectId: Int = 0,
    val userIndex: Int = 0,
    val variables: Map<String, Int> = emptyMap(),
    val configuredInWireds: List<Int> = emptyList(),
)

@Suppress("unused", "UNUSED_PARAMETER")
class WiredVariablesForObjectResponse {
    @Response(Outgoing.WIRED_VARIABLES_FOR_OBJECT)
    fun response(habboResponse: HabboResponse, data: WiredVariablesForObjectData) {
        habboResponse.apply {
            writeInt(data.type)
            if (data.type == 0) {
                writeInt(data.objectId)
            } else if (data.type == 1) {
                writeInt(data.userIndex)
            }
            writeInt(data.variables.size)
            data.variables.forEach { (name, value) ->
                if (isVersionBefore(2024, 12, 12)) {
                    writeInt(name.toIntOrNull() ?: 0)
                } else {
                    writeUTF(name)
                }
                writeInt(value)
            }
            if (data.type == 0 && isVersionAtLeast(2025, 9, 11)) {
                writeInt(data.configuredInWireds.size)
                data.configuredInWireds.forEach { wiredId ->
                    writeInt(wiredId)
                }
            }
        }
    }
}
