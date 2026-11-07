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

package ovh.rwx.habbo.communication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.database.release.ReleaseHeaderInfo
import ovh.rwx.habbo.database.release.ReleaseType

class HandlerValidationTest {
    private fun header(name: String, override: String?) = ReleaseHeaderInfo(ReleaseType.INCOMING, "R1", name, 10, override)

    @Test
    fun missingHeadersAreReportedPerRelease() {
        val expected = Incoming.entries - Incoming.RELEASE_CHECK
        val complete = mapOf("R1" to expected.mapIndexed { i, e -> i to e })
        assertTrue(HandlerValidation.missingHeaders("incoming", complete, expected).isEmpty())

        val partial = mapOf("R1" to expected.drop(1).mapIndexed { i, e -> i to e }, "R2" to expected.mapIndexed { i, e -> i to e })
        val errors = HandlerValidation.missingHeaders("incoming", partial, expected)
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("release=R1"))
        assertTrue(errors[0].contains(expected.first().name))
    }

    @Test
    fun overridesMustExist() {
        val name = Incoming.entries.first { it != Incoming.RELEASE_CHECK }.name
        val methods = mapOf("handle" to Any())

        val headers = listOf(header(name, "handle"), header(name, null), header(name, "DISABLED"), header(name, "sumiu"), header("NAO_EXISTE", "x"))
        val errors = HandlerValidation.invalidOverrides("incoming", headers, { runCatching { Incoming.valueOf(it) }.getOrNull() }) { methods }

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("sumiu"))
    }
}
