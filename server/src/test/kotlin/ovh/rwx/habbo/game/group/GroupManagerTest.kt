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

package ovh.rwx.habbo.game.group

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.database.group.GroupDao

/**
 * Fluxo de criação de grupo com write-behind: os INSERTs (grupo + dono) são enfileirados,
 * o GroupData nasce em memória (sem read-back síncrono — voltaria vazio antes do flush)
 * e o dono é semeado no set de members antes que o lazy dele leia o banco.
 */
class GroupManagerTest {

    @After
    fun tearDown() {
        unmockkObject(GroupDao)
    }

    @Test
    fun testCreateGroupBuildsGroupDataInMemoryAndSeedsOwner() {
        mockkObject(GroupDao)
        every { GroupDao.createGroup(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 42
        every { GroupDao.addMember(any(), any(), any()) } returns 7
        every { GroupDao.getGroupMembers(any()) } returns emptyList()

        val manager = GroupManager()
        val group = manager.createGroup(
            name = "Guilda",
            description = "desc",
            groupBadge = "b050",
            ownerId = 100,
            roomId = 5,
            backgroundColorPrimary = 1,
            backgroundColorSecondary = 2
        )

        // GroupData construído em memória: um read-back aqui chamaria o getGroupData real
        // (não stubado) e explodiria sem jdbi — o teste falharia alto
        assertEquals(42, group.groupData.id)
        assertEquals("Guilda", group.groupData.name)
        assertEquals("b050", group.groupData.badge)
        assertEquals(100, group.groupData.ownerId)
        assertEquals(5, group.groupData.roomId)
        assertSame(group, manager.groups[42])

        // dono semeado com o ID HiLo alocado para o INSERT enfileirado
        val owner = group.members.single()
        assertEquals(7, owner.id)
        assertEquals(100, owner.userId)
        assertEquals(2, owner.rank)
        assertTrue(group.admins.contains(owner))
    }
}
