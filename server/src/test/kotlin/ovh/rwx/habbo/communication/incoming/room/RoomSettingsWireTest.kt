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

package ovh.rwx.habbo.communication.incoming.room

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboPlatform
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.communication.ProtocolEra
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.encryption.decoder.HabboBase64
import ovh.rwx.habbo.encryption.decoder.HabboVl64
import java.time.LocalDateTime

/**
 * O parser temporal do ROOM_SAVESettings por era — a arqueologia de wire vira falha de teste,
 * não crash de cliente. Cada caso replica os bytes exatos que a compilação cliente da era
 * manda (binário moderno pós-Wedgie; B64/VL64 na era Wedgie).
 */
class RoomSettingsWireTest {

    private fun version(year: Int, month: Int, day: Int, platform: HabboPlatform = HabboPlatform.FLASH) =
        HabboVersion(LocalDateTime.of(year, month, day, 12, 0), 63, platform)

    // ---------- builders de bytes modernos (DataInput: int BE, short+len+utf, byte bool) ----------

    private fun modernRequest(build: ByteBuf.() -> Unit): HabboRequest {
        val buf = Unpooled.buffer()
        buf.build()
        return HabboRequest(0, buf)
    }

    private fun ByteBuf.i(v: Int) = writeInt(v)
    private fun ByteBuf.s(v: String) {
        val bytes = v.toByteArray(Charsets.UTF_8)
        writeShort(bytes.size)
        writeBytes(bytes)
    }

    private fun ByteBuf.b(v: Boolean) = writeByte(if (v) 1 else 0)

    // ---------- builder de bytes Wedgie (B64 length + VL64 int + 'A'/'B' bool) ----------

    private fun wedgieRequest(build: ByteBuf.() -> Unit): HabboRequest {
        val buf = Unpooled.buffer()
        buf.build()
        val req = HabboRequest(0, buf)
        req.incomingR63A = IncomingR63A.ROOM_SAVE_SETTINGS
        req.protocolEra = ProtocolEra.WEDGIE_PRE_SHUFFLE
        return req
    }

    private fun ByteBuf.vi(v: Int) {
        writeBytes(HabboVl64.encodeBytes(v))
    }

    private fun ByteBuf.ws(v: String) {
        val bytes = v.toByteArray(Charsets.UTF_8)
        writeBytes(HabboBase64.encode(bytes.size, 2))
        writeBytes(bytes)
    }

    private fun ByteBuf.wb(v: Boolean) = writeByte(if (v) 65 else 66)

    @Test
    fun `AIR 2026-05-18 manda chatFloodSensitivity + 6 flags e extingue os outros campos de chat`() {
        val req = modernRequest {
            s("Sala")                 // name
            s("desc")                 // description
            i(0)                      // state OPEN
            s("")                     // password
            i(50)                     // maxUsers
            i(5)                      // categoryId
            i(1)                      // tag count
            s("tag1")                 // tag
            i(1)                      // tradeState
            b(true)                   // allowPets
            b(false)                  // allowPetsEat
            b(true)                   // allowWalkThrough
            b(true)                   // hideWalls
            i(0)                      // wallThickness
            i(-1)                     // floorThickness
            i(0)                      // whoCanMute
            i(1)                      // whoCanKick
            i(0)                      // whoCanBan
            i(1)                      // chatFloodProtection (sensitivity)
            b(false)                  // leaveOnDoorTileEnabled
            b(true)                   // idleSleepEnabled
            i(120)                    // idleSleepTimeoutSeconds
            b(true)                   // idleAutokickEnabled
            i(600)                    // idleAutokickTimeoutSeconds
            b(false)                  // muteAllPets
        }

        val wire = parseRoomSettingsWire(req, version(2026, 5, 18, HabboPlatform.AIR))

        assertEquals("Sala", wire.name)
        assertEquals("desc", wire.description)
        assertEquals(0, wire.stateValue)
        assertEquals("", wire.password)
        assertEquals(50, wire.maxUsers)
        assertEquals(5, wire.categoryId)
        assertEquals(listOf("tag1"), wire.tags)
        assertEquals(1, wire.tradeState)
        assertTrue(wire.allowPets!!)
        assertFalse(wire.allowPetsEat!!)
        assertTrue(wire.allowWalkThrough!!)
        assertTrue(wire.hideWalls!!)
        assertEquals(0, wire.wallThickness)
        assertEquals(-1, wire.floorThickness)
        assertEquals(0, wire.whoCanMute)
        assertEquals(1, wire.whoCanKick)
        assertEquals(0, wire.whoCanBan)
        // chat moderno: só sensitivity + flags — os outros 4 campos foram extintos
        assertEquals(1, wire.chatFloodProtection)
        assertNull(wire.chatType)
        assertNull(wire.chatBalloon)
        assertNull(wire.chatSpeed)
        assertNull(wire.chatMaxDistance)
        assertNull(wire.allowNavigatorDynamicCats)
        assertFalse(wire.leaveOnDoorTileEnabled!!)
        assertTrue(wire.idleSleepEnabled!!)
        assertEquals(120, wire.idleSleepTimeoutSeconds)
        assertTrue(wire.idleAutokickEnabled!!)
        assertEquals(600, wire.idleAutokickTimeoutSeconds)
        assertFalse(wire.muteAllPets!!)
    }

    @Test
    fun `Flash 2015-12-01 manda 5 ints de chat + dynamic cats`() {
        val req = modernRequest {
            s("n"); s("d"); i(1); s("pw"); i(25); i(3)
            i(0)                      // tag count
            i(1)                      // tradeState int
            b(true); b(true); b(true) // pets trio
            b(false)                  // hideWalls
            i(0); i(0)                // thickness
            i(0); i(0); i(0)          // mute/kick/ban
            i(0); i(1); i(2); i(3); i(4) // chat: type, balloon, speed, maxDistance, flood
            b(true)                   // allowNavigatorDynamicCats
        }

        val wire = parseRoomSettingsWire(req, version(2015, 12, 1))

        assertEquals(0, wire.chatType)
        assertEquals(1, wire.chatBalloon)
        assertEquals(2, wire.chatSpeed)
        assertEquals(3, wire.chatMaxDistance)
        assertEquals(4, wire.chatFloodProtection)
        assertTrue(wire.allowNavigatorDynamicCats!!)
        // flags AIR não existem
        assertNull(wire.leaveOnDoorTileEnabled)
        assertNull(wire.idleSleepEnabled)
        assertNull(wire.muteAllPets)
    }

    @Test
    fun `Flash 2013-06-10 manda só 3 ints de chat — maxDistance e flood ficam ausentes`() {
        val req = modernRequest {
            s("n"); s("d"); i(0); s(""); i(25); i(3); i(0)
            i(2)                      // tradeState
            b(true); b(true); b(true)
            b(true)
            i(0); i(0)
            i(0); i(0); i(0)
            i(7); i(8); i(9)          // chat: type, balloon, speed
        }

        val wire = parseRoomSettingsWire(req, version(2013, 6, 10))

        assertEquals(7, wire.chatType)
        assertEquals(8, wire.chatBalloon)
        assertEquals(9, wire.chatSpeed)
        assertNull(wire.chatMaxDistance)
        assertNull(wire.chatFloodProtection)
        assertNull(wire.allowNavigatorDynamicCats)
    }

    @Test
    fun `Wedgie 2009-11-14 drena os 3 ints do Beta e mapeia allowTrading legado em tradeState`() {
        val req = wedgieRequest {
            ws("Quarto Beta")          // name
            ws("descricao")            // description
            vi(0)                      // state
            ws("senha")                 // password
            vi(25)                     // maxUsers
            vi(1)                      // legacy allowDoorbell (drenado)
            vi(1)                      // legacy allowTrading = true
            vi(0)                      // legacy showOwnerName (drenado)
            vi(4)                      // categoryId
            vi(1)                      // tag count
            ws("beta")                  // tag
            wb(true)                    // allowPets (>=2009-11-13, <2009-11-27: só 1 bool)
        }

        val wire = parseRoomSettingsWire(req, version(2009, 11, 14))

        assertEquals("Quarto Beta", wire.name)
        assertEquals(25, wire.maxUsers)
        assertEquals(4, wire.categoryId)
        assertEquals(listOf("beta"), wire.tags)
        // allowTrading legado true => 2 (todos); o int de tradeState não existe nesta era
        assertEquals(2, wire.tradeState)
        assertTrue(wire.allowPets!!)
        // tudo que nasceu depois é ausente — o applier preserva o estado do quarto
        assertNull(wire.allowPetsEat)
        assertNull(wire.allowWalkThrough)
        assertNull(wire.hideWalls)
        assertNull(wire.wallThickness)
        assertNull(wire.floorThickness)
        assertNull(wire.whoCanMute)
        assertNull(wire.chatType)
    }

    @Test
    fun `tag com vírgula e acima de 30 chars é descartada no parse`() {
        val longTag = "a".repeat(31)
        val req = modernRequest {
            s("n"); s("d"); i(0); s(""); i(25); i(3)
            i(2)                      // tag count
            s("válida,com,vírgula")   // vírgulas removidas
            s(longTag)                 // >30 descartada
            i(0)                      // tradeState
            b(true); b(true); b(true)
            b(true)
            i(0); i(0)
            i(0); i(0); i(0)
            i(0); i(1); i(2)
        }

        val wire = parseRoomSettingsWire(req, version(2013, 6, 10))

        assertEquals(listOf("válidacomvírgula"), wire.tags)
    }
}
