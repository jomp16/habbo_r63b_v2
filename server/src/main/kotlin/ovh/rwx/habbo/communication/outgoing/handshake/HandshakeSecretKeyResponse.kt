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

package ovh.rwx.habbo.communication.outgoing.handshake

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeSecretKeyResponse {
    @Response(Outgoing.SECRET_KEY)
    @ResponseR63A(OutgoingR63A.SECRET_KEY)
    fun response(habboResponse: HabboResponse, publicKey: String) {
        habboResponse.apply {
            // Nascimento do boolean: 2014-08-14 (era pacote do DH). true = o cliente cria o
            // segundo cipher RC4 (`_SafeStr_8088`) e passa a decriptar server→client; antes disso
            // o parser lê só a String e o boolean trailing é ignorado. A data aqui precisa
            // bater com o predicado do HandshakeSecretKeyHandler.
            val fullDuplex = isVersionAtLeast(2014, 8, 14)

            writeUTF(publicKey)
            writeBoolean(fullDuplex)

            // A própria resposta SecretKey vai em plaintext: o cliente só instancia o
            // cipher 2 depois de parsear este pacote.
            if (fullDuplex) plaintext = true
        }
    }
}