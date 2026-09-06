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
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.habbicon.HabbiconDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType
import java.net.URI

class HabbiconManager {
    private val log = LoggerFactory.getLogger(javaClass)

    val collections: MutableMap<Int, HabbiconCollection> = mutableMapOf()
    val habbicons: MutableMap<Int, Habbicon> = mutableMapOf()

    fun load() {
        syncAssets()
        collections.clear()
        habbicons.clear()

        val loadedCollections = HabbiconDao.getCollections()
        val loadedHabbicons = HabbiconDao.getHabbicons()

        loadedCollections.forEach { collections[it.id] = it }
        loadedHabbicons.forEach {
            habbicons[it.id] = it
            collections[it.collectionId]?.habbicons?.add(it)
        }

        log.info("Loaded {} habbicon collections and {} habbicons", collections.size, habbicons.size)
    }

    private fun syncAssets() {
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
                jacksonObjectMapper().readValue(it.readText())
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

    fun getCollection(collectionId: Int): HabbiconCollection? = collections[collectionId]
    fun getHabbicon(habbiconId: Int): Habbicon? = habbicons[habbiconId]

    fun buyHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val habbicon = habbicons[habbiconId] ?: return false
        if (!habbicon.enabled || !habbicon.purchasable) return false
        if (collections[habbicon.collectionId]?.enabled != true) return false

        val userHabbicons = habboSession.habboHabbicon.userHabbicons
        if (userHabbicons.containsKey(habbiconId)) return false

        if (!canAfford(habboSession, habbicon.priceCredits, habbicon.priceActivityPoints, habbicon.activityPointType)) {
            return false
        }

        deductCurrency(habboSession, habbicon.priceCredits, habbicon.priceActivityPoints, habbicon.activityPointType)

        val id = HabbiconDao.addUserHabbicon(habboSession.userInformation.id, habbiconId, UserHabbicon.STATE_OWNED)
        val userHabbicon = UserHabbicon(id, habboSession.userInformation.id, habbiconId, UserHabbicon.STATE_OWNED)
        habboSession.habboHabbicon.userHabbicons[habbiconId] = userHabbicon

        habboSession.sendHabboResponse(Outgoing.USER_HABBICON_STATUS_CHANGED, habbiconId, UserHabbicon.STATE_OWNED)
        habboSession.sendHabboResponse(Outgoing.INVENTORY_UNSEEN_ITEMS, true, 8, listOf(habbiconId))
        progressCollectedAchievement(habboSession)

        return true
    }

    fun buyHabbiconCollection(habboSession: HabboSession, collectionId: Int): Boolean {
        val collection = collections[collectionId] ?: return false
        if (!collection.enabled) return false

        val userHabbicons = habboSession.habboHabbicon.userHabbicons
        val habbiconsToBuy =
            collection.habbicons.filter { !userHabbicons.containsKey(it.id) && it.enabled && it.purchasable }

        if (habbiconsToBuy.isEmpty()) return false

        if (!canAfford(
                habboSession,
                collection.priceCredits,
                collection.priceActivityPoints,
                collection.activityPointType
            )
        ) return false

        deductCurrency(
            habboSession,
            collection.priceCredits,
            collection.priceActivityPoints,
            collection.activityPointType
        )

        val newHabbiconIds = mutableListOf<Int>()
        for ((habbiconId) in habbiconsToBuy) {
            val id = HabbiconDao.addUserHabbicon(habboSession.userInformation.id, habbiconId, UserHabbicon.STATE_OWNED)
            val userHabbicon = UserHabbicon(id, habboSession.userInformation.id, habbiconId, UserHabbicon.STATE_OWNED)
            habboSession.habboHabbicon.userHabbicons[habbiconId] = userHabbicon
            newHabbiconIds.add(habbiconId)
            progressCollectedAchievement(habboSession)
        }

        habboSession.sendHabboResponse(
            Outgoing.USER_HABBICONS,
            habboSession.habboHabbicon.getUserHabbiconList(),
            habboSession.habboHabbicon.recentHabbiconIds
        )
        newHabbiconIds.forEach { habbiconId ->
            habboSession.sendHabboResponse(Outgoing.INVENTORY_UNSEEN_ITEMS, true, 8, listOf(habbiconId))
        }

        return true
    }

    fun claimHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val habbicon = habbicons[habbiconId] ?: return false
        val collection = collections[habbicon.collectionId] ?: return false

        if (collection.rewardHabbiconId != habbiconId) return false

        val userHabbicons = habboSession.habboHabbicon.userHabbicons
        if (userHabbicons.containsKey(habbiconId)) return false

        val collectionHabbicons = collection.habbicons.filter { it.id != collection.rewardHabbiconId && it.enabled }
        val ownedCount = collectionHabbicons.count { userHabbicons.containsKey(it.id) }

        if (ownedCount < collectionHabbicons.size) return false

        val id = HabbiconDao.addUserHabbicon(habboSession.userInformation.id, habbiconId, collection.rewardState)
        val userHabbicon = UserHabbicon(id, habboSession.userInformation.id, habbiconId, collection.rewardState)
        habboSession.habboHabbicon.userHabbicons[habbiconId] = userHabbicon

        habboSession.sendHabboResponse(Outgoing.USER_HABBICON_STATUS_CHANGED, habbiconId, collection.rewardState)
        habboSession.sendHabboResponse(Outgoing.INVENTORY_UNSEEN_ITEMS, true, 8, listOf(habbiconId))
        progressCollectedAchievement(habboSession)

        HabboServer.habboGame.achievementManager.progress(
            habboSession,
            "ACH_HabbiconCollectionComp",
            1,
            accumulate = true
        )

        return true
    }

    fun favoriteHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val userHabbicon = habboSession.habboHabbicon.userHabbicons[habbiconId] ?: return false
        if (userHabbicon.state != UserHabbicon.STATE_OWNED) return false

        HabbiconDao.updateUserHabbiconState(habboSession.userInformation.id, habbiconId, UserHabbicon.STATE_FAVORITE)
        val updatedUserHabbicon = userHabbicon.copy(state = UserHabbicon.STATE_FAVORITE)
        habboSession.habboHabbicon.userHabbicons[habbiconId] = updatedUserHabbicon

        habboSession.sendHabboResponse(Outgoing.USER_HABBICON_STATUS_CHANGED, habbiconId, UserHabbicon.STATE_FAVORITE)

        return true
    }

    fun unfavoriteHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val userHabbicon = habboSession.habboHabbicon.userHabbicons[habbiconId] ?: return false
        if (userHabbicon.state != UserHabbicon.STATE_FAVORITE) return false

        HabbiconDao.updateUserHabbiconState(habboSession.userInformation.id, habbiconId, UserHabbicon.STATE_OWNED)
        val updatedUserHabbicon = userHabbicon.copy(state = UserHabbicon.STATE_OWNED)
        habboSession.habboHabbicon.userHabbicons[habbiconId] = updatedUserHabbicon

        habboSession.sendHabboResponse(Outgoing.USER_HABBICON_STATUS_CHANGED, habbiconId, UserHabbicon.STATE_OWNED)

        return true
    }

    fun triggerHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val userHabbicon = habboSession.habboHabbicon.userHabbicons[habbiconId] ?: return false
        if (userHabbicon.state != UserHabbicon.STATE_OWNED && userHabbicon.state != UserHabbicon.STATE_FAVORITE) return false

        recordRecentUse(habboSession, habbiconId)

        HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_HabbiconUsed", 1, accumulate = true)

        val roomUser = habboSession.roomUser
        if (roomUser != null) {
            habboSession.roomUser?.idle = false
            habboSession.currentRoom?.sendHabboResponse(Outgoing.ROOM_USE_HABBICON, roomUser.virtualID, habbiconId)
        }

        return true
    }

    fun sendHabbicon(habboSession: HabboSession, recipientId: Int, habbiconId: Int): Boolean {
        val userHabbicon = habboSession.habboHabbicon.userHabbicons[habbiconId] ?: return false
        if (userHabbicon.state != UserHabbicon.STATE_OWNED && userHabbicon.state != UserHabbicon.STATE_FAVORITE) return false

        if (!habboSession.habboMessenger.friends.containsKey(recipientId)) return false
        val recipientSession = HabboServer.habboSessionManager.getHabboSessionById(recipientId) ?: return false

        recordRecentUse(habboSession, habbiconId)
        HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_HabbiconUsed", 1, accumulate = true)

        recipientSession.sendHabboResponse(
            Outgoing.MESSENGER_CHAT,
            habboSession.userInformation.id,
            HabbiconChatContent(habbiconId),
            0,
            habboSession.userInformation.id,
            habboSession.userInformation.username,
            habboSession.userInformation.figure
        )

        return true
    }

    private fun recordRecentUse(habboSession: HabboSession, habbiconId: Int) {
        val recentIds = habboSession.habboHabbicon.recentHabbiconIds.toMutableList()
        recentIds.remove(habbiconId)
        recentIds.add(0, habbiconId)
        while (recentIds.size > 10) recentIds.removeAt(recentIds.size - 1)

        habboSession.habboHabbicon.recentHabbiconIds = recentIds
        HabbiconDao.updateUserHabbiconRecentIds(habboSession.userInformation.id, habbiconId, recentIds)
    }

    private fun progressCollectedAchievement(habboSession: HabboSession) {
        HabboServer.habboGame.achievementManager.progress(
            habboSession,
            "ACH_HabbiconCollected",
            habboSession.habboHabbicon.userHabbicons.size,
            accumulate = false
        )
    }

    private fun canAfford(
        habboSession: HabboSession,
        priceCredits: Int,
        priceActivityPoints: Int,
        activityPointType: Int
    ): Boolean {
        if (priceCredits > habboSession.userInformation.credits) return false

        if (priceActivityPoints > 0) {
            val pointType = ActivityPointType.fromType(activityPointType) ?: ActivityPointType.PIXELS
            val currentPoints = habboSession.userInformation.activityPointsCurrencies.getOrDefault(pointType, 0)
            if (priceActivityPoints > currentPoints) return false
        }

        return true
    }

    private fun deductCurrency(
        habboSession: HabboSession,
        priceCredits: Int,
        priceActivityPoints: Int,
        activityPointType: Int
    ) {
        if (priceCredits > 0) {
            habboSession.userInformation.credits -= priceCredits
        }

        if (priceActivityPoints > 0) {
            val pointType = ActivityPointType.fromType(activityPointType) ?: ActivityPointType.PIXELS
            habboSession.userInformation.activityPointsCurrencies.merge(pointType, -priceActivityPoints, Int::plus)
        }

        habboSession.updateAllCurrencies()
    }

}

data class HabbiconChatContent(val habbiconId: Int)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class HabbiconAssetFile(val habbicons: List<HabbiconAsset> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
private data class HabbiconAsset(val id: Int, val name: String? = null)
