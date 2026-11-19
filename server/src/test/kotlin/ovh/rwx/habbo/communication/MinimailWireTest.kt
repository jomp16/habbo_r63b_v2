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

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.netty.buffer.Unpooled
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.minimail.MinimailNewMessageResponse
import ovh.rwx.habbo.communication.outgoing.minimail.MinimailUnreadCountResponse
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.minimail.MinimailNotifier
import java.time.LocalDateTime

/** Os bytes do aviso do Minimail (MiniMailNewMessageEvent vazio, MiniMailUnreadCountEvent com um int) e quais clients o recebem. */
class MinimailWireTest {
    @After
    fun tearDown() = unmockkAll()

    private fun version(y: Int, m: Int, d: Int, platform: HabboPlatform = HabboPlatform.FLASH) =
        HabboVersion(LocalDateTime.of(y, m, d, 12, 0), 63, platform)

    private fun bytes(v: HabboVersion, outgoing: Outgoing, build: HabboResponse.() -> Unit): ByteArray {
        val response = HabboResponse(0, outgoing = outgoing, habboVersion = v)
        response.build()
        val out = ByteArray(response.byteBuf.writerIndex())
        response.byteBuf.getBytes(0, out)
        response.close()
        return out
    }

    private val eras = listOf(version(2012, 2, 29), version(2012, 7, 4), version(2013, 5, 22), version(2016, 11, 29),
        version(2023, 11, 20), version(2026, 11, 4, HabboPlatform.AIR))

    @Test
    fun `mensagem nova nao tem corpo em nenhuma era`() {
        for (v in eras) assertEquals("$v", 0, bytes(v, Outgoing.MINIMAIL_NEW_MESSAGE) { MinimailNewMessageResponse().response(this) }.size)
    }

    @Test
    fun `nao lidas e um int em todas as eras`() {
        val buf = Unpooled.buffer().writeInt(7)
        val expected = ByteArray(4).also { buf.getBytes(0, it); buf.release() }
        for (v in eras) {
            assertArrayEquals("$v", expected, bytes(v, Outgoing.MINIMAIL_UNREAD_COUNT) { MinimailUnreadCountResponse().response(this, 7) })
        }
    }

    private fun session(v: HabboVersion, preShuffle: Boolean = false): HabboSession {
        val s = mockk<HabboSession>()
        every { s.habboVersion } returns v
        every { s.isPreShuffle } returns preShuffle
        return s
    }

    @Test
    fun `so clients de 2012-02-29 em diante e pos-shuffle recebem o aviso`() {
        assertFalse(MinimailNotifier.supports(session(version(2011, 6, 1), preShuffle = true)))
        assertFalse(MinimailNotifier.supports(session(version(2011, 10, 5))))
        assertFalse(MinimailNotifier.supports(session(version(2011, 12, 13))))
        assertFalse(MinimailNotifier.supports(session(version(2012, 2, 28))))
        for (v in eras) assertTrue("$v", MinimailNotifier.supports(session(v)))
    }
}
