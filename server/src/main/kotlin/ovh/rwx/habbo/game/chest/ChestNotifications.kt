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

/**
 * IDs da notificação transacional wired (WIRED_TRANSACTION_NOTIFICATION / _SafeStr_2838).
 * O client renderiza o texto a partir de wired_transactions.notification.success.<id>.
 *
 * Correlação AS3/external_texts:
 *  0 -> "Depósito do baú realizado"
 *  1 -> "Conteúdos retirados do baú com sucesso"
 *  2 -> "Você ganhou uma recompensa!"
 *  3 -> "Pagamento realizado com sucesso"
 *  4 -> "Negociação realizada"
 *
 * Falhas usam o WIRED_TRADE_CANCELLED com [WiredTradeFailureType].
 */
enum class WiredTransactionNotification(val id: Int) {
    CHEST_DEPOSITED(0),
    CHEST_WITHDRAWN(1),
    REWARD_RECEIVED(2),
    PAYMENT_COMPLETED(3),
    TRADE_COMPLETED(4);

    companion object {
        fun fromId(id: Int): WiredTransactionNotification? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Failure types do popup "Negociação Wired falhou"
 * (wired_transactions.notification.fail.<id>, enviado pelo WIRED_TRADE_CANCELLED).
 */
enum class WiredTradeFailureType(val id: Int) {
    USER_CANCELLED(0),
    TRADE_INVALID(1),
    TIMEOUT(2),
    TRADE_CANCELLED(3),
    USER_ALREADY_TRADING(4),
    BAD_WIRED_SETUP(5),
    INSUFFICIENT_FUNDS(6),
    FUNDS_UNAVAILABLE(7),
    USER_CANNOT_TRADE(8),
    CHEST_OWNER_CANNOT_TRADE(9),
    EMPTY_TRANSACTION(10),
    CHEST_FULL(11),
    FEATURE_DISABLED(12),
    CHEST_NOT_IN_ROOM(13),
    TOO_MANY_CHESTS(14),
    NO_CHESTS_OR_LOCKED(15),
    CANNOT_GIVE_TO_MULTIPLE_USERS(16),
    TOO_MANY_OFFERINGS(17),
    TRADE_TOO_FAST(18),
    CHEST_CAPACITY_EXCEEDED(19),
    INTERNAL_ERROR(1000),
    DATABASE_ERROR(1001),
    DATABASE_ROOM_RELOAD_ERROR(1002);

    companion object {
        fun fromId(id: Int): WiredTradeFailureType? = entries.firstOrNull { it.id == id }
    }
}
