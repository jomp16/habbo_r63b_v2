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

package ovh.rwx.habbo.admin.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ovh.rwx.habbo.config.DebugToken
import ovh.rwx.habbo.config.DebugToolsConfig
import ovh.rwx.habbo.database.debug.DebugDao

class DebugToolsTest {
    private val config = DebugToolsConfig(readMinRank = 5, writeMinRank = 7)
    private val tool = DebugTool(
        "t", "teste", DebugAccess.WRITE,
        listOf(
            DebugParam("room_id", DebugParamType.INT, "", required = true),
            DebugParam("force", DebugParamType.BOOL, ""),
            DebugParam("sql", DebugParamType.STRING, ""),
        )
    ) { it.requireInt("room_id") }

    private fun badParam(raw: Map<String, Any?>) {
        try {
            DebugTools.coerceArgs(tool, raw)
            fail("devia recusar $raw")
        } catch (_: DebugToolException) {
        }
    }

    @Test
    fun coercesJsonAndConsoleValues() {
        val a = DebugTools.coerceArgs(tool, mapOf("room_id" to "12", "force" to "true", "sql" to 5))
        assertEquals(12, a.int("room_id"))
        assertTrue(a.bool("force"))
        assertEquals("5", a.string("sql"))

        val b = DebugTools.coerceArgs(tool, mapOf("room_id" to 12L, "force" to false))
        assertEquals(12, b.int("room_id"))
        assertFalse(b.bool("force", default = true))
        assertEquals(3, DebugTools.coerceArgs(tool, mapOf("room_id" to 3.0)).int("room_id"))
    }

    @Test
    fun rejectsInvalidArgs() {
        badParam(emptyMap())
        badParam(mapOf("room_id" to "abc"))
        badParam(mapOf("room_id" to 1.5))
        badParam(mapOf("room_id" to 1L + Int.MAX_VALUE))
        badParam(mapOf("room_id" to 1, "force" to "talvez"))
        badParam(mapOf("room_id" to 1, "outro" to 1))
        badParam(mapOf("room_id" to 1, "sql" to "x".repeat(DebugTools.MAX_STRING_LENGTH + 1)))
    }

    @Test
    fun accessLevels() {
        val read = DebugTool("r", "", DebugAccess.READ) { null }
        val eval = DebugTool("e", "", DebugAccess.EVAL) { null }
        assertNull(DebugTools.denyReason(read, 5, config))
        assertNotNull(DebugTools.denyReason(read, 4, config))
        assertNotNull(DebugTools.denyReason(tool, 6, config))
        assertNull(DebugTools.denyReason(tool, 7, config))
        // eval: desligado nega até para rank máximo
        assertNotNull(DebugTools.denyReason(eval, Int.MAX_VALUE, config))
        assertNull(DebugTools.denyReason(eval, 7, config.copy(evalEnabled = true)))
        assertNotNull(DebugTools.denyReason(eval, 6, config.copy(evalEnabled = true)))
    }

    @Test
    fun runChecksRankBeforeArgs() {
        val caller = DebugCaller(1, "x", 1, "test")
        assertEquals(DebugOutcome.Unknown, DebugTools.run("nao_existe", emptyMap(), caller, config))
        assertTrue(DebugTools.run("room_inspect", emptyMap(), caller, config) is DebugOutcome.Forbidden)
        assertTrue(DebugTools.run("room_inspect", emptyMap(), caller.copy(rank = 7), config) is DebugOutcome.BadParam)
        assertTrue(DebugTools.run("eval", mapOf("script" to "1"), caller.copy(rank = 99), config) is DebugOutcome.Forbidden)
    }

    @Test
    fun catalogIsConsistent() {
        val names = DebugTools.tools.map { it.name }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.containsAll(listOf("server_status", "room_inspect", "user_inspect", "room_purge_ghosts", "room_force_unload", "writebehind_status", "db_query", "eval")))
        DebugTools.tools.forEach { t -> assertTrue(t.name, t.name.matches(Regex("[a-z_]+"))) }
        assertEquals(DebugAccess.EVAL, DebugTools.find("eval")!!.access)
    }

    @Test
    fun tokensMatchOnlyTheirHash() {
        val token = "hdbg_" + "ab".repeat(32)
        val cfg = config.copy(tokens = listOf(DebugToken("claude", 42, DebugTools.sha256Hex(token).uppercase())))
        assertEquals(42, DebugTools.findToken(token, cfg)?.userId)
        assertNull(DebugTools.findToken(token + "x", cfg))
        assertNull(DebugTools.findToken("", cfg))
        assertNull(DebugTools.findToken(DebugTools.sha256Hex(token), cfg))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", DebugTools.sha256Hex(""))
    }

    @Test
    fun consoleArgsSplitOnlyOnKnownParams() {
        val q = DebugTools.find("db_query")!!
        assertEquals(
            mapOf("sql" to "SELECT * FROM rooms WHERE id=5 AND owner_id=1", "max_rows" to "3"),
            DebugConsole.parseArgs(q, "sql=SELECT * FROM rooms WHERE id=5 AND owner_id=1 max_rows=3")
        )
        // sem param=, vai para o único obrigatório
        assertEquals(mapOf("room_id" to "123"), DebugConsole.parseArgs(DebugTools.find("room_inspect")!!, "123"))
        assertEquals(emptyMap<String, String>(), DebugConsole.parseArgs(DebugTools.find("server_status")!!, ""))
    }

    @Test
    fun dbQueryOnlyAllowsSingleReads() {
        assertNull(DebugDao.rejectReason("SELECT * FROM rooms WHERE id = 1"))
        assertNull(DebugDao.rejectReason("  show create table rooms; "))
        assertNull(DebugDao.rejectReason("WITH x AS (SELECT 1) SELECT * FROM x"))
        assertNotNull(DebugDao.rejectReason(""))
        assertNotNull(DebugDao.rejectReason("DELETE FROM rooms"))
        assertNotNull(DebugDao.rejectReason("DROP TABLE rooms"))
        assertNotNull(DebugDao.rejectReason("SELECT 1; DROP TABLE rooms"))
        assertNotNull(DebugDao.rejectReason("SELECT * FROM users INTO OUTFILE '/tmp/x'"))
        assertNotNull(DebugDao.rejectReason("SELECT SLEEP(100)"))
        assertNotNull(DebugDao.rejectReason("selectx 1"))
    }
}
