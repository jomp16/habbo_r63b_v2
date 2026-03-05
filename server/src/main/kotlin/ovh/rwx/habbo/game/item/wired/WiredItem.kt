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

package ovh.rwx.habbo.game.item.wired

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.effect.WiredEffect
import ovh.rwx.habbo.game.room.Room

abstract class WiredItem(val room: Room, val roomItem: RoomItem) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    abstract fun code(): Int

    open val requiresItems: Boolean = false
    open val requiresUsers: Boolean = false

    // As subclasses devem sobrescrever isso se quiserem habilitar opções avançadas
    open val allowedFurniSources: List<WiredFurniSource>
        get() = if (requiresItems) {
            if (this is WiredEffect) {
                listOf(
                    WiredFurniSource.SELECTED_ITEMS,
                    WiredFurniSource.SELECTOR_ITEMS
                )
            } else {
                listOf(
                    WiredFurniSource.SELECTED_ITEMS,
                    WiredFurniSource.SELECTOR_ITEMS,
                    WiredFurniSource.SIGNAL_ITEMS,
                    WiredFurniSource.TRIGGERING_ITEM,
                )
            }
        } else {
            emptyList()
        }

    open val defaultFurniSource: WiredFurniSource
        get() = if (requiresItems) WiredFurniSource.SELECTED_ITEMS else WiredFurniSource.TRIGGERING_ITEM

    open val allowedUserSources: List<WiredUserSource>
        get() = if (requiresUsers) {
            listOf(
                WiredUserSource.TRIGGERING_USER, // Usar o usuário acionador (Padrão)
                WiredUserSource.SELECTOR_USERS,  // Usar usuários do seletor
                WiredUserSource.SIGNAL_USERS     // Usar usuários do sinal
            )
        } else {
            emptyList() // Se for false, esconde o menu de opções de usuário no client
        }

    open val defaultUserSource: WiredUserSource = WiredUserSource.TRIGGERING_USER

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

            // param5: stuffIds (Mobis selecionados no quarto)
            val itemsCount = habboRequest.readInt()
            val roomItemsIds = mutableListOf<Int>()
            repeat(itemsCount) { _ ->
                val itemId = habboRequest.readInt()
                if (room.itemManager.items.containsKey(itemId)) {
                    val roomItem1 = room.itemManager.items[itemId] ?: return@repeat
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
                        val quantifier = habboRequest.readInt() // TODO: implement quantifier
                        log.info("[WIRED] TODO: condition quantifier: {}", quantifier)
                    }

                    roomItem.furnishing.interactionType.name.startsWith("WIRED_SELECTOR") -> {
                        // Selector: param7 = resolveFilterField(), param8 = resolveInverseField()
                        it.filter = habboRequest.readBoolean()
                        it.inverse = habboRequest.readBoolean()
                    }
                }

                // param7/param8: furniSourceTypes (Ex: 0 = Selected Furni, 100 = Triggering Furni)
                val furniSourcesCount = habboRequest.readInt()
                val furniSources = mutableListOf<WiredFurniSource>()
                repeat(furniSourcesCount) { _ ->
                    furniSources += WiredFurniSource.fromCode(habboRequest.readInt()) ?: return@repeat
                }
                it.furniSources = furniSources

                // param8/param9: userSourceTypes (Ex: 0 = Triggering User)
                val userSourcesCount = habboRequest.readInt()
                val userSources = mutableListOf<WiredUserSource>()
                repeat(userSourcesCount) { _ ->
                    userSources += WiredUserSource.fromCode(habboRequest.readInt()) ?: return@repeat
                }
                it.userSources = userSources

                // param3: resolveVariableIds() - TODO: implement variableIds
                val variableIdsCount = habboRequest.readInt()
                if (variableIdsCount > 0) {
                    log.info("[WIRED] TODO: variableIdsCount: {}", variableIdsCount)
                    repeat(variableIdsCount) { _ ->
                        @Suppress("UNUSED_VARIABLE")
                        val variableId = habboRequest.readUTF() // CORRIGIDO: O AS3 envia uma String aqui!
                    }
                }

                // param6: getStuffIds2() (Mobis secundários)
                val stuffIds2Count = habboRequest.readInt()
                val roomItemsIds2 = mutableListOf<Int>()
                repeat(stuffIds2Count) { _ ->
                    val itemId = habboRequest.readInt()
                    if (room.itemManager.items.containsKey(itemId)) {
                        val roomItem2 = room.itemManager.items[itemId] ?: return@repeat
                        if (!roomItem2.furnishing.interactionType.name.startsWith("WIRED")) {
                            roomItemsIds2 += itemId
                        }
                    }
                }
                it.stuffIds2 = roomItemsIds2

            } else {
                // Delay apenas para effects na versão antiga
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                    it.delay = habboRequest.readInt()
                }
            }

            it.options = options
            it.items = roomItemsIds
            log.debug("WiredData salvo com sucesso: {}", it)
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
            if (requiresItems) {
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
                writeListOfIds(emptyList()) // todo: variable ids
                // furniSourceTypes: Pega do DB. Se estiver vazio (novo), usa o default definido no WiredItem
                val fSources =
                    wiredData.furniSources.ifEmpty { listOf(this@WiredItem.defaultFurniSource) }.map { it.code }
                writeListOfIds(fSources)

                // userSourceTypes: Pega do DB. Se estiver vazio (novo), usa o default definido no WiredItem
                val uSources =
                    wiredData.userSources.ifEmpty { listOf(this@WiredItem.defaultUserSource) }.map { it.code }
                writeListOfIds(uSources)
            }

            // Para o Habbo antes do AIR , a classe pai termina antes de CODE
            writeInt(code())

            if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
                writeDelay(wiredData.delay)
            } else if (habboAir && roomItem.furnishing.interactionType.name.startsWith("WIRED_SELECTOR")) {
                writeSelectorDefinitionSpecifics(wiredData.filter, wiredData.inverse)
            } else if (habboAir && roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) {
                writeConditionDefinitionSpecifics(quantifierCode = 0)
            }

            if (habboAir) {
                // A aba "Avançado" só deve aparecer se o Wired tiver mais de 1 opção de fonte de Mobi ou de Usuário!
                val hasAdvancedMode =
                    this@WiredItem.allowedFurniSources.size > 1 || this@WiredItem.allowedUserSources.size > 1
                writeBoolean(hasAdvancedMode)
                writeInputSourcesConf(this@WiredItem)
                writeBoolean(false) // allowWallFurni

                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) {
                    writeConditionTypeSpecifics(quantifierType = 0, isInvert = false)
                }

                writeWiredContext()
                writeDefaultIntParams()
            } else {
                // Legacy format
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_TRIGGER")) {
                    writeBlockedTriggers(wiredData)
                } else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_EFFECT")) {
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
            if (wiredData.items.isEmpty()) {
                writeInt(0)
            } else {
                wiredData.items.let { roomItems ->
                    writeInt(roomItems.size) // how many selected items
                    roomItems.forEach { writeInt(it) } // items
                }
            }

            if (habboAir) {
                // stuffIds2 (Lista secundária)
                wiredData.stuffIds2.let { stuffIds2 ->
                    writeInt(stuffIds2.size)
                    stuffIds2.forEach { writeInt(it) }
                }
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
                if (settings.size != exceptedSettingsSize) {
                    repeat((0 until exceptedSettingsSize).count()) { response.writeInt(0) }
                } else {
                    settings.forEach { response.writeInt(it) }
                }
            }

            if (!habboAir) {
                response.writeInt(0) // ???
            }
        }

        fun HabboResponse.writeListOfIds(listOfIds: List<Int>) {
            writeInt(listOfIds.size) // amountFurniSelections
            listOfIds.forEach {
                writeInt(it)
            }
        }

        /**
         * Constrói dinamicamente os menus dropdown de origem do Wired baseado
         * nas permissões definidas na subclasse (allowedFurniSources / allowedUserSources).
         */
        fun HabboResponse.writeInputSourcesConf(wiredItem: WiredItem) {
            val furniCodes = wiredItem.allowedFurniSources.map { it.code }
            val userCodes = wiredItem.allowedUserSources.map { it.code }

            // 1. _SafeStr_6921 (getAllowedFurniSources)
            // O cliente aceita múltiplas listas, mas 99% dos Wireds usam apenas 1 lista principal.
            // todo: corrigir acima, o wired mover mobi para mobi tem 2 furni source.
            writeInt(if (furniCodes.isNotEmpty()) 1 else 0) // Quantidade de grupos de seleção
            if (furniCodes.isNotEmpty()) {
                writeInt(furniCodes.size) // Quantidade de opções neste grupo
                furniCodes.forEach { writeInt(it) }
            }

            // 2. _SafeStr_6922 (getAllowedUserSources)
            writeInt(if (userCodes.isNotEmpty()) 1 else 0)
            if (userCodes.isNotEmpty()) {
                writeInt(userCodes.size)
                userCodes.forEach { writeInt(it) }
            }

            // 3. _SafeStr_6923 (defaultFurniSources)
            // Se o Wired suportar mobis, enviamos o padrão definido pela classe
            if (furniCodes.isNotEmpty()) {
                writeInt(1)
                writeInt(wiredItem.defaultFurniSource.code)
            } else {
                writeInt(0)
            }

            // 4. _SafeStr_6924 (defaultUserSources)
            // Se o Wired suportar usuários, enviamos o padrão definido pela classe
            if (userCodes.isNotEmpty()) {
                writeInt(1)
                writeInt(wiredItem.defaultUserSource.code)
            } else {
                writeInt(0)
            }
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