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

package ovh.rwx.habbo.game.catalog.gift

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.communication.HabboPlatform
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogGiftWrappingConfigurationData
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogGiftWrappingConfigurationResponse
import ovh.rwx.habbo.communication.outgoing.room.GiftOpenedData
import ovh.rwx.habbo.communication.outgoing.room.RoomItemOpenGiftResultResponse
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import java.time.LocalDateTime

/** Parse por era do pedido de presente, layout por era das respostas e as regras puras (giftability, embrulho). */
class GiftWireTest {
    private fun version(y: Int, m: Int, d: Int) =
        HabboVersion(LocalDateTime.of(y, m, d, 12, 0), 63, HabboPlatform.FLASH)

    private fun furnishing(
        name: String,
        type: ItemType = ItemType.FLOOR,
        canGift: Boolean = true,
        interaction: InteractionType = InteractionType.DEFAULT,
    ) = Furnishing(
        itemName = name, spriteId = 77, offerId = 1, type = type, stackHeight = listOf(1.0), canStack = true,
        canSit = false, canLay = false, walkable = false, allowRecycle = true, allowTrade = true,
        allowMarketplaceSell = true, canGift = canGift, allowInventoryStack = true, interactionType = interaction,
        vendingIds = emptyList()
    )

    private fun ByteBuf.s(v: String) {
        val bytes = v.toByteArray(Charsets.UTF_8)
        writeShort(bytes.size)
        writeBytes(bytes)
    }

    private fun giftRequest(withShowName: Boolean, show: Boolean = false): HabboRequest {
        val buf = Unpooled.buffer()
        buf.writeInt(5); buf.writeInt(99); buf.s("extra"); buf.s("Bob"); buf.s("oi")
        buf.writeInt(3000); buf.writeInt(4); buf.writeInt(7)
        if (withShowName) buf.writeByte(if (show) 1 else 0)
        return HabboRequest(0, buf)
    }

    @Test
    fun `20111005 le 8 campos e mostra o remetente`() {
        val req = giftRequest(withShowName = false)
        val parsed = GiftPurchaseRequest.read(req, version(2011, 10, 5))

        assertEquals(GiftPurchaseRequest(5, 99, "extra", "Bob", "oi", 3000, 4, 7, true), parsed)
        assertEquals(0, req.byteBuf.readableBytes())
    }

    @Test
    fun `20111213 em diante le o Boolean showPurchaserName`() {
        val req = giftRequest(withShowName = true, show = false)
        val parsed = GiftPurchaseRequest.read(req, version(2011, 12, 13))

        assertFalse(parsed.showPurchaserName)
        assertEquals(0, req.byteBuf.readableBytes())

        val req2 = giftRequest(withShowName = true, show = true)
        assertTrue(GiftPurchaseRequest.read(req2, version(2026, 5, 18)).showPurchaserName)
    }

    private fun bytes(version: HabboVersion, build: HabboResponse.() -> Unit): ByteArray {
        val response = HabboResponse(0, outgoing = Outgoing.ROOM_ITEM_OPEN_GIFT_RESULT, habboVersion = version)
        response.build()
        val out = ByteArray(response.byteBuf.writerIndex())
        response.byteBuf.getBytes(0, out)
        response.close()
        return out
    }

    private fun expected(version: HabboVersion, build: HabboResponse.() -> Unit) = bytes(version, build)

    @Test
    fun `PresentOpened por era 3 campos, 6 campos e 7 campos`() {
        val data = GiftOpenedData(furnishing("chair"), 1234, true)
        val r = RoomItemOpenGiftResultResponse()

        fun actual(v: HabboVersion) = bytes(v) { r.response(this, data) }

        // antes de 2011-11-08: itemType, classId, productCode
        val v20111005 = version(2011, 10, 5)
        assertEquals(
            expected(v20111005) { writeUTF("s"); writeInt(77); writeUTF("chair") }.toList(),
            actual(v20111005).toList()
        )

        // 2011-11-08 a 2012-01-19: + placedItemId, placedItemType, placedInRoom (sem petFigure)
        val v20111213 = version(2011, 12, 13)
        assertEquals(
            expected(v20111213) {
                writeUTF("s"); writeInt(77); writeUTF("chair"); writeInt(1234); writeUTF("s"); writeBoolean(true)
            }.toList(),
            actual(v20111213).toList()
        )

        // 2012-01-20 em diante: + petFigure ""
        for (v in listOf(version(2012, 1, 20), version(2012, 4, 2), version(2013, 5, 22), version(2026, 5, 18))) {
            assertEquals(
                expected(v) {
                    writeUTF("s"); writeInt(77); writeUTF("chair"); writeInt(1234); writeUTF("s")
                    writeBoolean(true); writeUTF("")
                }.toList(),
                actual(v).toList()
            )
        }
    }

    @Test
    fun `configuracao de embrulho so manda defaultStuffTypes a partir de 2011-12-13`() {
        val data = CatalogGiftWrappingConfigurationData(true, 1, listOf(10, 11), listOf(0, 1), listOf(0, 1, 2), listOf(5))
        val r = CatalogGiftWrappingConfigurationResponse()

        fun actual(v: HabboVersion) = bytes(v) { r.response(this, data) }

        val old = version(2011, 10, 5)
        assertEquals(
            expected(old) {
                writeBoolean(true); writeInt(1)
                writeInt(2); writeInt(10); writeInt(11)
                writeInt(2); writeInt(0); writeInt(1)
                writeInt(3); writeInt(0); writeInt(1); writeInt(2)
            }.toList(),
            actual(old).toList()
        )

        val new = version(2012, 4, 2)
        assertEquals(
            expected(new) {
                writeBoolean(true); writeInt(1)
                writeInt(2); writeInt(10); writeInt(11)
                writeInt(2); writeInt(0); writeInt(1)
                writeInt(3); writeInt(0); writeInt(1); writeInt(2)
                writeInt(1); writeInt(5)
            }.toList(),
            actual(new).toList()
        )
    }

    @Test
    fun `present_wrap ordena numericamente e present_gen exclui o sem numero`() {
        val names = listOf("present_wrap*10", "present_wrap*2", "present_gen", "present_gen3", "present_gen1", "present_wrap*1", "chair")

        assertEquals(
            listOf("present_wrap*1", "present_wrap*2", "present_wrap*10"),
            GiftWrapping.sortedWrapItems(names) { it }
        )
        assertEquals(listOf("present_gen1", "present_gen3"), GiftWrapping.sortedDefaultItems(names) { it })
    }

    @Test
    fun `tipos de caixa e fita filtrados pela data da build`() {
        val box = GiftWrappingKind.BOX
        val types = listOf(
            GiftWrappingType(box, 0, null, null),
            GiftWrappingType(box, 1, null, null),
            GiftWrappingType(box, 8, LocalDateTime.of(2025, 12, 10, 0, 0), null),
            GiftWrappingType(GiftWrappingKind.RIBBON, 0, null, null),
        )

        assertEquals(listOf(0, 1), GiftWrapping.typesFor(types, box, LocalDateTime.of(2012, 4, 2, 0, 0)))
        assertEquals(listOf(0, 1, 8), GiftWrapping.typesFor(types, box, LocalDateTime.of(2026, 1, 1, 0, 0)))
        assertEquals(listOf(0), GiftWrapping.typesFor(types, GiftWrappingKind.RIBBON, LocalDateTime.of(2012, 4, 2, 0, 0)))
    }

    private fun resolve(sprite: Int, box: Int, ribbon: Int, enabled: Boolean = true) = GiftWrapping.resolve(
        sprite, box, ribbon, enabled, price = 1, stuffTypes = listOf(10, 11), defaultStuffTypes = listOf(5, 6),
        boxTypes = listOf(0, 1, 2), ribbonTypes = listOf(0, 1), randomDefault = { it.first() }
    )

    @Test
    fun `embrulho pago exige listas validas e cobra o preco, caixa padrao e gratis`() {
        assertEquals(GiftWrappingChoice(10, 2, 1, 1), resolve(10, 2, 1))
        assertEquals(2001, resolve(10, 2, 1)!!.extra)
        assertNull(resolve(10, 9, 1)) // caixa fora da lista
        assertNull(resolve(10, 1, 9)) // fita fora da lista
        assertNull(resolve(10, 1, 1, enabled = false))
        assertEquals(GiftWrappingChoice(6, 0, 0, 0), resolve(6, 5, 5)) // padrao ignora box/ribbon e e gratis
        assertEquals(GiftWrappingChoice(5, 0, 0, 0), resolve(0, 0, 0)) // sprite 0 (client antigo) sorteia um padrao
        assertNull(resolve(999, 0, 0)) // sprite desconhecido
    }

    @Test
    fun `mensagem apara, remove controle (separador 7) e limita o tamanho`() {
        assertEquals("oi", GiftWrapping.cleanMessage("  oi\u0007 "))
        assertEquals(GiftWrapping.MAX_MESSAGE_LENGTH, GiftWrapping.cleanMessage("a".repeat(500)).length)
    }

    private fun giftable(
        vararg products: Furnishing,
        active: Boolean = true,
        club: Boolean = false,
        points: Int = 0,
        badge: String = "",
        petPage: Boolean = false,
    ) = GiftRules.isGiftable(active, club, points, badge, petPage, products.toList())

    @Test
    fun `giftability so mobi em creditos com allow_gift em todos os produtos`() {
        assertTrue(giftable(furnishing("chair")))
        assertTrue(giftable(furnishing("poster", ItemType.WALL)))
        assertTrue(giftable(furnishing("a"), furnishing("b"))) // pacote
        assertFalse(giftable(furnishing("a"), furnishing("b", canGift = false))) // um produto sem allow_gift
        assertFalse(giftable(furnishing("chair"), points = 5)) // moeda alternativa
        assertFalse(giftable(furnishing("chair"), club = true))
        assertFalse(giftable(furnishing("chair"), active = false))
        assertFalse(giftable(furnishing("chair"), badge = "ACH"))
        assertFalse(giftable(furnishing("chair"), petPage = true))
        assertFalse(giftable(furnishing("fx", ItemType.EFFECT)))
        assertFalse(giftable(furnishing("badge", ItemType.BADGE)))
        assertFalse(giftable(furnishing("bot", ItemType.BOT)))
        assertFalse(giftable(furnishing("pet", interaction = InteractionType.PET)))
        assertFalse(giftable(furnishing("box", interaction = InteractionType.GIFT)))
        assertFalse(giftable())
    }

    @Test
    fun `caixas de presente e do ecotron nao sao trocaveis`() {
        assertTrue(GiftRules.isGiftBox("present_wrap*1", InteractionType.DEFAULT, null))
        assertTrue(GiftRules.isGiftBox("present_gen3", InteractionType.DEFAULT, null))
        assertTrue(GiftRules.isGiftBox("qualquer", InteractionType.GIFT, null))
        assertTrue(GiftRules.isGiftBox("matic_box", InteractionType.DEFAULT, "matic_box"))
        assertFalse(GiftRules.isGiftBox("chair", InteractionType.DEFAULT, "matic_box"))
    }

    @Test
    fun `GiftData da caixa carrega extra, mensagem e remetente conforme showPurchaserName`() {
        val wrapping = GiftWrappingChoice(10, 3, 5, 1)
        val req = GiftPurchaseRequest(1, 2, "", "Bob", " oi ", 10, 3, 5, showPurchaserName = false)
        val data = CatalogGiftService.buildGiftData(wrapping, req, furnishing("chair"), "Ana", "fig")

        assertEquals(3005, data.openState)
        assertEquals("oi", data.message)
        assertEquals("chair", data.productCode)
        assertEquals("", data.purchaserName)
        assertFalse(data.showPurchaser)

        val shown = CatalogGiftService.buildGiftData(wrapping, req.copy(showPurchaserName = true), furnishing("chair"), "Ana", "fig")
        assertTrue(shown.showPurchaser)
        assertEquals("Ana", shown.purchaserName)
        assertEquals(3005, shown.toStuffData().roomExtra)
    }
}
