/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.Furnishing

/** Uma linha do pacote: [amount] unidades do mobi [itemName]. A ordem da lista é a ordem dos produtos na oferta. */
data class CatalogDealItem(val itemName: String, val amount: Int)

/** Um produto do pacote já com o mobi resolvido. */
data class CatalogDealProduct(val furnishing: Furnishing, val amount: Int)

/**
 * Pacote (deal): uma oferta do catálogo que entrega vários mobis de uma vez (`catalog_items.deal_id`).
 * [name] é o nome mostrado no CMS e usado como nome da oferta; [items] vem de `catalog_deal_items`, em ordem.
 */
data class CatalogDeal(
    val id: Int,
    val name: String,
    val items: List<CatalogDealItem>
) {
    /** Os produtos na ordem do pacote. Um mobi que não existe mais em `furnishings` fica de fora. */
    val products: List<CatalogDealProduct>
        get() = products(HabboServer.habboGame.itemManager.furnishings::get)

    fun products(resolve: (String) -> Furnishing?): List<CatalogDealProduct> =
        items.mapNotNull { item -> resolve(item.itemName)?.let { CatalogDealProduct(it, item.amount) } }
}
