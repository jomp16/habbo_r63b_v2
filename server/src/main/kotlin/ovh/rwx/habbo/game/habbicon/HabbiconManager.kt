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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.habbicon.HabbiconStatusData
import ovh.rwx.habbo.communication.outgoing.habbicon.RoomUseHabbiconData
import ovh.rwx.habbo.communication.outgoing.habbicon.UserHabbiconsData
import ovh.rwx.habbo.communication.outgoing.inventory.UnseenItemCategory
import ovh.rwx.habbo.communication.outgoing.inventory.UnseenItemsData
import ovh.rwx.habbo.communication.outgoing.messenger.MessengerChatData
import ovh.rwx.habbo.database.habbicon.HabbiconDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType
import java.util.concurrent.ConcurrentHashMap

data class HabbiconChatContent(val habbiconId: Int)

class HabbiconManager {
    private val log = LoggerFactory.getLogger(javaClass)

    val collections: ConcurrentHashMap<Int, HabbiconCollection> = ConcurrentHashMap()
    val habbicons: ConcurrentHashMap<Int, Habbicon> = ConcurrentHashMap()

    fun load() {
        HabbiconAssetSynchronizer.syncAssets()
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

        habboSession.sendHabboResponse(
            Outgoing.USER_HABBICON_STATUS_CHANGED,
            HabbiconStatusData(habbiconId, UserHabbicon.STATE_OWNED)
        )
        habboSession.sendResponse(
            Outgoing.INVENTORY_UNSEEN_ITEMS,
            OutgoingR63A.INVENTORY_UNSEEN_ITEMS,
            UnseenItemsData.single(UnseenItemCategory.HABBICON, listOf(habbiconId))
        )
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
            UserHabbiconsData(
                userHabbicons = habboSession.habboHabbicon.getUserHabbiconList(),
                recentHabbiconIds = habboSession.habboHabbicon.recentHabbiconIds,
            ),
        )
        if (newHabbiconIds.isNotEmpty()) {
            habboSession.sendResponse(
                Outgoing.INVENTORY_UNSEEN_ITEMS,
                OutgoingR63A.INVENTORY_UNSEEN_ITEMS,
                UnseenItemsData.single(UnseenItemCategory.HABBICON, newHabbiconIds)
            )
        }

        return true
    }

    fun claimHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val habbicon = habbicons[habbiconId] ?: return false
        val collection = collections[habbicon.collectionId] ?: return false

        if (collection.rewardHabbiconId != habbiconId) return false

        val userHabbicons = habboSession.habboHabbicon.userHabbicons
        if (userHabbicons.containsKey(habbiconId)) return false

        val targetState = if (collection.rewardState in listOf(UserHabbicon.STATE_OWNED, UserHabbicon.STATE_FAVORITE)) {
            collection.rewardState
        } else {
            UserHabbicon.STATE_OWNED
        }

        val id = HabbiconDao.addUserHabbicon(habboSession.userInformation.id, habbiconId, targetState)
        val userHabbicon = UserHabbicon(id, habboSession.userInformation.id, habbiconId, targetState)
        habboSession.habboHabbicon.userHabbicons[habbiconId] = userHabbicon

        habboSession.sendHabboResponse(
            Outgoing.USER_HABBICON_STATUS_CHANGED,
            HabbiconStatusData(habbiconId, targetState)
        )
        habboSession.sendResponse(
            Outgoing.INVENTORY_UNSEEN_ITEMS,
            OutgoingR63A.INVENTORY_UNSEEN_ITEMS,
            UnseenItemsData.single(UnseenItemCategory.HABBICON, listOf(habbiconId))
        )
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

        habboSession.sendHabboResponse(
            Outgoing.USER_HABBICON_STATUS_CHANGED,
            HabbiconStatusData(habbiconId, UserHabbicon.STATE_FAVORITE)
        )

        return true
    }

    fun unfavoriteHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val userHabbicon = habboSession.habboHabbicon.userHabbicons[habbiconId] ?: return false
        if (userHabbicon.state != UserHabbicon.STATE_FAVORITE) return false

        HabbiconDao.updateUserHabbiconState(habboSession.userInformation.id, habbiconId, UserHabbicon.STATE_OWNED)
        val updatedUserHabbicon = userHabbicon.copy(state = UserHabbicon.STATE_OWNED)
        habboSession.habboHabbicon.userHabbicons[habbiconId] = updatedUserHabbicon

        habboSession.sendHabboResponse(
            Outgoing.USER_HABBICON_STATUS_CHANGED,
            HabbiconStatusData(habbiconId, UserHabbicon.STATE_OWNED)
        )

        return true
    }

    fun triggerHabbicon(habboSession: HabboSession, habbiconId: Int): Boolean {
        val userHabbicon = habboSession.habboHabbicon.userHabbicons[habbiconId] ?: return false
        if (userHabbicon.state != UserHabbicon.STATE_OWNED && userHabbicon.state != UserHabbicon.STATE_FAVORITE) return false

        val roomUser = habboSession.roomUser
        if (roomUser != null) {
            habboSession.roomUser?.idle = false
            habboSession.currentRoom?.sendHabboResponse(
                Outgoing.ROOM_USE_HABBICON,
                RoomUseHabbiconData(roomUser.virtualID, habbiconId)
            )

            HabboServer.applicationScope.launch(Dispatchers.IO) {
                recordRecentUse(habboSession, habbiconId)

                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_HabbiconUsed",
                    1,
                    accumulate = true
                )
            }
        }

        return true
    }

    fun sendHabbicon(habboSession: HabboSession, recipientId: Int, habbiconId: Int): Boolean {
        val userHabbicon = habboSession.habboHabbicon.userHabbicons[habbiconId] ?: return false
        if (userHabbicon.state != UserHabbicon.STATE_OWNED && userHabbicon.state != UserHabbicon.STATE_FAVORITE) return false

        if (!habboSession.habboMessenger.friends.containsKey(recipientId)) return false
        val recipientSession = HabboServer.habboSessionManager.getHabboSessionById(recipientId) ?: return false

        recipientSession.sendHabboResponse(
            Outgoing.MESSENGER_CHAT,
            MessengerChatData(
                id = habboSession.userInformation.id,
                message = HabbiconChatContent(habbiconId),
                diffTimestamp = 0,
                userId = habboSession.userInformation.id,
                username = habboSession.userInformation.username,
                figure = habboSession.userInformation.figure
            )
        )

        HabboServer.applicationScope.launch(Dispatchers.IO) {
            recordRecentUse(habboSession, habbiconId)

            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_HabbiconUsed", 1, accumulate = true)
        }

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
