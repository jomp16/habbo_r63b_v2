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

package ovh.rwx.habbo.game.item.wired.variable

enum class WiredVariableItemType(val code: Int) {
    FURNI_VARIABLE(0),
    USER_VARIABLE(1),
    GLOBAL_VARIABLE(2),
    CONTEXT_VARIABLE(3),
    REFERENCE_VARIABLE(4),
    QUEST_VARIABLE(5),
    QUEST_CHAIN_VARIABLE(6),
    ECHO_VARIABLE(7),
    DAILY_TASK_VARIABLE(8);

    companion object {
        fun fromCode(code: Int): WiredVariableItemType? = entries.firstOrNull { it.code == code }
    }
}
