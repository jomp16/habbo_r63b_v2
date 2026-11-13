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
import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.game.catalog.gift.GiftRules
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.currency.ActivityPointTypes
import ovh.rwx.habbo.game.currency.Price
import java.util.concurrent.atomic.AtomicInteger

data class CatalogItem(
    val id: Int,
    val pageId: Int,
    val itemName: String,
    val orderNum: Int,
    val dealId: Int,
    val catalogName: String,
    val badge: String,
    val costCredits: Int,
    /** Quantidade da moeda alternativa (o client lê UMA por oferta); o tipo está em [pointType]. */
    val pricePoints: Int,
    val pointType: Int,
    val amount: Int,
    val clubOnly: Boolean,
    val limitedSells: AtomicInteger,
    val limitedTotal: Int,
    val offerActive: Boolean,
    val extraData: Map<String, Any>
) : IHabboResponseSerialize {
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[itemName]!!
    val deal: CatalogDeal?
        get() = if (dealId > 0) HabboServer.habboGame.catalogManager.getDeal(dealId) else null

    /** Os produtos do pacote na ordem da oferta; null se a oferta não é um pacote (vazio = pacote sem mobi válido). */
    val dealProducts: List<CatalogDealProduct>?
        get() = if (dealId > 0) deal?.products.orEmpty() else null

    /** O nome da oferta no client: o `catalog_name`; em branco, o nome do pacote ou o classname do mobi. */
    val offerName: String
        get() = catalogName.ifBlank { if (dealId > 0) deal?.name.orEmpty() else furnishing.itemName }
    val limited = limitedTotal > 0

    /** Regra de presente (ver [GiftRules]): só mobi, só créditos, `allow_gift` em todos os produtos. */
    val isGiftable: Boolean
        get() = GiftRules.isGiftable(
            offerActive,
            clubOnly,
            pricePoints,
            badge,
            HabboServer.habboGame.catalogManager.getPage(pageId)?.pageLayout?.startsWith("pets") == true,
            dealProducts?.map { it.furnishing } ?: listOf(furnishing)
        )

    val price = Price(costCredits, pointType, pricePoints)

    /** Trava por oferta LTD (só é usada em [withLimitedReservation]; ofertas comuns nunca a pegam). */
    private val limitedLock = Any()

    /**
     * Seção crítica da numeração LTD, sem buracos e sem repetição: com o lock desta oferta, confere o estoque
     * (`limitedSells + count <= limitedTotal`) e chama [block] com os números `atual+1..atual+count`. O contador só
     * avança (para `atual+count`) se [block] devolver true; se devolver false ou lançar, não avança e o próximo
     * comprador recebe os mesmos números. [block] roda com o lock preso (inclui o insert síncrono dos itens).
     */
    fun withLimitedReservation(count: Int, block: (IntRange) -> Boolean): LimitedOutcome = synchronized(limitedLock) {
        val current = limitedSells.get()
        if (!limited || count <= 0 || current.toLong() + count > limitedTotal) return@synchronized LimitedOutcome.SOLD_OUT

        if (!block((current + 1)..(current + count))) return@synchronized LimitedOutcome.DECLINED

        limitedSells.set(current + count)
        LimitedOutcome.COMMITTED
    }

    /**
     * A oferta só existe para clients que entendem a moeda dela (senão o preço sairia com um tipo desconhecido) e,
     * sendo um pacote, enquanto ele tiver ao menos um mobi válido (senão o pacote sairia vazio ou não sairia).
     */
    fun isAvailableFor(version: HabboVersion): Boolean =
        (pricePoints <= 0 || ActivityPointTypes.isSupported(version, pointType)) && dealProducts?.isEmpty() != true

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        val habboAir = params.isNotEmpty() && params[0] as? Boolean == true
        val wire = CatalogOfferWire(habboResponse.habboVersion)

        habboResponse.apply {
            writeInt(id) // offerId
            writeUTF(offerName) // localizationId
            if (isVersionAtLeast(2013, 2, 26)) {
                writeBoolean(false) // isRent
            }
            writeInt(costCredits) // priceInCredits
            writeInt(pricePoints) // priceInActivityPoints
            writeInt(pointType) // activityPointType

            if (isVersionAtLeast(2024, 1, 22)) {
                writeInt(0) // priceInSilver (moeda à parte, ainda sem saldo/cobrança no emulador)
            }

            // cada campo só existe a partir da build que o introduz (ver CatalogOfferWire: um byte a mais desalinha tudo)
            if (wire.giftable) {
                writeBoolean(isGiftable) // giftable
            }

            // products count and array
            val products = dealProducts
            val badgeProduct = badge.isNotBlank() && wire.badgeProduct
            writeInt((products?.size ?: 1) + if (badgeProduct) 1 else 0)

            if (badgeProduct) {
                writeUTF("b")
                writeUTF(badge)
            }

            if (products != null) {
                products.forEach { serializeItem(habboResponse, it.furnishing, it.amount, wire) }
            } else {
                serializeItem(habboResponse, furnishing, amount, wire)
            }

            // Campos que vêm APÓS o array de produtos
            writeInt(if (clubOnly) 1 else 0) // clubLevel
            if (wire.bundlePurchaseAllowed) {
                writeBoolean(offerActive && !limited) // bundlePurchaseAllowed
            }

            // isPet + previewImage nasceram em 2016-07-26 (CatalogPageMessageOfferData);
            // antes o offer termina no bundlePurchaseAllowed — campos extras desalinham o stream
            if (isVersionAtLeast(2016, 7, 26)) {
                writeBoolean(false) // isPet
                writeUTF(extraData.getOrDefault("preview_image", "") as String) // previewImage
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id) // offerId
            writeUTF(offerName) // localizationId
            writeInt(costCredits) // priceInCredits
            writeInt(pricePoints) // priceInActivityPoints

            if (isVersionAtLeast(2010, 12, 3)) {
                writeInt(pointType) // activityPointType
            }

            // Products: offer ← LOOP[product]; product = productType, furniClassId, extraParam, productCount, expiration
            // (idêntico de RELEASE34 a RELEASE63-29876; deals carregam vários produtos)
            val products = dealProducts
            if (products != null) {
                writeInt(products.size)
                products.forEach { serializeProductR63A(it.furnishing, it.amount) }
            } else {
                writeInt(1)
                serializeProductR63A(furnishing, amount)
            }

            if (isVersionAtLeast(2011, 6, 1)) {
                writeInt(if (clubOnly) 1 else 0) // clubLevel
            }
        }
    }

    private fun serializeItem(
        habboResponse: HabboResponse,
        furnishing: Furnishing,
        amount: Int,
        wire: CatalogOfferWire
    ) {
        habboResponse.apply {
            writeUTF(furnishing.type.type) // productType

            if (furnishing.type == ItemType.BADGE) {
                // Parser AS3 (todas as eras): case "b" lê UMA string e retorna — sem Int de count
                writeUTF(furnishing.itemName) // extraParam for badge
            } else {
                writeInt(furnishing.spriteId) // furniClassId
                writeUTF(
                    if (itemName == "wallpaper" || itemName == "floor" || itemName == "landscape") catalogName.split(
                        '_'
                    )[2] else ""
                ) // extraParam
                writeInt(amount) // productCount
                if (wire.productExpiration) {
                    writeInt(-1) // expiration (do início do protocolo até 2013-01-29)
                }
                // isUnique e a série limitada só existem a partir de 2012-03-20; antes o produto termina na expiração
                if (wire.productUniqueFlag) {
                    writeBoolean(limited) // isUnique (limited)

                    if (limited) {
                        writeInt(limitedTotal) // uniqueLimitedItemSeriesSize
                        writeInt(limitedTotal - limitedSells.get()) // uniqueLimitedItemsLeft
                    }
                }
            }
        }
    }

    private fun HabboResponse.serializeProductR63A(furnishing: Furnishing, amount: Int) {
        writeUTF(furnishing.type.type) // productType ('i' item, 's' wall item, 'e' effect)
        writeInt(furnishing.spriteId) // furniClassId
        if (itemName == "wallpaper" || itemName == "floor" || itemName == "landscape")
            writeUTF(catalogName.split('_')[2]) // extraParam
        else
            writeUTF("") // extraParam
        writeInt(amount) // productCount
        writeInt(-1) // expiration
    }
}

/** Resultado de [CatalogItem.withLimitedReservation]: o contador só avança em [COMMITTED]. */
enum class LimitedOutcome { COMMITTED, SOLD_OUT, DECLINED }
