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
import ovh.rwx.habbo.communication.outgoing.chest.ChestItemsChunkData
import ovh.rwx.habbo.database.chest.ChestDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordenador central do sistema de Baús (Chests).
 * Atua como fachada delegando para serviços especializados:
 * - [tradeService]: Sessões e fluxo de Wired Trade / depósitos
 * - [withdrawalService]: Retiradas de itens e câmbios
 * - [settingsService]: Configurações, opções, trancamento e upgrades
 * - [ChestPermissions]: Verificação de permissões e controle de acesso
 * - [ChestVisuals]: Formatação de extraData, pré-visualizações e estado visual
 * - [ChestNotifier]: Notificações transacionais e progressão de conquistas
 */
class ChestManager {

    internal val chests: ConcurrentHashMap<Int, ChestData> = ConcurrentHashMap()

    val tradeService: ChestTradeService = ChestTradeService(this)
    val withdrawalService: ChestWithdrawalService = ChestWithdrawalService(this)
    val settingsService: ChestSettingsService = ChestSettingsService(this)

    // ------------------------------------------------------------------
    // Ciclo de Vida do Baú no Quarto
    // ------------------------------------------------------------------

    fun getOrCreateChest(roomItem: RoomItem): ChestData? {
        if (roomItem.furnishing.interactionType != InteractionType.CHEST) return null

        return chests.computeIfAbsent(roomItem.id) { itemId ->
            val chest = ChestDao.getChest(itemId)?.also { it.typeName = roomItem.itemName } ?: run {
                val capacity = ChestConstants.defaultCapacity(roomItem.itemName)
                ChestDao.createChest(itemId, roomItem.userId, capacity)

                ChestData(
                    itemId = itemId,
                    userId = roomItem.userId,
                    name = "",
                    description = "",
                    capacity = capacity,
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
            ChestVisuals.updateChestExtraData(roomItem.room, chest, commitClient = false)

            chest
        }
    }

    fun removeChest(roomItem: RoomItem) {
        chests.remove(roomItem.id)
        ChestDao.deleteChest(roomItem.id)
    }

    internal fun reloadEntries(chest: ChestData) {
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
            habboSession.sendHabboResponse(
                Outgoing.CHEST_COINS,
                ChestCoinsData(chestItemId = chest.itemId, coins = chest.coins, isUpdate = false)
            )
        } else {
            val entries = chest.entries.toList()
            val chunks = entries.chunked(ChestConstants.CHUNK_SIZE)

            if (chunks.isEmpty()) {
                habboSession.sendHabboResponse(
                    Outgoing.CHEST_ITEMS_CHUNK,
                    ChestItemsChunkData(
                        chestItemId = chest.itemId,
                        totalFragments = 1,
                        fragmentNo = 0,
                        entries = emptyList()
                    )
                )
            } else {
                chunks.forEachIndexed { index, chunk ->
                    habboSession.sendHabboResponse(
                        Outgoing.CHEST_ITEMS_CHUNK,
                        ChestItemsChunkData(
                            chestItemId = chest.itemId,
                            totalFragments = chunks.size,
                            fragmentNo = index,
                            entries = chunk
                        )
                    )
                }
            }
        }

        ChestVisuals.setState(room, chest, open = true)
    }

    fun closeChest(habboSession: HabboSession, room: Room, chest: ChestData) {
        tradeService.cancelDeposit(habboSession)
        ChestVisuals.setState(room, chest, open = false)
    }

    /**
     * Auto-lock e fechamento: quando o usuário sai do quarto:
     * - Cancela depósitos pendentes do usuário
     * - Auto-lock: baús do usuário que saiu com auto_lock ativo são trancados
     * - Se o dono saiu, fecha o baú (se não for modo "sempre aberto")
     */
    fun onUserLeaveRoom(room: Room, habboSession: HabboSession) {
        tradeService.cancelDeposit(habboSession)

        val userId = habboSession.userInformation.id

        room.itemManager.items.values
            .filter { it.furnishing.interactionType == InteractionType.CHEST }
            .forEach { roomItem ->
                val chest = chests[roomItem.id] ?: return@forEach

                if (chest.userId == userId) {
                    var updated = false
                    if (chest.autoLock && !chest.locked) {
                        chest.locked = true
                        ChestDao.updateChest(chest)
                        updated = true
                    }

                    if (chest.stateMode != 1) {
                        ChestVisuals.setState(room, chest, open = false)
                    } else if (updated) {
                        ChestVisuals.updateChestExtraData(room, chest)
                    }
                }
            }
    }

    /**
     * Unload do quarto (Room.stopLoop):
     * - Encerra trades de depósito pendentes apontando para os baús do quarto.
     * - Executa o fluxo de auto-lock para qualquer baú do quarto com autoLock ativo que esteja destrancado.
     * - Garante que todo baú que não seja "sempre aberto" (stateMode 1) seja fechado antes do saveRoom().
     * - Enfileira os baús alterados para o savePendingItems persistir no banco.
     */
    fun onRoomUnload(room: Room) {
        val chestRoomItems = room.itemManager.items.values
            .filter { it.furnishing.interactionType == InteractionType.CHEST }

        if (chestRoomItems.isEmpty()) return

        val chestIds = chestRoomItems.map { it.id }.toSet()
        tradeService.clearTradesForChests(chestIds)

        chestRoomItems.forEach { roomItem ->
            val chest = chests[roomItem.id] ?: ChestDao.getChest(roomItem.id) ?: return@forEach

            var needsSave = false

            // Auto-lock ao descarregar o quarto
            if (chest.autoLock && !chest.locked) {
                chest.locked = true
                ChestDao.updateChest(chest)
                needsSave = true
            }

            if (ChestVisuals.formatForUnload(roomItem, chest, autoLockTriggered = needsSave)) {
                room.itemManager.addItemToSave(roomItem)
            }

            chests.remove(roomItem.id)
        }
    }

    // ------------------------------------------------------------------
    // Fachada de Depósito / Wired Trade
    // ------------------------------------------------------------------

    fun getDepositTarget(habboSession: HabboSession): Int =
        tradeService.getDepositTarget(habboSession)

    fun startDeposit(habboSession: HabboSession, room: Room, chest: ChestData) =
        tradeService.startDeposit(habboSession, room, chest)

    fun cancelDeposit(habboSession: HabboSession) =
        tradeService.cancelDeposit(habboSession)

    fun cancelDepositWithNotification(habboSession: HabboSession, failureTypeId: WiredTradeFailureType) =
        tradeService.cancelDepositWithNotification(habboSession, failureTypeId)

    fun wiredTradeAddDeleteItems(
        habboSession: HabboSession,
        room: Room,
        remove: Boolean,
        itemIds: List<Int>,
    ) = tradeService.wiredTradeAddDeleteItems(habboSession, room, remove, itemIds)

    fun wiredTradeConfirm(habboSession: HabboSession, room: Room, confirm: Boolean) =
        tradeService.wiredTradeConfirm(habboSession, room, confirm)

    fun wiredTradeCancel(habboSession: HabboSession) =
        tradeService.wiredTradeCancel(habboSession)

    // ------------------------------------------------------------------
    // Fachada de Retiradas
    // ------------------------------------------------------------------

    fun withdrawItems(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        isWallItem: Boolean,
        typeId: Int,
        amount: Int,
    ) = withdrawalService.withdrawItems(habboSession, room, chest, isWallItem, typeId, amount)

    fun withdrawAll(habboSession: HabboSession, room: Room, chest: ChestData) =
        withdrawalService.withdrawAll(habboSession, room, chest)

    fun withdrawCoins(habboSession: HabboSession, room: Room, chest: ChestData, amount: Int) =
        withdrawalService.withdrawCoins(habboSession, room, chest, amount)

    // ------------------------------------------------------------------
    // Fachada de Configurações e Upgrades
    // ------------------------------------------------------------------

    fun lockAll(habboSession: HabboSession, room: Room, lock: Boolean) =
        settingsService.lockAll(habboSession, room, lock)

    fun setOptions(
        habboSession: HabboSession,
        room: Room,
        chest: ChestData,
        locked: Boolean,
        autoLock: Boolean,
        capacity: Int,
    ) = settingsService.setOptions(habboSession, room, chest, locked, autoLock, capacity)

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
    ) = settingsService.setPreferences(
        habboSession, room, chest, name, description, anyoneCanOpen, anyoneCanDonate,
        stateMode, previewMode, previewAmount
    )

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
    ) = settingsService.setNotificationPreferences(
        habboSession, room, chest, notificationMode, notifyFull, notifyDonation,
        notifyWithdraw, notifyEmpty, notifyTransaction
    )

    fun upgrade(habboSession: HabboSession, room: Room, chest: ChestData, currencyType: Int) =
        settingsService.upgrade(habboSession, room, chest, currencyType)

    fun upgradeWired(habboSession: HabboSession, room: Room, chest: ChestData) =
        settingsService.upgradeWired(habboSession, room, chest)

    // ------------------------------------------------------------------
    // Logs de Transação
    // ------------------------------------------------------------------

    fun getTransactionLogs(chestItemId: Int, pageSize: Int, page: Int): Pair<Int, List<ChestDao.ChestLog>> =
        ChestDao.getChestLogs(chestItemId, pageSize.coerceIn(1, 100), page.coerceAtLeast(1))

    fun getTransactionLogDetails(transactionId: Long): ChestDao.ChestLog? =
        ChestDao.getChestLog(transactionId)
}
