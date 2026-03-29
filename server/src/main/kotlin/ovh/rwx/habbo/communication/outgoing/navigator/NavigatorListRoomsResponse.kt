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

package ovh.rwx.habbo.communication.outgoing.navigator

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.navigator.NavigatorListPayload

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorListRoomsResponse {
    @ResponseR63A(OutgoingR63A.NAVIGATOR_LIST_ROOMS)
    fun responseR63A(habboResponse: HabboResponse, payload: NavigatorListPayload) {
        habboResponse.apply {
            // --- HEADER ---
            // A R34 e as builds antigas usavam esse formato
            if (isVersionBefore(2010, 11, 12)) {
                writeInt(payload.forceDisplay)
                writeInt(payload.mode.mode)
            } else {
                writeInt(payload.categoryId)
            }

            writeUTF(payload.query)
            writeInt(payload.rooms.size)

            payload.rooms.forEach {
                serialize(it, payload.showEvents, false)
            }

            // Revelação R62: O "Ad/Promo" no fim da página surgiu em Nov/2010
            if (isVersionAtLeast(2010, 11, 12)) {
                writeBoolean(payload.isOfficialRoom) // O primeiro booleano depois do loop
                serializeNavigatorAd(habboResponse)
            }
        }
    }

    private fun serializeNavigatorAd(habboResponse: HabboResponse) {
        habboResponse.apply {
            // Bloco vazio básico (Presente na R62 e na maioria da R63)
            writeInt(-1) // ID do Ad
            writeUTF("")
            writeUTF("")
            writeInt(0)
            writeUTF("")
            writeUTF("")
            writeInt(0)
            writeInt(0)
            writeInt(0)
            writeUTF("")

            // Revelação do Caos: Sub-Classe Dinâmica do Ad
            // Na Build R63 Base (Dez/2010) e na Phoenix (Ago/2011), há uma CLASS aqui.
            // Em outras (Abril/2011), há um Boolean simples.

            // Se for a Phoenix (Marco de Ouro) ou a R63 Base, preenchemos a sub-classe vazia
            if (isExactVersion(2010, 12, 3) || isVersionAtLeast(2011, 5, 9)) {
                // Estrutura da sub-classe (CLASS(§_-f1§) do trace da Phoenix)
                // String | Int | Int | String | Int | Int
                writeUTF("") // Nome/Tipo
                writeInt(0)
                writeInt(0)
                writeUTF("")
                writeInt(0)
                writeInt(0)

                writeBoolean(false) // O Boolean final que sempre vem depois do Ad
            }
            // Build intermediárias onde a sub-classe foi retirada temporariamente
            else if (isVersionAtLeast(2011, 4, 5)) {
                writeBoolean(false) // Apenas o boolean final
            }
        }
    }
}
