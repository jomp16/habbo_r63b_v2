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

package ovh.rwx.habbo.game.item.wired

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ovh.rwx.habbo.communication.HabboPlatform
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.communication.ProtocolEra
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.encryption.decoder.HabboBase64
import ovh.rwx.habbo.encryption.decoder.HabboVl64
import ovh.rwx.habbo.game.item.WiredKind
import java.time.LocalDateTime

/**
 * Matriz de eras do wire do diálogo Wired (save incoming + dialog outgoing).
 * A arqueologia de wire vira falha de build, não crash de cliente — cada caso
 * replica os bytes exatos que a compilação cliente da era manda (binário moderno
 * pós-Wedgie; B64/VL64 na era Wedgie).
 */
class WiredDialogWireTest {

    private fun version(year: Int, month: Int, day: Int, platform: HabboPlatform = HabboPlatform.FLASH) =
        HabboVersion(LocalDateTime.of(year, month, day, 12, 0), 63, platform)

    // ---------- builders de bytes modernos (DataInput: int BE, short+len+utf, byte bool, long BE) ----------

    private fun modernRequest(build: ByteBuf.() -> Unit): HabboRequest {
        val buf = Unpooled.buffer()
        buf.build()
        return HabboRequest(0, buf)
    }

    private fun ByteBuf.i(v: Int) = writeInt(v)
    private fun ByteBuf.l(v: Long) = writeLong(v)
    private fun ByteBuf.s(v: String) {
        val bytes = v.toByteArray(Charsets.UTF_8)
        writeShort(bytes.size)
        writeBytes(bytes)
    }

    private fun ByteBuf.b(v: Boolean) = writeByte(if (v) 1 else 0)

    // ---------- builder de bytes Wedgie (B64 length + VL64 int) ----------

    private fun wedgieRequest(build: ByteBuf.() -> Unit): HabboRequest {
        val buf = Unpooled.buffer()
        buf.build()
        val req = HabboRequest(0, buf)
        req.incomingR63A = IncomingR63A.WIRED_SAVE_EFFECT
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

    // ---------- write side (outgoing) ----------

    /** Espera os bytes escritos por um HabboResponse moderno (não-Wedgie). */
    private fun modernResponseBytes(version: HabboVersion, build: HabboResponse.() -> Unit): ByteArray {
        val response = HabboResponse(0, outgoing = Outgoing.WIRED_EFFECT_DIALOG, habboVersion = version)
        response.build()
        val bytes = ByteArray(response.byteBuf.writerIndex())
        response.byteBuf.getBytes(0, bytes)
        response.close()
        return bytes
    }

    // ---------- PARSE: save incoming ----------

    @Test
    fun `Flash classico 2012 EFFECT manda delay + selectionCode e nao manda fontes de usuario`() {
        val req = modernRequest {
            i(2)          // intParams count
            i(1)          // option 1
            i(2)          // option 2
            s("msg")      // stringParam
            i(2)          // stuffIds count
            i(100)        // stuffId
            i(101)        // stuffId
            i(5)          // delay (EFFECT)
            i(0)          // stuffTypeSelectionCode (0 = selected items)
        }

        val wire = parseWiredSaveWire(req, version(2012, 6, 1), WiredKind.EFFECT)

        assertEquals(listOf(1, 2), wire.options)
        assertEquals("msg", wire.message)
        assertEquals(listOf(100, 101), wire.stuffIds)
        assertEquals(5, wire.delay)
        assertEquals(listOf(WiredFurniSource.SELECTED_ITEMS), wire.furniSources)
        assertNull(wire.userSources)
        assertNull(wire.variableIds)
        assertNull(wire.stuffIds2)
        assertNull(wire.quantifier)
        assertNull(wire.filter)
    }

    @Test
    fun `Flash classico 2012 TRIGGER nao drena delay (so EFFECT le)`() {
        val req = modernRequest {
            i(0)          // intParams
            s("kw")       // stringParam
            i(0)          // stuffIds
            i(1)          // stuffTypeSelectionCode (1 = triggering furni)
        }

        val wire = parseWiredSaveWire(req, version(2012, 6, 1), WiredKind.TRIGGER)

        assertNull(wire.delay)
        assertEquals(listOf(WiredFurniSource.TRIGGERING_ITEM), wire.furniSources)
    }

    @Test
    fun `AIR 2023 EFFECT manda furni e user sources mas nao variableIds nem stuffIds2`() {
        val req = modernRequest {
            i(0)          // intParams
            s("msg")      // stringParam
            i(0)          // stuffIds
            i(3)          // delay (EFFECT)
            i(1)          // furniSources count
            i(100)        // SELECTED_ITEMS (códigos AS3: 0=triggering, 100=selected, 200=selector)
            i(2)          // userSources count
            i(0)          // TRIGGERING_USER
            i(200)        // SELECTOR_USERS
        }

        val wire = parseWiredSaveWire(req, version(2023, 6, 1, HabboPlatform.AIR), WiredKind.EFFECT)

        assertEquals(3, wire.delay)
        assertEquals(listOf(WiredFurniSource.SELECTED_ITEMS), wire.furniSources)
        assertEquals(
            listOf(WiredUserSource.TRIGGERING_USER, WiredUserSource.SELECTOR_USERS),
            wire.userSources
        )
        assertNull(wire.variableIds)
        assertNull(wire.stuffIds2)
    }

    @Test
    fun `AIR 2024-07 manda variableIds como Long`() {
        val req = modernRequest {
            i(0)          // intParams
            s("")         // stringParam
            i(0)          // stuffIds
            i(0)          // delay (EFFECT)
            i(0)          // furniSources
            i(0)          // userSources
            i(2)          // variableIds count
            l(42L)        // variableId Long
            l(43L)        // variableId Long
        }

        val wire = parseWiredSaveWire(req, version(2024, 7, 9, HabboPlatform.AIR), WiredKind.EFFECT)

        assertEquals(listOf("42", "43"), wire.variableIds)
    }

    @Test
    fun `AIR 2025-01 manda variableIds como UTF`() {
        val req = modernRequest {
            i(0)          // intParams
            s("")         // stringParam
            i(0)          // stuffIds
            i(0)          // delay (EFFECT)
            i(0)          // furniSources
            i(0)          // userSources
            i(1)          // variableIds count
            s("var:a")    // variableId UTF
        }

        val wire = parseWiredSaveWire(req, version(2025, 1, 10, HabboPlatform.AIR), WiredKind.EFFECT)

        assertEquals(listOf("var:a"), wire.variableIds)
    }

    @Test
    fun `AIR 2026 SELECTOR drena filter + inverse e stuffIds2`() {
        val req = modernRequest {
            i(0)          // intParams
            s("")         // stringParam
            i(0)          // stuffIds
            b(true)       // filter (SELECTOR)
            b(false)      // inverse (SELECTOR)
            i(0)          // furniSources
            i(0)          // userSources
            i(0)          // variableIds
            i(2)          // stuffIds2 count
            i(200)        // stuffId2
            i(201)        // stuffId2
        }

        val wire = parseWiredSaveWire(req, version(2026, 5, 18, HabboPlatform.AIR), WiredKind.SELECTOR)

        assertEquals(true, wire.filter)
        assertEquals(false, wire.inverse)
        assertEquals(listOf(200, 201), wire.stuffIds2)
    }

    @Test
    fun `Wedgie 2011 EFFECT parseia B64+VL64`() {
        val req = wedgieRequest {
            vi(1)         // intParams count
            vi(7)         // option
            ws("msg")     // stringParam
            vi(1)         // stuffIds count
            vi(100)       // stuffId
            vi(5)         // delay (EFFECT)
            vi(0)         // stuffTypeSelectionCode
        }

        val wire = parseWiredSaveWire(req, version(2011, 6, 1), WiredKind.EFFECT)

        assertEquals(listOf(7), wire.options)
        assertEquals("msg", wire.message)
        assertEquals(listOf(100), wire.stuffIds)
        assertEquals(5, wire.delay)
        assertEquals(listOf(WiredFurniSource.SELECTED_ITEMS), wire.furniSources)
    }

    // ---------- WRITE: dialog outgoing ----------

    private fun dialogData(
        kind: WiredKind?,
        message: String = "msg",
        options: List<Int> = listOf(1),
        items: List<Int> = listOf(100, 101),
    ) = WiredDialogWireData(
        kind = kind,
        spriteId = 1234,
        roomItemId = 77,
        requiresItems = true,
        stuffTypeSelectionEnabled = true,
        stuffIds = items,
        stuffIds2 = listOf(200),
        message = message,
        options = options,
        code = 42,
        delay = 5,
        filter = true,
        inverse = false,
        selectionCode = 1,
        variableIds = listOf("42"),
        furniSourceCodes = listOf(100),
        userSourceCodes = listOf(0, 200),
        furniSourceGroups = listOf(listOf(WiredFurniSource.SELECTED_ITEMS, WiredFurniSource.SELECTOR_ITEMS)),
        userSourceGroups = emptyList(),
        defaultFurniSourceGroups = listOf(WiredFurniSource.SELECTED_ITEMS),
        defaultUserSourceGroups = listOf(WiredUserSource.TRIGGERING_USER),
        wiredContextBlocks = emptyList(),
    )

    @Test
    fun `dialog Flash 2012 EFFECT escreve selectionCode boolean, selectionCode e blockedActions`() {
        val bytes = modernResponseBytes(version(2012, 6, 1)) {
            writeWiredDialogWire(this, version(2012, 6, 1), dialogData(WiredKind.EFFECT))
        }

        val expected = Unpooled.buffer().apply {
            writeBoolean(true)      // stuffTypeSelectionEnabled (2011-02-22 ..< 2023-04-14)
            writeInt(20)           // selectable items
            writeInt(2)            // stuffIds count
            writeInt(100)
            writeInt(101)
            writeInt(1234)         // spriteId
            writeInt(77)           // roomItemId
            // settings
            val msg = "msg".toByteArray(Charsets.UTF_8)
            writeShort(msg.size); writeBytes(msg)
            writeInt(1)            // settingsCount = options.size
            writeInt(1)            // option
            writeInt(1)            // selectionCode (Flash clássico)
            writeInt(42)           // code
            writeInt(5)            // delay (EFFECT)
            writeInt(0)            // blockedActions (legacy conflictingActions)
        }

        assertArrayEquals(byteArray(expected), bytes)
    }

    @Test
    fun `dialog AIR 2023 EFFECT escreve fontes e sem variableIds`() {
        val v = version(2023, 6, 1, HabboPlatform.AIR)
        val bytes = modernResponseBytes(v) {
            writeWiredDialogWire(this, v, dialogData(WiredKind.EFFECT))
        }

        val expected = Unpooled.buffer().apply {
            writeInt(20); writeInt(2); writeInt(100); writeInt(101)  // items (sem stuffIds2 em 2023)
            writeInt(1234); writeInt(77)                             // item info
            val msg = "msg".toByteArray(Charsets.UTF_8)
            writeShort(msg.size); writeBytes(msg)
            writeInt(1); writeInt(1)                                 // settings (sem selectionCode no AIR)
            // sem variableIds (nasce 2024-05-06)
            writeInt(1); writeInt(100)                                 // furniSourceCodes
            writeInt(2); writeInt(0); writeInt(200)                    // userSourceCodes
            writeInt(42)                                             // code
            writeInt(5)                                              // delay (EFFECT)
            writeBoolean(true)                                       // hasAdvancedMode (grupo de 2)
            writeInt(1); writeInt(2); writeInt(100); writeInt(200)   // _7491 furni groups
            writeInt(0)                                              // _7492 user groups
            writeInt(1); writeInt(100)                                 // _7493 default furni (SELECTED_ITEMS)
            writeInt(1); writeInt(0)                                  // _7494 default user (TRIGGERING_USER)
            writeBoolean(false)                                       // allowWallFurni
            // sem wiredContext (2023 < 2024-05-06), sem defaultIntParams (2023 < 2025-05-06)
        }

        assertArrayEquals(byteArray(expected), bytes)
    }

    @Test
    fun `dialog AIR 2026-05 CONDITION escreve variableIds UTF, context e defaultIntParams`() {
        val v = version(2026, 5, 18, HabboPlatform.AIR)
        val bytes = modernResponseBytes(v) {
            writeWiredDialogWire(this, v, dialogData(WiredKind.CONDITION))
        }

        val expected = Unpooled.buffer().apply {
            writeInt(20)                                            // selectable items
            writeInt(2); writeInt(100); writeInt(101)               // stuffIds
            writeInt(1); writeInt(200)                              // stuffIds2 (≥ 2025-05-06)
            writeInt(1234); writeInt(77)                            // item info
            val msg = "msg".toByteArray(Charsets.UTF_8)
            writeShort(msg.size); writeBytes(msg)
            writeInt(1); writeInt(1)                                 // settings
            writeInt(1)                                             // variableIds count
            val varId = "42".toByteArray(Charsets.UTF_8)
            writeShort(varId.size); writeBytes(varId)                // UTF (≥ 2024-12-12)
            writeInt(1); writeInt(100)                                 // furniSourceCodes
            writeInt(2); writeInt(0); writeInt(200)                    // userSourceCodes
            writeInt(42)                                             // code
            writeInt(0)                                             // quantifierCode (CONDITION)
            writeBoolean(true)                                       // hasAdvancedMode
            writeInt(1); writeInt(2); writeInt(100); writeInt(200)   // _7491
            writeInt(0)                                              // _7492
            writeInt(1); writeInt(100)                                 // _7493
            writeInt(1); writeInt(0)                                  // _7494
            writeBoolean(false)                                       // allowWallFurni
            writeByte(0)                                             // quantifierType (hasWiredContext)
            writeBoolean(false)                                       // isInvert
            writeInt(0)                                              // wiredContext blocks (0 blocos)
            writeInt(0)                                              // defaultIntParams (≥ 2025-05-06)
        }

        assertArrayEquals(byteArray(expected), bytes)
    }

    @Test
    fun `dialog AIR 2024-07 escreve variableIds como Long`() {
        val v = version(2024, 7, 9, HabboPlatform.AIR)
        val bytes = modernResponseBytes(v) {
            writeWiredDialogWire(this, v, dialogData(WiredKind.CONDITION))
        }

        // variableIds Long: 4 bytes count + 8 bytes por id — basta verificar o tamanho e o trecho
        val buf = Unpooled.wrappedBuffer(bytes)
        // pula até depois de settings (layout idêntico ao caso 2026 até variableIds):
        // items(2 ints head) + 2 ids + item info + msg + settings
        var idx = 4 + 4 + 4 + 4   // 20, count, 100, 101
        idx += 4 + 4              // spriteId, roomItemId
        val msgLen = buf.getShort(idx)
        idx += 2 + msgLen + 4 + 4  // msg + settingsCount + 1 option
        // variableIds
        assertEquals(1, buf.getInt(idx))
        assertEquals(42L, buf.getLong(idx + 4))
        buf.release()
    }

    @Test
    fun `dialog Wedgie 2011 escreve VL64 e boolean I-H`() {
        val v = version(2011, 6, 1)
        val response = HabboResponse(
            0,
            outgoingR63A = OutgoingR63A.WIRED_EFFECT_DIALOG,
            protocolEra = ProtocolEra.WEDGIE_PRE_SHUFFLE,
            habboVersion = v
        )
        writeWiredDialogWire(response, v, dialogData(WiredKind.EFFECT))
        val bytes = ByteArray(response.byteBuf.writerIndex())
        response.byteBuf.getBytes(0, bytes)
        response.close()

        val expected = Unpooled.buffer().apply {
            writeByte(73)                           // stuffTypeSelectionEnabled true = 'I' (Wedgie)
            writeBytes(HabboVl64.encodeBytes(20))    // selectable items
            writeBytes(HabboVl64.encodeBytes(2))     // stuffIds count
            writeBytes(HabboVl64.encodeBytes(100))
            writeBytes(HabboVl64.encodeBytes(101))
            writeBytes(HabboVl64.encodeBytes(1234))  // spriteId
            writeBytes(HabboVl64.encodeBytes(77))    // roomItemId
            // writeUTF Wedgie: bytes UTF-8 + terminador 2
            writeBytes("msg".toByteArray(Charsets.UTF_8)); writeByte(2)
            writeBytes(HabboVl64.encodeBytes(1))     // settingsCount
            writeBytes(HabboVl64.encodeBytes(1))     // option
            writeBytes(HabboVl64.encodeBytes(1))     // selectionCode (Flash clássico)
            writeBytes(HabboVl64.encodeBytes(42))    // code
            writeBytes(HabboVl64.encodeBytes(5))     // delay
            writeBytes(HabboVl64.encodeBytes(0))     // blockedActions
        }

        assertArrayEquals(byteArray(expected), bytes)
    }

    private fun byteArray(buf: ByteBuf): ByteArray {
        val bytes = ByteArray(buf.writerIndex())
        buf.getBytes(0, bytes)
        buf.release()
        return bytes
    }
}
