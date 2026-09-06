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

package ovh.rwx.habbo.communication.outgoing.wiredtrade

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

/**
 * Espelho de _SafeStr_3382 (_-E2U), parser de FurnitureTradeRequirements.../TradeRequirement.
 *
 * Wire: type:I, youGetText:S, layoutType:S, [se type == 4: rules], 
 * showRequirementsImmediate:B, overridePreviousTrade:B, timeoutSeconds:I.
 *
 * Para o depósito manual no baú usamos type 0 (sem rules) — o client abre a
 * sub-página wired_trading do inventário ao receber este pacote
 * (WiredTradingModel.onWiredTradeInitiate -> toggleInventorySubPage("wired_trading")).
 */
@Suppress("unused", "UNUSED_PARAMETER")
class WiredTradeInitiateResponse {
    @Response(Outgoing.WIRED_TRADE_INITIATE)
    fun response(habboResponse: HabboResponse, timeoutSeconds: Int) {
        habboResponse.apply {
            writeInt(0) // requirement type (0 = sem rules)
            writeUTF("")
            writeUTF("")
            writeBoolean(false) // showRequirementsImmediate
            writeBoolean(false) // overridePreviousTrade
            writeInt(timeoutSeconds)
        }
    }
}
