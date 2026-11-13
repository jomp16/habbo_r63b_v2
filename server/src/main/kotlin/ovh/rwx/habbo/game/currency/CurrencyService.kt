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

package ovh.rwx.habbo.game.currency

import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType

/**
 * Preço no formato que o client lê por oferta: credits + UMA moeda alternativa (tipo + quantidade).
 */
data class Price(val credits: Int, val pointType: Int, val points: Int) {
    val isFree: Boolean get() = credits <= 0 && points <= 0
}

/** Resultado de [CurrencyService.check]; [pointType] é a moeda alternativa envolvida (para a mensagem de saldo insuficiente). */
data class BalanceCheck(val notEnoughCredits: Boolean, val notEnoughPoints: Boolean, val pointType: Int) {
    val ok: Boolean get() = !notEnoughCredits && !notEnoughPoints
}

/**
 * Único ponto de verificação, débito e crédito de moedas: catálogo, pets, club, câmera, habbicon, baú, grupos, wired,
 * recompensas etc. passam por aqui, em vez de cada um conhecer pixels/diamantes. Toda mutação de saldo usa o mesmo
 * lock (`userInformation`); não existe débito sem checagem (use [tryCharge]).
 */
object CurrencyService {
    fun canAfford(session: HabboSession, price: Price, times: Int = 1): Boolean = check(session, price, times).ok

    fun check(session: HabboSession, price: Price, times: Int = 1): BalanceCheck {
        val info = session.userInformation
        val credits = price.credits.toLong() * times
        val points = price.points.toLong() * times

        return BalanceCheck(
            notEnoughCredits = price.credits > 0 && info.credits < credits,
            notEnoughPoints = price.points > 0 && info.activityPointsCurrencies[price.pointType] < points,
            pointType = price.pointType
        )
    }

    /** True se o client dessa sessão conhece a moeda do preço (preço sem pontos sempre vale). */
    fun isSupported(session: HabboSession, price: Price): Boolean =
        price.points <= 0 || ActivityPointTypes.isSupported(session.habboVersion, price.pointType)

    /**
     * Confere o saldo e debita de forma atômica (o lock é o próprio `userInformation`): só debita se [BalanceCheck.ok].
     * Duas compras concorrentes da mesma conta não passam ambas pelo mesmo saldo. Reenvia os saldos ao client quando debitou.
     */
    fun tryCharge(session: HabboSession, price: Price, times: Int = 1): BalanceCheck {
        val info = session.userInformation
        val result = tryDebit(info, SessionWallet(info), price, times)
        if (result.ok) session.updateAllCurrencies()
        return result
    }

    /** Devolve [price] × [times] (compensação de uma compra que falhou depois da cobrança) e reenvia os saldos. */
    fun refund(session: HabboSession, price: Price, times: Int = 1) {
        val info = session.userInformation
        applyRefund(info, SessionWallet(info), price, times)
        session.updateAllCurrencies()
    }

    /**
     * Credita prêmios/pagamentos: [credits] em créditos e [points] da moeda [pointType], sob o mesmo lock de
     * [tryCharge]/[refund] (nenhuma atualização se perde), saturando em [Int.MAX_VALUE]. Reenvia os saldos ao client.
     */
    fun grant(session: HabboSession, credits: Int = 0, points: Int = 0, pointType: Int = ActivityPointType.PIXELS.code) {
        grantAll(session, credits, if (points > 0) mapOf(pointType to points) else emptyMap())
    }

    /** Como [grant], mas com várias moedas alternativas de uma vez (recompensa periódica) e um único reenvio de saldos. */
    fun grantAll(session: HabboSession, credits: Int = 0, points: Map<Int, Int> = emptyMap()) {
        val info = session.userInformation
        applyGrant(info, SessionWallet(info), credits, points)
        session.updateAllCurrencies()
    }

    /** Parte pura/testável: saldo mínimo necessário para a conta; independe de [HabboSession]. */
    internal interface Wallet {
        var credits: Int
        fun points(type: Int): Int
        fun addPoints(type: Int, delta: Int)
    }

    private class SessionWallet(private val info: ovh.rwx.habbo.game.user.information.UserInformation) : Wallet {
        override var credits: Int
            get() = info.credits
            set(value) { info.credits = value }

        override fun points(type: Int): Int = info.activityPointsCurrencies[type]
        override fun addPoints(type: Int, delta: Int) = info.activityPointsCurrencies.add(type, delta)
    }

    internal fun checkWallet(wallet: Wallet, price: Price, times: Int): BalanceCheck = BalanceCheck(
        notEnoughCredits = price.credits > 0 && wallet.credits < price.credits.toLong() * times,
        notEnoughPoints = price.points > 0 && wallet.points(price.pointType) < price.points.toLong() * times,
        pointType = price.pointType
    )

    internal fun tryDebit(lock: Any, wallet: Wallet, price: Price, times: Int): BalanceCheck = synchronized(lock) {
        val result = checkWallet(wallet, price, times)
        if (result.ok) {
            if (price.credits > 0) wallet.credits -= price.credits * times
            if (price.points > 0) wallet.addPoints(price.pointType, -(price.points * times))
        }
        result
    }

    internal fun applyGrant(lock: Any, wallet: Wallet, credits: Int, points: Map<Int, Int>) = synchronized(lock) {
        if (credits > 0) wallet.credits = (wallet.credits.toLong() + credits).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        for ((type, amount) in points) if (amount > 0) wallet.addPoints(type, amount)
    }

    internal fun applyRefund(lock: Any, wallet: Wallet, price: Price, times: Int) = synchronized(lock) {
        if (price.credits > 0) wallet.credits += price.credits * times
        if (price.points > 0) wallet.addPoints(price.pointType, price.points * times)
    }
}
