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

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.chest.ChestDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.logic.ChestFurnitureLogic
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.stuff.StuffData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType
import java.util.concurrent.ConcurrentHashMap

class ChestManager {
    private val log = LoggerFactory.getLogger(javaClass)

    private val chests: ConcurrentHashMap<Int, ChestData> = ConcurrentHashMap()

    /**
     * Sessões de Wired Trade (depósito no baú), por userId.
     * Espelho de WiredTradingModel (client): seleção de itens + baú alvo.
     */
    data class ChestTradeSession(
        val chestItemId: Int,
        val isDonation: Boolean,
        val selectedItemIds: MutableSet<Int> = mutableSetOf(),
    )

    private val trades: ConcurrentHashMap<Int, ChestTradeSession> = ConcurrentHashMap()

    // Capacidades iniciais (Central de Informações: starter 100, furni 1000, coins 5000)
    private val defaultCapacity: (String) -> Int = { itemName ->
        when {
            itemName.contains("starter") -> 100
            itemName.contains("coins") -> 5000
            else -> 1000
        }
    }

    fun getDepositTarget(habboSession: HabboSession): Int = trades[habboSession.userInformation.id]?.chestItemId ?: 0

    /**
     * "Iniciar depósito" (client envia CHEST_START_ADDING).
     * Abre a transação Wired Trade: responde WIRED_TRADE_INITIATE, que faz o client
     * abrir a sub-página wired_trading do inventário.
     */
    fun startDeposit(habboSession: HabboSession, room: Room, chest: ChestData) {
        if (!canDonate(habboSession, room, chest)) return

        trades[habboSession.userInformation.id] = ChestTradeSession(
            chestItemId = chest.itemId,
            isDonation = chest.userId != habboSession.userInformation.id,
        )

        habboSession.sendHabboResponse(Outgoing.WIRED_TRADE_INITIATE, DEPOSIT_TIMEOUT_SECONDS)
    }

    fun cancelDeposit(habboSession: HabboSession) {
        trades.remove(habboSession.userInformation.id)
    }

    fun cancelDepositWithNotification(habboSession: HabboSession, failureTypeId: WiredTradeFailureType) {
        trades.remove(habboSession.userInformation.id)
        habboSession.sendHabboResponse(Outgoing.WIRED_TRADE_CANCELLED, failureTypeId)
    }

    fun getOrCreateChest(roomItem: RoomItem): ChestData? {
        if (roomItem.furnishing.interactionType != InteractionType.CHEST) return null

        return chests.computeIfAbsent(roomItem.id) { itemId ->
            val chest = ChestDao.getChest(itemId)?.also { it.typeName = roomItem.itemName } ?: run {
                ChestDao.createChest(itemId, roomItem.userId, defaultCapacity(roomItem.itemName))

                ChestData(
                    itemId = itemId,
                    userId = roomItem.userId,
                    name = "",
                    description = "",
                    capacity = defaultCapacity(roomItem.itemName),
                    coins = 0,
                    isWired = false,
                    locked = true,
                    autoLock = false,
                    stateMode = 0,
                    previewMode = 0,
                    previewAmount = 1,
                    anyoneCanOpen = false,
                    anyoneCanDonate = false,
                    notificationMode = 0,
                    notifyFull = false,
                    notifyDonation = false,
                    notifyWithdraw = false,
                    notifyEmpty = false,
                    notifyTransaction = false,
                ).also { it.typeName = roomItem.itemName }
            }

            chest.entries += ChestDao.getChestEntries(itemId)

            updateChestExtraData(roomItem.room, chest, commitClient = false)

            chest
        }
    }

    fun removeChest(roomItem: RoomItem) {
        chests.remove(roomItem.id)
        ChestDao.deleteChest(roomItem.id)
    }

    private fun reloadEntries(chest: ChestData) {
        chest.entries.clear()
        chest.entries += ChestDao.getChestEntries(chest.itemId)
    }

    fun getRoomItem(room: Room, chestItemId: Int): RoomItem? = room.itemManager.items[chestItemId]

    fun getChest(room: Room, chestItemId: Int): ChestData? =
        getRoomItem(room, chestItemId)?.let { getOrCreateChest(it) }

    fun sendChestOpen(habboSession: HabboSession, chestItemId: Int) {
        habboSession.sendHabboResponse(Outgoing.CHEST_OPEN, chestItemId)
    }

    fun sendContents(habboSession: HabboSession, room: Room, chest: ChestData) {
        if (chest.type == ChestType.COINS) {
            habboSession.sendHabboResponse(Outgoing.CHEST_COINS, chest.itemId, chest.coins, false)
        } else {
            val entries = chest.entries.toList()
            val chunks = entries.chunked(CHUNK_SIZE)

            if (chunks.isEmpty()) {
                habboSession.sendHabboResponse(Outgoing.CHEST_ITEMS_CHUNK, chest.itemId, 1, 0, emptyList<ChestEntry>())
            } else {
                chunks.forEachIndexed { index, chunk ->
                    habboSession.sendHabboResponse(Outgoing.CHEST_ITEMS_CHUNK, chest.itemId, chunks.size, index, chunk)
                }
            }
        }

        setState(room, chest, open = true)
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

        val chest = getChest(room, trade.chestItemId) ?: run {
            trades.remove(habboSession.userInformation.id)

            return
        }

        val normalizedIds = itemIds.map { kotlin.math.abs(it) }

        if (remove) {
            normalizedIds.forEach { trade.selectedItemIds.remove(it) }
        } else {
            val remainingCapacity = (chest.capacity - chest.usedCount).coerceAtLeast(0)

            normalizedIds.forEach { itemId ->
                if (trade.selectedItemIds.size >= remainingCapacity) return@forEach

                val userItem = habboSession.habboInventory.items[itemId] ?: return@forEach

                // Baú de créditos só aceita Habbo Câmbios; baú não aninha baú
                if (chest.type == ChestType.COINS && getCreditFurniValue(userItem.itemName) == null) return@forEach
                if (chest.type == ChestType.FURNI && userItem.furnishing.interactionType == InteractionType.CHEST) return@forEach

                trade.selectedItemIds += itemId
            }
        }

        sendTradeItemsUpdate(habboSession, chest)
    }

    private fun sendTradeItemsUpdate(habboSession: HabboSession, chest: ChestData) {
        val trade = trades[habboSession.userInformation.id] ?: return
        val selectedItems = trade.selectedItemIds.mapNotNull { habboSession.habboInventory.items[it] }

        // A lista de itens ofertados renderiza a partir do array (populateItemGroups);
        // numCredits é informativo (câmbios no coin chest agregam o valor em créditos).
        val coinValue =
            if (chest.type == ChestType.COINS) selectedItems.sumOf { getCreditFurniValue(it.itemName) ?: 0 } else 0

        habboSession.sendHabboResponse(
            Outgoing.WIRED_TRADE_ITEMS_UPDATE,
            habboSession,
            chest,
            selectedItems,
            coinValue,
            selectedItems.isNotEmpty(),
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
            sendTradeItemsUpdate(habboSession, getChest(room, trade.chestItemId) ?: return)

            return
        }

        val chest = getChest(room, trade.chestItemId) ?: run {
            trades.remove(habboSession.userInformation.id)
            habboSession.sendHabboResponse(Outgoing.WIRED_TRADE_CANCELLED, WiredTradeFailureType.USER_CANCELLED)

            return
        }

        val userItems = trade.selectedItemIds.mapNotNull { habboSession.habboInventory.items[it] }
        trades.remove(habboSession.userInformation.id)

        if (userItems.isEmpty()) {
            habboSession.sendHabboResponse(Outgoing.WIRED_TRADE_CANCELLED, WiredTradeFailureType.USER_CANCELLED)

            return
        }

        val isDonation = chest.userId != habboSession.userInformation.id
        var depositFurniCount = 0
        var depositCoinsCount = 0

        if (chest.type == ChestType.COINS) {
            // Baú de créditos: Habbo Câmbios viram créditos no depósito
            userItems.forEach { userItem ->
                val creditValue = getCreditFurniValue(userItem.itemName) ?: return@forEach

                if (chest.coins + depositCoinsCount + creditValue > chest.capacity) return@forEach

                ItemDao.deleteItems(listOf(userItem.id))
                habboSession.habboInventory.removeItems(listOf(userItem.id))

                depositCoinsCount += creditValue
            }

            chest.coins += depositCoinsCount
            ChestDao.updateChest(chest)
        } else {
            val accepted = userItems.takeWhile { !chest.isFull() }

            if (accepted.isNotEmpty()) {
                val itemIds = accepted.map { it.id }

                if (isDonation) ItemDao.updateItemsOwner(itemIds, chest.userId)

                ChestDao.insertChestItems(chest.itemId, itemIds)
                reloadEntries(chest)

                habboSession.habboInventory.removeItems(itemIds)

                depositFurniCount = itemIds.size
            }
        }

        logTransaction(
            room,
            chest,
            habboSession,
            depositFurniCount = depositFurniCount,
            depositCoinsCount = depositCoinsCount,
        )

        if (depositFurniCount > 0 || depositCoinsCount > 0) {
            // Feedback transacional para quem depositou (success.0 = "Depósito do baú realizado")
            habboSession.sendHabboResponse(
                Outgoing.WIRED_TRANSACTION_NOTIFICATION,
                WiredTransactionNotification.CHEST_DEPOSITED
            )

            if (isDonation) {
                notifyDonation(chest, habboSession.userInformation.username)
                notifyOwnerTransaction(chest)
            }
        }

        updateChestExtraData(room, chest)

        if (chest.type == ChestType.COINS) {
            habboSession.sendHabboResponse(Outgoing.CHEST_COINS, chest.itemId, chest.coins, true)
        } else {
            sendContentsUpdated(
                room,
                chest,
                removedIds = emptyList(),
                addedEntries = chest.entries.takeLast(depositFurniCount)
            )
        }

        habboSession.sendHabboResponse(Outgoing.WIRED_TRADE_COMPLETED)

        if (chest.isFull()) notifyFull(chest)

        progressFurniOrganizer(room, chest)
    }

    /**
     * Cancelamento da Wired Trade (WIRED_TRADE_CANCEL).
     */
    fun wiredTradeCancel(habboSession: HabboSession) {
        cancelDepositWithNotification(habboSession, WiredTradeFailureType.USER_CANCELLED)
    }

    /**
     * Notificação transacional para o DONO quando outra pessoa deposita
     * (success.0 = "Depósito do baú realizado", do ponto de vista do baú).
     */
    private fun notifyOwnerTransaction(chest: ChestData) {
        if (!chest.notifyTransaction) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)
            ?.sendHabboResponse(Outgoing.WIRED_TRANSACTION_NOTIFICATION, WiredTransactionNotification.CHEST_DEPOSITED)
    }


    fun withdrawItems(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        isWallItem: Boolean,
        typeId: Int,
        amount: Int,
    ) {
        if (!canWithdraw(habboSession, room, chest)) return

        val matched = chest.entries
            .filter { entry ->
                val furnishing = entry.item.furnishing
                (furnishing.type == ovh.rwx.habbo.game.item.ItemType.WALL) == isWallItem && furnishing.spriteId == typeId
            }
            .take(amount.coerceIn(0, chest.entries.size))

        if (matched.isEmpty()) return

        performWithdraw(habboSession, room, chest, matched)
    }

    fun withdrawAll(habboSession: HabboSession, room: Room, chest: ChestData) {
        if (!canWithdraw(habboSession, room, chest)) return
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
        reloadEntries(chest)

        val userItems = entries.map { entry -> entry.item.copy(userId = habboSession.userInformation.id) }
        habboSession.habboInventory.addItems(userItems)

        // Se o dono anterior está online (e não é quem retirou), remove do inventário dele
        val previousOwnerId = chest.userId
        if (previousOwnerId != habboSession.userInformation.id) {
            HabboServer.habboSessionManager.getHabboSessionById(previousOwnerId)?.habboInventory?.removeItems(itemIds)
        }

        logTransaction(room, chest, habboSession, withdrawFurniCount = itemIds.size)

        if (previousOwnerId != habboSession.userInformation.id) notifyWithdraw(
            chest,
            habboSession.userInformation.username
        )

        // Feedback transacional para quem retirou (success.1 = "Conteúdos retirados do baú com sucesso")
        habboSession.sendHabboResponse(
            Outgoing.WIRED_TRANSACTION_NOTIFICATION,
            WiredTransactionNotification.CHEST_WITHDRAWN
        )

        updateChestExtraData(room, chest)
        sendContentsUpdated(room, chest, removedIds = itemIds, addedEntries = emptyList())

        if (chest.entries.isEmpty()) notifyEmpty(chest)
    }

    fun withdrawCoins(habboSession: HabboSession, room: Room, chest: ChestData, amount: Int) {
        if (!canWithdraw(habboSession, room, chest)) return
        if (chest.type != ChestType.COINS) return

        val withdrawAmount = amount.coerceIn(0, chest.coins)
        if (withdrawAmount == 0) return

        chest.coins -= withdrawAmount
        ChestDao.updateChest(chest)

        habboSession.userInformation.credits += withdrawAmount

        logTransaction(room, chest, habboSession, withdrawCoinsCount = withdrawAmount)

        if (chest.userId != habboSession.userInformation.id) notifyWithdraw(
            chest,
            habboSession.userInformation.username
        )

        updateChestExtraData(room, chest)
        habboSession.sendHabboResponse(Outgoing.CHEST_COINS, chest.itemId, chest.coins, true)
        sendCurrencyBalances(habboSession)
    }

    fun lockAll(habboSession: HabboSession, room: Room, lock: Boolean) {
        if (!room.userManager.hasRights(habboSession)) return

        room.itemManager.items.values
            .filter { it.furnishing.interactionType == InteractionType.CHEST }
            .forEach { roomItem ->
                val chest = getOrCreateChest(roomItem) ?: return@forEach

                // Apenas o dono do baú pode destrancar
                if (lock || chest.userId == habboSession.userInformation.id) {
                    chest.locked = lock
                    ChestDao.updateChest(chest)

                    updateChestExtraData(room, chest)
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
        if (!canEdit(habboSession, room, chest)) return

        // Apenas o dono do baú pode destrancar
        if (!locked || chest.userId == habboSession.userInformation.id) chest.locked = locked

        chest.autoLock = autoLock
        chest.capacity = capacity.coerceIn(0, maxCapacity(chest))

        ChestDao.updateChest(chest)
        updateChestExtraData(room, chest)

        habboSession.sendHabboResponse(Outgoing.CHEST_PREFERENCES_UPDATE_SUCCESS, chest.itemId, false)
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
        if (!canEdit(habboSession, room, chest)) return

        chest.name = name.take(64)
        chest.description = description.take(128)
        chest.anyoneCanOpen = anyoneCanOpen
        chest.anyoneCanDonate = anyoneCanDonate
        chest.stateMode = stateMode.coerceIn(0, 3)
        chest.previewMode = previewMode.coerceIn(0, 7)
        chest.previewAmount = previewAmount.coerceIn(1, 4)

        ChestDao.updateChest(chest)
        updateChestExtraData(room, chest)

        habboSession.sendHabboResponse(Outgoing.CHEST_PREFERENCES_UPDATE_SUCCESS, chest.itemId, false)
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

        habboSession.sendHabboResponse(Outgoing.CHEST_PREFERENCES_UPDATE_SUCCESS, chest.itemId, true)
    }

    fun upgrade(habboSession: HabboSession, room: Room, chest: ChestData, currencyType: Int) {
        val roomItem = getRoomItem(room, chest.itemId)

        if (roomItem == null) {
            habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 4)

            return
        }

        if (chest.isStarter) {
            habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 10)

            return
        }

        if (chest.userId != habboSession.userInformation.id) {
            habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 7)

            return
        }

        val maxCap = maxCapacity(chest)

        if (chest.capacity >= maxCap) {
            habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 2)

            return
        }

        // Upgrade: 10 da moeda escolhida (credits = 0, diamonds = 5)
        val (activityPointType, cost) =
            if (currencyType == ActivityPointType.DIAMONDS.code) ActivityPointType.DIAMONDS to UPGRADE_COST else null to UPGRADE_COST

        if (activityPointType != null) {
            val balance = habboSession.userInformation.activityPointsCurrencies.getOrDefault(activityPointType, 0)

            if (balance < cost) {
                habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 6)

                return
            }

            habboSession.userInformation.activityPointsCurrencies.merge(activityPointType, -cost, Int::plus)
        } else {
            if (habboSession.userInformation.credits < cost) {
                habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 5)

                return
            }

            habboSession.userInformation.credits -= cost
        }

        val capacityStep = if (chest.type == ChestType.COINS) COINS_CAPACITY_STEP else FURNI_CAPACITY_STEP

        chest.capacity = (chest.capacity + capacityStep).coerceAtMost(maxCap)

        ChestDao.updateChest(chest)
        updateChestExtraData(room, chest)
        sendCurrencyBalances(habboSession)

        habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 0)
    }

    fun upgradeWired(habboSession: HabboSession, room: Room, chest: ChestData) {
        if (chest.isStarter) {
            habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 10)

            return
        }

        if (chest.userId != habboSession.userInformation.id) {
            habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 7)

            return
        }

        if (chest.isWired) {
            habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 0)

            return
        }

        // Upgrade para Baú Wired é gratuito (Central de Informações)
        chest.isWired = true

        ChestDao.updateChest(chest)
        updateChestExtraData(room, chest)

        habboSession.sendHabboResponse(Outgoing.CHEST_UPGRADE_RESULT, chest.itemId, 0)
    }

    fun closeChest(habboSession: HabboSession, room: Room, chest: ChestData) {
        cancelDeposit(habboSession)
        setState(room, chest, open = false)
    }

    /**
     * Auto-lock: baús do usuário que saiu com auto_lock ativo são trancados.
     */
    fun onUserLeaveRoom(room: Room, habboSession: HabboSession) {
        cancelDeposit(habboSession)

        val userId = habboSession.userInformation.id

        room.itemManager.items.values
            .filter { it.furnishing.interactionType == InteractionType.CHEST }
            .forEach { roomItem ->
                val chest = chests[roomItem.id] ?: return@forEach

                if (chest.userId == userId && chest.autoLock && !chest.locked) {
                    chest.locked = true
                    ChestDao.updateChest(chest)

                    updateChestExtraData(room, chest)
                }
            }
    }

    /**
     * Unload do quarto (Room.stopLoop): encerra qualquer Wired Trade de depósito
     * pendente apontando para os baús do quarto. O estado "aberto" é efêmero —
     * o banco sempre tem o baú fechado, nada a persistir aqui.
     */
    fun onRoomUnload(room: Room) {
        val chestIds = room.itemManager.items.values
            .filter { it.furnishing.interactionType == InteractionType.CHEST }
            .map { it.id }
            .toSet()

        if (chestIds.isEmpty()) return

        trades.entries.removeIf { it.value.chestItemId in chestIds }
    }

    // ------------------------------------------------------------------
    // Permissões
    // ------------------------------------------------------------------

    fun isOwner(habboSession: HabboSession, chest: ChestData): Boolean =
        chest.userId == habboSession.userInformation.id

    fun canEdit(habboSession: HabboSession, room: Room, chest: ChestData): Boolean =
        isOwner(habboSession, chest)

    fun canView(habboSession: HabboSession, room: Room, chest: ChestData): Boolean {
        if (isOwner(habboSession, chest)) return true
        if (room.userManager.hasRights(habboSession)) return true

        return chest.anyoneCanOpen
    }

    fun canWithdraw(habboSession: HabboSession, room: Room, chest: ChestData): Boolean {
        if (isOwner(habboSession, chest)) return true

        if (chest.locked) {
            // Dono do quarto pode trancar, mas apenas o dono do baú retira de um baú trancado
            return false
        }

        if (chest.isWired && room.userManager.hasRights(habboSession)) return true

        return false
    }

    fun canDonate(habboSession: HabboSession, room: Room, chest: ChestData): Boolean {
        if (isOwner(habboSession, chest)) return true

        if (chest.locked) return false

        if (chest.isWired && room.userManager.hasRights(habboSession)) return true

        return chest.anyoneCanDonate
    }

    // ------------------------------------------------------------------
    // Logs
    // ------------------------------------------------------------------

    fun getTransactionLogs(chestItemId: Int, pageSize: Int, page: Int): Pair<Int, List<ChestDao.ChestLog>> =
        ChestDao.getChestLogs(chestItemId, pageSize.coerceIn(1, 100), page.coerceAtLeast(1))

    fun getTransactionLogDetails(transactionId: Long): ChestDao.ChestLog? =
        ChestDao.getChestLog(transactionId)

    private fun logTransaction(
        room: Room,
        chest: ChestData,
        habboSession: HabboSession,
        withdrawFurniCount: Int = 0,
        depositFurniCount: Int = 0,
        withdrawCoinsCount: Int = 0,
        depositCoinsCount: Int = 0,
    ) {
        runCatching {
            ChestDao.insertChestLog(
                chestItemId = chest.itemId,
                roomId = room.roomData.id,
                userId = habboSession.userInformation.id,
                username = habboSession.userInformation.username,
                withdrawFurniCount = withdrawFurniCount,
                depositFurniCount = depositFurniCount,
                withdrawCoinsCount = withdrawCoinsCount,
                depositCoinsCount = depositCoinsCount,
            )
        }.onFailure { log.warn("Failed to insert chest log for chest {}", chest.itemId, it) }
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private fun maxCapacity(chest: ChestData): Int =
        if (chest.type == ChestType.COINS) COINS_MAX_CAPACITY else FURNI_MAX_CAPACITY

    private fun setState(room: Room, chest: ChestData, open: Boolean) {
        val stateMode = chest.stateMode

        // stateMode: 0 = abre quando observado, 1 = sempre aberto, 2 = sempre fechado, 3 = controle Wired
        val shouldOpen = when (stateMode) {
            1 -> true
            2 -> false
            else -> open
        }

        val roomItem = getRoomItem(room, chest.itemId) ?: return
        val values = ChestFurnitureLogic.parseChestExtraData(roomItem.extraData)

        // Estado aberto é efêmero (client-only): o banco tem SEMPRE o baú fechado
        values[StuffData.KEY_STATE] = if (shouldOpen) "1" else "0"
        values[ChestFurnitureLogic.KEY_VISUALS] = if (shouldOpen) getPreviewVisuals(chest) else ""
        applyChestValues(values, chest)

        roomItem.extraData = ChestFurnitureLogic.formatChestExtraData(values)
        roomItem.update(updateDb = false, updateClient = true)
    }

    private fun updateChestExtraData(room: Room, chest: ChestData, commitClient: Boolean = true) {
        val roomItem = getRoomItem(room, chest.itemId) ?: return
        val values = ChestFurnitureLogic.parseChestExtraData(roomItem.extraData)
        val isOpen = values[StuffData.KEY_STATE] == "1"

        applyChestValues(values, chest)

        // Persistência: SEMPRE fechado no banco (o estado aberto é efêmero)
        values[StuffData.KEY_STATE] = "0"
        values[ChestFurnitureLogic.KEY_VISUALS] = ""
        roomItem.extraData = ChestFurnitureLogic.formatChestExtraData(values)

        if (commitClient) roomItem.update(updateDb = true, updateClient = false)

        if (isOpen && commitClient) {
            // Reabre client-side com os dados atualizados (sem persistir aberto)
            values[StuffData.KEY_STATE] = "1"
            values[ChestFurnitureLogic.KEY_VISUALS] = getPreviewVisuals(chest)
            roomItem.extraData = ChestFurnitureLogic.formatChestExtraData(values)
            roomItem.update(updateDb = false, updateClient = true)
        }
    }

    private fun applyChestValues(values: LinkedHashMap<String, String>, chest: ChestData) {
        values[ChestFurnitureLogic.KEY_IS_WIRED_ENABLED] = if (chest.isWired) "1" else "0"
        values[ChestFurnitureLogic.KEY_CHEST_NAME] = chest.name
        values[ChestFurnitureLogic.KEY_CHEST_DESC] = chest.description
        values[ChestFurnitureLogic.KEY_EVERYONE_CAN_OPEN] = if (chest.anyoneCanOpen) "1" else "0"
        values[ChestFurnitureLogic.KEY_EVERYONE_CAN_DONATE] = if (chest.anyoneCanDonate) "1" else "0"
        values[ChestFurnitureLogic.KEY_STATE_CONTROL_MODE] = chest.stateMode.toString()
        values[ChestFurnitureLogic.KEY_PREVIEW_MODE] = chest.previewMode.toString()
        values[ChestFurnitureLogic.KEY_PREVIEW_AMOUNT] = chest.previewAmount.toString()
        values[ChestFurnitureLogic.KEY_NOTIFY_MODE] = chest.notificationMode.toString()
        values[ChestFurnitureLogic.KEY_NOTIFICATION_CHEST_FULL] = if (chest.notifyFull) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_DONATION] = if (chest.notifyDonation) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_SOMEONE_WITHDRAWS] = if (chest.notifyWithdraw) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_CHEST_EMPTY] = if (chest.notifyEmpty) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_WIRED_TRANSACTION] = if (chest.notifyTransaction) "1" else "0"
        values[ChestFurnitureLogic.KEY_CONTENTS_COUNT] = chest.entries.size.toString()
        values[ChestFurnitureLogic.KEY_CONTENTS_COINS] = chest.coins.toString()
    }

    /**
     * Pré-visualização dos itens quando o baú está aberto.
     * previewMode: 0 nenhuma, 1-2 aleatórios, 3-4 mais recentes, 5-6 mais antigos, 7 Wired.
     * Modos pares (2/4/6) preferem tipos diferentes; 7 (fila Wired) ainda não existe -> aleatório.
     */
    private fun getPreviewVisuals(chest: ChestData): String {
        if (chest.previewMode == 0) return ""

        val distinctTypes = chest.previewMode in intArrayOf(2, 4, 6)
        val amount = chest.previewAmount.coerceAtLeast(1)

        val candidates = when (chest.previewMode) {
            3, 4 -> chest.entries.sortedByDescending { it.chestItemId }
            5, 6 -> chest.entries.sortedBy { it.chestItemId }
            else -> chest.entries.shuffled()
        }

        val selected = mutableListOf<ChestEntry>()
        val seenTypes = mutableSetOf<Pair<Boolean, Int>>()

        for (entry in candidates) {
            if (selected.size >= amount) break

            val furnishing = entry.item.furnishing
            val typeKey = (furnishing.type == ovh.rwx.habbo.game.item.ItemType.WALL) to furnishing.spriteId

            if (distinctTypes) {
                if (!seenTypes.add(typeKey)) continue
            }

            selected += entry
        }

        return selected.joinToString(ChestFurnitureLogic.VISUALS_ITEM_SEPARATOR) { entry ->
            val furnishing = entry.item.furnishing
            val isWallItem = furnishing.type == ovh.rwx.habbo.game.item.ItemType.WALL
            val legacyPosterId = if (entry.item.itemName.contains("poster")) entry.item.extraData else ""

            itemTypeToString(isWallItem, furnishing.spriteId, legacyPosterId)
        }
    }

    private fun sendContentsUpdated(
        room: Room,
        chest: ChestData,
        removedIds: List<Int>,
        addedEntries: List<ChestEntry>
    ) {
        room.userManager.entities.values
            .filterIsInstance<RoomUser>()
            .map { it.habboSession }
            .forEach { session ->
                session.sendHabboResponse(Outgoing.CHEST_ITEMS_UPDATED, chest.itemId, removedIds, addedEntries)
            }
    }

    private fun notifyDonation(chest: ChestData, username: String) {
        if (!chest.notifyDonation) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendNotification(
            $$"${notification.wired_chests.donation.message}".replace("%user_name%", username)
                .replace("%chest_name%", chest.name.ifBlank { $$"${wiredchests.furni_chest}" })
        )
    }

    private fun notifyWithdraw(chest: ChestData, username: String) {
        if (!chest.notifyWithdraw) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendNotification(
            $$"${notification.wired_chests.someone_withdraws.message}".replace("%user_name%", username)
                .replace("%chest_name%", chest.name.ifBlank { $$"${wiredchests.furni_chest}" })
        )
    }

    private fun notifyFull(chest: ChestData) {
        if (!chest.notifyFull) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendNotification(
            $$"${notification.wired_chests.chest_full.message}".replace(
                "%chest_name%",
                chest.name.ifBlank { $$"${wiredchests.furni_chest}" })
        )
    }

    private fun notifyEmpty(chest: ChestData) {
        if (!chest.notifyEmpty) return

        HabboServer.habboSessionManager.getHabboSessionById(chest.userId)?.sendNotification(
            $$"${notification.wired_chests.chest_empty.message}".replace(
                "%chest_name%",
                chest.name.ifBlank { $$"${wiredchests.furni_chest}" })
        )
    }

    private fun sendCurrencyBalances(habboSession: HabboSession) {
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

    private fun progressFurniOrganizer(room: Room, chest: ChestData) {
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

    private fun getCreditFurniValue(itemName: String): Int? {
        if (!itemName.startsWith("CF_") && !itemName.startsWith("CFC_")) return null

        val split = itemName.split('_')

        return if (split.size > 2 && split[1] == "diamond") {
            split[2].toIntOrNull()
        } else if (split.size > 1) {
            split[1].toIntOrNull()
        } else {
            null
        }
    }

    companion object {
        private const val CHUNK_SIZE = 200
        private const val DEPOSIT_TIMEOUT_SECONDS = 60
        private const val UPGRADE_COST = 10
        private const val FURNI_CAPACITY_STEP = 1000
        private const val COINS_CAPACITY_STEP = 5000
        private const val FURNI_MAX_CAPACITY = 5000
        private const val COINS_MAX_CAPACITY = 25000
    }
}
