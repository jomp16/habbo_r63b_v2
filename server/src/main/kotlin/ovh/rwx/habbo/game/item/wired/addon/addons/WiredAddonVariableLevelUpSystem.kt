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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Suppress("unused")
@WiredItemInteractor(InteractionType.WIRED_EXTRA_VARIABLE_LEVELUP_SYSTEM)
class WiredAddonVariableLevelUpSystem(room: Room, roomItem: RoomItem) : WiredAddon(room, roomItem) {
    override val addonType = WiredAddonType.VARIABLE_LEVEL_UP

    override fun onAddon(wiredContext: WiredContext) {
        val options = roomItem.wiredData?.options ?: emptyList()
        val mask = options.getOrElse(0) { 0xFF }
        val mode = options.getOrElse(1) { 1 }

        val targetVarId = roomItem.wiredData?.variableIds?.firstOrNull() ?: "xp"
        val totalXp = (wiredContext.getVariable(targetVarId) as? Number)?.toLong()
            ?: (wiredContext.getVariable("xp") as? Number)?.toLong()
            ?: (wiredContext.getVariable("current_xp") as? Number)?.toLong()
            ?: 0L

        var maxLevel = 50
        var currentLevel = 1
        var xpRequired = 100L
        var xpForCurrentLevel = 0L
        var xpForNextLevel = 100L

        when (mode) {
            1 -> { // Linear
                val stepSize = max(1, options.getOrElse(2) { 100 })
                maxLevel = max(2, options.getOrElse(3) { 50 })
                currentLevel = min(maxLevel, (totalXp / stepSize).toInt() + 1)
                xpForCurrentLevel = (currentLevel - 1).toLong() * stepSize
                xpForNextLevel = if (currentLevel >= maxLevel) xpForCurrentLevel else currentLevel.toLong() * stepSize
                xpRequired = stepSize.toLong()
            }

            2 -> { // Exponential
                val firstLevelXp = max(1, options.getOrElse(2) { 100 })
                val factor = max(1, options.getOrElse(3) { 20 })
                maxLevel = max(2, options.getOrElse(4) { 50 })

                var accumulated = 0L
                currentLevel = 1
                for (lvl in 1..maxLevel) {
                    val req = (firstLevelXp * (1.0 + factor / 100.0).pow((lvl - 1).toDouble())).toLong()
                    if (totalXp < accumulated + req || lvl == maxLevel) {
                        currentLevel = lvl
                        xpForCurrentLevel = accumulated
                        xpRequired = req
                        xpForNextLevel = accumulated + req
                        break
                    }
                    accumulated += req
                }
            }

            0 -> { // Manual table
                val lines = (roomItem.wiredData?.message ?: "").lines().filter { it.contains("=") }
                val table = sortedMapOf<Int, Long>()
                for (line in lines) {
                    val parts = line.split("=")
                    val lvl = parts.getOrNull(0)?.trim()?.toIntOrNull()
                    val xp = parts.getOrNull(1)?.trim()?.toLongOrNull()
                    if (lvl != null && xp != null) {
                        table[lvl] = xp
                    }
                }
                if (table.isNotEmpty()) {
                    maxLevel = table.lastKey()
                    currentLevel = 1
                    for ((lvl, xp) in table) {
                        if (totalXp >= xp) {
                            currentLevel = lvl
                            xpForCurrentLevel = xp
                        } else {
                            xpForNextLevel = xp
                            break
                        }
                    }
                    val nextXp = table[currentLevel + 1] ?: xpForCurrentLevel
                    xpRequired = max(1L, nextXp - xpForCurrentLevel)
                    xpForNextLevel = nextXp
                }
            }
        }

        val isMaxed = currentLevel >= maxLevel
        val progress = if (isMaxed) xpRequired else max(0L, totalXp - xpForCurrentLevel)
        val progressPercentage = if (isMaxed) 100 else min(100, (progress * 100 / max(1L, xpRequired)).toInt())
        val xpRemaining = if (isMaxed) 0L else max(0L, xpForNextLevel - totalXp)

        val subVars = mapOf(
            0 to Pair("current_level", currentLevel),
            1 to Pair("current_xp", totalXp),
            2 to Pair("progress", progress),
            3 to Pair("progress_percentage", progressPercentage),
            4 to Pair("xp_required", xpRequired),
            5 to Pair("xp_remaining", xpRemaining),
            6 to Pair("is_maxed", if (isMaxed) 1 else 0),
            7 to Pair("max_level", maxLevel)
        )

        for ((bit, pair) in subVars) {
            if ((mask and (1 shl bit)) != 0 || mask == 0) {
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
