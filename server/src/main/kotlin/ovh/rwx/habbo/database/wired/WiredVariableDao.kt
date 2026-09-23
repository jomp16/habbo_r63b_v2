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

package ovh.rwx.habbo.database.wired

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.wired.variable.*
import ovh.rwx.habbo.kotlin.localDateTime

object WiredVariableDao {
    fun getVariablesForOwner(
        ownerId: Int,
        ownerType: VariableOwnerType
    ): List<Pair<WiredVariable, WiredVariableValue>> {
        return getVariablesForOwners(listOf(ownerId), ownerType)
    }

    fun getVariablesForOwners(
        ownerIds: Collection<Int>,
        ownerType: VariableOwnerType
    ): List<Pair<WiredVariable, WiredVariableValue>> {
        if (ownerIds.isEmpty()) return emptyList()

        val idsString = ownerIds.joinToString(",")

        return HabboServer.database {
            select(
                """
                SELECT variable_id, variable_name, variable_type, availability_type, owner_id, owner_type, value, created_at, updated_at
                FROM wired_variables
                WHERE owner_type = :owner_type AND owner_id IN ($idsString)
                """.trimIndent(),
                mapOf(
                    "owner_type" to ownerType.name
                )
            ) {
                val variable = WiredVariable(
                    variableId = it.string("variable_id"),
                    variableType = WiredVariableType.fromCode(it.int("variable_type"))
                        ?: WiredVariableType.USER_DEFINED,
                    variableName = it.string("variable_name"),
                    availabilityType = VariableAvailabilityType.fromCode(it.int("availability_type"))
                )
                val value = WiredVariableValue(
                    variableId = it.string("variable_id"),
                    value = it.stringOrNull("value") ?: "",
                    createdAt = it.localDateTime("created_at"),
                    updatedAt = it.localDateTime("updated_at"),
                    ownerId = it.int("owner_id"),
                    ownerType = VariableOwnerType.valueOf(it.string("owner_type"))
                )
                variable to value
            }
        }
    }

    fun saveVariables(variables: List<Pair<WiredVariable, WiredVariableValue>>) {
        if (variables.isEmpty()) return

        HabboServer.database {
            batchUpdate(
                """
                INSERT INTO wired_variables
                    (variable_id, variable_name, variable_type, availability_type, owner_id, owner_type, value, created_at, updated_at)
                VALUES
                    (:variable_id, :variable_name, :variable_type, :availability_type, :owner_id, :owner_type, :value, :created_at, :updated_at)
                ON DUPLICATE KEY UPDATE
                    variable_name = VALUES(variable_name),
                    variable_type = VALUES(variable_type),
                    availability_type = VALUES(availability_type),
                    value = VALUES(value),
                    updated_at = VALUES(updated_at)
                """.trimIndent(),
                variables.map { (variable, value) ->
                    mapOf(
                        "variable_id" to variable.variableId,
                        "variable_name" to variable.variableName,
                        "variable_type" to variable.variableType.code,
                        "availability_type" to variable.availabilityType.code,
                        "owner_id" to value.ownerId,
                        "owner_type" to value.ownerType.name,
                        "value" to value.value.toString(),
                        "created_at" to value.createdAt,
                        "updated_at" to value.updatedAt
                    )
                }
            )
        }
    }

    fun deleteVariable(variableId: String, ownerId: Int, ownerType: VariableOwnerType) {
        HabboServer.database {
            update(
                """
                DELETE FROM wired_variables
                WHERE variable_id = :variable_id AND owner_id = :owner_id AND owner_type = :owner_type
                """.trimIndent(),
                mapOf(
                    "variable_id" to variableId,
                    "owner_id" to ownerId,
                    "owner_type" to ownerType.name
                )
            )
        }
    }

    fun getGlobalPlaceholderNamesForRooms(roomIds: Collection<Int>): List<Pair<Int, String>> {
        if (roomIds.isEmpty()) return emptyList()
        val idsString = roomIds.joinToString(",")
        return HabboServer.database {
            select(
                """
                SELECT i.room_id, iw.message
                FROM items i
                JOIN items_wired iw ON iw.item_id = i.id
                JOIN furnishings f ON f.item_name = i.item_name
                WHERE i.room_id IN ($idsString)
                  AND (f.interaction_type IN ('wf_xtra_text_input_variable', 'wf_xtra_text_output_variable')
                       OR f.interaction_type LIKE '%placeholder%'
                       OR f.interaction_type LIKE '%global_placeholder%')
                """.trimIndent()
            ) {
                val rId = it.int("room_id")
                val msg = it.stringOrNull("message") ?: ""
                val name = msg.split("\t").firstOrNull()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    rId to name
                } else null
            }.filterNotNull()
        }
    }
}
