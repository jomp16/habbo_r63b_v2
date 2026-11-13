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
import ovh.rwx.habbo.plugin.event.events.catalog.CatalogPurchaseEvent
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseNotAllowedErrorResponse
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseOkData
import ovh.rwx.habbo.database.catalog.CatalogDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.currency.BalanceCheck
import ovh.rwx.habbo.game.currency.Price
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.user.CreatedItems
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.item.user.isLimitedEarlyBird
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.currency.CurrencyService

/** Resultado de uma compra criada: o que [CatalogPurchaseService.chargeAndCreate] devolveu + os números LTD DESTA compra. */
internal data class ChargedCreation<T>(val result: T, val limitedNumbers: List<Int>)

class CatalogPurchaseService {

    fun purchase(habboSession: HabboSession, catalogItem: CatalogItem, extraData: String, amount: Int) {
        // 2. Cálculo do total com desconto em lote (bulk discount)
        val totalAmountToPurchase = CatalogDiscount.calculateTotalToPay(amount)

        // 1, 3 e 4. Disponibilidade/HC, saldo e estoque LTD (responde o erro ao client quando recusa)
        if (!ensurePurchasable(habboSession, catalogItem, catalogItem.price, totalAmountToPurchase)) return

        // 5. Construção (pura) das instâncias de mobílias a comprar; pedido inválido não cobra nem reserva nada
        val requested = buildPurchaseDataList(habboSession, catalogItem, extraData, amount)
        if (requested.isEmpty()) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        // 5b, 6. Estoque LTD + cobrança atômica + persistência dos itens (estorna se criar falhar); se algo recusa,
        // a resposta de erro já foi enviada
        val charged = chargeAndCreate(
            habboSession, catalogItem, catalogItem.price, totalAmountToPurchase, requested
        ) { numbered, created -> createItems(habboSession.userInformation.id, catalogItem, numbered, created) } ?: return
        val userItems = charged.result

        // 7. Inventário e resposta
        habboSession.habboInventory.addItems(userItems)
        // Teleportes: o parceiro do par nasce em ItemDao.addItems e entra no inventário,
        // mas na confirmação de compra (CATALOG_PURCHASE_OK) só enviamos os itens pedidos
        // (requested.size) para evitar notificações duplicadas de "item comprado".
        val purchaseOkItems = userItems.take(requested.size)
        habboSession.sendResponse(
            Outgoing.CATALOG_PURCHASE_OK,
            OutgoingR63A.CATALOG_PURCHASE_OK,
            CatalogPurchaseOkData(catalogItem, purchaseOkItems)
        )

        // 8. (a cobrança já foi feita em 5b)

        // 9. Efeitos colaterais (LTD, emblemas, conquistas)
        processPurchaseSideEffects(habboSession, catalogItem, charged.limitedNumbers)

        HabboServer.pluginManager.executeEventAsync(
            CatalogPurchaseEvent(habboSession, catalogItem, extraData, amount, userItems)
        )
    }

    /**
     * Checagens comuns de compra e presente (a ordem importa: as respostas de erro são as do client):
     * oferta ativa e HC, moeda suportada, saldo para [price] × [times] e estoque LTD. Responde o erro e devolve false.
     */
    internal fun ensurePurchasable(
        habboSession: HabboSession,
        catalogItem: CatalogItem,
        price: Price,
        times: Int
    ): Boolean {
        if (!catalogItem.offerActive || (catalogItem.clubOnly && !habboSession.habboSubscription.validUserSubscription)) {
            habboSession.sendResponse(
                Outgoing.CATALOG_PURCHASE_NOT_ALLOWED_ERROR,
                OutgoingR63A.CATALOG_PURCHASE_NOT_ALLOWED_ERROR,
                CatalogPurchaseNotAllowedErrorResponse.CatalogPurchaseNotAllowedError.NOT_HC
            )
            return false
        }

        // A moeda precisa existir para o client dessa sessão e um pacote precisa ter mobi válido (a oferta nem aparece
        // no catálogo quando não)
        if (!catalogItem.isAvailableFor(habboSession.habboVersion)) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return false
        }

        val balance = CurrencyService.check(habboSession, price, times)
        if (!balance.ok) {
            habboSession.sendResponse(
                Outgoing.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                OutgoingR63A.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                balance.notEnoughCredits,
                balance.notEnoughPoints,
                balance.pointType
            )
            return false
        }

        if (catalogItem.limited && catalogItem.limitedSells.get() >= catalogItem.limitedTotal) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_LIMITED_SOLD_OUT)
            return false
        }

        return true
    }

    /**
     * Cria os itens da compra para [ownerId] (sem sala) e a numeração LTD; NÃO mexe em inventário nem em resposta.
     * Tudo que for criado é registrado em [created] para o chamador desfazer se a tentativa falhar adiante.
     */
    internal fun createItems(
        ownerId: Int,
        catalogItem: CatalogItem,
        furnishingsToPurchase: List<CatalogPurchaseData>,
        created: CreatedItems,
    ): List<UserItem> {
        val userItems = ItemDao.addItems(
            ownerId,
            furnishingsToPurchase.map {
                ItemPurchaseData(
                    it.furnishing,
                    it.extraData,
                    it.limitedNumber > 0,
                    buildersClub = false
                )
            },
        )
        created.record(userItems)

        furnishingsToPurchase.forEachIndexed { index, purchaseData ->
            if (purchaseData.limitedNumber > 0) {
                ItemDao.addLimitedItem(
                    userItems[index].id,
                    purchaseData.limitedNumber,
                    catalogItem.limitedTotal
                )
            }
        }

        return userItems
    }

    internal fun buildPurchaseDataList(
        habboSession: HabboSession,
        catalogItem: CatalogItem,
        extraData: String,
        amount: Int,
    ): List<CatalogPurchaseData> {
        val itemManager = HabboServer.habboGame.itemManager
        return buildPurchaseDataList(catalogItem, amount) { furnishing ->
            itemManager.correctExtradataCatalog(habboSession, extraData, furnishing)
        }
    }

    /**
     * Uma entrada por UNIDADE comprada. Não duplica teletransportes: o parceiro do par é criado por
     * `ItemDao.addItems` (ver `planNewItems`); duplicar aqui entregaria 4 itens por unidade.
     * Função pura: `limitedNumber` sai 0 (a numeração LTD é atribuída por [chargeAndCreate]); lista vazia = recusar.
     *
     * @param correctExtraData extra data corrigido para o mobi, ou null se o pedido é inválido.
     */
    internal fun buildPurchaseDataList(
        catalogItem: CatalogItem,
        amount: Int,
        correctExtraData: (Furnishing) -> String?,
    ): List<CatalogPurchaseData> {
        val result = mutableListOf<CatalogPurchaseData>()

        val dealProducts = catalogItem.dealProducts
        if (dealProducts != null) {
            // Pacote: cada produto na ordem dele, `quantidade do produto × quantidade comprada` (a cobrança já é por
            // `amount`, com o desconto em lote; entregar 1 pacote só seria cobrar por N)
            // null = pedido inválido (as lógicas só devolvem null para cancelar a compra): um produto inválido
            // invalida o pacote inteiro, nunca se entrega pacote parcial cobrando o preço cheio
            dealProducts.forEach { (furnishing, productAmount) ->
                val correctedExtraData = correctExtraData(furnishing) ?: return emptyList()

                repeat(productAmount * amount) {
                    // teleporte: o parceiro do par é criado (e devolvido) por ItemDao.addItems
                    result += CatalogPurchaseData(furnishing, correctedExtraData, 0)
                }
            }
        } else {
            val correctedExtraData = correctExtraData(catalogItem.furnishing) ?: return emptyList()
            val totalCount = catalogItem.amount * amount

            repeat(totalCount) {
                // teleporte: o parceiro do par é criado (e devolvido) por ItemDao.addItems
                result += CatalogPurchaseData(catalogItem.furnishing, correctedExtraData, 0)
            }
        }

        return result
    }

    /**
     * Cobra ([CurrencyService.tryCharge]) e cria ([create], recebe as unidades já numeradas) como uma unidade só: se
     * criar lançar, estorna e relança. Em oferta LTD tudo roda dentro de [CatalogItem.withLimitedReservation] (lock por
     * oferta, preso durante o insert síncrono em `ItemDao.addItems`): o contador só avança depois do sucesso, então não
     * há número perdido nem repetido; o novo total é gravado logo em seguida. Ofertas comuns não pegam lock.
     * Se [create] lançar, tudo que registrou no [CreatedItems] é removido pela fila de escrita (sem linha órfã), a
     * cobrança é estornada e a exceção é relançada.
     * Devolve o resultado de [create] com os números LTD atribuídos a ESTA compra, ou null depois de responder o erro
     * (esgotado / saldo) sem escrever nem cobrar.
     */
    internal fun <T : Any> chargeAndCreate(
        habboSession: HabboSession,
        catalogItem: CatalogItem,
        price: Price,
        times: Int,
        units: List<CatalogPurchaseData>,
        create: (List<CatalogPurchaseData>, CreatedItems) -> T,
    ): ChargedCreation<T>? {
        var created: ChargedCreation<T>? = null
        var balance: BalanceCheck? = null

        fun attempt(numbered: List<CatalogPurchaseData>): Boolean {
            val check = CurrencyService.tryCharge(habboSession, price, times)
            if (!check.ok) {
                balance = check
                return false
            }
            val scope = CreatedItems()
            try {
                created = ChargedCreation(create(numbered, scope), numbered.map { it.limitedNumber }.filter { it > 0 })
            } catch (e: Exception) {
                scope.rollback()
                CurrencyService.refund(habboSession, price, times)
                throw e
            }
            return true
        }

        if (catalogItem.limited) {
            val outcome = catalogItem.withLimitedReservation(units.size) { range ->
                attempt(units.mapIndexed { i, u -> u.copy(limitedNumber = range.first + i) })
            }
            when (outcome) {
                LimitedOutcome.SOLD_OUT -> {
                    habboSession.sendHabboResponse(Outgoing.CATALOG_LIMITED_SOLD_OUT)
                    return null
                }
                // monotônico: a fila lê o valor atual do contador na execução
                LimitedOutcome.COMMITTED -> CatalogDao.updateLimitedSells(catalogItem)
                LimitedOutcome.DECLINED -> Unit
            }
        } else {
            attempt(units)
        }

        balance?.let { check ->
            habboSession.sendResponse(
                Outgoing.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                OutgoingR63A.CATALOG_PURCHASE_ERROR_NOT_ENOUGH_BALANCE,
                check.notEnoughCredits,
                check.notEnoughPoints,
                check.pointType
            )
            return null
        }
        return created
    }

    /** [limitedNumbers]: os números LTD atribuídos a esta compra (de [ChargedCreation]); vazio para oferta comum. */
    internal fun processPurchaseSideEffects(
        habboSession: HabboSession,
        catalogItem: CatalogItem,
        limitedNumbers: List<Int>,
    ) {
        // Achievements de LTD
        if (catalogItem.limited) {
            progressLimitedAchievements(habboSession, catalogItem, limitedNumbers)
        }

        if (catalogItem.badge.isNotEmpty()) {
            habboSession.habboBadge.addBadge(catalogItem.badge)
        }

        processLimitedSales(catalogItem)
    }

    /** Contagem de vendas LTD: avisa os clients conectados (o total já foi gravado na reserva; o comprador de presente também passa aqui). */
    internal fun processLimitedSales(catalogItem: CatalogItem) {
        if (catalogItem.limited) {
            // Notifica todos os usuários autenticados conectados
            HabboServer.habboSessionManager.habboSessions.values
                .filter { it.authenticated && !it.isPreShuffle }
                .forEach { it.sendHabboResponse(Outgoing.CATALOG_OFFER, catalogItem) }
        }
    }

    private fun progressLimitedAchievements(habboSession: HabboSession, catalogItem: CatalogItem, limitedNumbers: List<Int>) {
        if (catalogItem.limited) {
            HabboServer.habboGame.achievementManager.progress(
                habboSession,
                "ACH_LTDPurchaser",
                1,
                accumulate = true
            )

            // pelo número que ESTE comprador recebeu na reserva, nunca pelo contador vivo (outro comprador já pode ter avançado)
            if (isLimitedEarlyBird(limitedNumbers, catalogItem.limitedTotal)) {
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_LTDEarlyBird",
                    1,
                    accumulate = true
                )
            }
        }
    }
}
