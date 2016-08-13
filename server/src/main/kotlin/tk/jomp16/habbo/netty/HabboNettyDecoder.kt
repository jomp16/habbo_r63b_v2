/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.HabboSessionManager
import tk.jomp16.habbo.kotlin.ip

class HabboNettyDecoder : ByteToMessageDecoder() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        if (msg.readableBytes() < 6) return

        msg.markReaderIndex()

        val delimiter = msg.readByte()

        msg.resetReaderIndex()

        if (delimiter == 60.toByte()) {
            // flash policy
            msg.readerIndex(msg.readableBytes())
            msg.discardSomeReadBytes()

            ctx.writeAndFlush("""<?xml version="1.0"?>
<!DOCTYPE cross-domain-policy SYSTEM "/xml/dtds/cross-domain-policy.dtd">
<cross-domain-policy>
<allow-access-from domain="*" to-ports="*" />
</cross-domain-policy>""")
                    .addListener(io.netty.channel.ChannelFutureListener.CLOSE)

            return
        } else {
            val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
            val username = try {
                habboSession.userInformation.username
            } catch (exception: UninitializedPropertyAccessException) {
                ctx.channel().ip()
            }

            if (delimiter != 0.toByte()) {
                msg.readerIndex(msg.readableBytes())
                msg.discardSomeReadBytes()

                log.error(
                        "Invalid delimiter! Excepted 60 or 0, got {}. This is probably a invalid shared RC4 key. Disconnecting user!",
                        delimiter)

                ctx.disconnect()

                return
            }

            msg.markReaderIndex()

            val messageLength = msg.readInt()

            if (messageLength < 2 || messageLength > 2048) return

            val headerId = msg.readShort().toInt()

            val size = messageLength - 2

            if (msg.readableBytes() < size) {
                msg.resetReaderIndex()

                if (!HabboServer.habboHandler.blacklistIds.contains(headerId)) {
                    log.warn("({}) - HEADER: {}; readable bytes: {}; requested bytes: {}", username, headerId, msg.readableBytes(), size)
                }

                return
            }

            out += HabboRequest(headerId, msg.readSlice(size).retain())

            if (log.isDebugEnabled) {
                out.forEach {
                    if (it is HabboRequest && !HabboServer.habboHandler.blacklistIds.contains(it.headerId)) {
                        log.trace("({}) - GOT  --> [{}][{}] -- {}", username, headerId.toString().padEnd(4),
                                HabboServer.habboHandler.incomingNames[it.headerId]?.padEnd(HabboServer.habboHandler.largestNameSize), it.toString())
                    }
                }
            }
        }
    }
}