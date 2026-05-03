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

package ovh.rwx.habbo.game.room.trading

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Representa uma sessão de troca entre dois usuários.
 *
 * THREAD-SAFETY:
 * - Todas as operações públicas são protegidas por [tradeLock] (ReentrantLock)
 * - Usa ConcurrentHashMap para as grades de itens
 * - Operações atômicas garantem que não há race conditions
 *
 * SEGURANÇA:
 * - Valida propriedade dos itens antes de adicionar
 * - Previne duplicação de IDs na mesma grade
 * - Reseta estado ACCEPTED quando grade é modificada
 * - Trava itens no inventário para prevenir uso em outra troca
 */
class Trade(
    val room: Room,
    val user1: RoomUser,
    val user2: RoomUser
) {
    // Lock principal para thread-safety em todas as operações
    private val tradeLock = ReentrantLock()

    // Estado atual da troca
    var state: TradeState = TradeState.OPEN
        private set

    // Grades de itens de cada usuário (ConcurrentHashMap para segurança)
    private val user1Items = ConcurrentHashMap<Int, TradeItem>()
    private val user2Items = ConcurrentHashMap<Int, TradeItem>()

    // Status de aceite de cada usuário
    private var user1Accepted = false
    private var user2Accepted = false

    // Confirmação final de cada usuário
    private var user1Confirmed = false
    private var user2Confirmed = false

    // IDs dos usuários
    val user1Id: Int get() = user1.habboSession.userInformation.id
    val user2Id: Int get() = user2.habboSession.userInformation.id

    val user1VirtualId: Int get() = user1.virtualID
    val user2VirtualId: Int get() = user2.virtualID

    /**
     * Verifica se um usuário específico é o usuário 1.
     */
    fun isUser1(roomUser: RoomUser): Boolean = roomUser.virtualID == user1VirtualId

    /**
     * Obtém a grade de itens de um usuário específico.
     */
    fun getItems(roomUser: RoomUser): Map<Int, TradeItem> = tradeLock.withLock {
        if (isUser1(roomUser)) user1Items.toMap() else user2Items.toMap()
    }

    /**
     * Obtém todos os itens de ambos os usuários (para serialização).
     */
    fun getAllItems(): Pair<Map<Int, TradeItem>, Map<Int, TradeItem>> = tradeLock.withLock {
        user1Items.toMap() to user2Items.toMap()
    }

    /**
     * Adiciona um item à grade de troca de um usuário.
     * 
     * REGRAS DE SEGURANÇA:
     * - Só pode adicionar se a troca estiver OPEN
     * - Valida se o usuário possui o item no inventário
     * - Previne duplicação de ID na grade
     * - Reseta estado ACCEPTED se já estava aceito
     * - Trava o item no inventário
     * 
     * @return Resultado da operação (Success, Error)
     */
    fun addItem(roomUser: RoomUser, item: UserItem): AddItemResult = tradeLock.withLock {
        // Verifica estado da troca
        if (state != TradeState.OPEN) {
            return@withLock AddItemResult.NotOpen
        }

        // Verifica se o item já está na grade (previne duplicação)
        val targetItems = if (isUser1(roomUser)) user1Items else user2Items
        if (targetItems.containsKey(item.id)) {
            return@withLock AddItemResult.AlreadyInTrade
        }

        // Verifica se o usuário realmente possui o item
        val inventory = roomUser.habboSession.habboInventory.items
        if (!inventory.containsKey(item.id)) {
            return@withLock AddItemResult.ItemNotOwned
        }

        // Verifica se o item não está em outra troca (double-check)
        if (room.tradeManager.isItemInTrade(item.id, roomUser)) {
            return@withLock AddItemResult.ItemLocked
        }

        // Verifica se o item pode ser trocado (allowTrade)
        if (!item.furnishing.allowTrade) {
            return@withLock AddItemResult.NotTradable
        }

        // Adiciona o item à grade
        val tradeItem = TradeItem(userItem = item, locked = true)
        targetItems[item.id] = tradeItem

        // Reseta o estado de aceite (qualquer modificação invalida o aceite)
        resetAcceptState(roomUser)

        // Envia atualização da lista de itens para ambos
        sendItemListToBoth()

        return@withLock AddItemResult.Success
    }

    /**
     * Remove um item da grade de troca de um usuário.
     * 
     * REGRAS DE SEGURANÇA:
     * - Só pode remover se a troca estiver OPEN
     * - Reseta estado ACCEPTED se já estava aceito
     * - Destrava o item no inventário
     * 
     * @return true se removeu com sucesso, false se item não existia
     */
    fun removeItem(roomUser: RoomUser, itemId: Int): Boolean = tradeLock.withLock {
        // Verifica estado da troca
        if (state != TradeState.OPEN) {
            return@withLock false
        }

        val targetItems = if (isUser1(roomUser)) user1Items else user2Items
        val removedItem = targetItems.remove(itemId)

        if (removedItem != null) {
            // Destrava o item
            removedItem.locked = false

            // Reseta o estado de aceite
            resetAcceptState(roomUser)

            // Envia atualização da lista de itens para ambos
            sendItemListToBoth()

            return@withLock true
        }

        return@withLock false
    }

    /**
     * Aceita a troca por um usuário.
     *
     * Se ambos aceitarem e a troca estiver OPEN, transita para ACCEPTED e depois CONFIRMING.
     * Envia TRADING_ACCEPT para cada aceite e TRADING_CONFIRMATION quando ambos aceitarem.
     */
    fun acceptTrade(roomUser: RoomUser): Boolean = tradeLock.withLock {
        if (state !in listOf(TradeState.OPEN, TradeState.ACCEPTED, TradeState.CONFIRMING)) {
            return@withLock false
        }

        // Verifica se o usuário já não aceitou
        val alreadyAccepted = if (isUser1(roomUser)) user1Accepted else user2Accepted
        if (alreadyAccepted) {
            return@withLock false
        }

        // Marca o aceite do usuário
        if (isUser1(roomUser)) {
            user1Accepted = true
        } else {
            user2Accepted = true
        }

        // Verifica se ambos aceitaram
        val bothAccepted = user1Accepted && user2Accepted

        if (bothAccepted) {
            when (state) {
                TradeState.OPEN -> {
                    state = TradeState.ACCEPTED
                    // Transita imediatamente para CONFIRMING
                    state = TradeState.CONFIRMING
                    // Envia TRADING_CONFIRMATION para ambos quando entra em estado de confirmação
                    sendConfirmationEvent()
                }

                TradeState.ACCEPTED -> {
                    state = TradeState.CONFIRMING
                    // Envia TRADING_CONFIRMATION para ambos quando entra em estado de confirmação
                    sendConfirmationEvent()
                }

                TradeState.CONFIRMING -> {
                    // Já está em confirmação, não faz nada
                }

                else -> {}
            }
        }

        // Envia evento de aceite para ambos (mostra quem aceitou)
        sendAcceptEvent(roomUser.habboSession.userInformation.id, true)

        return@withLock true
    }

    /**
     * Remove o aceite de um usuário (Unaccept).
     * A troca volta para OPEN se estava em ACCEPTED.
     */
    fun unacceptTrade(roomUser: RoomUser): Boolean = tradeLock.withLock {
        if (state !in listOf(TradeState.OPEN, TradeState.ACCEPTED, TradeState.CONFIRMING)) {
            return@withLock false
        }

        // Remove o aceite do usuário
        if (isUser1(roomUser)) {
            user1Accepted = false
            user1Confirmed = false
        } else {
            user2Accepted = false
            user2Confirmed = false
        }

        // Se estava em CONFIRMING, volta para OPEN
        if (state == TradeState.CONFIRMING) {
            state = TradeState.OPEN
        }

        // Envia evento de aceite atualizado (accepted=false)
        sendAcceptEvent(roomUser.habboSession.userInformation.id, false)

        return@withLock true
    }

    /**
     * Confirmação final da troca (ConfirmAccept).
     * Ambos devem confirmar para completar a troca.
     * 
     * Fluxo:
     * 1. Usuário confirma → Envia TRADING_ACCEPT para ambos
     * 2. Ambos confirmaram → Envia TRADING_COMPLETED e executa a troca
     */
    fun confirmAccept(roomUser: RoomUser): ConfirmResult = tradeLock.withLock {
        if (state != TradeState.CONFIRMING) {
            return@withLock ConfirmResult.NotConfirming
        }

        // Marca confirmação do usuário
        if (isUser1(roomUser)) {
            user1Confirmed = true
        } else {
            user2Confirmed = true
        }

        // Envia TRADING_ACCEPT para ambos (mostra que este usuário confirmou)
        sendAcceptEvent(roomUser.habboSession.userInformation.id, true)

        // Verifica se ambos confirmaram
        val bothConfirmed = user1Confirmed && user2Confirmed

        if (bothConfirmed) {
            // Executa a transferência de itens
            return@withLock when (val result = executeTrade()) {
                TradeExecutionResult.Success -> {
                    state = TradeState.COMPLETED
                    sendCompletedEvent()
                    ConfirmResult.Success
                }

                TradeExecutionResult.ItemNotFound -> {
                    // Item não existe mais no inventário (possível exploit)
                    state = TradeState.CANCELLED
                    unlockAllItems()
                    sendCloseEvent()
                    ConfirmResult.ItemNotFound
                }

                TradeExecutionResult.DatabaseError -> {
                    // Erro no banco, cancela a troca
                    state = TradeState.CANCELLED
                    unlockAllItems()
                    sendCloseEvent()
                    ConfirmResult.DatabaseError
                }
            }
        }

        return@withLock ConfirmResult.WaitingOther
    }

    /**
     * Declina a confirmação final (ConfirmDecline).
     * Cancela a troca imediatamente.
     */
    fun confirmDecline(): Boolean = tradeLock.withLock {
        if (state != TradeState.CONFIRMING) {
            return@withLock false
        }

        cancelTrade()

        return@withLock true
    }

    /**
     * Fecha/cancela a troca.
     * Destrava todos os itens e notifica ambos os usuários.
     */
    fun closeTrade(): Boolean = tradeLock.withLock {
        if (state in listOf(TradeState.COMPLETED, TradeState.CANCELLED)) {
            return@withLock false
        }

        cancelTrade()

        return@withLock true
    }

    /**
     * Cancela internamente a troca.
     * Deve ser chamado com lock adquirido.
     */
    private fun cancelTrade() {
        state = TradeState.CANCELLED
        unlockAllItems()
        sendCloseEvent()
    }

    /**
     * Destrava todos os itens de ambos os usuários.
     */
    private fun unlockAllItems() {
        (user1Items.values + user2Items.values).forEach { tradeItem ->
            tradeItem.locked = false
        }
    }

    /**
     * Reseta o estado de aceite quando a grade é modificada.
     * Deve ser chamado com lock adquirido.
     * 
     * REGRAS:
     * - Qualquer modificação na grade (adicionar/remover item) reseta o aceite de AMBOS os usuários
     * - A troca volta para OPEN se estava em ACCEPTED ou CONFIRMING
     */
    private fun resetAcceptState(modifierUser: RoomUser) {
        // Reseta o aceite de AMBOS os usuários
        user1Accepted = false
        user1Confirmed = false
        user2Accepted = false
        user2Confirmed = false

        // Se estava em ACCEPTED ou CONFIRMING, volta para OPEN
        if (state in listOf(TradeState.ACCEPTED, TradeState.CONFIRMING)) {
            state = TradeState.OPEN
        }

        // Envia TRADING_ACCEPT(false) para ambos, mostrando que o aceite foi resetado
        sendAcceptEvent(modifierUser.habboSession.userInformation.id, false)
    }

    /**
     * Executa a transferência de itens no banco de dados.
     * Usa transação atômica para garantir consistência.
     */
    private fun executeTrade(): TradeExecutionResult {
        val allUser1Items = user1Items.values.map { it.userItem }
        val allUser2Items = user2Items.values.map { it.userItem }

        if (allUser1Items.isEmpty() && allUser2Items.isEmpty()) {
            return TradeExecutionResult.Success // Troca vazia, só completa
        }

        try {
            // Transação atômica: transfere todos os itens de uma vez
            ItemDao.transferTradeItems(
                user1Id = user1Id,
                user2Id = user2Id,
                user1Items = allUser1Items.map { it.id },
                user2Items = allUser2Items.map { it.id }
            )

            // Atualiza inventários em memória e envia pacotes de update
            // User1 recebe os itens do User2 e remove os itens enviados
            user1.habboSession.let { session ->
                val receivedItems = allUser2Items.map { it.copy(userId = user1Id) }
                session.habboInventory.addItems(receivedItems)
                // Remove os itens que foram enviados (envia pacote de remoção)
                session.habboInventory.removeItems(allUser1Items.map { it.id }, delete = false)
            }

            // User2 recebe os itens do User1 e remove os itens enviados
            user2.habboSession.let { session ->
                val receivedItems = allUser1Items.map { it.copy(userId = user2Id) }
                session.habboInventory.addItems(receivedItems)
                // Remove os itens que foram enviados (envia pacote de remoção)
                session.habboInventory.removeItems(allUser2Items.map { it.id }, delete = false)
            }

            return TradeExecutionResult.Success
        } catch (e: Exception) {
            return TradeExecutionResult.DatabaseError
        }
    }

    // ===== MÉTODOS DE ENVIO DE EVENTOS =====
    private fun sendItemListToBoth() {
        val (items1, items2) = getAllItems()

        user1.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(
                    OutgoingR63A.TRADING_ITEM_LIST,
                    user1Id, items1.values.map { it.userItem },
                    user2Id, items2.values.map { it.userItem }
                )
            } else {
                session.sendHabboResponse(
                    Outgoing.TRADING_ITEM_LIST,
                    user1Id, items1.values.map { it.userItem },
                    user2Id, items2.values.map { it.userItem }
                )
            }
        }
        user2.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(
                    OutgoingR63A.TRADING_ITEM_LIST,
                    user1Id, items1.values.map { it.userItem },
                    user2Id, items2.values.map { it.userItem }
                )
            } else {
                session.sendHabboResponse(
                    Outgoing.TRADING_ITEM_LIST,
                    user1Id, items1.values.map { it.userItem },
                    user2Id, items2.values.map { it.userItem }
                )
            }
        }
    }

    private fun sendAcceptEvent(userId: Int, accepted: Boolean) {
        // Envia para user1
        user1.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_ACCEPT, userId, accepted)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_ACCEPT, userId, accepted)
            }
        }

        // Envia para user2
        user2.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_ACCEPT, userId, accepted)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_ACCEPT, userId, accepted)
            }
        }
    }

    private fun sendConfirmationEvent() {
        user1.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_CONFIRMATION)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_CONFIRMATION)
            }
        }
        user2.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_CONFIRMATION)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_CONFIRMATION)
            }
        }
    }

    private fun sendCompletedEvent() {
        user1.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_COMPLETED)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_COMPLETED)
            }
        }
        user2.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_COMPLETED)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_COMPLETED)
            }
        }
    }

    private fun sendCloseEvent() {
        user1.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_CLOSE, user1Id)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_CLOSE, user1Id)
            }
        }
        user2.habboSession.let { session ->
            if (session.release == "R63A") {
                session.sendHabboResponse(OutgoingR63A.TRADING_CLOSE, user2Id)
            } else {
                session.sendHabboResponse(Outgoing.TRADING_CLOSE, user2Id)
            }
        }
    }

    /**
     * Manipula desconexão de um usuário.
     * Cancela a troca e destrava itens.
     */
    fun onUserDisconnect() = tradeLock.withLock {
        cancelTrade()
    }

    /**
     * Verifica se um item específico está nesta troca.
     */
    fun containsItem(itemId: Int): Boolean = tradeLock.withLock {
        user1Items.containsKey(itemId) || user2Items.containsKey(itemId)
    }
}

// ===== RESULT TYPES =====
sealed class AddItemResult {
    object Success : AddItemResult()
    object NotOpen : AddItemResult()
    object AlreadyInTrade : AddItemResult()
    object ItemNotOwned : AddItemResult()
    object ItemLocked : AddItemResult()
    object NotTradable : AddItemResult()
}

sealed class ConfirmResult {
    object Success : ConfirmResult()
    object NotConfirming : ConfirmResult()
    object ItemNotFound : ConfirmResult()
    object DatabaseError : ConfirmResult()
    object WaitingOther : ConfirmResult()
}

enum class TradeExecutionResult {
    Success,
    ItemNotFound,
    DatabaseError
}
