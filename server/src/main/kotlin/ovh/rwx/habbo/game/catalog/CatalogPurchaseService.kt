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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseNotAllowedErrorResponse
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseOkData
import ovh.rwx.habbo.database.catalog.CatalogDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType

class CatalogPurchaseService {

    fun purchase(habboSession: HabboSession, catalogItem: CatalogItem, extraData: String, amount: Int) {
        // 1. Checagem de disponibilidade e assinatura HC
        if (!catalogItem.offerActive || (catalogItem.clubOnly && !habboSession.habboSubscription.validUserSubscription)) {
            habboSession.sendResponse(
                Outgoing.CATALOG_PURCHASE_NOT_ALLOWED_ERROR,
                OutgoingR63A.CATALOG_PURCHASE_NOT_ALLOWED_ERROR,
                CatalogPurchaseNotAllowedErrorResponse.CatalogPurchaseNotAllowedError.NOT_HC
            )
            return
        }

        // 2. Cálculo do total com desconto em lote (bulk discount)
        val totalAmountToPurchase = CatalogDiscount.calculateTotalToPay(amount)

        // 3. Verificação de saldos
        val currentPixels =
            habboSession.userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.PIXELS, 0)
        val currentVipPoints =
            habboSession.userInformation.activityPointsCurrencies.getOrDefault(ActivityPointType.DIAMONDS, 0)

        val notEnoughCredits =
            catalogItem.costCredits > 0 && habboSession.userInformation.credits < catalogItem.costCredits * totalAmountToPurchase
        val notEnoughPixels =
            catalogItem.costPixels > 0 && currentPixels < catalogItem.costPixels * totalAmountToPurchase
        val notEnoughVipPoints =
            catalogItem.costVip > 0 && currentVipPoints < catalogItem.costVip * totalAmountToPurchase

        if (notEnoughCredits || notEnoughPixels || notEnoughVipPoints) {
            val missingPointType = if (notEnoughVipPoints) ActivityPointType.DIAMONDS else ActivityPointType.PIXELS

            habboSession.sendResponse(
                Outgoing.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                OutgoingR63A.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                notEnoughCredits,
                notEnoughPixels || notEnoughVipPoints,
                missingPointType
            )
            return
        }

        // 4. Checagem de estoque LTD
        if (catalogItem.limited && catalogItem.limitedSells.get() >= catalogItem.limitedTotal) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_LIMITED_SOLD_OUT)
            return
        }

        // 5. Construção das instâncias de mobílias a comprar
        val furnishingsToPurchase = buildPurchaseDataList(habboSession, catalogItem, extraData, amount)

        // 6. Persistência no banco de dados
        val userItems = ItemDao.addItems(
            habboSession.userInformation.id,
            furnishingsToPurchase.map {
                ItemPurchaseData(
                    it.furnishing,
                    it.extraData,
                    it.limitedNumber > 0,
                    buildersClub = false
                )
            },
        )

        furnishingsToPurchase.forEachIndexed { index, purchaseData ->
            if (purchaseData.limitedNumber > 0) {
                ItemDao.addLimitedItem(
                    userItems[index].id,
                    purchaseData.limitedNumber,
                    catalogItem.limitedTotal
                )
            }
        }

        // 7. Atualização do inventário e resposta ao cliente
        habboSession.habboInventory.addItems(userItems)
        habboSession.sendResponse(
            Outgoing.CATALOG_PURCHASE_OK,
            OutgoingR63A.CATALOG_PURCHASE_OK,
            CatalogPurchaseOkData(catalogItem, userItems)
        )

        // 8. Dedução de saldos
        deductCurrencies(habboSession, catalogItem, totalAmountToPurchase)

        // 9. Efeitos colaterais (LTD, emblemas, conquistas)
        processPurchaseSideEffects(habboSession, catalogItem)
    }

    private fun buildPurchaseDataList(
        habboSession: HabboSession,
        catalogItem: CatalogItem,
        extraData: String,
        amount: Int,
    ): List<CatalogPurchaseData> {
        val result = mutableListOf<CatalogPurchaseData>()
        val itemManager = HabboServer.habboGame.itemManager

        val deal = catalogItem.deal
        if (catalogItem.dealId > 0 && deal != null) {
            deal.furnishings.forEachIndexed { i, furnishing ->
                val correctedExtraData =
                    itemManager.correctExtradataCatalog(habboSession, extraData, furnishing) ?: return@forEachIndexed
                val count = deal.amounts[i]

                repeat(count) {
                    val limitedNumber = if (catalogItem.limited) catalogItem.limitedSells.incrementAndGet() else 0
                    result += CatalogPurchaseData(furnishing, correctedExtraData, limitedNumber)

                    if (furnishing.interactionType == InteractionType.TELEPORT) {
                        result += result.last()
                    }
                }
            }
        } else {
            val correctedExtraData =
                itemManager.correctExtradataCatalog(habboSession, extraData, catalogItem.furnishing)
                    ?: return emptyList()
            val totalCount = catalogItem.amount * amount

            repeat(totalCount) {
                val limitedNumber = if (catalogItem.limited) catalogItem.limitedSells.incrementAndGet() else 0
                result += CatalogPurchaseData(catalogItem.furnishing, correctedExtraData, limitedNumber)

                if (catalogItem.furnishing.interactionType == InteractionType.TELEPORT) {
                    result += result.last()
                }
            }
        }

        return result
    }

    private fun deductCurrencies(
        habboSession: HabboSession,
        catalogItem: CatalogItem,
        totalAmountToPurchase: Int
    ) {
        if (catalogItem.costCredits > 0) {
            habboSession.userInformation.credits -= catalogItem.costCredits * totalAmountToPurchase
        }
        if (catalogItem.costPixels > 0) {
            habboSession.userInformation.activityPointsCurrencies.merge(
                ActivityPointType.PIXELS,
                -(catalogItem.costPixels * totalAmountToPurchase),
                Int::plus
            )
        }
        if (catalogItem.costVip > 0) {
            habboSession.userInformation.activityPointsCurrencies.merge(
                ActivityPointType.DIAMONDS,
                -(catalogItem.costVip * totalAmountToPurchase),
                Int::plus
            )
        }

        habboSession.updateAllCurrencies()
    }

    private fun processPurchaseSideEffects(habboSession: HabboSession, catalogItem: CatalogItem) {
        // Achievements de LTD
        if (catalogItem.limited) {
            HabboServer.habboGame.achievementManager.progress(
                habboSession,
                "ACH_LTDPurchaser",
                1,
                accumulate = true
            )

            val earlyBirdThreshold = (catalogItem.limitedTotal * 0.1).toInt()
            if (catalogItem.limitedSells.get() <= earlyBirdThreshold) {
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_LTDEarlyBird",
                    1,
                    accumulate = true
                )
            }
        }

        if (catalogItem.badge.isNotEmpty()) {
            habboSession.habboBadge.addBadge(catalogItem.badge)
        }

        if (catalogItem.limited) {
            // Notifica todos os usuários autenticados conectados
            HabboServer.habboSessionManager.habboSessions.values
                .filter { it.authenticated && it.release != "R63A" }
                .forEach { it.sendHabboResponse(Outgoing.CATALOG_OFFER, catalogItem) }

            CatalogDao.updateLimitedSells(catalogItem)
        }
    }
}
