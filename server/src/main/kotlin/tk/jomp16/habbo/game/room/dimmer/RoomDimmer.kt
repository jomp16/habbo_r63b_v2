/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.game.room.dimmer

import tk.jomp16.habbo.game.item.room.RoomItem

data class RoomDimmer(
        val id: Int,
        val roomItem: RoomItem,
        var enabled: Boolean,
        var currentPreset: Int,
        val presets: MutableList<RoomDimmerPreset>
) {
    fun generateExtraData(roomDimmerPreset: RoomDimmerPreset = presets[currentPreset - 1]): String {
        return "${(if (enabled) 2 else 1)},$currentPreset,${if (roomDimmerPreset.backgroundOnly) 2 else 1},${roomDimmerPreset.colorCode},${roomDimmerPreset.colorIntensity}"
    }

    fun switchState() {
        enabled = !enabled

        roomItem.extraData = generateExtraData()

        roomItem.update(true, true)
    }

    fun updatePreset(preset: Int, colorCode: String, intensity: Int, backgroundOnly: Boolean) {
        if (!isValidColor(colorCode) || !isValidIntensity(intensity)) return

        presets[preset - 1] = generatePreset("$colorCode,$intensity,${if (backgroundOnly) 1 else 0}")

        currentPreset = preset

        roomItem.extraData = generateExtraData()
        roomItem.update(true, true)
    }

    companion object {
        fun generatePreset(data: String): RoomDimmerPreset {
            val bits = data.split(',').toMutableList()

            if (!isValidColor(bits[0])) bits[0] = "#000000"

            return RoomDimmerPreset(bits[0], bits[1].toInt(), bits[2] == "1")
        }

        fun isValidColor(color: String): Boolean {
            when (color) {
                "#000000", "#0053F7", "#EA4532", "#82F349", "#74F5F5", "#E759DE", "#F2F851" -> return true
            }

            return false
        }

        fun isValidIntensity(colorIntensity: Int): Boolean = colorIntensity >= 0 && colorIntensity <= 255
    }
}