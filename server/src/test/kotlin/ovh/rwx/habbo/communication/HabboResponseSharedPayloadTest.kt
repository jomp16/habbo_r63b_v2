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
import io.netty.channel.embedded.EmbeddedChannel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import ovh.rwx.habbo.encryption.RC4Encryption
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.HabboSessionManager
import ovh.rwx.habbo.netty.HabboNettyEncoder
import java.time.LocalDateTime

/**
 * Resposta de broadcast com payload compartilhado: tem de gerar EXATAMENTE os mesmos bytes no fio que uma resposta
 * normal com o mesmo corpo (com e sem o RC4 de saída), sem alocar buffer próprio.
 */
class HabboResponseSharedPayloadTest {
    private val version = HabboVersion(LocalDateTime.now(), 63)

    private fun normal(headerId: Int): HabboResponse = HabboResponse(headerId, habboVersion = version).apply {
        writeInt(123456)
        writeUTF("olá, mundo")
        writeBoolean(true)
        writeInt(-7)
    }

    private fun payloadOf(response: HabboResponse): ByteArray {
        val buf = response.byteBuf
        return ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
    }

    private fun session(rc4Key: ByteArray?): Pair<EmbeddedChannel, HabboSession> {
        val channel = EmbeddedChannel(HabboNettyEncoder())
        val session = HabboSession(channel)
        channel.attr(HabboSessionManager.habboSessionAttributeKey).set(session)
        if (rc4Key != null) session.rc4EncryptionOut = RC4Encryption(rc4Key)
        return channel to session
    }

    private fun bytesOf(buf: ByteBuf): ByteArray = ByteArray(buf.readableBytes()).also { buf.readBytes(it); buf.release() }

    @Test
    fun `payloadLength e writePayloadTo refletem o payload compartilhado`() {
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val shared = HabboResponse.shared(7, payload, habboVersion = version)

        assertEquals(5, shared.payloadLength)

        val out = Unpooled.buffer()
        shared.writePayloadTo(out)
        assertArrayEquals(payload, bytesOf(out))
    }

    @Test
    fun `close e idempotente e nao libera nada compartilhado`() {
        val shared = HabboResponse.shared(7, byteArrayOf(9, 9), habboVersion = version)

        shared.close()
        shared.close()

        assertEquals(2, shared.payloadLength)
    }

    @Test
    fun `escrever numa resposta compartilhada falha rapido`() {
        val shared = HabboResponse.shared(7, byteArrayOf(1), habboVersion = version)

        assertThrows(Exception::class.java) { shared.writeInt(1) }
    }

    @Test
    fun `no fio sem RC4 a resposta compartilhada e identica a normal`() {
        val (normalChannel, _) = session(null)
        val (sharedChannel, _) = session(null)

        val reference = normal(321)
        val payload = payloadOf(reference)
        reference.close()

        normalChannel.writeOutbound(normal(321))
        sharedChannel.writeOutbound(HabboResponse.shared(321, payload, habboVersion = version))

        assertArrayEquals(bytesOf(normalChannel.readOutbound()), bytesOf(sharedChannel.readOutbound()))
    }

    @Test
    fun `no fio com RC4 de saida a resposta compartilhada e identica a normal em varios pacotes seguidos`() {
        val key = ByteArray(16) { (it * 7 + 3).toByte() }
        val (normalChannel, _) = session(key)
        val (sharedChannel, _) = session(key)

        repeat(30) { i ->
            val reference = normal(100 + i)
            val payload = payloadOf(reference)
            reference.close()

            normalChannel.writeOutbound(normal(100 + i))
            sharedChannel.writeOutbound(HabboResponse.shared(100 + i, payload, habboVersion = version))

            // o keystream do RC4 é contínuo: se um pacote divergisse, TODOS os seguintes divergiriam
            assertArrayEquals("pacote $i", bytesOf(normalChannel.readOutbound()), bytesOf(sharedChannel.readOutbound()))
        }
    }
}
