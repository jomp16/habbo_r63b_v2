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

package ovh.rwx.habbo.game.achievement

import org.junit.Assert.assertEquals
import org.junit.Test

class CreditFurniValueTest {
    @Test
    fun `moeda comum e de catalogo valem o numero do nome`() {
        assertEquals(100, CreditFurniValue.of("CF_100"))
        assertEquals(5, CreditFurniValue.of("CFC_5"))
    }

    @Test
    fun `diamante usa o terceiro trecho`() {
        assertEquals(10, CreditFurniValue.of("CF_diamond_10"))
    }

    @Test
    fun `item que nao e moeda ou nome malformado vale zero`() {
        assertEquals(0, CreditFurniValue.of("chair_basic"))
        assertEquals(0, CreditFurniValue.of("CF_"))
        assertEquals(0, CreditFurniValue.of("CF_abc"))
        assertEquals(0, CreditFurniValue.of("CF"))
    }
}
