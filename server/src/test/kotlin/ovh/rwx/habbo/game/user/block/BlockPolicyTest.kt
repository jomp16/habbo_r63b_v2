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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {
    private class FakeView(
        private val blocking: Set<Int> = emptySet(),
        private val blockedBy: Set<Int> = emptySet()
    ) : BlockView {
        override fun isBlocking(userId: Int) = userId in blocking
        override fun isBlockedBy(userId: Int) = userId in blockedBy
    }

    // o dono da visão é "eu"; o outro é o id 2; o id 9 não tem relação nenhuma
    private val other = 2
    private val unrelated = 9
    private val neither = FakeView()
    private val iBlockedThem = FakeView(blocking = setOf(other))
    private val theyBlockedMe = FakeView(blockedBy = setOf(other))
    private val both = FakeView(blocking = setOf(other), blockedBy = setOf(other))

    private fun assertEitherWayRule(rule: (BlockView, Int) -> Boolean) {
        // true = ação permitida
        assertTrue(rule(neither, other))
        assertFalse(rule(iBlockedThem, other))
        assertFalse(rule(theyBlockedMe, other))
        assertFalse(rule(both, other))
        for (v in listOf(neither, iBlockedThem, theyBlockedMe, both)) assertTrue(rule(v, unrelated))
    }

    @Test
    fun isBlockedEitherWay() {
        assertFalse(BlockPolicy.isBlockedEitherWay(neither, other))
        assertTrue(BlockPolicy.isBlockedEitherWay(iBlockedThem, other))
        assertTrue(BlockPolicy.isBlockedEitherWay(theyBlockedMe, other))
        assertTrue(BlockPolicy.isBlockedEitherWay(both, other))
        assertFalse(BlockPolicy.isBlockedEitherWay(both, unrelated))
    }

    @Test
    fun chatWhisperAcceptTradeMessageAreBidirectional() {
        assertEitherWayRule(BlockPolicy::canHearChat)
        assertEitherWayRule(BlockPolicy::canWhisper)
        assertEitherWayRule(BlockPolicy::canAcceptFriendRequest)
        assertEitherWayRule(BlockPolicy::canTrade)
        assertEitherWayRule(BlockPolicy::canMessage)
    }

    @Test
    fun roomEntryOnlyDeniedWhenOwnerBlockedEntrant() {
        assertFalse(BlockPolicy.roomEntryDenied(neither, other, false))
        assertFalse(BlockPolicy.roomEntryDenied(iBlockedThem, other, false))
        assertTrue(BlockPolicy.roomEntryDenied(theyBlockedMe, other, false))
        assertTrue(BlockPolicy.roomEntryDenied(both, other, false))
        assertFalse(BlockPolicy.roomEntryDenied(both, unrelated, false))
    }

    @Test
    fun roomEntryStaffBypass() {
        for (v in listOf(neither, iBlockedThem, theyBlockedMe, both)) {
            assertFalse(BlockPolicy.roomEntryDenied(v, other, true))
        }
    }

    @Test
    fun friendRequestErrorCodes() {
        assertNull(BlockPolicy.friendRequestError(neither, other))
        assertEquals(8, BlockPolicy.friendRequestError(iBlockedThem, other))
        assertEquals(7, BlockPolicy.friendRequestError(theyBlockedMe, other))
        // os dois: "bloqueado por ele" (7) tem prioridade
        assertEquals(7, BlockPolicy.friendRequestError(both, other))
        assertNull(BlockPolicy.friendRequestError(both, unrelated))
        assertEquals(7, BlockPolicy.FRIEND_REQUEST_BLOCKED_BY_THEM)
        assertEquals(8, BlockPolicy.FRIEND_REQUEST_BLOCKED_BY_YOU)
    }

    @Test
    fun inviteOnlyDeniedWhenRecipientBlockedInviter() {
        // a visão é a do destinatário; o convidante é o id 2
        assertTrue(BlockPolicy.canInvite(neither, other))
        assertFalse(BlockPolicy.canInvite(iBlockedThem, other))
        assertTrue(BlockPolicy.canInvite(theyBlockedMe, other))
        assertFalse(BlockPolicy.canInvite(both, other))
        assertTrue(BlockPolicy.canInvite(both, unrelated))
    }

    @Test
    fun giftReceiverHiddenOnlyWhenReceiverBlockedBuyer() {
        assertFalse(BlockPolicy.giftReceiverHidden(neither, other))
        assertFalse(BlockPolicy.giftReceiverHidden(iBlockedThem, other))
        assertTrue(BlockPolicy.giftReceiverHidden(theyBlockedMe, other))
        assertTrue(BlockPolicy.giftReceiverHidden(both, other))
        assertFalse(BlockPolicy.giftReceiverHidden(both, unrelated))
    }

    @Test
    fun navigatorHidesOnlyOwnersWhoBlockedViewer() {
        assertFalse(BlockPolicy.hidesRoomOwner(neither, other))
        assertFalse(BlockPolicy.hidesRoomOwner(iBlockedThem, other))
        assertTrue(BlockPolicy.hidesRoomOwner(theyBlockedMe, other))
        assertTrue(BlockPolicy.hidesRoomOwner(both, other))
        assertFalse(BlockPolicy.hidesRoomOwner(both, unrelated))
    }
}
