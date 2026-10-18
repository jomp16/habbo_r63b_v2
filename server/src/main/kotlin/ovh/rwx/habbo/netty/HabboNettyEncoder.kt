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

@ChannelHandler.Sharable
class HabboNettyEncoder : MessageToByteEncoder<HabboResponse>() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun encode(ctx: ChannelHandlerContext, msg: HabboResponse, out: ByteBuf) {
        try {
            val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
            val username =
                if (habboSession.authenticated) habboSession.userInformation.username else habboSession.channel.ip()

            if (log.isDebugEnabled) {
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

            val byteBuf = msg.byteBuf

            out.apply {
                if (!habboSession.isPreShuffle || habboSession.protocolEra == ProtocolEra.PRE_SHUFFLE_MODERN) {
                    writeInt(byteBuf.writerIndex() + 2)
                    writeShort(msg.headerId)
                    writeBytes(byteBuf)
                } else {
                    writeBytes(HabboBase64.encodeBytes(msg.headerId))
                    writeBytes(byteBuf)
                    writeByte(1)
                }
            }

            // Full-duplex RC4 (era pacote, ≥ 2014-08-14): XOR do frame INTEIRO (length +
            // header + corpo) com o keystream do cipher de saída — espelho do leitor AS3
            // `_SafeStr_8113`, que decripta length e corpo continuamente com `_SafeStr_8088`.
            habboSession.rc4EncryptionOut?.takeIf { !msg.plaintext }?.let { crypto ->
                val plain = ByteArray(out.readableBytes())
                out.readBytes(plain)
                out.clear()
                out.writeBytes(crypto.parse(plain))
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