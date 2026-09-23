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

package ovh.rwx.habbo.communication.incoming.wired

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.wired.WiredVariablesForObjectData
import ovh.rwx.habbo.game.item.wired.variable.VariableOwnerType
import ovh.rwx.habbo.game.item.wired.variable.WiredVariableTarget
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class WiredSetObjectVariableValueHandler {
    @Handler(Incoming.WIRED_SET_OBJECT_VARIABLE_VALUE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        if (!room.userManager.hasRights(habboSession)) return

        val typeCode = habboRequest.readInt()
        val targetId = habboRequest.readInt()
        val variableId = if (habboSession.habboVersion.isVersionBefore(2024, 12, 12)) {
            habboRequest.readInt().toString()
        } else {
            habboRequest.readUTF()
        }
        val value = habboRequest.readInt()
        val delay = habboRequest.readInt()
        val targetType = WiredVariableTarget.fromCode(typeCode) ?: WiredVariableTarget.CONTEXT

        when (targetType) {
            WiredVariableTarget.FURNI -> room.wiredVariableManager.setVariableValue(
                variableId,
                value,
                ownerId = targetId,
                ownerType = VariableOwnerType.FURNI
            )

            WiredVariableTarget.USER -> {
                val entity = room.userManager.entities[targetId]
                    ?: room.userManager.entities.values.firstOrNull { (it as? RoomUser)?.habboSession?.userInformation?.id == targetId }
                val userId = (entity as? RoomUser)?.habboSession?.userInformation?.id ?: targetId
                room.wiredVariableManager.setVariableValue(
                    variableId,
                    value,
                    ownerId = userId,
                    ownerType = VariableOwnerType.USER
                )
            }

            WiredVariableTarget.GLOBAL, WiredVariableTarget.CONTEXT -> room.wiredVariableManager.setVariableValue(
                variableId,
                value,
                ownerId = room.roomData.id,
                ownerType = VariableOwnerType.ROOM
            )
        }

        val variablesMap = mutableMapOf<String, Int>()
        val configuredInWireds = mutableListOf<Int>()

        when (targetType) {
            WiredVariableTarget.FURNI -> {
                val furniVars = room.wiredVariableManager.getFurniVariables(targetId)
                furniVars.forEach { (varId, v) ->
                    val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                    variablesMap[varId] = intVal
                }
                val wiredItems = room.itemManager.floorItems.values
                    .filter { item ->
                        val wd = item.wiredData
                        wd != null && (targetId in wd.items || targetId in wd.stuffIds2)
                    }
                    .map { it.id }
                configuredInWireds.addAll(wiredItems)
            }

            WiredVariableTarget.USER -> {
                val entity = room.userManager.entities[targetId]
                    ?: room.userManager.entities.values.firstOrNull { (it as? RoomUser)?.habboSession?.userInformation?.id == targetId }
                val userId = (entity as? RoomUser)?.habboSession?.userInformation?.id ?: targetId
                val userVars = room.wiredVariableManager.getUserVariables(userId)
                userVars.forEach { (varId, v) ->
                    val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                    variablesMap[varId] = intVal
                }
            }

            WiredVariableTarget.GLOBAL, WiredVariableTarget.CONTEXT -> {
                val roomVars = room.wiredVariableManager.getRoomVariables()
                roomVars.forEach { (varId, v) ->
                    val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                    variablesMap[varId] = intVal
                }
            }
        }

        habboSession.sendHabboResponse(
            Outgoing.WIRED_VARIABLES_FOR_OBJECT,
            WiredVariablesForObjectData(
                type = typeCode,
                objectId = if (targetType == WiredVariableTarget.FURNI) targetId else 0,
                userIndex = if (targetType == WiredVariableTarget.USER) targetId else 0,
                variables = variablesMap,
                configuredInWireds = configuredInWireds,
            )
        )
    }
}
