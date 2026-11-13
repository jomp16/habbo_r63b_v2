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

import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType

/**
 * Quem pode ser presenteado. Só mobi (chão/parede, inclusive pacotes e LTD), pago só em créditos, com `allow_gift` em
 * todos os produtos; fora: oferta de club/HC, mascote, bot, emblema, efeito, moeda alternativa e o próprio presente.
 */
object GiftRules {
    fun isGiftable(
        offerActive: Boolean,
        clubOnly: Boolean,
        pricePoints: Int,
        badge: String,
        isPetPage: Boolean,
        products: List<Furnishing>,
    ): Boolean {
        if (!offerActive || clubOnly || pricePoints > 0 || badge.isNotBlank() || isPetPage) return false
        if (products.isEmpty()) return false

        return products.all { it.isGiftableProduct() }
    }

    /**
     * Caixa de presente (interação gift, `present_gen*` / `present_wrap*`) ou a caixa do Ecotron ([recyclerBox]): não
     * pode ser trocada nem vendida; o CONTEÚDO vira trocável depois de aberto.
     */
    fun isGiftBox(itemName: String, interactionType: InteractionType, recyclerBox: String?): Boolean =
        interactionType == InteractionType.GIFT
                || itemName.startsWith("present_gen")
                || itemName.startsWith("present_wrap")
                || (recyclerBox != null && itemName == recyclerBox)

    private fun Furnishing.isGiftableProduct(): Boolean =
        (type == ItemType.FLOOR || type == ItemType.WALL)
                && canGift
                && interactionType != InteractionType.PET
                && interactionType != InteractionType.GIFT
}
