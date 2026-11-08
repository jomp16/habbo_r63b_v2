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
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.ReferenceCountUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.ProtocolEra
import ovh.rwx.habbo.encryption.decoder.HabboBase64
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.kotlin.ip
import ovh.rwx.habbo.metrics.ServerMetrics

@ChannelHandler.Sharable
class HabboNettyEncoder : MessageToByteEncoder<HabboResponse>() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    // Rascunho do RC4 de saída: o encoder é sharable, mas cada encode roda na thread do event loop do canal
    private val rc4Scratch: ThreadLocal<ByteArray> = ThreadLocal.withInitial { ByteArray(4096) }

    override fun encode(ctx: ChannelHandlerContext, msg: HabboResponse, out: ByteBuf) {
        ServerMetrics.recordPacketOut()
        try {
            val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()

            if (log.isDebugEnabled) {
                // só calculado quando o log de pacotes está ligado (antes era a cada pacote, inclusive o ip() de sessões não autenticadas)
                val username =
                    if (habboSession.authenticated) habboSession.userInformation.username else habboSession.channel.ip()
                val outgoing: String = if (!habboSession.isPreShuffle) {
                    HabboServer.habboHandler.outgoingNames[habboSession.release]?.find { it.first == msg.headerId }?.second?.name
                        ?: "null"
                } else {
                    HabboServer.habboHandler.outgoingNamesR63A[habboSession.release]?.find { it.first == msg.headerId }?.second?.name
                        ?: "null"
                }

                log.trace(
                    "({}) - SENT --> [{}][{}] -- {}",
                    username,
                    msg.headerId.toString().padEnd(4),
                    (outgoing).padEnd(HabboServer.habboHandler.largestNameSize),
                    msg.toString()
                )
            }

            // o corpo vem do buffer próprio da resposta ou, num broadcast, do payload compartilhado (sem buffer por ouvinte)
            out.apply {
                if (!habboSession.isPreShuffle || habboSession.protocolEra == ProtocolEra.PRE_SHUFFLE_MODERN) {
                    writeInt(msg.payloadLength + 2)
                    writeShort(msg.headerId)
                    msg.writePayloadTo(this)
                } else {
                    writeBytes(HabboBase64.encodeBytes(msg.headerId))
                    msg.writePayloadTo(this)
                    writeByte(1)
                }
            }

            // Full-duplex RC4 (era pacote, ≥ 2014-08-14): XOR do frame INTEIRO (length +
            // header + corpo) com o keystream do cipher de saída — espelho do leitor AS3
            // `_SafeStr_8113`, que decripta length e corpo continuamente com `_SafeStr_8088`.
            habboSession.rc4EncryptionOut?.takeIf { !msg.plaintext }?.let { crypto ->
                // No lugar, com buffer de rascunho por thread: antes eram 2 ByteArray novos por pacote por sessão
                // (a maior fonte de lixo do emulador sob carga). O frame é do tamanho de um pacote: cabe no rascunho.
                val length = out.readableBytes()
                var scratch = rc4Scratch.get()
                if (scratch.size < length) {
                    scratch = ByteArray(maxOf(length, scratch.size * 2))
                    rc4Scratch.set(scratch)
                }

                out.getBytes(out.readerIndex(), scratch, 0, length)
                crypto.parseInPlace(scratch, 0, length)
                out.setBytes(out.readerIndex(), scratch, 0, length)
            }
        } finally {
            // Guaranteed to run, preventing the memory leak
            if (!msg.keepCopy) {
                msg.close()
            } else {
                ReferenceCountUtil.release(msg.byteBuf)
            }
        }
    }
}