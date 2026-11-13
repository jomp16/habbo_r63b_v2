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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plano de rollback das criações (ordem, sem repetição, idempotente) e a decisão do EarlyBird LTD. */
class CreatedItemsTest {
    private fun item(id: Int) = UserItem(id, 1, "chair", "0", false)

    @Test
    fun `registra na ordem de criacao e o plano contem tudo`() {
        val scope = CreatedItems()
        scope.record(listOf(item(10), item(11), item(12))) // conteúdo + parceiro de teleporte
        scope.record(item(13)) // caixa de presente

        assertEquals(listOf(10, 11, 12, 13), scope.itemIds)
        assertEquals(listOf(10, 11, 12, 13), scope.rollbackPlan())
    }

    @Test
    fun `rollback entrega o plano uma vez e esvazia o coletor`() {
        val scope = CreatedItems()
        scope.record(listOf(item(1), item(2), item(1)))
        val deleted = mutableListOf<List<Int>>()

        scope.rollback { deleted += it }
        scope.rollback { deleted += it }

        assertEquals(listOf(listOf(1, 2)), deleted)
        assertTrue(scope.itemIds.isEmpty())
    }

    @Test
    fun `sem nada criado nao ha delete`() {
        var called = false
        CreatedItems().rollback { called = true }
        assertFalse(called)
    }

    @Test
    fun `early bird depende do numero atribuido e nao do contador`() {
        // série de 100: limiar 10
        assertTrue(isLimitedEarlyBird(listOf(1), 100))
        assertTrue(isLimitedEarlyBird(listOf(10), 100))
        assertFalse(isLimitedEarlyBird(listOf(11), 100))
        // vários números na mesma compra: basta um nos primeiros 10%
        assertTrue(isLimitedEarlyBird(listOf(9, 10, 11), 100))
        assertFalse(isLimitedEarlyBird(listOf(50, 51), 100))
        // oferta comum (sem números) e série pequena (limiar 0) nunca ganham
        assertFalse(isLimitedEarlyBird(emptyList(), 100))
        assertFalse(isLimitedEarlyBird(listOf(1), 9))
    }
}
