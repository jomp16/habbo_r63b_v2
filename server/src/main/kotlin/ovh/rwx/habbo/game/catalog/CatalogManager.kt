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
import ovh.rwx.habbo.database.catalog.GiftWrappingTypeDao
import ovh.rwx.habbo.game.catalog.gift.CatalogGiftService
import ovh.rwx.habbo.game.catalog.gift.GiftWrappingType
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.user.subscription.ClubType

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
    // Snapshots imutáveis trocados atomicamente no load(): leitores nunca veem estado vazio/parcial
    @Volatile
    var catalogPages: List<CatalogPage> = emptyList()
        private set
    @Volatile
    var catalogItems: List<CatalogItem> = emptyList()
        private set
    @Volatile
    var catalogClubOffers: List<CatalogClubOffer> = emptyList()
        private set
    @Volatile
    var catalogDeals: List<CatalogDeal> = emptyList()
        private set
    @Volatile
    var recyclerRewards: Map<Int, List<String>> = emptyMap()
        private set
    @Volatile
    var giftWrappingTypes: List<GiftWrappingType> = emptyList()
        private set

    // Índices de busca O(1) de alta performance
    @Volatile
    private var pagesById: Map<Int, CatalogPage> = emptyMap()
    @Volatile
    private var itemsById: Map<Int, CatalogItem> = emptyMap()
    @Volatile
    private var itemsByPageId: Map<Int, List<CatalogItem>> = emptyMap()
    @Volatile
    private var dealsById: Map<Int, CatalogDeal> = emptyMap()

    val purchaseService: CatalogPurchaseService = CatalogPurchaseService()
    val giftService: CatalogGiftService = CatalogGiftService(purchaseService)
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
        val loadedWrappingTypes = GiftWrappingTypeDao.getAll()
        val loadedRewards = CatalogDao.getRecyclerRewards()
            .groupBy { it.first }
            .mapValues { it.value.map { pair -> pair.second } }

        // Constrói os índices O(1) fora das referências ativas e troca de uma vez (sem clear/refill)
        val newItemsByPageId = loadedItems.groupBy { it.pageId }
            .mapValues { (_, items) -> items.sortedBy { it.id }.sortedBy { it.orderNum } }

        pagesById = loadedPages.associateBy { it.id }
        itemsById = loadedItems.associateBy { it.id }
        itemsByPageId = newItemsByPageId
        dealsById = loadedDeals.associateBy { it.id }

        catalogPages = loadedPages.toList()
        catalogItems = loadedItems.toList()
        catalogClubOffers = loadedClubOffers.toList()
        catalogDeals = loadedDeals.toList()
        recyclerRewards = loadedRewards
        giftWrappingTypes = loadedWrappingTypes

        log.info("Loaded {} catalog pages!", catalogPages.size - 2)
        log.info("Loaded {} catalog items!", catalogItems.size)
        log.info("Loaded {} club offers!", catalogClubOffers.size)
        log.info("Loaded {} gift wrapping types!", giftWrappingTypes.size)
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

    fun getRandomRecyclerReward(): RecyclerDraw? =
        recyclerService.getRandomRecyclerReward(recyclerRewards)

    fun getRandomRecyclerLevel(): Int =
        recyclerService.getRandomRecyclerLevel(recyclerRewards)

    companion object {
        const val FREE_AMOUNT = CatalogDiscount.BULK_DISCOUNT_BATCH_SIZE
    }
}