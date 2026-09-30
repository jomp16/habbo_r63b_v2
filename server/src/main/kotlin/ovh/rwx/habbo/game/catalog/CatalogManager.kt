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

package ovh.rwx.habbo.game.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.catalog.CatalogDao
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.subscription.ClubType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gerenciador central do Catálogo do Habbo.
 * Atua como repositório em memória e fachada coordenadora para:
 * - [purchaseService]: Compra de mobílias, pacotes/deals, LTDs e mobílias duplas (teleportes)
 * - [clubService]: Compra e extensão de Habbo Club e Builders Club
 * - [petService]: Validação, compra e adoção de mascotes
 * - [voucherService]: Resgate de vouchers
 * - [recyclerService]: Sorteio e premiação do Ecotron/Recycler
 */
class CatalogManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    // Coleções compatíveis para acesso público externo
    val catalogPages: MutableList<CatalogPage> = CopyOnWriteArrayList()
    val catalogItems: MutableList<CatalogItem> = CopyOnWriteArrayList()
    val catalogClubOffers: MutableList<CatalogClubOffer> = CopyOnWriteArrayList()
    val catalogDeals: MutableList<CatalogDeal> = CopyOnWriteArrayList()
    val recyclerRewards: ConcurrentHashMap<Int, List<String>> = ConcurrentHashMap()

    // Índices de busca O(1) de alta performance
    private val pagesById: ConcurrentHashMap<Int, CatalogPage> = ConcurrentHashMap()
    private val itemsById: ConcurrentHashMap<Int, CatalogItem> = ConcurrentHashMap()
    private val itemsByPageId: ConcurrentHashMap<Int, List<CatalogItem>> = ConcurrentHashMap()
    private val dealsById: ConcurrentHashMap<Int, CatalogDeal> = ConcurrentHashMap()

    val purchaseService: CatalogPurchaseService = CatalogPurchaseService()
    val clubService: CatalogClubService = CatalogClubService(this)
    val petService: CatalogPetService = CatalogPetService()
    val voucherService: CatalogVoucherService = CatalogVoucherService()
    val recyclerService: CatalogRecyclerService = CatalogRecyclerService()

    fun load() {
        log.info("Loading catalog...")

        // Carrega dados do banco
        val loadedPages = mutableListOf(
            CatalogPage.createRoot(-1, "root"),
            CatalogPage.createRoot(-2, "root"),
        )
        loadedPages += CatalogDao.getCatalogPages()

        val loadedItems = CatalogDao.getCatalogItems()
        val loadedClubOffers = CatalogDao.getCatalogClubOffers()
        val loadedDeals = CatalogDao.getCatalogDeals()
        val loadedRewards = CatalogDao.getRecyclerRewards()
            .groupBy { it.first }
            .mapValues { it.value.map { pair -> pair.second } }

        // Atualiza índices O(1)
        pagesById.clear()
        loadedPages.forEach { pagesById[it.id] = it }

        itemsById.clear()
        loadedItems.forEach { itemsById[it.id] = it }

        itemsByPageId.clear()
        loadedItems.groupBy { it.pageId }.forEach { (pageId, items) ->
            itemsByPageId[pageId] = items.sortedBy { it.id }.sortedBy { it.orderNum }
        }

        dealsById.clear()
        loadedDeals.forEach { dealsById[it.id] = it }

        // Atualiza coleções públicas thread-safe
        catalogPages.clear()
        catalogPages.addAll(loadedPages)

        catalogItems.clear()
        catalogItems.addAll(loadedItems)

        catalogClubOffers.clear()
        catalogClubOffers.addAll(loadedClubOffers)

        catalogDeals.clear()
        catalogDeals.addAll(loadedDeals)

        recyclerRewards.clear()
        recyclerRewards.putAll(loadedRewards)

        log.info("Loaded {} catalog pages!", catalogPages.size - 2)
        log.info("Loaded {} catalog items!", catalogItems.size)
        log.info("Loaded {} club offers!", catalogClubOffers.size)
        log.info("Loaded {} catalog deals!", catalogDeals.size)
        log.info(
            "Loaded {} recycler levels and {} recycler rewards!",
            recyclerRewards.size,
            recyclerRewards.values.sumOf { it.size }
        )
    }

    // ------------------------------------------------------------------
    // Consultas Rápidas O(1)
    // ------------------------------------------------------------------

    fun getPage(id: Int): CatalogPage? = pagesById[id]

    fun getItem(id: Int): CatalogItem? = itemsById[id]

    fun getDeal(id: Int): CatalogDeal? = dealsById[id]

    fun getItemsForPage(pageId: Int): List<CatalogItem> = itemsByPageId[pageId] ?: emptyList()

    fun getClubOffer(itemId: Int, clubType: ClubType): CatalogClubOffer? =
        catalogClubOffers.find { it.itemId == itemId && it.clubType == clubType }

    // ------------------------------------------------------------------
    // Fachada de Compras
    // ------------------------------------------------------------------

    fun purchase(habboSession: HabboSession, catalogItem: CatalogItem, extraData: String, amount: Int) =
        purchaseService.purchase(habboSession, catalogItem, extraData, amount)

    fun purchaseHC(habboSession: HabboSession, itemId: Int) =
        clubService.purchaseHC(habboSession, itemId)

    fun purchaseBuildersClub(habboSession: HabboSession, itemId: Int) =
        clubService.purchaseBuildersClub(habboSession, itemId)

    fun purchasePet(habboSession: HabboSession, catalogItem: CatalogItem, extraData: String) =
        petService.purchasePet(habboSession, catalogItem, extraData)

    // ------------------------------------------------------------------
    // Vouchers
    // ------------------------------------------------------------------

    fun redeemVoucher(habboSession: HabboSession, voucherCode: String) =
        voucherService.redeemVoucher(habboSession, voucherCode)

    // ------------------------------------------------------------------
    // Recycler / Ecotron
    // ------------------------------------------------------------------

    fun getRandomRecyclerReward(): Furnishing? =
        recyclerService.getRandomRecyclerReward(recyclerRewards)

    fun getRandomRecyclerLevel(): Int =
        recyclerService.getRandomRecyclerLevel(recyclerRewards)

    companion object {
        const val FREE_AMOUNT = CatalogDiscount.BULK_DISCOUNT_BATCH_SIZE
    }
}