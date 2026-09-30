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
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.habbicon.HabbiconDao
import java.net.URI

@JsonIgnoreProperties(ignoreUnknown = true)
private data class HabbiconAssetFile(val habbicons: List<HabbiconAsset> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
private data class HabbiconAsset(val id: Int, val name: String? = null)

object HabbiconAssetSynchronizer {
    private val log = LoggerFactory.getLogger(javaClass)
    private val jsonMapper = jacksonObjectMapper()

    fun syncAssets() {
        runCatching {
            val variables = URI.create(HabboServer.habboConfig.externalVariablesTxt)
                .toURL()
                .openStream()
                .bufferedReader()
                .useLines { lines ->
                    lines.mapNotNull { line ->
                        val separator = line.indexOf('=')
                        if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
                    }.toMap()
                }

            val hash = variables["habbicons.hash"]?.takeIf { it.isNotBlank() }
                ?: error("habbicons.hash was not found in external variables")
            val assetRoot = (variables["habbicons.url"] ?: "https://images.habbo.com/habbicons").trimEnd('/')
            val metadataUrl = URI.create("$assetRoot/$hash/habbicons.json").toURL()

            val assets: HabbiconAssetFile = metadataUrl.openStream().bufferedReader().use {
                jsonMapper.readValue(it.readText())
            }

            val grouped = assets.habbicons
                .filter { it.id > 0 && !it.name.isNullOrBlank() }
                .groupBy { it.name!!.substringBefore('_') }

            grouped.forEach { (collectionName, entries) ->
                val collectionId = HabbiconDao.getOrCreateCollection(collectionName)
                val rewardId = HabbiconDao.getCollectionRewardId(collectionId)
                    ?: entries.random().id.also { HabbiconDao.setCollectionReward(collectionId, it) }

                entries.forEach { asset ->
                    HabbiconDao.upsertHabbicon(
                        id = asset.id,
                        collectionId = collectionId,
                        name = asset.name!!,
                        purchasable = asset.id != rewardId
                    )
                }
            }

            log.info("Synchronized {} Habbicons from {}", grouped.values.sumOf { it.size }, metadataUrl)
        }.onFailure { error ->
            log.warn("Could not synchronize Habbicons from external variables", error)
        }
    }
}
