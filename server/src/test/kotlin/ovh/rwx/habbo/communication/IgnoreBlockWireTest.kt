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
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerBlockUserUpdateResponse
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerBlockedUsersData
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerBlockedUsersResponse
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerErrorResponse
import ovh.rwx.habbo.communication.outgoing.room.RoomIgnoreUserResultData
import ovh.rwx.habbo.communication.outgoing.room.RoomIgnoreUserResultResponse
import ovh.rwx.habbo.communication.outgoing.room.RoomIgnoredUsersData
import ovh.rwx.habbo.communication.outgoing.room.RoomIgnoredUsersResponse
import ovh.rwx.habbo.encryption.decoder.HabboVl64
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.block.HabboBlocks
import ovh.rwx.habbo.game.user.block.isBlockedEitherWay
import ovh.rwx.habbo.game.user.ignore.HabboIgnores
import ovh.rwx.habbo.game.user.ignore.IgnoreTarget
import ovh.rwx.habbo.game.user.ignore.IgnoreTargetReader
import ovh.rwx.habbo.game.user.ignore.IgnoredUser
import java.time.LocalDateTime

/** Bytes exatos por era dos pacotes de ignorar/bloquear e o parse do alvo do ignorar. */
class IgnoreBlockWireTest {
    @After
    fun tearDown() = unmockkAll()

    private fun version(y: Int, m: Int, d: Int) =
        HabboVersion(LocalDateTime.of(y, m, d, 12, 0), 63, HabboPlatform.FLASH)

    private fun bytes(v: HabboVersion, build: HabboResponse.() -> Unit): ByteArray {
        val response = HabboResponse(0, outgoing = Outgoing.ROOM_IGNORED_USERS, habboVersion = v)
        response.build()
        return drain(response)
    }

    private fun drain(response: HabboResponse): ByteArray {
        val out = ByteArray(response.byteBuf.writerIndex())
        response.byteBuf.getBytes(0, out)
        response.close()
        return out
    }

    private fun ByteBuf.s(v: String) {
        val b = v.toByteArray(Charsets.UTF_8)
        writeShort(b.size)
        writeBytes(b)
    }

    private fun expected(build: ByteBuf.() -> Unit): ByteArray {
        val buf = Unpooled.buffer()
        buf.build()
        val out = ByteArray(buf.writerIndex())
        buf.getBytes(0, out)
        buf.release()
        return out
    }

    // ---------- resultado de ignorar ----------

    @Test
    fun `resultado de ignorar antes de 2012-04-02 manda so o int`() {
        val r = RoomIgnoreUserResultResponse()
        for (v in listOf(version(2011, 10, 5), version(2011, 12, 13))) {
            assertArrayEquals(expected { writeInt(1) }, bytes(v) { r.response(this, RoomIgnoreUserResultData(1, 9, "Bob")) })
        }
    }

    @Test
    fun `resultado de ignorar entre 2012-04-02 e 2025-12-09 manda int mais nome`() {
        val r = RoomIgnoreUserResultResponse()
        for (v in listOf(version(2012, 4, 2), version(2012, 7, 4), version(2013, 5, 22), version(2016, 11, 29),
            version(2023, 11, 20), version(2025, 12, 9))) {
            assertArrayEquals(
                "$v", expected { writeInt(2); s("Bob") },
                bytes(v) { r.response(this, RoomIgnoreUserResultData(2, 9, "Bob")) }
            )
        }
    }

    @Test
    fun `resultado de ignorar a partir de 2025-12-10 manda int mais id`() {
        val r = RoomIgnoreUserResultResponse()
        for (v in listOf(version(2025, 12, 10), version(2026, 10, 27))) {
            assertArrayEquals(expected { writeInt(3); writeInt(9) }, bytes(v) { r.response(this, RoomIgnoreUserResultData(3, 9, "Bob")) })
        }
    }

    @Test
    fun `resultado de ignorar no R63A pre-shuffle manda so o int em VL64`() {
        val v = version(2011, 6, 1)
        val response = HabboResponse(
            0, outgoingR63A = OutgoingR63A.ROOM_IGNORE_USER_RESULT, protocolEra = ProtocolEra.WEDGIE_PRE_SHUFFLE, habboVersion = v
        )
        RoomIgnoreUserResultResponse().response(response, RoomIgnoreUserResultData(1, 9, "Bob"))
        assertArrayEquals(HabboVl64.encodeBytes(1)!!, drain(response))
    }

    // ---------- lista de ignorados ----------

    @Test
    fun `lista de ignorados manda nomes antes de 2025-12-10 e ids depois`() {
        val r = RoomIgnoredUsersResponse()
        val data = RoomIgnoredUsersData(listOf(IgnoredUser(5, "Ana"), IgnoredUser(7, "Bob")))

        assertArrayEquals(
            expected { writeInt(2); s("Ana"); s("Bob") },
            bytes(version(2025, 12, 9)) { r.response(this, data) }
        )
        assertArrayEquals(
            expected { writeInt(2); s("Ana"); s("Bob") },
            bytes(version(2011, 10, 5)) { r.response(this, data) }
        )
        assertArrayEquals(
            expected { writeInt(2); writeInt(5); writeInt(7) },
            bytes(version(2025, 12, 10)) { r.response(this, data) }
        )
    }

    @Test
    fun `lista de ignorados vazia manda so a contagem zero`() {
        val r = RoomIgnoredUsersResponse()
        assertArrayEquals(expected { writeInt(0) }, bytes(version(2012, 4, 2)) { r.response(this, RoomIgnoredUsersData(emptyList())) })
        assertArrayEquals(expected { writeInt(0) }, bytes(version(2026, 1, 6)) { r.response(this, RoomIgnoredUsersData(emptyList())) })
    }

    // ---------- bloquear ----------

    @Test
    fun `lista de bloqueados manda contagem e ids`() {
        val r = MessengerBlockedUsersResponse()
        assertArrayEquals(
            expected { writeInt(3); writeInt(4); writeInt(8); writeInt(15) },
            bytes(version(2026, 1, 6)) { r.response(this, MessengerBlockedUsersData(listOf(4, 8, 15))) }
        )
        assertArrayEquals(expected { writeInt(0) }, bytes(version(2026, 1, 6)) { r.response(this, MessengerBlockedUsersData(emptyList())) })
    }

    @Test
    fun `atualizacao de bloqueio manda resultado e id`() {
        val r = MessengerBlockUserUpdateResponse()
        assertArrayEquals(expected { writeInt(1); writeInt(42) }, bytes(version(2026, 1, 6)) { r.response(this, 1, 42) })
        assertArrayEquals(expected { writeInt(0); writeInt(42) }, bytes(version(2026, 1, 6)) { r.response(this, 0, 42) })
    }

    @Test
    fun `MESSENGER_ERROR manda clientMessageId e codigo 7 ou 8`() {
        val r = MessengerErrorResponse()
        assertArrayEquals(
            expected { writeInt(0); writeInt(7) },
            bytes(version(2026, 1, 6)) { r.response(this, 0, MessengerErrorResponse.MessengerError.BLOCKED_BY_THEM) }
        )
        assertArrayEquals(
            expected { writeInt(0); writeInt(8) },
            bytes(version(2026, 1, 6)) { r.response(this, 0, MessengerErrorResponse.MessengerError.BLOCKED_BY_YOU) }
        )
        assertArrayEquals(
            expected { writeInt(55); writeInt(8) },
            bytes(version(2026, 1, 6)) { r.response(this, 55, MessengerErrorResponse.MessengerError.BLOCKED_BY_YOU) }
        )
    }

    // ---------- parse do alvo de ignorar ----------

    private fun session(v: HabboVersion, ignoredNow: List<IgnoredUser> = emptyList()): HabboSession {
        val ignores = mockk<HabboIgnores>()
        every { ignores.snapshot() } returns ignoredNow
        val session = mockk<HabboSession>()
        every { session.habboVersion } returns v
        every { session.habboIgnores } returns ignores
        return session
    }

    private fun request(build: ByteBuf.() -> Unit): HabboRequest {
        val buf = Unpooled.buffer()
        buf.build()
        return HabboRequest(0, buf)
    }

    // UserInformationDao nao inicializa em teste (le HabboServer.habboConfig), entao so os caminhos que NAO tocam o DAO
    // sao cobertos aqui: nome em branco (retorna antes da consulta) e designorar por nome presente na lista propria.

    @Test
    fun `alvo por nome em 2012-04-02 e 2012-07-04 drena o 2o texto opcional`() {
        for (v in listOf(version(2012, 4, 2), version(2012, 7, 4))) {
            val req = request { s(""); s("texto do balao") }
            assertEquals(IgnoreTarget(0, ""), IgnoreTargetReader.read(session(v), req, ignoring = true))
            assertEquals("2o texto deve ser consumido em $v", 0, req.byteBuf.readableBytes())
        }
    }

    @Test
    fun `alvo por nome fora da janela 2012-04-02 a 2013-05-22 nao le o 2o texto`() {
        for (v in listOf(version(2011, 12, 13), version(2013, 5, 22), version(2016, 11, 29))) {
            val req = request { s(""); s("sobra") }
            IgnoreTargetReader.read(session(v), req, ignoring = true)
            assertEquals("2o texto nao deve ser consumido em $v", 2 + "sobra".length, req.byteBuf.readableBytes())
        }
    }

    @Test
    fun `designorar nunca drena um 2o texto`() {
        val req = request { s(""); s("sobra") }
        IgnoreTargetReader.read(session(version(2012, 4, 2)), req, ignoring = false)
        assertEquals(2 + "sobra".length, req.byteBuf.readableBytes())
    }

    @Test
    fun `designorar por nome usa a lista propria ignorando caixa`() {
        val sess = session(version(2016, 11, 29), listOf(IgnoredUser(8, "Outro"), IgnoredUser(9, "BobNovo")))

        assertEquals(IgnoreTarget(9, "BobNovo"), IgnoreTargetReader.read(sess, request { s("bobnovo") }, ignoring = false))
    }

    @Test
    fun `nome em branco resolve para id 0 mantendo o texto recebido`() {
        assertEquals(IgnoreTarget(0, "  "), IgnoreTargetReader.read(session(version(2016, 11, 29)), request { s("  ") }, true))
        assertEquals(IgnoreTarget(0, ""), IgnoreTargetReader.read(session(version(2016, 11, 29)), request { s("") }, false))
    }

    // ---------- predicados de bloqueio ----------

    private fun blockSession(blocking: Set<Int>, blockedBy: Set<Int>): HabboSession {
        val blocks = mockk<HabboBlocks>()
        every { blocks.isBlocking(any()) } answers { firstArg<Int>() in blocking }
        every { blocks.isBlockedBy(any()) } answers { firstArg<Int>() in blockedBy }
        val session = mockk<HabboSession>()
        every { session.habboBlocks } returns blocks
        return session
    }

    @Test
    fun `isBlockedEitherWay vale nos dois sentidos e so para o par`() {
        val s = blockSession(blocking = setOf(2), blockedBy = setOf(3))
        assertEquals(true, s.isBlockedEitherWay(2))
        assertEquals(true, s.isBlockedEitherWay(3))
        assertEquals(false, s.isBlockedEitherWay(4))
        assertEquals(false, blockSession(emptySet(), emptySet()).isBlockedEitherWay(2))
    }
}
