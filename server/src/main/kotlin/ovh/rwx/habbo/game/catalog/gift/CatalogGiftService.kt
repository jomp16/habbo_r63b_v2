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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseOkData
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.user.UserInformationDao
import ovh.rwx.habbo.database.user.UserStatsDao
import ovh.rwx.habbo.game.catalog.CatalogItem
import ovh.rwx.habbo.game.catalog.CatalogPurchaseService
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.stuff.GiftData
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.plugin.event.events.catalog.CatalogPurchaseEvent

/**
 * Compra de presente: os itens de conteúdo REAIS são criados já para o destinatário e escondidos dentro da caixa
 * (`items_gift_contents`); abrir a caixa só os revela (mesmos ids, numeração LTD preservada). Reaproveita as checagens,
 * a criação de itens e os efeitos colaterais de [CatalogPurchaseService].
 */
class CatalogGiftService(private val purchaseService: CatalogPurchaseService) {

    fun purchaseGift(habboSession: HabboSession, request: GiftPurchaseRequest) {
        val catalogManager = HabboServer.habboGame.catalogManager
        val itemManager = HabboServer.habboGame.itemManager

        val catalogItem = catalogManager.getItem(request.offerId)

        // Resolve os embrulhos (barato) e decide tudo de forma pura ANTES de qualquer escrita ou cobrança; o
        // destinatário (banco) só é consultado se oferta e embrulho passaram.
        val wrapping = if (catalogItem == null) null else GiftWrappingService.resolve(
            habboSession.habboVersion.buildDate, request.spriteId, request.boxId, request.ribbonId
        )
        val boxFurnishing = wrapping?.let { w ->
            (itemManager.giftWrapWrapper + itemManager.giftDefaultWrapper).firstOrNull { it.spriteId == w.spriteId }
        }

        val decision = GiftPurchaseRules.validate(
            offer = catalogItem?.let { GiftOfferFacts(it.pageId, it.isGiftable, it.costCredits, it.pointType) },
            requestPageId = request.pageId,
            wrapping = wrapping,
            boxResolved = boxFurnishing != null,
            buyerBlocks = habboSession.habboBlocks,
            receiverId = { UserInformationDao.getUserInformationByUsername(request.receiverName.trim())?.id },
        )

        when (decision) {
            is GiftDecision.Reject -> {
                when (decision.reason.reply) {
                    GiftReply.PURCHASE_ERROR ->
                        habboSession.sendResponse(Outgoing.CATALOG_PURCHASE_ERROR, OutgoingR63A.CATALOG_PURCHASE_ERROR, 0)

                    GiftReply.RECEIVER_NOT_FOUND -> sendReceiverNotFound(habboSession)
                }
                return
            }

            is GiftDecision.Proceed -> Unit
        }

        // As validações passaram: oferta, embrulho e caixa existem
        catalogItem!!
        wrapping!!
        boxFurnishing!!
        val receiverId = decision.receiverId
        val price = decision.price
        if (!purchaseService.ensurePurchasable(habboSession, catalogItem, price, 1)) return

        val purchaseDatas = purchaseService.buildPurchaseDataList(habboSession, catalogItem, request.extraData, 1)
        if (purchaseDatas.isEmpty()) {
            habboSession.sendResponse(Outgoing.CATALOG_PURCHASE_ERROR, OutgoingR63A.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        // Estoque LTD + cobrança atômica + criação dos itens e da caixa (estorna se falhar; o contador LTD só avança
        // no sucesso); recusa já respondida (esgotado / saldo) sem escrever nada
        val (contents, box) = purchaseService.chargeAndCreate(
            habboSession, catalogItem, price, 1, purchaseDatas
        ) { numbered, scope ->
            val created = purchaseService.createItems(receiverId, catalogItem, numbered, scope)

            val giftData = buildGiftData(
                wrapping,
                request,
                numbered.first().furnishing,
                habboSession.userInformation.username,
                habboSession.userInformation.figure
            )
            val giftBox = ItemDao.addGiftBox(receiverId, boxFurnishing, giftData.toExtraData(), created)
            scope.record(giftBox)
            created to giftBox
        }?.result ?: return

        // Online: a caixa entra no inventário agora; offline: carrega no login (o vínculo já está na fila de escrita)
        val receiverSession = HabboServer.habboSessionManager.getHabboSessionById(receiverId)
        receiverSession?.habboInventory?.addItems(listOf(box))

        habboSession.sendResponse(
            Outgoing.CATALOG_PURCHASE_OK,
            OutgoingR63A.CATALOG_PURCHASE_OK,
            CatalogPurchaseOkData(catalogItem, contents.take(purchaseDatas.size)).copy(isGift = true)
        )

        purchaseService.processLimitedSales(catalogItem)
        recordStats(habboSession, receiverId, receiverSession)

        HabboServer.pluginManager.executeEventAsync(
            CatalogPurchaseEvent(habboSession, catalogItem, request.extraData, 1, contents)
        )
    }

    private fun sendReceiverNotFound(habboSession: HabboSession) {
        habboSession.sendResponse(Outgoing.CATALOG_RECEIVER_NOT_FOUND, OutgoingR63A.CATALOG_RECEIVER_NOT_FOUND)
    }

    /**
     * Comprador: giftsGiven + ACH_GiftGiver. Destinatário: giftsReceived + ACH_GiftReceiver; offline só o contador é
     * persistido (soma direta na linha) e o login reaplica a conquista a partir dele, porque o AchievementManager não
     * sobe nível/emblema para sessão offline. Presente para si mesmo conta só do lado do comprador (sem farm).
     */
    private fun recordStats(buyer: HabboSession, receiverId: Int, receiverSession: HabboSession?) {
        val achievements = HabboServer.habboGame.achievementManager

        buyer.userStats.giftsGiven += 1
        achievements.progress(buyer, "ACH_GiftGiver", 1, accumulate = true)

        if (receiverId == buyer.userInformation.id) return

        if (receiverSession != null) {
            receiverSession.userStats.giftsReceived += 1
            achievements.progress(receiverSession, "ACH_GiftReceiver", 1, accumulate = true)
        } else {
            UserStatsDao.addGiftReceived(receiverId)
        }
    }

    companion object {
        /**
         * GiftData da caixa: o "extra" do quarto é caixa * 1000 + fita (openStateA/B); o remetente só aparece se
         * [GiftPurchaseRequest.showPurchaserName]; PRODUCT_CODE = classname do primeiro produto da oferta.
         */
        fun buildGiftData(
            wrapping: GiftWrappingChoice,
            request: GiftPurchaseRequest,
            firstProduct: Furnishing,
            purchaserName: String,
            purchaserFigure: String,
        ): GiftData = GiftData(
            message = GiftWrapping.cleanMessage(request.message),
            productCode = firstProduct.itemName,
            openStateA = wrapping.boxId,
            openStateB = wrapping.ribbonId,
            showPurchaser = request.showPurchaserName,
            purchaserName = if (request.showPurchaserName) purchaserName else "",
            purchaserFigure = if (request.showPurchaserName) purchaserFigure else "",
            extraParam = "",
        )
    }
}
