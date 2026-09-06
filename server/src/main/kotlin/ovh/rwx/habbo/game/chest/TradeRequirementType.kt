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

package ovh.rwx.habbo.game.chest

/**
 * Tipos de requisito para transações Wired Trade (WIRED_TRADE_INITIATE / _SafeStr_3382).
 * Espelho de com.sulake.habbo.communication.messages.parser.userdefinedroomevents.wiredtrading.trade.requirements.TradeRequirement.
 *
 * Correlação com o AS3:
 *  - COINS_ONLY (0 / _SafeStr_7555): Apenas Habbo Câmbios (item.className.indexOf("CF_") == 0).
 *      UI do client: exibe anyCoinsText ("Qualquer moeda") e filtra moedas no inventário.
 *  - FURNI_ONLY (1 / _SafeStr_7556): Apenas Mobílias normais (item.className.indexOf("CF_") != 0).
 *      UI do client: exibe anyFurniText ("Qualquer mobília") e oculta câmbios no inventário.
 *  - ALL (2 / _SafeStr_7557): Qualquer item negociável (mobis e moedas).
 *      UI do client: exibe anyAllText ("Qualquer item").
 *  - RULES (4 / _SafeStr_7558): Regras personalizadas / contrato Wired de troca (TradeRequirementRules).
 */
enum class TradeRequirementType(val id: Int) {
    COINS_ONLY(0),
    FURNI_ONLY(1),
    ALL(2),
    RULES(4);

    companion object {
        fun fromId(id: Int): TradeRequirementType? = entries.firstOrNull { it.id == id }
    }
}
