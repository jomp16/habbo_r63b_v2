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

package ovh.rwx.habbo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BytePatternsTest {
    private val patterns = listOf("&#25;", "&#28;", "&#29;").map { it.toByteArray() }

    private fun strip(text: String): String = String(BytePatterns.removeAll(text.toByteArray(), patterns))

    @Test
    fun removesEveryPatternAnywhere() {
        assertEquals("<a>xy</a>", strip("<a>x&#25;y&#28;</a>&#29;"))
        assertEquals("", strip("&#25;&#28;&#29;"))
        assertEquals("ab", strip("&#25;ab&#29;"))
    }

    @Test
    fun keepsOtherEntitiesAndPartialMatches() {
        assertEquals("&amp; &#26; &#2 &#", strip("&amp; &#26; &#2 &#"))
        // um prefixo que falha não engole o padrão que começa logo depois
        assertEquals("&&#2", strip("&&#2&#25;"))
        assertEquals("&#2", strip("&#2&#28;"))
    }

    @Test
    fun noMatchReturnsSameArray() {
        val bytes = "<furnidata/>".toByteArray()
        assertSame(bytes, BytePatterns.removeAll(bytes, patterns))
        assertSame(bytes, BytePatterns.removeAll(bytes, emptyList()))
    }

    @Test
    fun removalDoesNotCreateNewMatches() {
        // como as 3 camadas antigas (o &#25; é removido antes do &#29;), não volta para casar sobre o trecho removido
        assertEquals("&#25;", strip("&#2&#29;5;"))
    }
}
