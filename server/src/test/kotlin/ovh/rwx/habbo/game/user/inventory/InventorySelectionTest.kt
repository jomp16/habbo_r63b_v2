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

package ovh.rwx.habbo.game.user.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventorySelectionTest {
    private val owned = mapOf(1 to "a", 2 to "b", 3 to "c")

    @Test
    fun `tudo certo devolve os itens na ordem do pedido`() {
        assertEquals(listOf("c", "a", "b"), InventorySelection.select(owned, listOf(3, 1, 2)))
    }

    @Test
    fun `id repetido anula`() {
        assertNull(InventorySelection.select(owned, listOf(1, 1, 2)))
    }

    @Test
    fun `id alheio anula mesmo com os outros validos (nunca seleciona parcial)`() {
        assertNull(InventorySelection.select(owned, listOf(1, 2, 99)))
    }

    @Test
    fun `predicado reprovado anula`() {
        assertNull(InventorySelection.select(owned, listOf(1, 2)) { it != "b" })
    }

    @Test
    fun `pedido vazio anula`() {
        assertNull(InventorySelection.select(owned, emptyList()))
    }
}
