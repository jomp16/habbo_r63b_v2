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

package ovh.rwx.habbo.communication.outgoing.subscription

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBetween
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.subscription.Subscription
import java.time.Duration
import java.time.LocalDateTime

@Suppress("unused", "UNUSED_PARAMETER")
class SubscriptionInfoResponse {
    @Response(Outgoing.HABBO_CLUB_INFO)
    fun response(habboResponse: HabboResponse, subscription: Subscription?) {
        // todo: stub
        val now = LocalDateTime.now()
        val activated = subscription?.takeIf { !it.trial }?.activated

        val currentStreak = activated?.let { Duration.between(it, now).toDays().toInt() } ?: 0
        val firstSubDate = activated?.format(HabboServer.DATE_TIME_FORMATTER_WITH_HOURS).orEmpty()
        val kickbackPercentage = if (activated != null) 0.50 else 0.0
        val bonus = if (activated != null) 5 else 0
        val monthlyReward = if (activated != null) 150 else 0
        val timeUntilPayday = if (activated != null) {
            Duration.between(now, now.plusMonths(2)).toMinutes().toInt()
        } else {
            0
        }

        habboResponse.apply {
            // Campo #1: currentHcStreak (Dias contínuos de HC)
            writeInt(currentStreak)

            // Campo #2: firstSubscriptionDate (Introduzido no Redesign de 20160726+)
            if (isVersionAtLeast(2016, 7, 26)) {
                writeUTF(firstSubDate)
            }

            // Campo #3: kickbackPercentage (ou divisor de cálculo antes de 2016)
            if (isVersionAtLeast(2016, 7, 26)) {
                writeDouble(kickbackPercentage)
            } else {
                writeInt((kickbackPercentage * 10).toInt())
            }

            // Campos #4 & #5: totalCreditsMissed / totalCreditsRewarded
            // (Na fórmula legada pré-2016: atuavam como multiplicador e limiar de desconto)
            writeInt(0) // totalCreditsMissed
            writeInt(0) // totalCreditsRewarded

            // Campo #6: Tabela de Patamares de Desconto Progressivo vs Créditos Gastos
            if (isVersionBetween(2012, 1, 30, 2016, 7, 26)) {
                /*
                 * JANELA 2012.01.30 -> 2016.07.25 (RELEASE63):
                 * No ActionScript (_-0Et / getter _-0CC consumido pela classe _-26x):
                 * Este campo é um LOOP[Int] representando os patamares/degraus (thresholds)
                 * de compra do Habbo Club para cálculo de desconto cumulativo:
                 *
                 * for each (tier in _-0CC) {
                 *     if (amountSpent >= tier) discountMultiplier++;
                 * }
                 *
                 * Estrutura enviada na rede:
                 * writeInt(thresholds.size) // Quantidade de patamares
                 * thresholds.forEach { writeInt(it) } // Cada degrau em moedas (ex: 50, 100, 200)
                 *
                 * Se enviado vazio (0), nenhum desconto por degrau adicional é aplicado na UI:
                 */
                val discountThresholds = emptyList<Int>() // Ex: listOf(10, 50, 100)
                writeInt(discountThresholds.size)
                discountThresholds.forEach { threshold ->
                    writeInt(threshold)
                }
            } else {
                // Antes de 2012 e a partir de 2016 (onde voltou a ser um Int escalar simples: totalCreditsSpent)
                writeInt(0)
            }

            // Campo #7: creditRewardForStreakBonus (Ausente apenas no intervalo [2011-12-13, 2012-01-30))
            if (!isVersionBetween(2011, 12, 13, 2012, 1, 30)) {
                writeInt(bonus)
            }

            // Campos #8 & #9: Recompensa mensal e tempo restante até o Payday (20160726+)
            if (isVersionAtLeast(2016, 7, 26)) {
                writeInt(monthlyReward)
                writeInt(timeUntilPayday)
            }
        }
    }
}