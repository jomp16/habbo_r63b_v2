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

package ovh.rwx.habbo.game.catalog.gift

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ovh.rwx.habbo.game.item.ItemType

class GiftOpenRulesTest {
    private fun validate(
        active: Boolean = true,
        boxOwner: Int = 1,
        session: Int = 1,
        inRoom: Boolean = true,
        contents: Int = 1,
        legacy: Int? = null,
    ) = GiftOpenRules.validate(active, boxOwner, session, inRoom, contents, legacy)

    @Test
    fun claimOnlyOnce() {
        val claims = OpeningClaims()
        assertTrue(claims.tryClaim(10))
        assertFalse(claims.tryClaim(10)) // clique duplo
        assertTrue(claims.tryClaim(11)) // outra caixa não é afetada
    }

    @Test
    fun releaseAllowsReopenAfterAbort() {
        val claims = OpeningClaims()
        assertTrue(claims.tryClaim(10))
        claims.release(10)
        assertFalse(claims.isClaimed(10))
        assertTrue(claims.tryClaim(10))
    }

    @Test
    fun preflight() {
        assertTrue(GiftOpenRules.preflight(1, 1, true))
        assertFalse(GiftOpenRules.preflight(2, 1, true))
        assertFalse(GiftOpenRules.preflight(1, 1, false))
    }

    @Test
    fun proceedNewModelAndLegacy() {
        assertEquals(OpenDecision.PROCEED, validate(contents = 1))
        assertEquals(OpenDecision.PROCEED, validate(contents = 3))
        assertEquals(OpenDecision.PROCEED, validate(contents = 0, legacy = 1))
        assertEquals(OpenDecision.PROCEED, validate(contents = 0, legacy = 5))
    }

    @Test
    fun abortReasons() {
        assertEquals(OpenDecision.ABORT_SESSION_INACTIVE, validate(active = false))
        assertEquals(OpenDecision.ABORT_OWNER_CHANGED, validate(boxOwner = 2))
        assertEquals(OpenDecision.ABORT_BOX_GONE, validate(inRoom = false))
        assertEquals(OpenDecision.ABORT_EMPTY, validate(contents = 0))
        assertEquals(OpenDecision.ABORT_EMPTY, validate(contents = 0, legacy = 0))
        assertEquals(OpenDecision.ABORT_EMPTY, validate(contents = 0, legacy = -2))
    }

    @Test
    fun legacyAmountDecidesEmptyNotContents() {
        // legado presente: manda o amount (o modelo novo vazio é o normal)
        assertEquals(OpenDecision.PROCEED, validate(contents = 0, legacy = 2))
        assertEquals(OpenDecision.ABORT_EMPTY, validate(contents = 4, legacy = 0))
    }

    @Test
    fun abortPrecedence() {
        assertEquals(
            OpenDecision.ABORT_SESSION_INACTIVE,
            validate(active = false, boxOwner = 2, inRoom = false, contents = 0)
        )
        assertEquals(OpenDecision.ABORT_OWNER_CHANGED, validate(boxOwner = 2, inRoom = false, contents = 0))
        assertEquals(OpenDecision.ABORT_BOX_GONE, validate(inRoom = false, contents = 0))
    }

    @Test
    fun placement() {
        assertEquals(GiftPlacement.TRY_PLACE_IN_ROOM, GiftOpenRules.placement(listOf(ItemType.FLOOR)))
        assertEquals(GiftPlacement.INVENTORY, GiftOpenRules.placement(listOf(ItemType.WALL)))
        assertEquals(GiftPlacement.INVENTORY, GiftOpenRules.placement(listOf(ItemType.FLOOR, ItemType.FLOOR)))
        assertEquals(GiftPlacement.INVENTORY, GiftOpenRules.placement(listOf(ItemType.FLOOR, ItemType.WALL)))
        assertEquals(GiftPlacement.INVENTORY, GiftOpenRules.placement(emptyList()))
    }
}
