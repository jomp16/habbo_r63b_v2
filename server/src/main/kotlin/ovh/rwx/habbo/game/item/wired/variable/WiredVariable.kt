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

package ovh.rwx.habbo.game.item.wired.variable

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionBefore

data class WiredVariable(
    val variableId: String,
    val variableType: WiredVariableType = WiredVariableType.USER_DEFINED,
    val variableName: String,
    val availabilityType: VariableAvailabilityType = VariableAvailabilityType.TEMPORARY_ROOM,
    val variableTarget: WiredVariableTarget = WiredVariableTarget.FURNI,
    val alwaysAvailable: Boolean = true,
    val canCreateAndDelete: Boolean = true,
    val hasValue: Boolean = true,
    val canWriteValue: Boolean = true,
    val canInterceptChanges: Boolean = true,
    val isInvisible: Boolean = false,
    val canReadCreationTime: Boolean = true,
    val canReadLastUpdateTime: Boolean = true,
    val textConnectors: Map<Int, String> = emptyMap()
) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            if (isVersionBefore(2024, 12, 12)) {
                writeInt(variableId.toIntOrNull() ?: 0)
            } else {
                writeUTF(variableId)
            }
            writeInt(variableType.code)
            writeUTF(variableName)
            writeInt(availabilityType.code)
            writeInt(variableTarget.code)
            writeBoolean(alwaysAvailable)
            writeBoolean(canCreateAndDelete)
            writeBoolean(hasValue)
            writeBoolean(canWriteValue)
            writeBoolean(canInterceptChanges)
            writeBoolean(isInvisible)
            writeBoolean(canReadCreationTime)
            writeBoolean(canReadLastUpdateTime)

            val hasConnectors = textConnectors.isNotEmpty()
            writeBoolean(hasConnectors)
            if (hasConnectors) {
                writeInt(textConnectors.size)
                textConnectors.forEach { (connectorId, connectorValue) ->
                    writeInt(connectorId)
                    writeUTF(connectorValue)
                }
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        TODO("Not yet implemented")
    }
}
