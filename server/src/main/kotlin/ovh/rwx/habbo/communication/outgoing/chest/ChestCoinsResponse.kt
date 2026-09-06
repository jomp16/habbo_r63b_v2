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

package ovh.rwx.habbo.communication.outgoing.chest

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

/**
 * Payload estruturado para envio de saldo de moedas do baú (CHEST_COINS).
 *
 * @param chestItemId ID do item do baú no quarto.
 * @param coins Quantidade atual de moedas no baú.
 * @param isUpdate Se true, atualiza a UI existente; se false, inicialização de abertura.
 */
data class ChestCoinsData(
    val chestItemId: Int,
    val coins: Int,
    val isUpdate: Boolean = false,
)

@Suppress("unused", "UNUSED_PARAMETER")
class ChestCoinsResponse {
    @Response(Outgoing.CHEST_COINS)
    fun response(habboResponse: HabboResponse, data: ChestCoinsData) {
        habboResponse.apply {
            writeInt(data.chestItemId)
            writeInt(data.coins)
            writeBoolean(data.isUpdate)
        }
    }
}
