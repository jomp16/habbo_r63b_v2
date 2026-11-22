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

package ovh.rwx.habbo.game.habbicon

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.habbicon.HabbiconDao
import ovh.rwx.habbo.database.habbicon.HabbiconUpsertRow
import ovh.rwx.habbo.game.gamedata.ExternalVariables
import java.net.URI

@JsonIgnoreProperties(ignoreUnknown = true)
private data class HabbiconAssetFile(val habbicons: List<HabbiconAsset> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
private data class HabbiconAsset(val id: Int, val name: String? = null)

object HabbiconAssetSynchronizer {
    private val log = LoggerFactory.getLogger(javaClass)
    private val jsonMapper = jacksonObjectMapper()

    /** Cada habbicon comprável custa 1 crédito: a coleção inteira sai com [COLLECTION_DISCOUNT] de desconto. */
    private const val COLLECTION_DISCOUNT = 0.2

    /** Preço (créditos) de uma coleção com [purchasableCount] habbicons compráveis; nunca abaixo de 1. */
    internal fun collectionPrice(purchasableCount: Int): Int =
        Math.round(purchasableCount * (1 - COLLECTION_DISCOUNT)).toInt().coerceAtLeast(1)

    fun syncAssets() {
        runCatching {
            val hash = ExternalVariables.require(ExternalVariables.HABBICONS_HASH)
            val assetRoot = ExternalVariables.require(ExternalVariables.HABBICONS_URL).trimEnd('/')
            val metadataUrl = URI.create("$assetRoot/$hash/habbicons.json").toURL()

            val assets: HabbiconAssetFile = metadataUrl.openStream().bufferedReader().use {
                jsonMapper.readValue(it.readText())
            }

            val grouped = assets.habbicons
                .filter { it.id > 0 && !it.name.isNullOrBlank() }
                .groupBy { it.name!!.substringBefore('_') }

            // Uma consulta para todas as coleções e um lote para todos os habbicons: cada ida ao banco custa ~125 ms
            // com banco remoto, e isto roda a cada boot, dentro do carregamento do jogo (antes: ~60 idas em série)
            val knownCollections = HabbiconDao.getCollectionRefs().associateBy { it.name }
            val rows = ArrayList<HabbiconUpsertRow>()
            val collectionPrices = HashMap<Int, Int>()

            grouped.forEach { (collectionName, entries) ->
                val known = knownCollections[collectionName]
                val collectionId = known?.id ?: HabbiconDao.getOrCreateCollection(collectionName)
                val rewardId = known?.rewardHabbiconId
                    ?: entries.random().id.also { HabbiconDao.setCollectionReward(collectionId, it) }

                collectionPrices[collectionId] = collectionPrice(entries.count { it.id != rewardId })

                entries.mapTo(rows) { asset ->
                    HabbiconUpsertRow(
                        id = asset.id,
                        collectionId = collectionId,
                        name = asset.name!!,
                        purchasable = asset.id != rewardId
                    )
                }
            }

            HabbiconDao.upsertHabbicons(rows)
            HabbiconDao.setDefaultCollectionPrices(collectionPrices)

            log.info("Synchronized {} Habbicons from {}", grouped.values.sumOf { it.size }, metadataUrl)
        }.onFailure { error ->
            log.warn("Could not synchronize Habbicons from external variables", error)
        }
    }
}
