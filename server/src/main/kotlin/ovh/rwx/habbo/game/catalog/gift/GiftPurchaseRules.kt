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

import ovh.rwx.habbo.game.currency.Price
import ovh.rwx.habbo.game.user.block.BlockPolicy
import ovh.rwx.habbo.game.user.block.BlockView

/** Resposta enviada ao comprador quando o presente é recusado. */
enum class GiftReply { PURCHASE_ERROR, RECEIVER_NOT_FOUND }

/** Por que o presente foi recusado; cada motivo mapeia 1:1 para uma [GiftReply]. */
enum class GiftRejectReason(val reply: GiftReply) {
    /** Oferta inexistente, de outra página ou não presenteável. */
    OFFER_INVALID(GiftReply.PURCHASE_ERROR),

    /** Embrulho inválido ou furnishing da caixa não resolvido. */
    WRAPPING_INVALID(GiftReply.PURCHASE_ERROR),

    /** Destinatário inexistente. */
    RECEIVER_NOT_FOUND(GiftReply.RECEIVER_NOT_FOUND),

    /** Destinatário é um bot (id >= [GiftPurchaseRules.BOT_ID_START]). */
    RECEIVER_IS_BOT(GiftReply.RECEIVER_NOT_FOUND),

    /** O destinatário bloqueou o comprador: o mesmo "não encontrado" (não revela o bloqueio). */
    RECEIVER_BLOCKED_BUYER(GiftReply.RECEIVER_NOT_FOUND),
}

sealed interface GiftDecision {
    data class Reject(val reason: GiftRejectReason) : GiftDecision

    /** Tudo válido: [price] = créditos da oferta + embrulho, na moeda alternativa da oferta (sem pontos). */
    data class Proceed(val receiverId: Int, val price: Price) : GiftDecision
}

/** O que a decisão precisa saber da oferta do catálogo (valores simples, sem `CatalogItem`). */
data class GiftOfferFacts(val pageId: Int, val giftable: Boolean, val costCredits: Int, val pointType: Int)

/** Decisão pura da compra de presente; acontece ANTES de qualquer escrita ou cobrança. */
object GiftPurchaseRules {
    /** Ids a partir daqui são de bots, nunca destinatários. */
    const val BOT_ID_START = 2_000_000_000

    /**
     * Ordem (a das respostas ao client): oferta -> embrulho/caixa -> destinatário. O destinatário é resolvido de forma
     * preguiçosa por [receiverId] (consulta ao banco) só se os passos anteriores passaram. [receiverId] devolve null se
     * não existe.
     *
     * @param offer a oferta pedida, ou null se não existe.
     * @param requestPageId a página enviada pelo client (precisa ser a da oferta).
     * @param wrapping o embrulho resolvido, ou null se inválido.
     * @param boxResolved se o furnishing da caixa do embrulho foi encontrado.
     * @param buyerBlocks a visão de bloqueios do comprador.
     */
    fun validate(
        offer: GiftOfferFacts?,
        requestPageId: Int,
        wrapping: GiftWrappingChoice?,
        boxResolved: Boolean,
        buyerBlocks: BlockView,
        receiverId: () -> Int?,
    ): GiftDecision {
        if (offer == null || offer.pageId != requestPageId || !offer.giftable) {
            return GiftDecision.Reject(GiftRejectReason.OFFER_INVALID)
        }

        if (wrapping == null || !boxResolved) return GiftDecision.Reject(GiftRejectReason.WRAPPING_INVALID)

        val receiver = receiverId() ?: return GiftDecision.Reject(GiftRejectReason.RECEIVER_NOT_FOUND)
        if (receiver >= BOT_ID_START) return GiftDecision.Reject(GiftRejectReason.RECEIVER_IS_BOT)
        if (BlockPolicy.giftReceiverHidden(buyerBlocks, receiver)) {
            return GiftDecision.Reject(GiftRejectReason.RECEIVER_BLOCKED_BUYER)
        }

        return GiftDecision.Proceed(receiver, price(offer, wrapping))
    }

    /** Créditos da oferta + preço do embrulho; a moeda alternativa da oferta não é cobrada (ofertas com pontos nem são presenteáveis). */
    fun price(offer: GiftOfferFacts, wrapping: GiftWrappingChoice): Price =
        Price(offer.costCredits + wrapping.price, offer.pointType, 0)
}
