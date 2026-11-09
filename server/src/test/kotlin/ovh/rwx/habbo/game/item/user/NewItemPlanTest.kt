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

package ovh.rwx.habbo.game.item.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewItemPlanTest {
    private fun spec(name: String, extra: String = "0", limited: Boolean = false) = NewItemSpec(name, extra, limited, false)

    private val teleports = setOf("tele_a", "tele_b")
    private fun plan(specs: List<NewItemSpec>, isTeleport: (String) -> Boolean = { it in teleports }): NewItemPlan {
        var next = 100
        return planNewItems(7, specs, isTeleport) { next++ }
    }

    @Test
    fun `comprar 1 teletransporte entrega 2 itens ligados em par`() {
        val plan = plan(listOf(spec("tele_a")))

        assertEquals(2, plan.all.size)
        assertEquals(listOf(100), plan.requested.map { it.id })
        assertEquals(listOf(101), plan.teleportPartners.map { it.id })
        assertEquals(listOf(100 to 101), plan.teleportLinks)
    }

    @Test
    fun `comprar N teletransportes entrega 2N itens e N pares, sem duplicar`() {
        val n = 4
        val plan = plan(List(n) { spec("tele_a") })

        assertEquals(2 * n, plan.all.size)
        assertEquals(n, plan.teleportLinks.size)
        // todos os ids são distintos e cada item está em exatamente um par
        assertEquals(2 * n, plan.all.map { it.id }.toSet().size)
        val idsInLinks = plan.teleportLinks.flatMap { listOf(it.first, it.second) }
        assertEquals(plan.all.map { it.id }.toSet(), idsInLinks.toSet())
        assertEquals(2 * n, idsInLinks.size)
    }

    @Test
    fun `os parceiros vao para o fim e os indices dos pedidos nao se deslocam`() {
        val plan = plan(listOf(spec("chair"), spec("tele_a"), spec("lamp"), spec("tele_b")))

        assertEquals(listOf("chair", "tele_a", "lamp", "tele_b"), plan.requested.map { it.itemName })
        assertEquals(listOf(100, 101, 102, 103), plan.requested.map { it.id })
        assertEquals(listOf("tele_a", "tele_b"), plan.teleportPartners.map { it.itemName })
        assertEquals(listOf(104, 105), plan.teleportPartners.map { it.id })
        assertEquals(plan.requested + plan.teleportPartners, plan.all)
    }

    @Test
    fun `o parceiro herda o dono e o extra data mas nunca e limitado nem builders club`() {
        val plan = plan(listOf(NewItemSpec("tele_a", "abc", limited = true, buildersClub = true)))

        val (item, partner) = plan.all
        assertEquals(7, partner.userId)
        assertEquals("abc", partner.extraData)
        assertTrue(item.limited && item.buildersClub)
        assertTrue(!partner.limited && !partner.buildersClub)
    }

    @Test
    fun `itens que nao sao teletransporte nao ganham parceiro`() {
        val plan = plan(listOf(spec("chair"), spec("lamp")))

        assertEquals(2, plan.all.size)
        assertTrue(plan.teleportPartners.isEmpty())
        assertTrue(plan.teleportLinks.isEmpty())
    }

    @Test
    fun `sem teletransporte reconhecido nenhum par e criado (ignoreSpecialHandling)`() {
        val plan = plan(List(3) { spec("tele_a") }, isTeleport = { false })

        assertEquals(3, plan.all.size)
        assertTrue(plan.teleportLinks.isEmpty())
    }

    @Test
    fun `confirmacao de compra do catalogo recebe apenas os itens pedidos sem parceiro duplicado`() {
        val specs = listOf(spec("tele_a"))
        val plan = plan(specs)

        // No inventário entram todos (2 itens: pedido + parceiro)
        assertEquals(2, plan.all.size)

        // Na confirmação CATALOG_PURCHASE_OK, pegamos apenas os pedidos (specs.size)
        val purchaseOkItems = plan.all.take(specs.size)
        assertEquals(1, purchaseOkItems.size)
        assertEquals("tele_a", purchaseOkItems.first().itemName)
        assertEquals(plan.requested.first().id, purchaseOkItems.first().id)
    }
}
