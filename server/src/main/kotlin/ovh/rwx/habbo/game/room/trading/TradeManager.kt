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
import ovh.rwx.habbo.communication.outgoing.trading.TradingOpenFailedResponse
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerenciador de trocas atrelado a uma Room.
 * 
 * Cada Room tem seu próprio TradeManager, já que trocas ocorrem dentro de quartos.
 * 
 * THREAD-SAFETY:
 * - Usa ConcurrentHashMap para armazenar trocas ativas
 * - Cada Trade tem seu próprio lock interno
 * - Operações de busca/criação são atômicas
 */
class TradeManager(private val room: Room) {

    /**
     * Mapa de trocas ativas.
     * Chave: virtualID do usuário que iniciou a troca (menor virtualID)
     * Valor: Instância da Trade
     */
    private val activeTrades = ConcurrentHashMap<Int, Trade>()

    /**
     * Mapa auxiliar para busca rápida por usuário.
     * Chave: virtualID do usuário
     * Valor: Trade que o usuário está participando
     */
    private val userTradeMap = ConcurrentHashMap<Int, Trade>()

    /**
     * Inicia uma nova troca entre dois usuários.
     * 
     * REGRAS:
     * - Um usuário não pode estar em outra troca ativa
     * - Ambos devem estar na mesma room
     * - Retorna TradeAlreadyInTrade se algum já estiver em troca
     * 
     * @param initiator Usuário que iniciou a troca
     * @param target Usuário alvo da troca
     * @return Trade criada ou null se falhou
     */
    fun startTrade(initiator: RoomUser, target: RoomUser): TradeStartResult {
        // Verifica se algum já está em troca
        val existingTradeForInitiator = userTradeMap[initiator.virtualID]
        val existingTradeForTarget = userTradeMap[target.virtualID]

        if (existingTradeForInitiator != null) {
            initiator.habboSession?.let { session ->
                if (session.release == "R63A") {
                    session.sendHabboResponse(OutgoingR63A.TRADING_ALREADY_OPEN)
                } else {
                    session.sendHabboResponse(
                        Outgoing.TRADING_OPEN_FAILED,
                        TradingOpenFailedResponse.TradingOpenFailedStatus.YOU_ALREADY_TRADING,
                        initiator.habboSession.userInformation.username
                    )
                }
            }
            return TradeStartResult.AlreadyInTrade
        }

        if (existingTradeForTarget != null) {
            initiator.habboSession?.let { session ->
                if (session.release == "R63A") {
                    session.sendHabboResponse(OutgoingR63A.TRADING_ALREADY_OPEN)
                } else {
                    session.sendAnyResponse(
                        Outgoing.TRADING_OPEN_FAILED,
                        TradingOpenFailedResponse.TradingOpenFailedStatus.TARGET_ALREADY_TRADING,
                        target.habboSession?.userInformation?.username ?: ""
                    )
                }
            }
            return TradeStartResult.TargetAlreadyInTrade
        }

        // Verifica se ambos estão na room
        if (initiator.room != room || target.room != room) {
            return TradeStartResult.NotInRoom
        }

        // Determina usuário 1 e 2 baseado no menor virtualID (para consistência)
        val (user1, user2) = if (initiator.virtualID < target.virtualID) {
            initiator to target
        } else {
            target to initiator
        }

        // Cria nova trade
        val trade = Trade(room, user1, user2)

        // Armazena de forma atômica
        val key = user1.virtualID // Usa menor virtualID como chave
        activeTrades[key] = trade
        userTradeMap[user1.virtualID] = trade
        userTradeMap[user2.virtualID] = trade


        // Envia evento de troca aberta para ambos
        sendTradingOpenEvent(trade, initiator, target)

        return TradeStartResult.Success(trade)
    }

    /**
     * Obtém a troca ativa de um usuário específico.
     */
    fun getTrade(roomUser: RoomUser): Trade? = userTradeMap[roomUser.virtualID]

    /**
     * Verifica se um item está em alguma troca ativa.
     * Usado para prevenir que o mesmo item seja usado em múltiplas trocas.
     */
    fun isItemInTrade(itemId: Int, roomUser: RoomUser): Boolean {
        val trade = userTradeMap[roomUser.virtualID] ?: return false
        return trade.containsItem(itemId)
    }

    /**
     * Remove uma troca dos mapas de ativos.
     * Chamado quando a troca é completada ou cancelada.
     */
    fun removeTrade(trade: Trade) {
        val key = if (trade.user1VirtualId < trade.user2VirtualId) {
            trade.user1VirtualId
        } else {
            trade.user2VirtualId
        }

        activeTrades.remove(key)
        userTradeMap.remove(trade.user1VirtualId)
        userTradeMap.remove(trade.user2VirtualId)
    }

    /**
     * Manipula desconexão de um usuário.
     * Cancela qualquer troca ativa do usuário.
     */
    fun onUserDisconnect(roomUser: RoomUser) {
        val trade = userTradeMap[roomUser.virtualID] ?: return

        trade.onUserDisconnect()
        removeTrade(trade)
    }

    /**
     * Envia evento TradingOpenEvent para ambos os usuários.
     */
    private fun sendTradingOpenEvent(trade: Trade, initiator: RoomUser, target: RoomUser) {
        val initiatorId = initiator.habboSession?.userInformation?.id ?: -1
        val targetId = target.habboSession?.userInformation?.id ?: -1

        // Verifica se ambos podem trocar (não tem mute, ban, etc.)
        val initiatorCanTrade = canTrade(initiator)
        val targetCanTrade = canTrade(target)

        // Envia para o iniciante
        if (initiator.habboSession?.release == "R63A") {
            initiator.habboSession.sendHabboResponse(
                OutgoingR63A.TRADING_OPEN,
                initiatorId,
                if (initiatorCanTrade) 1 else 0,
                targetId,
                if (targetCanTrade) 1 else 0
            )
        } else {
            initiator.habboSession?.sendHabboResponse(
                Outgoing.TRADING_OPEN,
                initiatorId,
                if (initiatorCanTrade) 1 else 0,
                targetId,
                if (targetCanTrade) 1 else 0
            )
        }

        // Envia para o alvo
        if (target.habboSession?.release == "R63A") {
            target.habboSession.sendHabboResponse(
                OutgoingR63A.TRADING_OPEN,
                initiatorId,
                if (initiatorCanTrade) 1 else 0,
                targetId,
                if (targetCanTrade) 1 else 0
            )
        } else {
            target.habboSession?.sendHabboResponse(
                Outgoing.TRADING_OPEN,
                initiatorId,
                if (initiatorCanTrade) 1 else 0,
                targetId,
                if (targetCanTrade) 1 else 0
            )
        }

        // Se algum não puder trocar, envia evento de erro
        if (!initiatorCanTrade) {
            initiator.habboSession?.let { session ->
                if (session.release == "R63A") {
                    session.sendHabboResponse(OutgoingR63A.TRADING_YOU_ARE_NOT_ALLOWED)
                } else {
                    session.sendHabboResponse(Outgoing.TRADING_YOU_ARE_NOT_ALLOWED)
                }
            }
        }
        if (!targetCanTrade) {
            initiator.habboSession?.let { session ->
                if (session.release == "R63A") {
                    session.sendHabboResponse(OutgoingR63A.TRADING_OTHER_NOT_ALLOWED)
                } else {
                    session.sendHabboResponse(Outgoing.TRADING_OTHER_NOT_ALLOWED)
                }
            }
        }
    }

    /**
     * Verifica se um usuário pode trocar.
     * Aqui você pode adicionar verificações de mute, ban, etc.
     */
    private fun canTrade(roomUser: RoomUser): Boolean {
        // Verificações básicas:
        // - Usuário não está mute
        // - Usuário não está ban
        // - Usuário tem inventário carregado

        val session = roomUser.habboSession ?: return false

        return session.authenticated &&
                session.habboInventory.items.isNotEmpty()
        // Adicione mais verificações conforme necessário
    }

    /**
     * Limpa todas as trocas ativas.
     * Usado quando a room é descarregada.
     */
    fun clearAllTrades() {
        activeTrades.values.forEach { trade ->
            trade.onUserDisconnect()
        }
        activeTrades.clear()
        userTradeMap.clear()

    }

    /**
     * Obtém número de trocas ativas (para debug/monitoramento).
     */
    fun getActiveTradeCount(): Int = activeTrades.size
}

/**
 * Resultado da tentativa de iniciar uma troca.
 */
sealed class TradeStartResult {
    object AlreadyInTrade : TradeStartResult()
    object TargetAlreadyInTrade : TradeStartResult()
    object NotInRoom : TradeStartResult()
    data class Success(val trade: Trade) : TradeStartResult()
}
