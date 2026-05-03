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

package ovh.rwx.habbo.game.pet

enum class PetTrick(
    val commandName: String,
    val levelRequired: Int,
    val energyCost: Int,
    val experienceReward: Int,
    val durationTicks: Int,
    val statusKey: String
) {
    FREE("free", 0, 0, 0, 1, ""),
    SIT("sit", 0, 2, 10, 8, "sit"),
    LAY("down", 1, 2, 10, 8, "lay"),
    BEG("beg", 4, 3, 15, 6, "beg"),
    PLAY_DEAD("play dead", 6, 5, 20, 8, "ded"),
    JUMP("jump", 3, 5, 20, 4, "jmp"),
    SPEAK("speak", 2, 3, 15, 2, ""),
    STAND("stand", 0, 0, 5, 1, ""),
    FOLLOW("follow", 5, 4, 15, -1, "");

    companion object {
        fun fromCommand(command: String): PetTrick? =
            entries.find { it.commandName.equals(command, ignoreCase = true) }
    }
}
