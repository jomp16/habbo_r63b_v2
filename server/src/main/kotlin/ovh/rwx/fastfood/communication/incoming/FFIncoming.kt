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

package ovh.rwx.fastfood.communication.incoming

enum class FFIncoming(val headerId: Int) {
    HANDSHAKE_SSO_TICKET(1),
    EXECUTE_ACTION(3),
    USER_DISCONNECT(5),
    JOIN_LOBBY_GAME(6),
    HANDSHAKE_GET_CLIENT_LOCALIZATIONS(7),
    GET_POWER_UPS(8),
    GET_POWER_UPS_PRICE(9),
    PURCHASE_POWER_UP(10),
    GET_GAME_COUNTS(11),
    GET_GLOBAL_SCORES(13),
    GET_USER_CREDITS(14)
}
