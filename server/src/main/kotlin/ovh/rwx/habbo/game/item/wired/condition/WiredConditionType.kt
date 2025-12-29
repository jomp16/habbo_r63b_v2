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

package ovh.rwx.habbo.game.item.wired.condition

enum class WiredConditionType(val code: Int) {
    STATES_MATCH(0), // has custom UI
    FURNIS_HAVE_AVATARS(1), // has custom UI
    TRIGGERER_IS_ON_FURNI(2),
    TIME_ELAPSED_MORE(3), // has custom UI
    TIME_ELAPSED_LESS(4), // has custom UI
    USER_COUNT_IN(5), // has custom UI
    ACTOR_IS_IN_TEAM(6), // has custom UI
    HAS_STACKED_FURNIS(7), // has custom UI
    STUFF_TYPE_MATCHES(8),
    STUFFS_IN_FORMATION(9), // has custom UI
    ACTOR_IS_GROUP_MEMBER(10), // has custom UI
    ACTOR_IS_WEARING_BADGE(11), // has custom UI
    ACTOR_IS_WEARING_EFFECT(12), // has custom UI
    NOT_STATES_MATCH(13),
    NOT_FURNIS_HAVE_AVATARS(14), // has custom UI
    NOT_TRIGGERER_IS_ON_FURNI(15),
    NOT_USER_COUNT_IN(16),
    NOT_ACTOR_IS_IN_TEAM(17),
    NOT_HAS_STACKED_FURNIS(18), // has custom UI
    NOT_STUFF_TYPE_MATCHES(19),
    NOT_STUFFS_IN_FORMATION(20),
    NOT_ACTOR_IS_GROUP_MEMBER(21),
    NOT_ACTOR_IS_WEARING_BADGE(22),
    NOT_ACTOR_IS_WEARING_EFFECT(23),
    DATE_RANGE_ACTIVE(24), // has custom UI
    ACTOR_HAS_HANDITEM(25), // has custom UI
    TRIGGERER_MATCHES(26), // has custom UI
    NOT_TRIGGERER_MATCHES(27),
    TIME_MATCHES(28), // has custom UI
    DATE_MATCHES(29), // has custom UI
    NOT_HAS_HANDITEM(30),
    TEAM_IS_WINNING(31), // has custom UI
    PERFORMING_ACTION(32), // has custom UI
    NOT_PERFORMING_ACTION(33),
    TEAM_HAS_SCORE(34), // has custom UI
    CLOCK_TIME_MATCHES(35), // has custom UI
    FURNI_HAS_ALTITUDE(36), // has custom UI
    USER_DIRECTION(37), // has custom UI
    INPUT_SOURCE_QUANTITY(38), // has custom UI
    CAN_PERFORM_MOVE(39),
    HAS_VARIABLE(40), // has custom UI
    NOT_HAS_VARIABLE(41),
    VARIABLE_VALUE(42), // has custom UI
    VARIABLE_AGE(43), // has custom UI
    USER_LEVEL(44) // has custom UI
}
