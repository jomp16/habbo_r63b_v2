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
import ovh.rwx.habbo.game.currency.Price
import ovh.rwx.habbo.game.user.block.BlockView

class GiftPurchaseRulesTest {
    private class FakeView(
        private val blocking: Set<Int> = emptySet(),
        private val blockedBy: Set<Int> = emptySet(),
    ) : BlockView {
        override fun isBlocking(userId: Int) = userId in blocking
        override fun isBlockedBy(userId: Int) = userId in blockedBy
    }

    private val offer = GiftOfferFacts(pageId = 5, giftable = true, costCredits = 10, pointType = 0)
    private val wrapping = GiftWrappingChoice(spriteId = 1, boxId = 2, ribbonId = 3, price = 4)
    private val receiver = 7
    private val buyerId = 1

    private fun validate(
        offer: GiftOfferFacts? = this.offer,
        pageId: Int = 5,
        wrapping: GiftWrappingChoice? = this.wrapping,
        boxResolved: Boolean = true,
        blocks: BlockView = FakeView(),
        receiverId: () -> Int? = { receiver },
    ) = GiftPurchaseRules.validate(offer, pageId, wrapping, boxResolved, blocks, receiverId)

    private fun reason(d: GiftDecision) = (d as GiftDecision.Reject).reason

    @Test
    fun offerRejects() {
        assertEquals(GiftRejectReason.OFFER_INVALID, reason(validate(offer = null)))
        assertEquals(GiftRejectReason.OFFER_INVALID, reason(validate(pageId = 6)))
        assertEquals(GiftRejectReason.OFFER_INVALID, reason(validate(offer = offer.copy(giftable = false))))
    }

    @Test
    fun wrappingRejects() {
        assertEquals(GiftRejectReason.WRAPPING_INVALID, reason(validate(wrapping = null)))
        assertEquals(GiftRejectReason.WRAPPING_INVALID, reason(validate(boxResolved = false)))
    }

    @Test
    fun receiverRejects() {
        assertEquals(GiftRejectReason.RECEIVER_NOT_FOUND, reason(validate(receiverId = { null })))
        assertEquals(
            GiftRejectReason.RECEIVER_IS_BOT,
            reason(validate(receiverId = { GiftPurchaseRules.BOT_ID_START }))
        )
        assertEquals(
            GiftRejectReason.RECEIVER_IS_BOT,
            reason(validate(receiverId = { GiftPurchaseRules.BOT_ID_START + 5 }))
        )
        assertEquals(
            GiftRejectReason.RECEIVER_BLOCKED_BUYER,
            reason(validate(blocks = FakeView(blockedBy = setOf(receiver))))
        )
    }

    @Test
    fun replyMapping() {
        assertEquals(GiftReply.PURCHASE_ERROR, GiftRejectReason.OFFER_INVALID.reply)
        assertEquals(GiftReply.PURCHASE_ERROR, GiftRejectReason.WRAPPING_INVALID.reply)
        assertEquals(GiftReply.RECEIVER_NOT_FOUND, GiftRejectReason.RECEIVER_NOT_FOUND.reply)
        assertEquals(GiftReply.RECEIVER_NOT_FOUND, GiftRejectReason.RECEIVER_IS_BOT.reply)
        // bloqueio indistinguível de "não encontrado" para o comprador
        assertEquals(GiftReply.RECEIVER_NOT_FOUND, GiftRejectReason.RECEIVER_BLOCKED_BUYER.reply)
    }

    @Test
    fun precedenceOfferBeforeWrappingBeforeReceiver() {
        // tudo errado: a oferta vence
        assertEquals(
            GiftRejectReason.OFFER_INVALID,
            reason(validate(offer = null, wrapping = null, receiverId = { null }))
        )
        // oferta ok, embrulho e destinatário errados: o embrulho vence
        assertEquals(
            GiftRejectReason.WRAPPING_INVALID,
            reason(validate(wrapping = null, receiverId = { null }, blocks = FakeView(blockedBy = setOf(receiver))))
        )
    }

    @Test
    fun receiverLookupIsLazy() {
        var lookups = 0
        validate(offer = null, receiverId = { lookups++; receiver })
        validate(wrapping = null, receiverId = { lookups++; receiver })
        assertEquals(0, lookups)
        validate(receiverId = { lookups++; receiver })
        assertEquals(1, lookups)
    }

    @Test
    fun botCheckedBeforeBlock() {
        val bot = GiftPurchaseRules.BOT_ID_START + 1
        assertEquals(
            GiftRejectReason.RECEIVER_IS_BOT,
            reason(validate(receiverId = { bot }, blocks = FakeView(blockedBy = setOf(bot))))
        )
    }

    @Test
    fun buyerBlockedReceiverIsAllowed() {
        // só "o destinatário bloqueou o comprador" esconde; o comprador ter bloqueado o destinatário não impede
        assertTrue(validate(blocks = FakeView(blocking = setOf(receiver))) is GiftDecision.Proceed)
    }

    @Test
    fun selfGiftAllowed() {
        val d = validate(receiverId = { buyerId })
        assertEquals(buyerId, (d as GiftDecision.Proceed).receiverId)
    }

    @Test
    fun proceedCarriesReceiverAndPrice() {
        val d = validate() as GiftDecision.Proceed
        assertEquals(receiver, d.receiverId)
        assertEquals(Price(14, 0, 0), d.price)
    }

    @Test
    fun priceIsOfferCreditsPlusWrappingWithOfferPointType() {
        val o = offer.copy(costCredits = 25, pointType = 5)
        assertEquals(Price(25 + 4, 5, 0), GiftPurchaseRules.price(o, wrapping))
        assertEquals(Price(25, 5, 0), GiftPurchaseRules.price(o, wrapping.copy(price = 0)))
        assertFalse(GiftPurchaseRules.price(o, wrapping).isFree)
    }

    @Test
    fun botIdBoundary() {
        assertTrue(validate(receiverId = { GiftPurchaseRules.BOT_ID_START - 1 }) is GiftDecision.Proceed)
    }
}
