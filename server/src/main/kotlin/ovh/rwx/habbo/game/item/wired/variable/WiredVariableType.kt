/*
 * Copyright (C) 2015-2025 jomp16 <root@rwx.ovh>
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

enum class WiredVariableType(val code: Int) {
    CONDITION_EVALUATION(0), // has custom UI
    UNKNOWN_1(1), // has custom UI
    UNKNOWN_2(2), // has custom UI
    UNKNOWN_3(3), // has custom UI
    UNKNOWN_4(4), // has custom UI
    UNKNOWN_5(5), // has custom UI
    NO_MOVE_ANIMATION(6), // has custom UI
    UNKNOWN_7(7), // has custom UI
    CARRY_USERS(8),
    ANIMATION_TIME(9),
    FURNI_SELECTOR_FILTER(10),
    USER_SELECTOR_FILTER(11),
    FURNI_VARIABLE_FILTER(12),
    USER_VARIABLE_FILTER(13),
    USERNAME_PLACEHOLDER(14),
    VARIABLE_PLACEHOLDER(15),
    VARIABLE_CAPTURER(16),
    EXECUTE_IN_ORDER(17),
    VARIABLE_TEXT_CONVERTER(1000),
    VARIABLE_LEVEL_UP(1001),
    VARIABLE_TIME_UTIL(1002),
    GLOBAL_PLACEHOLDER(2000)
}