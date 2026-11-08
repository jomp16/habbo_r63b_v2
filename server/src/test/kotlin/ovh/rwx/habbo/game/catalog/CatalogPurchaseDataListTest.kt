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

package ovh.rwx.habbo.game.catalog

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import java.util.concurrent.atomic.AtomicInteger

/**
 * Regressão: a compra NÃO pode duplicar teletransportes. O parceiro do par nasce em ItemDao.addItems (planNewItems);
 * duplicar aqui entregava 4 itens por unidade comprada (e só 2 apareciam no inventário até o jogador relogar).
 */
class CatalogPurchaseDataListTest {
    private val service = CatalogPurchaseService()

    private fun furnishing(type: InteractionType) = mockk<Furnishing> {
        every { interactionType } returns type
        every { itemName } returns type.name.lowercase()
    }

    private fun offer(furnishing: Furnishing, amount: Int = 1, limitedTotal: Int = 0) = mockk<CatalogItem> {
        every { dealId } returns 0
        every { deal } returns null
        every { this@mockk.furnishing } returns furnishing
        every { this@mockk.amount } returns amount
        every { limited } returns (limitedTotal > 0)
        every { limitedSells } returns AtomicInteger(0)
    }

    @Test
    fun `comprar N teletransportes gera N entradas, nao 2N`() {
        val teleport = furnishing(InteractionType.TELEPORT)
        repeat(5) { index ->
            val purchases = service.buildPurchaseDataList(offer(teleport), amount = index + 1) { "0" }
            assertEquals("quantidade ${index + 1}", index + 1, purchases.size)
        }
    }

    @Test
    fun `a quantidade da oferta multiplica a quantidade comprada`() {
        val purchases = service.buildPurchaseDataList(offer(furnishing(InteractionType.TELEPORT), amount = 3), amount = 2) { "0" }

        assertEquals(6, purchases.size)
    }

    @Test
    fun `mobi comum segue uma entrada por unidade`() {
        val purchases = service.buildPurchaseDataList(offer(furnishing(InteractionType.DEFAULT)), amount = 4) { "x" }

        assertEquals(4, purchases.size)
        assertTrue(purchases.all { it.extraData == "x" })
    }

    @Test
    fun `extra data invalido cancela a compra`() {
        val purchases = service.buildPurchaseDataList(offer(furnishing(InteractionType.DEFAULT)), amount = 2) { null }

        assertTrue(purchases.isEmpty())
    }
}
