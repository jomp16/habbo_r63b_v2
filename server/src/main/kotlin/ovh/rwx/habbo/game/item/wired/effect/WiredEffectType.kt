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

package ovh.rwx.habbo.game.item.wired.effect

enum class WiredEffectType(val code: Int) {
    TOGGLE_STATE(0),
    RESET_TIMERS(1),
    MATCH_SSHOT(3), // has custom UI
    MOVE_ROTATE(4), // has custom UI
    GIVE_SCORE(6), // has custom UI
    SHOW_MESSAGE(7), // has custom UI
    TELEPORT(8),
    JOIN_TEAM(9), // has custom UI
    LEAVE_TEAM(10),
    CHASE(11),
    FLEE(12),
    MOVE_DIRECTION(13), // has custom UI
    GIVE_SCORE_TEAM(14), // has custom UI
    TOGGLE_RANDOM(15),
    MOVE_FURNI_TO(16), // has custom UI
    GIVE_REWARD(17), // has custom UI
    CALL_STACKS(18),
    KICK_USER(19), // has custom UI
    MUTE_TRIGGER(20), // has custom UI
    BOT_TELEPORT(21), // has custom UI
    BOT_MOVE(22), // has custom UI
    BOT_TALK(23), // has custom UI
    BOT_GIVE_HANDITEM(24), // has custom UI
    BOT_FOLLOW_AVATAR(25), // has custom UI
    BOT_CLOTHES(26), // has custom UI
    BOT_TALK_TO_AVATAR(27) // has custom UI
}
