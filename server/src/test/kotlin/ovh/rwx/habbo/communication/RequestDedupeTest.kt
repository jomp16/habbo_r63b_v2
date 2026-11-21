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
import io.netty.buffer.Unpooled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Descarte de pacotes idênticos repetidos em rajada ([Handler.dedupeMs]). */
class RequestDedupeTest {
    private val window = 250_000_000L
    private val header = 2312

    private fun payload(roomId: Int, password: String = ""): ByteBuf =
        Unpooled.buffer().writeInt(roomId).writeShort(password.length).writeBytes(password.toByteArray())

    @Test
    fun `repetido idêntico dentro da janela é descartado`() {
        val dedupe = RequestDedupe()
        assertFalse(dedupe.isDuplicate(header, payload(14), window, nowNanos = 0))
        assertTrue(dedupe.isDuplicate(header, payload(14), window, nowNanos = 5_000_000))
        assertTrue(dedupe.isDuplicate(header, payload(14), window, nowNanos = 10_000_000))
    }

    @Test
    fun `payload diferente passa (outro quarto, nova senha)`() {
        val dedupe = RequestDedupe()
        assertFalse(dedupe.isDuplicate(header, payload(14), window, nowNanos = 0))
        assertFalse(dedupe.isDuplicate(header, payload(15), window, nowNanos = 1))
        assertFalse(dedupe.isDuplicate(header, payload(15, "abc"), window, nowNanos = 2))
    }

    @Test
    fun `header diferente com o mesmo payload passa`() {
        val dedupe = RequestDedupe()
        assertFalse(dedupe.isDuplicate(header, payload(14), window, nowNanos = 0))
        assertFalse(dedupe.isDuplicate(2519, payload(14), window, nowNanos = 1))
    }

    @Test
    fun `o instante só avança quando atendido - retry contínuo é respondido a cada janela`() {
        val dedupe = RequestDedupe()
        assertFalse(dedupe.isDuplicate(header, payload(14), window, nowNanos = 0))
        assertTrue(dedupe.isDuplicate(header, payload(14), window, nowNanos = 200_000_000))
        // 250 ms depois do ÚLTIMO ATENDIDO (não do último descartado) já passa
        assertFalse(dedupe.isDuplicate(header, payload(14), window, nowNanos = 250_000_000))
    }

    @Test
    fun `janela zero nunca descarta`() {
        val dedupe = RequestDedupe()
        assertFalse(dedupe.isDuplicate(header, payload(14), 0, nowNanos = 0))
        assertFalse(dedupe.isDuplicate(header, payload(14), 0, nowNanos = 0))
    }

    @Test
    fun `não consome o payload que o handler vai ler`() {
        val dedupe = RequestDedupe()
        val buf = payload(14)
        dedupe.isDuplicate(header, buf, window, nowNanos = 0)
        assertEquals(14, buf.readInt())
    }
}
