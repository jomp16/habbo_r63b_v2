/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.wired.effect

enum class WiredEffectType(val code: Int) {
    TOGGLE_STATE(0),
    RESET_TIMERS(1),
    MATCH_SSHOT(3),
    MOVE_ROTATE(4),
    GIVE_SCORE(6),
    SHOW_MESSAGE(7),
    TELEPORT(8),
    JOIN_TEAM(9),
    LEAVE_TEAM(10),
    CHASE(11),
    FLEE(12),
    MOVE_DIRECTION(13),
    GIVE_SCORE_TEAM(14),
    TOGGLE_RANDOM(15),
    MOVE_FURNI_TO(16),
    GIVE_REWARD(17),
    CALL_STACKS(18),
    KICK_USER(19),
    MUTE_TRIGGER(20),
    BOT_TELEPORT(21),
    BOT_MOVE(22),
    BOT_TALK(23),
    BOT_GIVE_HANDITEM(24),
    BOT_FOLLOW_AVATAR(25),
    BOT_CLOTHES(26),
    BOT_TALK_TO_AVATAR(27)
}
