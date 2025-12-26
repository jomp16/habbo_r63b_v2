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

package ovh.rwx.habbo.communication.outgoing

enum class OutgoingR63A {
    ACHIEVEMENT_SCORE,
    AVAILABILITY_STATUS,
    CATALOG_INDEX,
    CATALOG_PAGE,
    ENABLE_TRADING,
    HANDSHAKE_AUTHENTICATION_OK,
    HANDSHAKE_SESSION_PARAMS,
    HOME_ROOM,
    INVENTORY_BADGES,
    INVENTORY_ITEMS,
    INVENTORY_NEW_OBJECTS,
    INVENTORY_PETS,
    INVENTORY_REMOVE_OBJECT,
    INVENTORY_UPDATE,
    MESSENGER_CHAT,
    MESSENGER_CHAT_ERROR,
    MESSENGER_FRIENDS,
    MESSENGER_FRIENDS_UPDATE,
    MESSENGER_REQUESTS,
    MISC_BROADCAST_NOTIFICATION,
    MISC_GENERIC_ERROR,
    MISC_MOTD_NOTIFICATION,
    MISC_PONG,
    MODERATION_INIT,
    NAVIGATOR_FAVORITES,
    NAVIGATOR_LIST_ROOMS,
    NAVIGATOR_POPULAR_TAGS,
    NAVIGATOR_ROOM_CATEGORIES,
    ROOM_DECORATION,
    ROOM_DIMMER_INFO,
    ROOM_DOORBELL,
    ROOM_DOORBELL_ACCEPT,
    ROOM_DOORBELL_DENIED,
    ROOM_ERROR,
    ROOM_EXIT,
    ROOM_FLOORMAP,
    ROOM_FLOOR_ITEMS,
    ROOM_FLOOR_ITEM_REMOVE,
    ROOM_FLOOR_ITEM_UPDATE,
    ROOM_GROUPS_BADGES,
    ROOM_HEIGHTMAP,
    ROOM_INFO,
    ROOM_INITIAL_INFO,
    ROOM_ITEM_ADDED,
    ROOM_ITEM_ALIASES,
    ROOM_OPEN,
    ROOM_OWNER,
    ROOM_OWNERSHIP,
    ROOM_POST_IT,
    ROOM_RIGHT,
    ROOM_SEND_ADVERTISEMENT,
    ROOM_URL,
    ROOM_USERS,
    ROOM_USERS_STATUSES,
    ROOM_USER_CHAT,
    ROOM_USER_DANCE,
    ROOM_USER_EFFECT,
    ROOM_USER_HANDITEM,
    ROOM_USER_IDLE,
    ROOM_USER_REMOVE,
    ROOM_USER_SHOUT,
    ROOM_USER_TYPING,
    ROOM_USER_WHISPER,
    ROOM_VISUALIZATION_THICKNESS,
    ROOM_WALL_ITEMS,
    ROOM_WALL_ITEM_ADDED,
    ROOM_WALL_ITEM_REMOVE,
    ROOM_WALL_ITEM_UPDATE,
    SUBSCRIPTION_STATUS,
    USER_ACTIVITY_POINTS_BALANCE,
    USER_AVATAR_EFFECTS,
    USER_BADGES,
    USER_CREDITS_BALANCE,
    USER_OBJECT,
    USER_RIGHTS,
    USER_SETTINGS,
    USER_TAGS,
    USER_UPDATE,
    USER_WARDROBES,
}
