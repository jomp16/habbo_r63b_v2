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

package ovh.rwx.habbo.game.user.block

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockListTest {
    @Test
    fun blocksAndUnblocks() {
        val list = BlockList()

        assertEquals(BlockResult.BLOCKED, list.block(1, 2))
        assertTrue(list.isBlocked(2))
        assertEquals(listOf(2), list.snapshot().toList())

        assertEquals(BlockResult.UNBLOCKED, list.unblock(2))
        assertFalse(list.isBlocked(2))
    }

    @Test
    fun refusesSelfDuplicatesAndUnknown() {
        val list = BlockList()

        assertEquals(BlockResult.REFUSED, list.block(1, 1))
        assertEquals(BlockResult.BLOCKED, list.block(1, 2))
        assertEquals(BlockResult.REFUSED, list.block(1, 2))
        assertEquals(BlockResult.REFUSED, list.unblock(3))
        assertEquals(1, list.snapshot().size)
    }

    @Test
    fun keepsOrderAndLoads() {
        val list = BlockList()
        list.load(listOf(5, 3, 9))
        list.block(1, 4)

        assertEquals(listOf(5, 3, 9, 4), list.snapshot().toList())
        assertTrue(list.isBlocked(9))
    }
}
