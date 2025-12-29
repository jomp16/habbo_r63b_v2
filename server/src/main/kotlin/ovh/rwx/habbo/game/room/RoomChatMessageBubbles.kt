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

package ovh.rwx.habbo.game.room

enum class RoomChatMessageBubbles(
    val type: Int,
    val bubbleName: String,
    val permission: String,
    val overridable: Boolean,
    val triggersTalkingFurniture: Boolean
) {
    NORMAL(0, "NORMAL", "", true, true),
    ALERT(1, "ALERT", "", true, true),
    BOT(2, "BOT", "", true, true),
    RED(3, "RED", "", true, true),
    BLUE(4, "BLUE", "", true, true),
    YELLOW(5, "YELLOW", "", true, true),
    GREEN(6, "GREEN", "", true, true),
    BLACK(7, "BLACK", "", true, true),
    FORTUNE_TELLER(8, "FORTUNE_TELLER", "", false, false),
    ZOMBIE_ARM(9, "ZOMBIE_ARM", "", true, false),
    SKELETON(10, "SKELETON", "", true, false),
    LIGHT_BLUE(11, "LIGHT_BLUE", "", true, true),
    PINK(12, "PINK", "", true, true),
    PURPLE(13, "PURPLE", "", true, true),
    DARK_YELLOW(14, "DARK_YELLOW", "", true, true),
    DARK_BLUE(15, "DARK_BLUE", "", true, true),
    HEARTS(16, "HEARTS", "", true, true),
    ROSES(17, "ROSES", "", true, true),
    UNUSED(18, "UNUSED", "", true, true),
    PIG(19, "PIG", "", true, true),
    DOG(20, "DOG", "", true, true),
    BLAZE_IT(21, "BLAZE_IT", "", true, true),
    DRAGON(22, "DRAGON", "", true, true),
    STAFF(23, "STAFF", "", false, true),
    BATS(24, "BATS", "", true, false),
    MESSENGER(25, "MESSENGER", "", true, false),
    STEAMPUNK(26, "STEAMPUNK", "", true, false),
    THUNDER(27, "THUNDER", "", true, true),
    PARROT(28, "PARROT", "", false, false),
    PIRATE(29, "PIRATE", "", false, false),
    BOT_GUIDE(30, "BOT_GUIDE", "", true, true),
    BOT_RENTABLE(31, "BOT_RENTABLE", "", true, true),
    SCARY_THING(32, "SCARY_THING", "", true, false),
    FRANK(33, "FRANK", "", true, false),
    WIRED(34, "WIRED", "", false, true),
    GOAT(35, "GOAT", "", true, false),
    SANTA(36, "SANTA", "", true, false),
    AMBASSADOR(37, "AMBASSADOR", "acc_ambassador", false, true),
    RADIO(38, "RADIO", "", true, false),
    UNKNOWN_39(39, "UNKNOWN_39", "", true, false),
    UNKNOWN_40(40, "UNKNOWN_40", "", true, false),
    UNKNOWN_41(41, "UNKNOWN_41", "", true, false),
    UNKNOWN_42(42, "UNKNOWN_42", "", true, false),
    UNKNOWN_43(43, "UNKNOWN_43", "", true, false),
    UNKNOWN_44(44, "UNKNOWN_44", "", true, false),
    UNKNOWN_45(45, "UNKNOWN_45", "", true, false),
    NOTIFICATION_RED(200, "NOTIFICATION_RED", "", true, true),
    NOTIFICATION_GREEN(201, "NOTIFICATION_GREEN", "", true, true),
    NOTIFICATION_BLUE(202, "NOTIFICATION_BLUE", "", true, true),
    ALERT_NOTIFICATION(210, "ALERT_NOTIFICATION", "", true, true),
    INFO_NOTIFICATION(211, "INFO_NOTIFICATION", "", true, true),
    WARNING_NOTIFICATION(212, "WARNING_NOTIFICATION", "", true, true),
    WRONG(220, "WRONG", "", true, true),
    WRONG_CIRCLE(221, "WRONG_CIRCLE", "", true, true),
    CORRECT(222, "CORRECT", "", true, true),
    CORRECT_CIRCLE(223, "CORRECT_CIRCLE", "", true, true),
    QUESTION_MARK(224, "QUESTION_MARK", "", true, true),
    QUESTION_MARK_CIRCLE(225, "QUESTION_MARK_CIRCLE", "", true, true),
    ARROW_UP(226, "ARROW_UP", "", true, true),
    ARROW_UP_CIRCLE(227, "ARROW_UP_CIRCLE", "", true, true),
    ARROW_DOWN(228, "ARROW_DOWN", "", true, true),
    ARROW_DOWN_CIRCLE(229, "ARROW_DOWN_CIRCLE", "", true, true),
    SKULL(250, "SKULL", "", true, true),
    SKULL_2(251, "SKULL_2", "", true, true),
    MAGNIFYING_GLASS(252, "MAGNIFYING_GLASS", "", true, true);

    companion object {
        fun fromType(type: Int) = values().find { it.type == type } ?: NORMAL
    }
}