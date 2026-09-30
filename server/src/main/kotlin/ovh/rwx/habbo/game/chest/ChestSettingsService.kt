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

package ovh.rwx.habbo.game.chest

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.chest.ChestPreferencesUpdateSuccessData
import ovh.rwx.habbo.communication.outgoing.chest.ChestUpgradeResult
import ovh.rwx.habbo.communication.outgoing.chest.ChestUpgradeResultData
import ovh.rwx.habbo.database.chest.ChestDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType

class ChestSettingsService(private val chestManager: ChestManager) {

    fun lockAll(habboSession: HabboSession, room: Room, lock: Boolean) {
        if (!room.userManager.hasRights(habboSession)) return

        room.itemManager.items.values
            .filter { it.furnishing.interactionType == InteractionType.CHEST }
            .forEach { roomItem ->
                val chest = chestManager.getOrCreateChest(roomItem) ?: return@forEach

                // Apenas o dono do baú pode destrancar
                if (lock || chest.userId == habboSession.userInformation.id) {
                    chest.locked = lock
                    ChestDao.updateChest(chest)

                    ChestVisuals.updateChestExtraData(room, chest)
                }
            }
    }

    fun setOptions(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        locked: Boolean,
        autoLock: Boolean,
        capacity: Int
    ) {
        if (!ChestPermissions.canEdit(habboSession, room, chest)) return

        // Apenas o dono do baú pode destrancar
        if (!locked || chest.userId == habboSession.userInformation.id) chest.locked = locked

        chest.autoLock = autoLock
        chest.capacity = capacity.coerceIn(0, chest.maxCapacity)

        ChestDao.updateChest(chest)
        ChestVisuals.updateChestExtraData(room, chest)

        habboSession.sendHabboResponse(
            Outgoing.CHEST_PREFERENCES_UPDATE_SUCCESS,
            ChestPreferencesUpdateSuccessData(chestItemId = chest.itemId, isNotificationPreferences = false)
        )
    }

    fun setPreferences(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        name: String,
        description: String,
        anyoneCanOpen: Boolean,
        anyoneCanDonate: Boolean,
        stateMode: Int,
        previewMode: Int,
        previewAmount: Int,
    ) {
        if (!ChestPermissions.canEdit(habboSession, room, chest)) return

        chest.name = name.take(64)
        chest.description = description.take(128)
        chest.anyoneCanOpen = anyoneCanOpen
        chest.anyoneCanDonate = anyoneCanDonate
        chest.stateMode = stateMode.coerceIn(0, 3)
        chest.previewMode = previewMode.coerceIn(0, 7)
        chest.previewAmount = previewAmount.coerceIn(1, 4)

        ChestDao.updateChest(chest)
        ChestVisuals.updateChestExtraData(room, chest)

        habboSession.sendHabboResponse(
            Outgoing.CHEST_PREFERENCES_UPDATE_SUCCESS,
            ChestPreferencesUpdateSuccessData(chestItemId = chest.itemId, isNotificationPreferences = false)
        )
    }

    fun setNotificationPreferences(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        notificationMode: Int,
        notifyFull: Boolean,
        notifyDonation: Boolean,
        notifyWithdraw: Boolean,
        notifyEmpty: Boolean,
        notifyTransaction: Boolean,
    ) {
        if (chest.userId != habboSession.userInformation.id) return

        chest.notificationMode = notificationMode.coerceIn(0, 1)
        chest.notifyFull = notifyFull
        chest.notifyDonation = notifyDonation
        chest.notifyWithdraw = notifyWithdraw
        chest.notifyEmpty = notifyEmpty
        chest.notifyTransaction = notifyTransaction

        ChestDao.updateChest(chest)

        habboSession.sendHabboResponse(
            Outgoing.CHEST_PREFERENCES_UPDATE_SUCCESS,
            ChestPreferencesUpdateSuccessData(chestItemId = chest.itemId, isNotificationPreferences = true)
        )
    }

    fun upgrade(habboSession: HabboSession, room: Room, chest: ChestData, currencyType: Int) {
        val roomItem = chestManager.getRoomItem(room, chest.itemId)

        if (roomItem == null) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_UPGRADE_RESULT,
                ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.CANNOT_UPGRADE)
            )

            return
        }

        if (chest.isStarter) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_UPGRADE_RESULT,
                ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.NOT_OWNER)
            )

            return
        }

        if (chest.userId != habboSession.userInformation.id) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_UPGRADE_RESULT,
                ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.CHEST_LOCKED)
            )

            return
        }

        val maxCap = chest.maxCapacity

        if (chest.capacity >= maxCap) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_UPGRADE_RESULT,
                ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.NOT_ENOUGH_CREDITS)
            )

            return
        }

        // Upgrade: 10 da moeda escolhida (credits = 0, diamonds = 5)
        val activityPointType = if (currencyType == ActivityPointType.DIAMONDS.code) ActivityPointType.DIAMONDS else null
        val cost = ChestConstants.UPGRADE_COST

        if (activityPointType != null) {
            val balance = habboSession.userInformation.activityPointsCurrencies.getOrDefault(activityPointType, 0)

            if (balance < cost) {
                habboSession.sendHabboResponse(
                    Outgoing.CHEST_UPGRADE_RESULT,
                    ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.MAX_CAPACITY_REACHED)
                )

                return
            }

            habboSession.userInformation.activityPointsCurrencies.merge(activityPointType, -cost, Int::plus)
        } else {
            if (habboSession.userInformation.credits < cost) {
                habboSession.sendHabboResponse(
                    Outgoing.CHEST_UPGRADE_RESULT,
                    ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.INSUFFICIENT_VIP)
                )

                return
            }

            habboSession.userInformation.credits -= cost
        }

        val capacityStep = if (chest.type == ChestType.COINS) ChestConstants.COINS_CAPACITY_STEP else ChestConstants.FURNI_CAPACITY_STEP

        chest.capacity = (chest.capacity + capacityStep).coerceAtMost(maxCap)

        ChestDao.updateChest(chest)
        ChestVisuals.updateChestExtraData(room, chest)
        ChestNotifier.sendCurrencyBalances(habboSession)

        habboSession.sendHabboResponse(
            Outgoing.CHEST_UPGRADE_RESULT,
            ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.SUCCESS)
        )
    }

    fun upgradeWired(habboSession: HabboSession, room: Room, chest: ChestData) {
        if (chest.isStarter) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_UPGRADE_RESULT,
                ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.NOT_OWNER)
            )

            return
        }

        if (chest.userId != habboSession.userInformation.id) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_UPGRADE_RESULT,
                ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.CHEST_LOCKED)
            )

            return
        }

        if (chest.isWired) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_UPGRADE_RESULT,
                ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.SUCCESS)
            )

            return
        }

        // Upgrade para Baú Wired é gratuito (Central de Informações)
        chest.isWired = true

        ChestDao.updateChest(chest)
        ChestVisuals.updateChestExtraData(room, chest)

        habboSession.sendHabboResponse(
            Outgoing.CHEST_UPGRADE_RESULT,
            ChestUpgradeResultData(chest.itemId, ChestUpgradeResult.SUCCESS)
        )
    }
}
