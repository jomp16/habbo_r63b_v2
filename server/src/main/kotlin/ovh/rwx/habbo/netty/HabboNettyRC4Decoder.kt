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

package ovh.rwx.habbo.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import ovh.rwx.habbo.encryption.decoder.HabboBase64
import ovh.rwx.habbo.encryption.wedgie.WedgieWrapper
import ovh.rwx.habbo.game.user.HabboSessionManager

class HabboNettyRC4Decoder : ByteToMessageDecoder() {
    private var expectedBase64Length = 0

    override fun decode(ctx: ChannelHandlerContext, incomingByteBuf: ByteBuf, out: MutableList<Any>) {
        val habboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
        val crypto = habboSession.rc4Encryption

        if (crypto is WedgieWrapper) {

            // 1. O Header Wedgie tem exatamente 4 bytes binários.
            // Codificados em Base64 SEM padding (estilo Habbo), eles viram exatos 6 bytes/caracteres.
            if (expectedBase64Length == 0) {
                if (incomingByteBuf.readableBytes() < 6) return

                val base64HeaderBytes = ByteArray(6)
                incomingByteBuf.readBytes(base64HeaderBytes)

                // Adiciona o padding "==" que o AS3 omite, senão o Base64Decoder do Java chora
                val base64HeaderStr = String(base64HeaderBytes) + "=="
                val encryptedHeader = java.util.Base64.getDecoder().decode(base64HeaderStr)

                // Agora SIM nós deciframos os 4 bytes binários!
                val decryptedHeader = crypto.inner.parse(encryptedHeader)

                val b1 = decryptedHeader[1].toInt()
                val b2 = decryptedHeader[2].toInt()
                val b3 = decryptedHeader[3].toInt()

                expectedBase64Length = ((b1 and 0x3F) shl 12) or ((b2 and 0x3F) shl 6) or (b3 and 0x3F)
            }

            // 2. Aguarda a string Base64 do Payload inteiro
            if (incomingByteBuf.readableBytes() < expectedBase64Length) return

            val base64PayloadBytes = ByteArray(expectedBase64Length)
            incomingByteBuf.readBytes(base64PayloadBytes)

            expectedBase64Length = 0 // Reseta para o próximo frame

            try {
                var base64PayloadStr = String(base64PayloadBytes)

                // Completa o padding para o Payload
                val pad = base64PayloadStr.length % 4
                if (pad > 0) base64PayloadStr += "=".repeat(4 - pad)

                val encryptedPayload = java.util.Base64.getDecoder().decode(base64PayloadStr)

                // Decifra a S-Box Outer
                val decryptedData = crypto.outer.parse(encryptedPayload)

                // Verifica no HabboRandom do servidor quantos bytes de lixo o cliente jogou
                val paddingLength = crypto.rng.nextInt() % 5

                val payloadSize = decryptedData.size - paddingLength

                // Reconstrói a "cabeça" do pacote que o AS3 cortou
                val lengthBytes = HabboBase64.encode(payloadSize, 3)

                val buffer = PooledByteBufAllocator.DEFAULT.buffer(3 + payloadSize)
                buffer.writeBytes(lengthBytes)
                buffer.writeBytes(decryptedData, paddingLength, payloadSize)

                out.add(buffer)
            } catch (e: Exception) {
                ctx.close() // Em caso de falha severa, derruba
            }
        } else {
            // Se for a versão RSA, segue normal (sem as insanidades de Base64)
            var bytes = ByteArray(incomingByteBuf.readableBytes())
            incomingByteBuf.readBytes(bytes)

            if (habboSession.rc4Encryption != null) bytes = habboSession.rc4Encryption!!.parse(bytes)

            out.add(PooledByteBufAllocator.DEFAULT.buffer(bytes.size, bytes.size).writeBytes(bytes))
        }
    }
}