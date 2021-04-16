/*
 * Copyright (C) 2015-2021 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.incoming

enum class IncomingR63A {
    HANDSHAKE_INIT_CRYPTO,
    HANDSHAKE_SSO_TICKET,
    USER_CREDITS_BALANCE,
    USER_ACTIVITY_POINTS_BALANCE,
    USER_INFO_RETRIEVE,
    SUBSCRIPTION_STATUS,
    USER_SETTINGS,
    MESSENGER_INIT,
    ROOM_OPEN_FLAT,
    NAVIGATOR_FLAT_CATEGORIES,
    CATALOG_INDEX,
    CATALOG_RECYCLER_REWARDS,
    CATALOG_CONFIGURATION,
    CATALOG_GIFT_WRAPPING,
    MISC_EVENT_TRACKER,
    MISC_GET_MOTD,
    MISC_PING,
    MESSENGER_IGNORED_USERS,
    ROOM_ITEM_ALIASES,
    ROOM_GROUPS_BADGES,
    ROOM_MODEL,
    ROOM_ITEMS,
    ROOM_INFO,
    MESSENGER_REQUESTS,
    ROOM_MOVE,
    ROOM_DIMMER_INFO,
    ROOM_DIMMER_SWITCH,
    ROOM_DIMMER_UPDATE,
    ROOM_USER_WAVE,
    ROOM_USER_DANCE,
    ROOM_DOORBELL,
    ROOM_USER_CHAT,
    ROOM_USER_SHOUT,
    ROOM_USER_WHISPER,
    USER_BADGES,
}