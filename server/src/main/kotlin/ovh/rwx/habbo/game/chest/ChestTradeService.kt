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
import ovh.rwx.habbo.communication.outgoing.chest.ChestCoinsData
import ovh.rwx.habbo.communication.outgoing.wiredtrade.WiredTradeInitiateData
import ovh.rwx.habbo.communication.outgoing.wiredtrade.WiredTradeItemsUpdateData
import ovh.rwx.habbo.database.chest.ChestDao
import ovh.rwx.habbo.database.chest.ChestLogItemEntry
import ovh.rwx.habbo.database.chest.ChestLogItemsData
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.wired.trigger.EmptyTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerTransactionComplete
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerTransactionFail
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Sessão de Wired Trade (depósito no baú), por userId.
 * Espelho de WiredTradingModel (client): seleção de itens + baú alvo.
 */
data class ChestTradeSession(
    val chestItemId: Int,
    val isDonation: Boolean,
    val selectedItemIds: MutableSet<Int> = mutableSetOf(),
)

class ChestTradeService(private val chestManager: ChestManager) {

    private val trades: ConcurrentHashMap<Int, ChestTradeSession> = ConcurrentHashMap()

    fun getDepositTarget(habboSession: HabboSession): Int =
        trades[habboSession.userInformation.id]?.chestItemId ?: 0

    /**
     * "Iniciar depósito" (client envia CHEST_START_ADDING).
     * Abre a transação Wired Trade: responde WIRED_TRADE_INITIATE, que faz o client
     * abrir a sub-página wired_trading do inventário.
     */
    fun startDeposit(habboSession: HabboSession, room: Room, chest: ChestData) {
        if (!ChestPermissions.canDonate(habboSession, room, chest)) return

        trades[habboSession.userInformation.id] = ChestTradeSession(
            chestItemId = chest.itemId,
            isDonation = chest.userId != habboSession.userInformation.id,
        )

        habboSession.sendHabboResponse(
            Outgoing.WIRED_TRADE_INITIATE,
            WiredTradeInitiateData(
                requirementType = chest.type.tradeRequirementType,
                timeoutSeconds = ChestConstants.DEPOSIT_TIMEOUT_SECONDS,
            ),
        )
    }

    fun cancelDeposit(habboSession: HabboSession) {
        trades.remove(habboSession.userInformation.id)
    }

    fun cancelDepositWithNotification(habboSession: HabboSession, failureTypeId: WiredTradeFailureType) {
        trades.remove(habboSession.userInformation.id)
        habboSession.sendHabboResponse(Outgoing.WIRED_TRADE_CANCELLED, failureTypeId)
        habboSession.roomUser?.room?.itemManager?.wiredHandler?.triggerWired(
            WiredTriggerTransactionFail::class,
            habboSession.roomUser,
            EmptyTriggerData
        )
    }

    /**
     * Client adiciona/remove itens da seleção da Wired Trade (WIRED_TRADE_ADD_DELETE_ITEMS).
     * Responde WIRED_TRADE_ITEMS_UPDATE com o estado dos dois lados.
     */
    fun wiredTradeAddDeleteItems(
        habboSession: HabboSession,
        room: Room,
        remove: Boolean,
        itemIds: List<Int>,
    ) {
        val trade = trades[habboSession.userInformation.id] ?: run {
            cancelDepositWithNotification(habboSession, WiredTradeFailureType.USER_CANCELLED)
            return
        }

        val chest = chestManager.getChest(room, trade.chestItemId) ?: run {
            trades.remove(habboSession.userInformation.id)
            habboSession.sendHabboResponse(
                Outgoing.WIRED_TRADE_TRANSACTION_NOTIFICATION,
                WiredTradeErrorType.CHEST_NOT_FOUND
            )
            habboSession.sendHabboResponse(
                Outgoing.WIRED_TRADE_CANCELLED,
                WiredTradeFailureType.CHEST_NOT_IN_ROOM
            )
            return
        }

        val normalizedIds = itemIds.map { abs(it) }

        if (remove) {
            normalizedIds.forEach { trade.selectedItemIds.remove(it) }
        } else {
            val remainingCapacity = chest.remainingCapacity

            normalizedIds.forEach { itemId ->
                if (trade.selectedItemIds.size >= remainingCapacity) {
                    habboSession.sendHabboResponse(
                        Outgoing.WIRED_TRADE_TRANSACTION_NOTIFICATION,
                        WiredTradeErrorType.EXCEEDS_CHEST_CAPACITY
                    )
                    return@forEach
                }

                val userItem = habboSession.habboInventory.items[itemId] ?: run {
                    habboSession.sendHabboResponse(
                        Outgoing.WIRED_TRADE_TRANSACTION_NOTIFICATION,
                        WiredTradeErrorType.INVALID_ITEM
                    )
                    return@forEach
                }

                // Baú de créditos só aceita Habbo Câmbios; baú não aninha baú
                if (chest.type == ChestType.COINS && ChestConstants.getCreditFurniValue(userItem.itemName) == null) {
                    habboSession.sendHabboResponse(
                        Outgoing.WIRED_TRADE_TRANSACTION_NOTIFICATION,
                        WiredTradeErrorType.INVALID_ITEM
                    )
                    return@forEach
                }
                if (chest.type == ChestType.FURNI && userItem.furnishing.interactionType == InteractionType.CHEST) {
                    habboSession.sendHabboResponse(
                        Outgoing.WIRED_TRADE_TRANSACTION_NOTIFICATION,
                        WiredTradeErrorType.INVALID_ITEM
                    )
                    return@forEach
                }

                trade.selectedItemIds += itemId
            }
        }

        sendTradeItemsUpdate(habboSession, chest)
    }

    fun sendTradeItemsUpdate(habboSession: HabboSession, chest: ChestData) {
        val trade = trades[habboSession.userInformation.id] ?: return
        val selectedItems = trade.selectedItemIds.mapNotNull { habboSession.habboInventory.items[it] }

        // A lista de itens ofertados renderiza a partir do array (populateItemGroups);
        // numCredits é informativo (câmbios no coin chest agregam o valor em créditos).
        val coinValue =
            if (chest.type == ChestType.COINS) {
                selectedItems.sumOf { ChestConstants.getCreditFurniValue(it.itemName) ?: 0 }
            } else {
                0
            }

        habboSession.sendHabboResponse(
            Outgoing.WIRED_TRADE_ITEMS_UPDATE,
            WiredTradeItemsUpdateData(
                ownUserId = habboSession.userInformation.id,
                chest = chest,
                ownItems = selectedItems,
                ownCredits = coinValue,
                canAccept = selectedItems.isNotEmpty(),
            ),
        )
    }

    /**
     * Accept/Confirm da Wired Trade (WIRED_TRADE_CONFIRM). Semântica do wire:
     *   confirm=false = ACCEPT (client state 1→2, countdown; nada a executar)
     *   confirm=true  = CONFIRM (executa o depósito dos itens selecionados)
     * (O cancelamento real usa o composer WIRED_TRADE_CANCEL.)
     */
    fun wiredTradeConfirm(habboSession: HabboSession, room: Room, confirm: Boolean) {
        val trade = trades[habboSession.userInformation.id] ?: return

        if (!confirm) {
            sendTradeItemsUpdate(habboSession, chestManager.getChest(room, trade.chestItemId) ?: return)
            return
        }

        val chest = chestManager.getChest(room, trade.chestItemId) ?: run {
            cancelDepositWithNotification(habboSession, WiredTradeFailureType.CHEST_NOT_IN_ROOM)
            return
        }

        val userItems = trade.selectedItemIds.mapNotNull { habboSession.habboInventory.items[it] }
        trades.remove(habboSession.userInformation.id)

        if (userItems.isEmpty()) {
            cancelDepositWithNotification(habboSession, WiredTradeFailureType.EMPTY_TRANSACTION)
            return
        }

        val isDonation = chest.userId != habboSession.userInformation.id
        var depositFurniCount = 0
        var depositCoinsCount = 0
        val depositedLogItems = mutableListOf<ChestLogItemEntry>()

        if (chest.type == ChestType.COINS) {
            // Baú de créditos: Habbo Câmbios viram créditos no depósito
            userItems.forEach { userItem ->
                val creditValue = ChestConstants.getCreditFurniValue(userItem.itemName) ?: return@forEach

                if (chest.coins + depositCoinsCount + creditValue > chest.capacity) return@forEach

                ItemDao.deleteItems(listOf(userItem.id))
                habboSession.habboInventory.removeItems(listOf(userItem.id))

                depositCoinsCount += creditValue
            }

            chest.coins += depositCoinsCount
            ChestDao.updateChest(chest)
        } else {
            val remainingCapacity = chest.remainingCapacity
            val accepted = userItems.take(remainingCapacity)

            if (accepted.isNotEmpty()) {
                val itemIds = accepted.map { it.id }

                if (isDonation) ItemDao.updateItemsOwner(itemIds, chest.userId)

                ChestDao.insertChestItems(chest.itemId, itemIds)
                chestManager.reloadEntries(chest)

                habboSession.habboInventory.removeItems(itemIds)

                depositFurniCount = itemIds.size

                accepted.groupBy { item ->
                    val furnishing = item.furnishing
                    val isWall = furnishing.type == ItemType.WALL
                    val poster = if (item.itemName.contains("poster")) item.extraData else ""
                    Triple(isWall, furnishing.spriteId, poster)
                }.forEach { (key, group) ->
                    depositedLogItems += ChestLogItemEntry(
                        isWallItem = key.first,
                        typeId = key.second,
                        legacyPosterId = key.third,
                        count = group.size,
                    )
                }
            }
        }

        ChestAuditLog.logTransaction(
            room,
            chest,
            habboSession,
            depositFurniCount = depositFurniCount,
            depositCoinsCount = depositCoinsCount,
            itemsData = if (depositedLogItems.isNotEmpty()) ChestLogItemsData(deposited = depositedLogItems) else null,
        )

        if (depositFurniCount > 0 || depositCoinsCount > 0) {
            // Feedback transacional para quem depositou (success.0 = "Depósito do baú realizado")
            habboSession.sendHabboResponse(
                Outgoing.WIRED_TRANSACTION_SUCCESS,
                WiredTransactionNotification.CHEST_DEPOSITED
            )

            room.itemManager.wiredHandler.triggerWired(
                WiredTriggerTransactionComplete::class,
                habboSession.roomUser,
                EmptyTriggerData
            )

            if (isDonation) {
                ChestNotifier.notifyDonation(chest, habboSession.userInformation.username)
                ChestNotifier.notifyOwnerTransaction(chest)
            }
        } else {
            habboSession.sendHabboResponse(
                Outgoing.WIRED_TRANSACTION_FAIL,
                if (chest.isFull()) WiredTradeFailureType.CHEST_FULL else WiredTradeFailureType.CHEST_CAPACITY_EXCEEDED
            )

            room.itemManager.wiredHandler.triggerWired(
                WiredTriggerTransactionFail::class,
                habboSession.roomUser,
                EmptyTriggerData
            )
        }

        ChestVisuals.updateChestExtraData(room, chest)

        if (chest.type == ChestType.COINS) {
            habboSession.sendHabboResponse(
                Outgoing.CHEST_COINS,
                ChestCoinsData(chestItemId = chest.itemId, coins = chest.coins, isUpdate = true)
            )
        } else {
            ChestVisuals.sendContentsUpdated(
                room,
                chest,
                removedIds = emptyList(),
                addedEntries = chest.entries.takeLast(depositFurniCount)
            )
        }

        habboSession.sendHabboResponse(Outgoing.WIRED_TRADE_COMPLETED)

        if (chest.isFull()) ChestNotifier.notifyFull(chest)

        ChestNotifier.progressFurniOrganizer(room, chest, chestManager.chests)
    }

    /**
     * Cancelamento da Wired Trade (WIRED_TRADE_CANCEL).
     */
    fun wiredTradeCancel(habboSession: HabboSession) {
        cancelDepositWithNotification(habboSession, WiredTradeFailureType.USER_CANCELLED)
    }

    fun clearTradesForChests(chestIds: Set<Int>) {
        trades.entries.removeIf { it.value.chestItemId in chestIds }
    }
}
