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

package tk.jomp16.fastfood.communication.outgoing

enum class FFOutgoing(val headerId: Int) {
    FAST_FOOD_MAINENTANCE(1),
    USER_JOIN_GAME(3),
    DROP_FOOD(4),
    NEXT_PLATE(5),
    END_GAME(6),
    USE_BIG_PARACHUTE(9),
    LAUNCH_MISSILE(10),
    HANDSHAKE_AUTHENTICATION_OK(11),
    ENABLE_SHIELD(12),
    HANDSHAKE_SEND_CLIENT_LOCALIZATIONS(13),
    POWER_UPS_INVENTORY(14),
    POWER_UPS_PRICE(15),
    USER_CREDITS(16),
    GAME_COUNT(19),
    USE_PARACHUTE(-1),
}