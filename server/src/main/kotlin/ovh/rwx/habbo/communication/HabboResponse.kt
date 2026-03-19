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

package ovh.rwx.habbo.communication

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.ReferenceCountUtil
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.encryption.decoder.HabboVl64

@Suppress("unused")
class HabboResponse(
    val headerId: Int,
    val outgoing: Outgoing?,
    val keepCopy: Boolean = false,
    val outgoingR63A: OutgoingR63A? = null,
    val r63ANewEncoding: Boolean = false,
) : AutoCloseable {
    private val _byteBuf: ByteBuf = PooledByteBufAllocator.DEFAULT.buffer()
    private val byteBufOutputStream: ByteBufOutputStream = ByteBufOutputStream(_byteBuf)
    private val debugString = StringBuilder()
    val byteBuf: ByteBuf
        get() = if (keepCopy) _byteBuf.duplicate() else _byteBuf

    fun writeUTF(s: String, breakChar: Int = 2) {
        val debugStr = s.replace("\n", "\\n").replace("\r", "\\r")
        debugString.append("{s:\"$debugStr\"}")
        if (outgoingR63A != null && !r63ANewEncoding) {
            byteBufOutputStream.writeBytes(s)
            byteBufOutputStream.writeByte(breakChar)
        } else {
            byteBufOutputStream.writeUTF(s)
        }
    }
    
    fun writeUTFWithoutBreak(s: String) {
        val debugStr = s.replace("\n", "\\n").replace("\r", "\\r")
        debugString.append("{s:\"$debugStr\"}")
        if (outgoingR63A != null && !r63ANewEncoding) {
            byteBufOutputStream.writeBytes(s)
        }
    }

    fun writeShort(i: Int) {
        debugString.append("{sh:\"$i\"}")
        byteBufOutputStream.writeShort(i)
    }

    fun writeInt(i: Int) {
        debugString.append("{i:$i}")
        if (outgoingR63A != null && !r63ANewEncoding) {
            HabboVl64.encodeBytes(i)?.let {
                byteBufOutputStream.write(it)
            }
        } else {
            byteBufOutputStream.writeInt(i)
        }
    }

    fun writeDouble(d: Double) {
        debugString.append("{dl:$d}")
        byteBufOutputStream.writeDouble(d)
    }

    fun writeFloat(d: Float) {
        debugString.append("{f:$d}")
        byteBufOutputStream.writeFloat(d)
    }

    fun writeBoolean(b: Boolean) {
        debugString.append("{b:$b}")
        if (outgoingR63A != null && !r63ANewEncoding) {
            byteBufOutputStream.writeByte((if (b) 73 else 72))
        } else {
            byteBufOutputStream.writeBoolean(b)
        }
    }

    fun writeByte(b: Int) {
        debugString.append("{by:$b}")
        byteBufOutputStream.writeByte(b)
    }

    fun serialize(habboResponseSerialize: IHabboResponseSerialize, vararg params: Any = arrayOf()) {
        if (outgoingR63A != null) {
            habboResponseSerialize.serializeHabboResponseR63A(this, *params)
        } else {
            habboResponseSerialize.serializeHabboResponse(this, *params)
        }
    }

    override fun toString(): String {
        return debugString.toString()
    }

    override fun close() {
        byteBufOutputStream.close()

        _byteBuf.readerIndex(_byteBuf.readableBytes())
        _byteBuf.discardSomeReadBytes()

        ReferenceCountUtil.release(_byteBuf)
    }
}