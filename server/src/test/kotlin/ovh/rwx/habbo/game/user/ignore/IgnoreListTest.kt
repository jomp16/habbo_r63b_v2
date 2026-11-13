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

package ovh.rwx.habbo.game.user.ignore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IgnoreListTest {
    @Test
    fun addsAndRemoves() {
        val list = IgnoreList()

        assertEquals(IgnoreResult.ADDED, list.add(1, 2, "bob").result)
        assertTrue(list.isIgnoring(2))
        assertEquals(listOf(IgnoredUser(2, "bob")), list.snapshot())

        assertEquals(IgnoreResult.REMOVED, list.remove(2))
        assertFalse(list.isIgnoring(2))
        assertTrue(list.isEmpty)
    }

    @Test
    fun refusesSelfAndDuplicates() {
        val list = IgnoreList()

        assertEquals(IgnoreResult.REFUSED, list.add(1, 1, "me").result)
        list.add(1, 2, "bob")
        assertEquals(IgnoreResult.REFUSED, list.add(1, 2, "bob").result)
        assertEquals(1, list.snapshot().size)
    }

    @Test
    fun unignoreOfSomeoneNotIgnoredIsRefused() {
        assertEquals(IgnoreResult.REFUSED, IgnoreList().remove(9))
    }

    @Test
    fun limitDropsOldest() {
        val list = IgnoreList(maxIgnores = 3)
        (10..12).forEach { assertNull(list.add(1, it, "u$it").droppedUserId) }

        val outcome = list.add(1, 13, "u13")

        assertEquals(IgnoreResult.ADDED_DROPPED_OLDEST, outcome.result)
        assertEquals(10, outcome.droppedUserId)
        assertEquals(listOf(11, 12, 13), list.snapshot().map { it.userId })
        assertFalse(list.isIgnoring(10))
    }

    @Test
    fun loadKeepsOrder() {
        val list = IgnoreList()
        list.load(listOf(IgnoredUser(5, "a"), IgnoredUser(3, "b")))

        assertEquals(listOf(5, 3), list.snapshot().map { it.userId })
        assertTrue(list.isIgnoring(3))
    }

    @Test
    fun resultCodesMatchProtocol() {
        assertEquals(listOf(0, 1, 2, 3), IgnoreResult.entries.map { it.code })
    }
}
