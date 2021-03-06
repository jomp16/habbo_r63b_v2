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

package ovh.rwx.habbo.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import ovh.rwx.habbo.game.user.HabboSessionManager

class HabboNettyRC4Decoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, incomingByteBuf: ByteBuf, out: MutableList<Any>) {
        val habboSession = ctx.channel().attr(HabboSessionManager.habboSessionAttributeKey).get()
        var bytes = ByteArray(incomingByteBuf.readableBytes())

        incomingByteBuf.readBytes(bytes)

        if (habboSession.rc4Encryption != null) bytes = habboSession.rc4Encryption!!.decrypt(bytes)

        out += PooledByteBufAllocator.DEFAULT.buffer(bytes.size, bytes.size).writeBytes(bytes)
    }
}