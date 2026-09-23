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

package ovh.rwx.habbo.game.item.wired.addon.addons

import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.WiredData
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.WiredContext
import ovh.rwx.habbo.game.item.wired.WiredItemInteractor
import ovh.rwx.habbo.game.item.wired.addon.WiredAddon
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.room.Room
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.IsoFields

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_VARIABLE_TIME_UTIL)
class WiredAddonVariableTimeUtil(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.VARIABLE_TIME_UTIL

    override fun onAddon(wiredContext: WiredContext) {
        val options = roomItem.wiredData?.options ?: emptyList()
        val mask = options.getOrElse(0) { 0xFFFFFFFF.toInt() }

        val targetVarId = roomItem.wiredData?.variableIds?.firstOrNull() ?: "timestamp"
        var rawTime = (wiredContext.getVariable(targetVarId) as? Number)?.toLong()
            ?: (wiredContext.getVariable("timestamp") as? Number)?.toLong()
            ?: (wiredContext.getVariable("time") as? Number)?.toLong()
            ?: System.currentTimeMillis()

        if (rawTime in 1..99_999_999_999L) {
            rawTime *= 1000L
        }

        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rawTime), ZoneId.systemDefault())

        val timeVars = mapOf(
            1 to Pair("milliseconds_of_seconds", zdt.nano / 1_000_000),
            2 to Pair("seconds_of_minute", zdt.second),
            3 to Pair("minute_of_hour", zdt.minute),
            4 to Pair("hour_of_day", zdt.hour),
            5 to Pair("day_of_week", zdt.dayOfWeek.value),
            6 to Pair("day_of_month", zdt.dayOfMonth),
            7 to Pair("day_of_year", zdt.dayOfYear),
            8 to Pair("week_of_year", zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)),
            9 to Pair("month_of_year", zdt.monthValue),
            10 to Pair("year", zdt.year),
            20 to Pair("millisecond", rawTime),
            21 to Pair("second", rawTime / 1000L),
            22 to Pair("minute", rawTime / 60000L),
            23 to Pair("hour", rawTime / 3600000L),
            24 to Pair("day", rawTime / 86400000L),
            25 to Pair("week", rawTime / (86400000L * 7L)),
            26 to Pair("month", (zdt.year * 12) + zdt.monthValue)
        )

        for ((id, pair) in timeVars) {
            val bit = 1 shl id
            if ((mask and bit) != 0 || mask == 0 || mask == -1) {
                wiredContext.variables[pair.first] = pair.second
                wiredContext.placeholders[pair.first] = pair.second.toString()
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun getDefaultWiredData(): WiredData {
            return WiredData(0, 0, emptyList(), "", emptyList(), "")
        }
    }
}
