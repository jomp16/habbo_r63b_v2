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

package ovh.rwx.habbo.game.gamedata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalVariablesTest {
    @Test
    fun parsesKeyValueLines() {
        val parsed = ExternalVariables.parse(
            sequenceOf(
                "recycler.number_of_slots=8\r",
                "  habbicons.url = https://swf.habbo.rwx.ovh/habbicons  ",
                "link.format.x=https://a/b?c=d&e=f",
                "empty.value=",
                "# comentario=ignorado",
                "sem separador",
                "=sem chave",
                "",
                "dup=1",
                "dup=2"
            )
        )

        assertEquals("8", parsed["recycler.number_of_slots"])
        assertEquals("https://swf.habbo.rwx.ovh/habbicons", parsed["habbicons.url"])
        // o valor pode conter '='
        assertEquals("https://a/b?c=d&e=f", parsed["link.format.x"])
        assertEquals("", parsed["empty.value"])
        assertNull(parsed["# comentario"])
        assertNull(parsed[""])
        // a última vence, como no override
        assertEquals("2", parsed["dup"])
        assertEquals(5, parsed.size)
    }

    private val complete = mapOf(
        "furnidata.load.url" to "https://swf/gamedata/furnidata_xml/0",
        "external.figurepartlist.txt" to "https://swf/gamedata/figuredata/0",
        "habbicons.hash" to "225e1f8a",
        "habbicons.url" to "https://swf/habbicons",
        "recycler.number_of_slots" to "8",
    )

    @Test
    fun acceptsEveryKeyTheServerReads() {
        assertEquals(emptyList<String>(), ExternalVariables.validate(complete))
        // só o nome antigo dos slots também serve
        val legacy = complete - "recycler.number_of_slots" + ("catalog.recycler.number.of.slots" to "8")
        assertEquals(emptyList<String>(), ExternalVariables.validate(legacy))
    }

    @Test
    fun reportsMissingBlankAndInvalidKeys() {
        assertEquals(ExternalVariables.REQUIREMENTS.size, ExternalVariables.validate(emptyMap()).size)

        val broken = complete + mapOf(
            "furnidata.load.url" to "  ",
            "recycler.number_of_slots" to "0",
        ) - "habbicons.hash"
        val problems = ExternalVariables.validate(broken)

        assertEquals(3, problems.size)
        assertTrue(problems.contains("furnidata.load.url"))
        assertTrue(problems.contains("habbicons.hash"))
        assertTrue(problems.any { it.startsWith("recycler.number_of_slots") })
    }

    @Test
    fun invalidCurrentSlotsFallBackToValidLegacyName() {
        val variables = complete + mapOf(
            "recycler.number_of_slots" to "oito",
            "catalog.recycler.number.of.slots" to "8",
        )
        assertEquals(emptyList<String>(), ExternalVariables.validate(variables))
    }
}
