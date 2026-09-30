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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession

object ChestNotifier {

    /**
     * Notificação transacional para o DONO quando outra pessoa deposita
     * (success.0 = "Depósito do baú realizado", do ponto de vista do baú).
     */
    fun notifyOwnerTransaction(chest: ChestData) {
        if (!chest.notifyTransaction) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)
            ?.sendHabboResponse(Outgoing.WIRED_TRANSACTION_SUCCESS, WiredTransactionNotification.CHEST_DEPOSITED)
    }

    fun notifyDonation(chest: ChestData, username: String) {
        if (!chest.notifyDonation) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendSuperNotification(
            MiscSuperNotificationResponse.MiscSuperNotificationKeys.WIRED_CHESTS_DONATION,
            mapOf("user_name" to username, "chest_name" to chest.name.ifBlank { "\${wiredchests.furni_chest}" })
        )
    }

    fun notifyWithdraw(chest: ChestData, username: String) {
        if (!chest.notifyWithdraw) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendSuperNotification(
            MiscSuperNotificationResponse.MiscSuperNotificationKeys.WIRED_CHESTS_SOMEONE_WITHDRAWS,
            mapOf("user_name" to username, "chest_name" to chest.name.ifBlank { "\${wiredchests.furni_chest}" })
        )
    }

    fun notifyFull(chest: ChestData) {
        if (!chest.notifyFull) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendSuperNotification(
            MiscSuperNotificationResponse.MiscSuperNotificationKeys.WIRED_CHESTS_CHEST_FULL,
            mapOf("chest_name" to chest.name.ifBlank { "\${wiredchests.furni_chest}" })
        )
    }

    fun notifyEmpty(chest: ChestData) {
        if (!chest.notifyEmpty) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendSuperNotification(
            MiscSuperNotificationResponse.MiscSuperNotificationKeys.WIRED_CHESTS_CHEST_EMPTY,
            mapOf("chest_name" to chest.name.ifBlank { "\${wiredchests.furni_chest}" })
        )
    }

    fun sendCurrencyBalances(habboSession: HabboSession) {
        if (habboSession.release == "R63A") {
            habboSession.sendHabboResponse(OutgoingR63A.CREDITS_BALANCE, habboSession.userInformation.credits)
            habboSession.sendHabboResponse(
                OutgoingR63A.ACTIVITY_POINTS_BALANCE,
                habboSession.userInformation.activityPointsCurrencies
            )
        } else {
            habboSession.sendHabboResponse(Outgoing.CREDITS_BALANCE, habboSession.userInformation.credits)
            habboSession.sendHabboResponse(
                Outgoing.ACTIVITY_POINTS_BALANCE,
                habboSession.userInformation.activityPointsCurrencies
            )
        }
    }

    fun progressFurniOrganizer(room: Room, chest: ChestData, chests: Map<Int, ChestData>) {
        if (chest.type != ChestType.FURNI) return

        val ownerSession = HabboServer.habboSessionManager.getHabboSessionById(chest.userId) ?: return

        val usedChests = room.itemManager.items.values
            .filter { it.furnishing.interactionType == InteractionType.CHEST }
            .count { roomItem ->
                val other = chests[roomItem.id] ?: return@count false

                other.type == ChestType.FURNI && other.entries.isNotEmpty()
            }

        HabboServer.habboGame.achievementManager.progress(
            ownerSession,
            "ACH_FurniOrganizer",
            usedChests,
            accumulate = false
        )
    }
}
