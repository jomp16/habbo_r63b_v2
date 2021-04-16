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

package ovh.rwx.habbo.communication.outgoing

enum class OutgoingR63A {
    HANDSHAKE_SESSION_PARAMS,
    HANDSHAKE_AUTHENTICATION_OK,
    USER_RIGHTS,
    USER_AVATAR_EFFECTS,
    NAVIGATOR_FAVORITES,
    AVAILABILITY_STATUS,
    ENABLE_TRADING,
    HOME_ROOM,
    MODERATION_INIT,
    USER_CREDITS_BALANCE,
    USER_ACTIVITY_POINTS_BALANCE,
    USER_OBJECT,
    USER_SETTINGS,
    MISC_MOTD_NOTIFICATION,
    ROOM_ERROR,
    ROOM_EXIT,
    MISC_GENERIC_ERROR,
    ROOM_DOORBELL,
    ROOM_DOORBELL_DENIED,
    ROOM_DOORBELL_ACCEPT,
    ROOM_OPEN,
    ROOM_DECORATION,
    ROOM_INITIAL_INFO,
    NAVIGATOR_ROOM_CATEGORIES,
    ROOM_ITEM_ALIASES,
    ROOM_GROUPS_BADGES,
    ROOM_FLOORMAP,
    ROOM_HEIGHTMAP,
    ROOM_OWNER,
    ROOM_RIGHT,
    ROOM_VISUALIZATION_THICKNESS,
    ROOM_OWNERSHIP,
    ROOM_USERS,
    ROOM_USERS_STATUSES,
    ROOM_FLOOR_ITEMS,
    ROOM_WALL_ITEMS,
    ROOM_USER_IDLE,
    ROOM_USER_DANCE,
    ROOM_USER_HANDITEM,
    ROOM_USER_EFFECT,
    ROOM_ITEM_ADDED,
    ROOM_WALL_ITEM_ADDED,
    ROOM_WALL_ITEM_UPDATE,
    ROOM_FLOOR_ITEM_UPDATE,
    ROOM_INFO,
    SUBSCRIPTION_STATUS,
    MESSENGER_FRIENDS,
    MESSENGER_CHAT,
    MESSENGER_REQUESTS,
    MESSENGER_FRIEND_UPDATE,
    ROOM_URL,
    ACHIEVEMENT_SCORE,
    ROOM_DIMMER_INFO,
    ROOM_USER_TYPING,
}