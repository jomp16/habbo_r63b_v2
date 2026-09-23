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

package ovh.rwx.habbo.game.item.wired.trigger

enum class WiredTriggerType(val code: Int) {
    AVATAR_SAYS_SOMETHING(0), // has custom UI
    WALKS_ON_FURNI(1),
    WALKS_OFF_FURNI(2),
    TRIGGER_ONCE(3), // has custom UI
    USE_STUFF(4),
    TRIGGER_PERIODICALLY(6), // has custom UI
    AVATAR_ENTERS_ROOM(7),
    GAME_STARTS(8),
    GAME_ENDS(9),
    SCORE_ACHIEVED(10), // has custom UI
    AVATAR_CAUGHT(11),
    PERIODIC_LONG(12), // has custom UI
    BOT_DESTINATION_REACHED(13), // has custom UI
    BOT_AVATAR_REACHED(14), // has custom UI
    CLOCK_REACH_TIME(15), // has custom UI
    USER_PERFORMS_ACTION(16), // has custom UI
    RECEIVE_SIGNAL(17),
    AVATAR_CLICKS_FURNI(18),
    PERIODIC_SHORT(19), // has custom UI
    STATE_CHANGE(20), // has custom UI
    AVATAR_CLICKS_TILE(21),
    VARIABLE_UPDATE(22), // has custom UI
    AVATAR_LEAVES_ROOM(23),
    USER_CLICKS_USER(24),
    TRANSACTION_COMPLETED(25),
    TRANSACTION_FAILED(26)
}
