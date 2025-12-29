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

package ovh.rwx.habbo.game.item.wired

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room

abstract class WiredItem(protected val room: Room, val roomItem: RoomItem) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    abstract fun code(): Int

    fun saveWired(habboRequest: HabboRequest, habboAir: Boolean): Boolean {
        roomItem.wiredData?.let {
            // param2: intParams
            val intParamsCount = habboRequest.readInt()
            val options = mutableListOf<Int>()
            repeat(intParamsCount) { _ ->
                options += habboRequest.readInt()
            }

            // param4: stringParam
            it.message = habboRequest.readUTF()

            // param5: stuffIds
            val itemsCount = habboRequest.readInt()
            val roomItemsIds = mutableListOf<Int>()
            repeat(itemsCount) { _ ->
                val itemId = habboRequest.readInt()
                if (room.roomItems.containsKey(itemId)) {
                    val roomItem1 = room.roomItems[itemId] ?: return@repeat
                    if (!roomItem1.furnishing.interactionType.name.startsWith("WIRED")) {
                        roomItemsIds += itemId
                    }
                }
            }

            if (habboAir) {
                // Diferentes tipos têm diferentes parâmetros extras
                when {
                    roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT") -> {
                        // Effect: param7 = getActionDelay()
                        it.delay = habboRequest.readInt()
                    }

                    roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION") -> {
                        // Condition: param7 = resolveQuantifier()
                        @Suppress("UNUSED_VARIABLE")
                        val quantifier = habboRequest.readInt() // TODO: implement quantifier
                        log.info("[WIRED] TODO: condition quantifier: {}", quantifier)
                    }

                    roomItem.furnishing.interactionType.name.startsWith("WIRED_SELECTOR") -> {
                        // Selector: param7 = resolveFilterField(), param8 = resolveInverseField()
                        @Suppress("UNUSED_VARIABLE")
                        val filterField = habboRequest.readBoolean() // TODO: implement filterField

                        @Suppress("UNUSED_VARIABLE")
                        val inverseField = habboRequest.readBoolean() // TODO: implement inverseField
                        log.info("[WIRED] TODO: selector filterField: {}, inverseField: {}", filterField, inverseField)
                    }
                }

                // param7/param8: resolveFurniSources() - TODO: implement furniSources
                val furniSourcesCount = habboRequest.readInt()
                if (furniSourcesCount > 0) {
                    log.info("[WIRED] TODO: furniSourcesCount: {}", furniSourcesCount)
                    repeat(furniSourcesCount) { _ ->
                        @Suppress("UNUSED_VARIABLE")
                        val furniSource = habboRequest.readInt()
                    }
                }

                // param8/param9: resolveUserSources() - TODO: implement userSources
                val userSourcesCount = habboRequest.readInt()
                if (userSourcesCount > 0) {
                    log.info("[WIRED] TODO: userSourcesCount: {}", userSourcesCount)
                    repeat(userSourcesCount) { _ ->
                        @Suppress("UNUSED_VARIABLE")
                        val userSource = habboRequest.readInt()
                    }
                }

                // param3: resolveVariableIds() - TODO: implement variableIds
                val variableIdsCount = habboRequest.readInt()
                if (variableIdsCount > 0) {
                    log.info("[WIRED] TODO: variableIdsCount: {}", variableIdsCount)
                    repeat(variableIdsCount) { _ ->
                        @Suppress("UNUSED_VARIABLE")
                        val variableId = habboRequest.readInt()
                    }
                }

                // param6: getStuffIds2() - TODO: implement stuffIds2
                val stuffIds2Count = habboRequest.readInt()
                if (stuffIds2Count > 0) {
                    log.info("[WIRED] TODO: stuffIds2Count: {}", stuffIds2Count)
                    repeat(stuffIds2Count) { _ ->
                        @Suppress("UNUSED_VARIABLE")
                        val stuffId2 = habboRequest.readInt()
                    }
                }
            } else {
                // Delay apenas para effects na versão antiga
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                    it.delay = habboRequest.readInt()
                }
            }

            it.options = options
            it.items = roomItemsIds
            setData()
            return true
        }
        return false
    }

    open fun setData() {
        // Override in subclasses to reload internal variables
    }

    fun writeDialog(habboResponse: HabboResponse, wiredData: WiredData, habboAir: Boolean) {
        habboResponse.apply {
            // Determine if this wired uses items
            val hasItems = wiredData.items.isNotEmpty()

            if (hasItems) {
                writeItems(wiredData, habboAir)
            } else {
                writeEmptyItems(habboAir)
            }

            writeItemInfo(roomItem)

            // Determine settings based on wired data
            val settingsCount = when {
                wiredData.message.isNotEmpty() && wiredData.options.isNotEmpty() -> wiredData.options.size
                wiredData.message.isNotEmpty() -> 0
                wiredData.options.isNotEmpty() -> wiredData.options.size
                else -> 0
            }

            writeSettings(this, wiredData.message, wiredData.options, settingsCount, habboAir)

            if (habboAir) {
                writeListOfIds(emptyList()) // variable ids
                writeListOfIds(if (hasItems) wiredData.items else emptyList()) // furni ids
                writeListOfIds(emptyList()) // user ids
            }

            // Para o Habbo antes do AIR , a classe pai termina antes de CODE
            writeInt(code())

            if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                writeDelay(wiredData.delay)
            } else if (habboAir && roomItem.furnishing.interactionType.name.startsWith("WIRED_SELECTOR")) {
                writeSelectorDefinitionSpecifics(isFilter = false, isInvert = false)
            } else if (habboAir && roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) {
                writeConditionDefinitionSpecifics(quantifierCode = 0)
            }

            if (habboAir) {
                writeBoolean(true) // advanced mode
                writeInputSourcesConf()
                writeBoolean(true) // allowWallFurni

                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) {
                    writeConditionTypeSpecifics(quantifierType = 0, isInvert = false)
                }

                writeWiredContext()
                writeDefaultIntParams()
            } else {
                // Legacy format
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                    writeBlockedTriggers(wiredData)
                } else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) {
                    writeBlockedActions(wiredData)
                }
            }
        }
    }

    companion object {
        fun HabboResponse.writeEmptyItems(habboAir: Boolean) {
            writeInt(0) // selectable items
            writeInt(0) // items

            if (habboAir) {
                writeInt(0) // todo: stuffIds2
            }
        }

        fun HabboResponse.writeItems(wiredData: WiredData, habboAir: Boolean) {
            writeInt(20) // selectable items
            if (wiredData.items.isEmpty()) writeInt(0)
            else wiredData.items.let { roomItems ->
                writeInt(roomItems.size) // how many selected items
                roomItems.forEach { writeInt(it) } // items
            }

            if (habboAir) {
                writeInt(0) // todo: stuffIds2
            }
        }

        fun HabboResponse.writeItemInfo(roomItem: RoomItem) {
            writeInt(roomItem.furnishing.spriteId)
            writeInt(roomItem.id)
        }

        fun HabboResponse.writeDelay(delay: Int) {
            writeInt(delay)
        }

        @Suppress("UNUSED_PARAMETER")
        fun HabboResponse.writeBlockedTriggers(wiredData: WiredData) {
            // todo
            /*val blockedTriggers = mutableSetOf<Int>()

            writeInt(blockedTriggers.size)
            blockedTriggers.forEach { writeInt(it) }*/

            writeInt(0)
        }

        @Suppress("UNUSED_PARAMETER")
        fun HabboResponse.writeBlockedActions(wiredData: WiredData) {
            // todo
            writeInt(0)
        }

        fun HabboResponse.writeEmptySettings(habboAir: Boolean = false) {
            writeUTF("") // no text box
            writeInt(0) // no options

            if (!habboAir) {
                writeInt(0) // ??? - algo sobre requiresFurni
            }
        }

        fun writeSettings(
            response: HabboResponse,
            textBox: String,
            settings: List<Int>,
            exceptedSettingsSize: Int,
            habboAir: Boolean
        ) {
            response.writeUTF(textBox)
            response.writeInt(exceptedSettingsSize)

            if (exceptedSettingsSize > 0) {
                @Suppress("ForEachParameterNotUsed")
                if (settings.size != exceptedSettingsSize) (0 until exceptedSettingsSize).forEach { response.writeInt(0) }
                else settings.forEach { response.writeInt(it) }
            }

            if (!habboAir) {
                response.writeInt(0) // ???
            }
        }

        fun HabboResponse.writeListOfIds(listOfIds: List<Int>) {
            // TODO: needs implementing
            writeInt(listOfIds.size) // amountFurniSelections
            listOfIds.forEach {
                writeInt(it)
            }
        }

        fun HabboResponse.writeInputSourcesConf() {
            // TODO: needs implementing
            writeInt(0) // amountFurniSelections
            // writeInt() size for for
            // writeInt() for
            writeInt(0) // amountUserSelections
            // writeInt() size for for
            // writeInt() for
            writeInt(0) // defaultFurniSources
            // writeInt() for
            writeInt(0) // defaultUserSources
            // writeInt() for
        }

        fun HabboResponse.writeWiredContext() {
            // TODO: needs implementing
            // var _loc4_:int = param1.readInteger();
            //         _loc3_ = 0;
            //         while(_loc3_ < _loc4_)
            //         {
            //            _loc2_ = param1.readInteger();
            //            switch(_loc2_)
            //            {
            //               case _SafeCls_4319._SafeStr_9768: // 0
            //                  _SafeStr_9214 = new AllVariablesInRoom(param1);
            //                  break;
            //               case _SafeCls_4319._SafeStr_9747: // 1
            //                  _SafeStr_8790 = new VariableInfoAndHolders(param1);
            //                  break;
            //               case _SafeCls_4319._SafeStr_9756: // 2
            //                  _SafeStr_9338 = new VariableInfoAndHolders(param1);
            //                  break;
            //               case _SafeCls_4319._SafeStr_9861: // 3
            //                  _SafeStr_9178 = new VariableInfoAndValue(param1);
            //                  break;
            //               case _SafeCls_4319._SafeStr_9816: // 4
            //                  _SafeStr_8575 = new SharedVariableList(param1);
            //                  break;
            //               case _SafeCls_4319._SafeStr_9748: // 5
            //                  _SafeStr_9560 = VariableList.createFromMessage(param1);
            //                  break;
            //               case _SafeCls_4319._SafeStr_9728: // 6
            //                  _SafeStr_8295 = new SharedGlobalPlaceholderList(param1);
            //            }
            //            _loc3_++;
            //         }
            writeInt(0) // TODO: needs implementing
            // start VariableInfoAndHolders
            //  public function VariableInfoAndHolders(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         _SafeStr_5460 = new WiredVariable(param1);
            //         _SafeStr_8148 = new Vector.<ObjectIdAndValuePair>();
            //         var _loc2_:int = param1.readInteger();
            //         var _loc3_:int = 0;
            //         while(0 < _loc2_)
            //         {
            //            _SafeStr_8148.push(new ObjectIdAndValuePair(param1));
            //            _loc3_++;
            //         }
            //      }
            // public function WiredVariable(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         variableId = param1.readString();
            //         variableType = param1.readInteger();
            //         _variableName = param1.readString();
            //         availabilityType = param1.readInteger();
            //         variableTarget = param1.readInteger();
            //         alwaysAvailable = param1.readBoolean();
            //         canCreateAndDelete = param1.readBoolean();
            //         hasValue = param1.readBoolean();
            //         canWriteValue = param1.readBoolean();
            //         canInterceptChanges = param1.readBoolean();
            //         isInvisible = param1.readBoolean();
            //         canReadCreationTime = param1.readBoolean();
            //         canReadLastUpdateTime = param1.readBoolean();
            //         var hasTextConnector:Boolean = param1.readBoolean(); // always send false, it seems to be useless
            //         if(hasTextConnector)
            //         {
            //            _SafeStr_7578 = new _SafeCls_89();
            //            var _loc3_:int = param1.readInteger();
            //            var _loc4_:int = 0;
            //         }
            //      }
            // public function ObjectIdAndValuePair(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         _SafeStr_4689 = param1.readInteger();
            //         _value = param1.readInteger();
            //      }
            // end VariableInfoAndHolders
            // public function VariableInfoAndValue(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         _SafeStr_5460 = new WiredVariable(param1);
            //         _value = param1.readInteger();
            //      }
            //       public function SharedVariableList(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         var _loc2_:int = param1.readInteger();
            //         var _loc4_:int = 0;
            //         while(0 < _loc2_)
            //         {
            //            var _loc3_:SharedVariable = new SharedVariable(param1);
            //            _SafeStr_7924.push(null);
            //            _SafeStr_7043.push(null.wiredVariable);
            //            _loc4_++;
            //         }
            //      }
            //       public function SharedVariable(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         _SafeStr_6493 = param1.readInteger();
            //         _roomName = param1.readString();
            //         _SafeStr_8232 = new WiredVariable(param1);
            //      }
            // public static function createFromMessage(param1:IMessageDataWrapper) : VariableList
            //      {
            //         var _loc5_:int = 0;
            //         var _loc4_:WiredVariable = null;
            //         var _loc2_:Array = [];
            //         var _loc3_:int = param1.readInteger();
            //         _loc5_ = 0;
            //         while(_loc5_ < _loc3_)
            //         {
            //            _loc4_ = new WiredVariable(param1);
            //            _loc2_.push(_loc4_);
            //            _loc5_++;
            //         }
            //         return new VariableList(_loc2_);
            //      }
            // public function SharedGlobalPlaceholderList(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         _SafeStr_8176 = new Vector.<SharedGlobalPlaceholder>();
            //         var _loc2_:int = param1.readInteger();
            //         var _loc3_:int = 0;
            //         while(0 < _loc2_)
            //         {
            //            _SafeStr_8176.push(new SharedGlobalPlaceholder(param1));
            //            _loc3_++;
            //         }
            //      }
            // public function SharedGlobalPlaceholder(param1:IMessageDataWrapper)
            //      {
            //         super();
            //         _SafeStr_6493 = param1.readInteger();
            //         _roomName = param1.readString();
            //         _placeholderName = param1.readString();
            //      }
        }

        fun HabboResponse.writeDefaultIntParams() {
            writeInt(0) // size
            // writeInt(0) // todo param
        }

        fun HabboResponse.writeSelectorDefinitionSpecifics(isFilter: Boolean, isInvert: Boolean) {
            writeBoolean(isFilter)
            writeBoolean(isInvert)
        }

        fun HabboResponse.writeConditionDefinitionSpecifics(quantifierCode: Int) {
            writeInt(quantifierCode)
        }

        fun HabboResponse.writeConditionTypeSpecifics(quantifierType: Byte, isInvert: Boolean) {
            writeByte(quantifierType.toInt())
            writeBoolean(isInvert)
        }
    }
}