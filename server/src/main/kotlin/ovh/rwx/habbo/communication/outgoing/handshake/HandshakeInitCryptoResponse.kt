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
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

@Suppress("unused", "UNUSED_PARAMETER")
class HandshakeInitCryptoResponse {
    @Response(Outgoing.INIT_CRYPTO)
    @ResponseR63A(OutgoingR63A.INIT_CRYPTO)
    fun response(habboResponse: HabboResponse, data: InitCryptoData) {
        habboResponse.apply {
            if (isVersionBefore(2014, 8, 14)) {
                // Era supersecret (preshuffle R63A + RELEASE63-2013-03-22..2014-07-09):
                // o DH prime/generator não trafega no socket — o cliente busca esteganografado
                // na imagem hotelview.banner.url?token=...; o pacote carrega só o token.
                writeUTF(data.token)
                writeBoolean(true) // isServerEncrypted
            } else {
                // Era pacote (2014-08-14 em diante): prime/generator RSA-assinados no corpo
                writeUTF(data.prime)
                writeUTF(data.generator)
            }
        }
    }
}

/**
 * Payload do InitDiffieHandshakeEvent/InitCryptoMessageEvent. A era define quais
 * campos trafegam: [InitCryptoData.token] na era supersecret (o cliente usa o token
 * para buscar a imagem com os parâmetros DH), [InitCryptoData.prime] e
 * [InitCryptoData.generator] na era pacote.
 */
data class InitCryptoData(
    val token: String,
    val prime: String,
    val generator: String,
)
