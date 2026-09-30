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
import ovh.rwx.habbo.communication.outgoing.chest.ChestCoinsData
import ovh.rwx.habbo.database.chest.ChestDao
import ovh.rwx.habbo.database.chest.ChestLogItemEntry
import ovh.rwx.habbo.database.chest.ChestLogItemsData
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession

class ChestWithdrawalService(private val chestManager: ChestManager) {

    fun withdrawItems(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        isWallItem: Boolean,
        typeId: Int,
        amount: Int,
    ) {
        if (!ChestPermissions.canWithdraw(habboSession, room, chest)) return

        val matched = chest.entries
            .filter { entry ->
                val furnishing = entry.item.furnishing
                (furnishing.type == ItemType.WALL) == isWallItem && furnishing.spriteId == typeId
            }
            .take(amount.coerceIn(0, chest.entries.size))

        if (matched.isEmpty()) return

        performWithdraw(habboSession, room, chest, matched)
    }

    fun withdrawAll(habboSession: HabboSession, room: Room, chest: ChestData) {
        if (!ChestPermissions.canWithdraw(habboSession, room, chest)) return
        if (chest.type == ChestType.COINS) return

        val matched = chest.entries.toList()

        if (matched.isEmpty()) return

        performWithdraw(habboSession, room, chest, matched)
    }

    private fun performWithdraw(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        entries: List<ChestEntry>,
    ) {
        val itemIds = entries.map { it.item.id }

        ItemDao.updateItemsOwner(itemIds, habboSession.userInformation.id)
        ChestDao.deleteChestItems(chest.itemId, itemIds)
        chestManager.reloadEntries(chest)

        val userItems = entries.map { entry -> entry.item.copy(userId = habboSession.userInformation.id) }
        habboSession.habboInventory.addItems(userItems)

        // Se o dono anterior está online (e não é quem retirou), remove do inventário dele
        val previousOwnerId = chest.userId
        if (previousOwnerId != habboSession.userInformation.id) {
            HabboServer.habboSessionManager.getHabboSessionById(previousOwnerId)?.habboInventory?.removeItems(itemIds)
        }

        val withdrawnLogItems = entries.groupBy { entry ->
            val furnishing = entry.item.furnishing
            val isWall = furnishing.type == ItemType.WALL
            val poster = if (entry.item.itemName.contains("poster")) entry.item.extraData else ""
            Triple(isWall, furnishing.spriteId, poster)
        }.map { (key, group) ->
            ChestLogItemEntry(
                isWallItem = key.first,
                typeId = key.second,
                legacyPosterId = key.third,
                count = group.size,
            )
        }

        ChestAuditLog.logTransaction(
            room,
            chest,
            habboSession,
            withdrawFurniCount = itemIds.size,
            itemsData = if (withdrawnLogItems.isNotEmpty()) ChestLogItemsData(withdrawn = withdrawnLogItems) else null,
        )

        if (previousOwnerId != habboSession.userInformation.id) {
            ChestNotifier.notifyWithdraw(chest, habboSession.userInformation.username)
        }

        // Feedback transacional para quem retirou (success.1 = "Conteúdos retirados do baú com sucesso")
        habboSession.sendHabboResponse(
            Outgoing.WIRED_TRANSACTION_SUCCESS,
            WiredTransactionNotification.CHEST_WITHDRAWN
        )

        ChestVisuals.updateChestExtraData(room, chest)
        ChestVisuals.sendContentsUpdated(room, chest, removedIds = itemIds, addedEntries = emptyList())

        if (chest.entries.isEmpty()) ChestNotifier.notifyEmpty(chest)
    }

    fun withdrawCoins(habboSession: HabboSession, room: Room, chest: ChestData, amount: Int) {
        if (!ChestPermissions.canWithdraw(habboSession, room, chest)) return
        if (chest.type != ChestType.COINS) return

        val withdrawAmount = amount.coerceIn(0, chest.coins)
        if (withdrawAmount == 0) return

        chest.coins -= withdrawAmount
        ChestDao.updateChest(chest)

        habboSession.userInformation.credits += withdrawAmount

        ChestAuditLog.logTransaction(room, chest, habboSession, withdrawCoinsCount = withdrawAmount)

        if (chest.userId != habboSession.userInformation.id) {
            ChestNotifier.notifyWithdraw(chest, habboSession.userInformation.username)
        }

        ChestVisuals.updateChestExtraData(room, chest)
        habboSession.sendHabboResponse(
            Outgoing.CHEST_COINS,
            ChestCoinsData(chestItemId = chest.itemId, coins = chest.coins, isUpdate = true)
        )
        ChestNotifier.sendCurrencyBalances(habboSession)
    }
}
