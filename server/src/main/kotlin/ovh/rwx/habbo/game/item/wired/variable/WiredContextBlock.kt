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

/**
 * AS3 WiredContext Type constants (_SafeStr_4327)
 */
object WiredContextType {
    const val ALL_VARIABLES_IN_ROOM = 0
    const val FURNI_VARIABLE_INFO_AND_HOLDERS = 1
    const val USER_VARIABLE_INFO_AND_HOLDERS = 2
    const val GLOBAL_VARIABLE_INFO_AND_VALUE = 3
    const val SHARED_VARIABLE_LIST = 4
    const val VARIABLE_LIST = 5
    const val SHARED_GLOBAL_PLACEHOLDER_LIST = 6
}

/**
 * Represents a pair of object/user ID and its integer value (_-YB.ObjectIdAndValuePair).
 */
data class ObjectIdAndValuePair(
    val objectId: Int,
    val value: Int
) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(objectId)
            writeInt(value)
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        TODO("Not yet implemented")
    }
}

/**
 * Represents a shared variable linked to a specific room (_-YB.SharedVariable).
 */
data class SharedVariable(
    val roomId: Int,
    val roomName: String,
    val variable: WiredVariable
) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(roomId)
            writeUTF(roomName)
            serialize(variable)
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        TODO("Not yet implemented")
    }
}

/**
 * Represents a shared global placeholder (_-315.SharedGlobalPlaceholder).
 */
data class SharedGlobalPlaceholder(
    val roomId: Int,
    val roomName: String,
    val placeholderName: String
) : IHabboResponseSerialize {
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(roomId)
            writeUTF(roomName)
            writeUTF(placeholderName)
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        TODO("Not yet implemented")
    }
}

/**
 * Polymorphic base class for all 7 WiredContext blocks supported in AS3
 * (_SafeStr_4327 / com.sulake.habbo.communication.messages.incoming.userdefinedroomevents.wiredcontext.WiredContext / _-315.*).
 */
sealed class WiredContextBlock(val typeCode: Int) : IHabboResponseSerialize {
    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        TODO("Not yet implemented")
    }

    /**
     * Type 0: AllVariablesInRoom (_-315.AllVariablesInRoom)
     */
    data class AllVariablesInRoom(val hash: Int) : WiredContextBlock(WiredContextType.ALL_VARIABLES_IN_ROOM) {
        override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
            habboResponse.apply {
                writeInt(typeCode)
                writeInt(hash)
            }
        }
    }

    /**
     * Base class for variable info with holders list (_-315.VariableInfoAndHolders).
     */
    abstract class VariableInfoAndHolders(
        typeCode: Int,
        open val variable: WiredVariable,
        open val holders: List<ObjectIdAndValuePair> = emptyList()
    ) : WiredContextBlock(typeCode) {
        override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
            habboResponse.apply {
                writeInt(typeCode)
                serialize(variable)
                writeInt(holders.size)
                holders.forEach {
                    serialize(it)
                }
            }
        }
    }

    /**
     * Type 1: VariableInfoAndHolders for Furni (furniVariableInfo).
     */
    data class FurniVariableInfoAndHolders(
        override val variable: WiredVariable,
        override val holders: List<ObjectIdAndValuePair> = emptyList()
    ) : VariableInfoAndHolders(WiredContextType.FURNI_VARIABLE_INFO_AND_HOLDERS, variable, holders)

    /**
     * Type 2: VariableInfoAndHolders for User (userVariableInfo).
     */
    data class UserVariableInfoAndHolders(
        override val variable: WiredVariable,
        override val holders: List<ObjectIdAndValuePair> = emptyList()
    ) : VariableInfoAndHolders(WiredContextType.USER_VARIABLE_INFO_AND_HOLDERS, variable, holders)

    /**
     * Type 3: VariableInfoAndValue (globalVariableInfo) (_-315.VariableInfoAndValue).
     */
    data class GlobalVariableInfoAndValue(
        val variable: WiredVariable,
        val value: Int
    ) : WiredContextBlock(WiredContextType.GLOBAL_VARIABLE_INFO_AND_VALUE) {
        override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
            habboResponse.apply {
                writeInt(typeCode)
                serialize(variable)
                writeInt(value)
            }
        }
    }

    /**
     * Type 4: SharedVariableList (referenceVariablesList) (_-315.SharedVariableList).
     */
    data class SharedVariableList(
        val sharedVariables: List<SharedVariable> = emptyList()
    ) : WiredContextBlock(WiredContextType.SHARED_VARIABLE_LIST) {
        constructor(vararg sharedVariables: SharedVariable) : this(sharedVariables.toList())

        override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
            habboResponse.apply {
                writeInt(typeCode)
                writeInt(sharedVariables.size)
                sharedVariables.forEach {
                    serialize(it)
                }
            }
        }
    }

    /**
     * Type 5: VariableList (rulesetVariables) (_-315.VariableList).
     */
    data class VariableList(
        val variables: List<WiredVariable> = emptyList()
    ) : WiredContextBlock(WiredContextType.VARIABLE_LIST) {
        constructor(vararg variables: WiredVariable) : this(variables.toList())

        override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
            habboResponse.apply {
                writeInt(typeCode)
                writeInt(variables.size)
                variables.forEach {
                    serialize(it)
                }
            }
        }
    }

    /**
     * Type 6: SharedGlobalPlaceholderList (referencePlaceholderList) (_-315.SharedGlobalPlaceholderList).
     */
    data class SharedGlobalPlaceholderList(
        val placeholders: List<SharedGlobalPlaceholder> = emptyList()
    ) : WiredContextBlock(WiredContextType.SHARED_GLOBAL_PLACEHOLDER_LIST) {
        constructor(vararg placeholders: SharedGlobalPlaceholder) : this(placeholders.toList())

        override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
            habboResponse.apply {
                writeInt(typeCode)
                writeInt(placeholders.size)
                placeholders.forEach {
                    serialize(it)
                }
            }
        }
    }
}
