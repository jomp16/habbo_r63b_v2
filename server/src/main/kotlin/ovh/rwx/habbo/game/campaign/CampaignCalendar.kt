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

package ovh.rwx.habbo.game.campaign

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Campanha de calendário (advent calendar), nascida no protocolo em
 * PRODUCTION-201512012203 (CampaignCalendarDataMessageEvent / CampaignCalendarDoorOpenedMessageEvent).
 *
 * `currentDay` é o índice 0-based enviado no CampaignCalendarDataMessageEvent e
 * reutilizado no OpenCampaignCalendarDoorComposer (doorIndex) — o cliente usa
 * `currentDay` como selectedIndex em CalendarView.
 */
data class CampaignCalendar(
    val name: String,
    val startDate: LocalDate,
    val days: Int,
    val campaignImage: String,
) {
    fun currentDay(today: LocalDate = LocalDate.now()): Int =
        ChronoUnit.DAYS.between(startDate, today).toInt().coerceIn(0, days - 1)

    fun isActive(today: LocalDate = LocalDate.now()): Boolean =
        !today.isBefore(startDate) && ChronoUnit.DAYS.between(startDate, today) < days
}

/**
 * Recompensa de uma porta do calendário. `productName` alimenta o
 * getProductData/productName do CampaignCalendarDoorOpenedMessageEvent (notificação),
 * `customImage` é sufixo da image gallery, `furnitureClassName` é o item entregue
 * no inventário (e ícone fallback via getFloorItemDataByName).
 */
data class CampaignCalendarReward(
    val campaignName: String,
    val doorIndex: Int,
    val productName: String,
    val customImage: String,
    val furnitureClassName: String,
)

/**
 * Payload do CampaignCalendarDataMessageEvent (Outgoing.CAMPAIGN_CALENDAR):
 * campaignName, campaignImage (campo 2 do parser, ex-"unknown"), currentDay,
 * daysOfCampaign, openedBoxes, lateBoxes (= missedDays no cliente, estado 3).
 */
data class CampaignCalendarData(
    val campaignName: String,
    val campaignImage: String,
    val currentDay: Int,
    val daysOfCampaign: Int,
    val openedBoxes: List<Int>,
    val lateBoxes: List<Int>,
)
