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
import ovh.rwx.habbo.game.chest.TradeRequirementType

/**
 * Payload estruturado para inicialização de transação Wired Trade (WIRED_TRADE_INITIATE).
 *
 * @param requirementType Tipo de exigência da troca ([TradeRequirementType]).
 * @param timeoutSeconds Duração do timer da transação em segundos.
 * @param youGetText Texto informativo de recompensa / o que você recebe.
 * @param layoutType Tipo de layout da janela no client.
 * @param showRequirementsImmediate Se exibe os requisitos imediatamente na abertura.
 * @param overridePreviousTrade Se sobrescreve qualquer transação anterior em andamento.
 */
data class WiredTradeInitiateData(
    val requirementType: TradeRequirementType,
    val timeoutSeconds: Int,
    val youGetText: String = "",
    val layoutType: String = "",
    val showRequirementsImmediate: Boolean = false,
    val overridePreviousTrade: Boolean = false,
)

/**
 * Espelho de _SafeStr_3382 (_-E2U), parser de FurnitureTradeRequirements.../TradeRequirement.
 *
 * Wire: type:I, youGetText:S, layoutType:S, [se type == 4: rules], 
 * showRequirementsImmediate:B, overridePreviousTrade:B, timeoutSeconds:I.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class WiredTradeInitiateResponse {
    @Response(Outgoing.WIRED_TRADE_INITIATE)
    fun response(habboResponse: HabboResponse, data: WiredTradeInitiateData) {
        habboResponse.apply {
            writeInt(data.requirementType.id)
            writeUTF(data.youGetText)
            writeUTF(data.layoutType)
            writeBoolean(data.showRequirementsImmediate)
            writeBoolean(data.overridePreviousTrade)
            writeInt(data.timeoutSeconds)
        }
    }
}
