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
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.ReferenceCountUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.game.user.HabboSessionManager
import tk.jomp16.habbo.kotlin.ip

@ChannelHandler.Sharable
class HabboNettyEncoder : MessageToByteEncoder<HabboResponse>() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun encode(ctx: ChannelHandlerContext, msg: HabboResponse, out: ByteBuf) {
        val habboSession: HabboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
        val username = try {
            habboSession.userInformation.username
        } catch (exception: UninitializedPropertyAccessException) {
            ctx.channel().ip()
        }

        if (log.isDebugEnabled) log.trace("({}) - SENT --> [{}] -- {}", username, msg.headerId, msg.toString())

        val byteBuf = msg.byteBuf

        byteBuf.let {
            out.writeInt(msg.byteBuf.writerIndex() + 2)
            out.writeShort(msg.headerId)
            out.writeBytes(msg.byteBuf)
        }

        if (!msg.keepCopy) msg.close()
        else ReferenceCountUtil.release(byteBuf)
    }
}