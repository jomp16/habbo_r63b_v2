/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.fastfood.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.communication.incoming.FFIncoming
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.kotlin.ip

@Suppress("unused")
class FastFoodNettyDecoder : ByteToMessageDecoder() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        if (msg.readableBytes() < 6) return

        msg.markReaderIndex()
        val delimiter = msg.readByte()

        msg.resetReaderIndex()

        if (delimiter != 0.toByte() && delimiter != 60.toByte()) {
            msg.readerIndex(msg.readableBytes())
            msg.discardSomeReadBytes()

            log.error("Invalid delimiter! Excepted 60 or 0, got {}.", delimiter)

            ctx.disconnect()

            return
        }

        if (delimiter == 60.toByte()) {
            // flash policy
            msg.readerIndex(msg.readableBytes())
            msg.discardSomeReadBytes()

            ctx.writeAndFlush("""<?xml version="1.0"?>
<!DOCTYPE cross-domain-policy SYSTEM "/xml/dtds/cross-domain-policy.dtd">
<cross-domain-policy>
<allow-access-from domain="*" to-ports="*" />
</cross-domain-policy>""")

            return
        } else {
            msg.markReaderIndex()
            val messageLength = msg.readInt()
            val headerId = msg.readUnsignedShort()
            val size = messageLength - 2

            if (msg.readableBytes() < size) {
                log.warn("Received message length less than excepted message length {} < {}! Waiting for new bytes!", msg.readableBytes(), size)

                msg.resetReaderIndex()

                return
            }

            out += HabboRequest(headerId, msg.readSlice(size).retain())

            if (log.isDebugEnabled) {
                val fastFoodSession: FastFoodSession = ctx.channel().attr(HabboSessionManager.fastFoodAttributeKey).get()
                val username = if (fastFoodSession.authenticated) fastFoodSession.habboSession!!.userInformation.username else fastFoodSession.channel.ip()

                out.forEach {
                    if (it is HabboRequest) {
                        log.trace("({}) - GOT  --> [{}][{}] -- {}", username, headerId.toString().padEnd(2), FFIncoming.values().firstOrNull { ffIncoming -> ffIncoming.headerId == headerId }?.name?.padEnd(HabboServer.fastFoodHandler.largestNameSize), it.toString())
                    }
                }
            }
        }
    }
}