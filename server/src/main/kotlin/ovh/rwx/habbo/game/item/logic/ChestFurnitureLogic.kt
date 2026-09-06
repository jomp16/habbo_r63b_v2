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

package ovh.rwx.habbo.game.item.logic

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.LimitedItemData
import ovh.rwx.habbo.game.item.stuff.MapStuffData
import ovh.rwx.habbo.game.item.stuff.StuffData
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Espelho de com.sulake.habbo.room.object.logic.furniture._SafeStr_1751/_SafeStr_1752 (client).
 *
 * O extraData do chest é armazenado como JSON (mobília nova, sem legado):
 * {"state":"0","is_wired_enabled":"0",...}
 *
 * O client recebe o StuffData.MapStuffData (formatKey 1) e expõe:
 *  - state                -> estado visual (ímpar = aberto, exibindo previews)
 *  - is_wired_enabled     -> furniture_chest_is_wired_enabled
 *  - state_control_mode   -> "0" (normal), "1" (sempre aberto), "2" (sempre fechado), "3" (wired)
 *  - is_locked            -> "1" se trancado
 *  - visuals              -> furniture_furni_chest_shown_asset_names ("isWallItem,typeId[,legacyPosterId];...")
 *  - chest_name           -> nome do baú no infostand
 *  - contents_count       -> indicador de itens no inventário/infostand
 */
class ChestFurnitureLogic : FurnitureLogic() {
    override val interactionTypes: List<InteractionType> = listOf(InteractionType.CHEST)

    override fun parseStuffData(
        extraData: String,
        furnishing: Furnishing,
        limitedItemData: LimitedItemData?,
        magicRemove: Boolean
    ): StuffData =
        MapStuffData(parseChestExtraData(extraData))

    override fun correctCatalogExtraData(
        habboSession: HabboSession,
        extraData: String,
        furnishing: Furnishing
    ): String =
        formatChestExtraData(defaultChestValues())

    override fun sanitizeForDatabase(extraData: String): String =
        Companion.sanitizeForDatabase(extraData)

    companion object {
        const val KEY_IS_WIRED_ENABLED = "is_wired_enabled"
        const val KEY_VISUALS = "visuals"
        const val KEY_CHEST_NAME = "chest_name"
        const val KEY_CHEST_DESC = "chest_desc"
        const val KEY_CONTENTS_COUNT = "contents_count"
        const val KEY_CONTENTS_COINS = "contents_coins"
        const val KEY_EVERYONE_CAN_OPEN = "everyone_can_open"
        const val KEY_EVERYONE_CAN_DONATE = "everyone_can_donate"
        const val KEY_AUTO_LOCK = "auto_lock"
        const val KEY_IS_LOCKED = "is_locked"
        const val KEY_STATE_CONTROL_MODE = "state_control_mode"
        const val KEY_PREVIEW_MODE = "preview_mode"
        const val KEY_PREVIEW_AMOUNT = "preview_amount"
        const val KEY_NOTIFY_MODE = "notify_mode"
        const val KEY_NOTIFICATION_CHEST_FULL = "notification_chest_full"
        const val KEY_NOTIFICATION_DONATION = "notification_donation"
        const val KEY_NOTIFICATION_SOMEONE_WITHDRAWS = "notification_someone_withdraws"
        const val KEY_NOTIFICATION_CHEST_EMPTY = "notification_chest_empty"
        const val KEY_NOTIFICATION_WIRED_TRANSACTION = "notification_wired_transaction"
        const val VISUALS_ITEM_SEPARATOR = ";"

        // Todas as keys lidas pelo ChestSettingsUI.onEdit / ChestNotificationSettingsUI.onEdit
        // (getValue() null no client = TypeError #1009 / checkboxes desmarcados)
        fun defaultChestValues(): LinkedHashMap<String, String> = linkedMapOf(
            StuffData.KEY_STATE to "0",
            KEY_IS_WIRED_ENABLED to "0",
            KEY_CHEST_NAME to "",
            KEY_CHEST_DESC to "",
            KEY_EVERYONE_CAN_OPEN to "0",
            KEY_EVERYONE_CAN_DONATE to "0",
            KEY_STATE_CONTROL_MODE to "0",
            KEY_PREVIEW_MODE to "0",
            KEY_PREVIEW_AMOUNT to "1",
            KEY_NOTIFY_MODE to "0",
            KEY_NOTIFICATION_CHEST_FULL to "0",
            KEY_NOTIFICATION_DONATION to "0",
            KEY_NOTIFICATION_SOMEONE_WITHDRAWS to "0",
            KEY_NOTIFICATION_CHEST_EMPTY to "0",
            KEY_NOTIFICATION_WIRED_TRANSACTION to "0",
            KEY_AUTO_LOCK to "0",
            KEY_IS_LOCKED to "0",
            KEY_VISUALS to "",
            KEY_CONTENTS_COUNT to "0",
            KEY_CONTENTS_COINS to "0",
        )

        private val jsonMapper = jacksonObjectMapper()

        fun parseChestExtraData(extraData: String): LinkedHashMap<String, String> {
            if (extraData.isBlank()) return defaultChestValues()

            return runCatching {
                jsonMapper.readValue<LinkedHashMap<String, String>>(extraData)
            }.getOrElse { defaultChestValues() }
        }

        fun formatChestExtraData(values: Map<String, String>): String =
            jsonMapper.writeValueAsString(values)

        /**
         * Higieniza o extraData para persistência no banco de dados.
         * Garante que baús com stateMode != 1 ("sempre aberto") sejam sempre salvos como fechados (state="0")
         * e sem previews visuais efêmeros.
         */
        fun sanitizeForDatabase(extraData: String): String {
            if (extraData.isBlank()) return extraData

            val map = parseChestExtraData(extraData)
            val stateMode = map[KEY_STATE_CONTROL_MODE]?.toIntOrNull() ?: 0
            if (stateMode != 1) {
                map[StuffData.KEY_STATE] = "0"
                map[KEY_VISUALS] = ""
            }

            return formatChestExtraData(map)
        }
    }
}
