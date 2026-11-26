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

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.encryption.RC4Encryption
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.HabboSessionManager

class DecoderPendingBytesTest {
    @Test
    fun `HabboNettyDecoder drainPendingBytes preserves partial frame across channels`() {
        val ch1 = EmbeddedChannel(HabboNettyDecoder())
        val s1 = HabboSession(ch1)
        ch1.attr(HabboSessionManager.habboSessionAttributeKey).set(s1)

        // Frame: length=6 (2 header + 4 int payload), headerId=1234, payload=0x0000002A (42)
        val full = Unpooled.buffer()
        full.writeInt(6)
        full.writeShort(1234)
        full.writeInt(42)
        val allBytes = ByteArray(full.readableBytes()).also { full.readBytes(it) }
        full.release()

        // Feed only first 5 bytes (incomplete frame)
        ch1.writeInbound(Unpooled.wrappedBuffer(allBytes.copyOfRange(0, 5)))
        assertEquals(0, ch1.inboundMessages().size)

        val decoder1 = ch1.pipeline().get(HabboNettyDecoder::class.java)
        val pending = decoder1.drainPendingBytes()
        assertEquals(5, pending.size)
        assertArrayEquals(allBytes.copyOfRange(0, 5), pending)

        // New channel receives drained bytes + remaining bytes
        val ch2 = EmbeddedChannel(HabboNettyDecoder())
        val s2 = HabboSession(ch2)
        ch2.attr(HabboSessionManager.habboSessionAttributeKey).set(s2)

        ch2.writeInbound(Unpooled.wrappedBuffer(pending))
        ch2.writeInbound(Unpooled.wrappedBuffer(allBytes.copyOfRange(5, allBytes.size)))

        val req = ch2.readInbound<HabboRequest>()
        assertEquals(1234, req.headerId)
        assertEquals(42, req.readInt())
        req.close()
        ch1.finishAndReleaseAll()
        ch2.finishAndReleaseAll()
    }

    @Test
    fun `HabboNettyRC4Decoder drainPendingBytes returns empty when no partial bytes`() {
        val key = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val ch = EmbeddedChannel(HabboNettyRC4Decoder())
        val session = HabboSession(ch).apply { rc4Encryption = RC4Encryption(key) }
        ch.attr(HabboSessionManager.habboSessionAttributeKey).set(session)

        val rc4Decoder = ch.pipeline().get(HabboNettyRC4Decoder::class.java)
        assertTrue(rc4Decoder.drainPendingBytes().isEmpty())
        ch.finishAndReleaseAll()
    }
}
