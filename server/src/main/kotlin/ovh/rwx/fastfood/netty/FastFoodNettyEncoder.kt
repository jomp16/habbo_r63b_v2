/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.ReferenceCountUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.kotlin.ip

@ChannelHandler.Sharable
@Suppress("unused")
class FastFoodNettyEncoder : MessageToByteEncoder<HabboResponse>() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun encode(ctx: ChannelHandlerContext, msg: HabboResponse, out: ByteBuf) {
        val fastFoodSession: FastFoodSession = ctx.channel().attr(HabboSessionManager.fastFoodAttributeKey).get()
        val username = if (fastFoodSession.authenticated) fastFoodSession.habboSession!!.userInformation.username else fastFoodSession.channel.ip()

        if (log.isDebugEnabled) {
            log.trace("({}) - SENT --> [{}][{}] -- {}", username, msg.headerId.toString().padEnd(2), FFOutgoing.values().firstOrNull { it.headerId == msg.headerId }?.name?.padEnd(HabboServer.fastFoodHandler.largestNameSize), msg.toString())
        }
        val byteBuf = msg.byteBuf

        out.apply {
            writeInt(byteBuf.writerIndex() + 2)
            writeShort(msg.headerId)
            writeBytes(byteBuf)
        }

        if (!msg.keepCopy) msg.close()
        else ReferenceCountUtil.release(byteBuf)
    }
}