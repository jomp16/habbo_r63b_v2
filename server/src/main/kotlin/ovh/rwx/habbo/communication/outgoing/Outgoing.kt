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

package ovh.rwx.habbo.communication.outgoing

enum class Outgoing {
    ACHIEVEMENT_SCORE,
    ACTIVITY_POINTS_BALANCE,
    AUTHENTICATION_OK,
    AUTHENTICATION_UNKNOWN_ID1,
    AUTHENTICATION_UNKNOWN_ID2,
    AUTHENTICATION_UNKNOWN_ID3,
    AVAILABILITY_STATUS,
    AVATAR_EFFECTS,
    BUILDERS_CLUB_MEMBERSHIP,
    CAMERA_FINISH_COMPETITION,
    CAMERA_FINISH_PUBLISH,
    CAMERA_FINISH_PURCHASE,
    CAMERA_PREVIEW_URL,
    CAMERA_PRICE,
    CAMERA_THUMBNAIL_ALERT,
    CAMPAIGN,
    CAMPAIGN_CALENDAR,
    CATALOG_BUILDERS_BORROWED,
    CATALOG_CONFIGURATION,
    CATALOG_GIFT_WRAPPING,
    CATALOG_HABBO_CLUB_PAGE,
    CATALOG_INDEX,
    CATALOG_LIMITED_SOLD_OUT,
    CATALOG_OPEN_RECYCLER_RESULT,
    CATALOG_OFFER,
    CATALOG_OFFER_CONFIGURATION,
    CATALOG_PAGE,
    CATALOG_PURCHASE_ERROR,
    CATALOG_PURCHASE_NOT_ALLOWED_ERROR,
    CATALOG_PURCHASE_OK,
    CATALOG_PURCHASE_RENTAL_DIALOG,
    CATALOG_RECEIVER_NOT_FOUND,
    CATALOG_RECYCLER_REWARDS,
    CATALOG_RECYCLE_ITEMS_RESULT,
    CATALOG_UPDATE,
    CATALOG_VOUCHER_REDEEMED,
    CATALOG_VOUCHER_REDEEM_ERROR,
    CREDITS_BALANCE,
    ENABLE_TRADING,
    FLOOR_PLAN_DOOR,
    FLOOR_PLAN_USED_SQUARES,
    FORUM_LIST,
    GAME_CENTER_ACCOUNT_STATUS,
    GAME_CENTER_ACHIEVEMENTS,
    GAME_CENTER_CAN_PLAY_GAME,
    GAME_CENTER_FEATURED_LUCKY_LOSER_OF_THE_WEEK,
    GAME_CENTER_LEADERBOARD,
    GAME_CENTER_LIST,
    GAME_CENTER_LOAD_GAME,
    GAME_CENTER_UPDATE_LEADERBOARD,
    HABBO_CLUB_INFO,
    HOME_ROOM,
    INIT_CRYPTO,
    INVENTORY_BADGES,
    INVENTORY_BOTS,
    INVENTORY_ITEMS,
    INVENTORY_NEW_OBJECTS,
    INVENTORY_PETS,
    INVENTORY_REMOVE_OBJECT,
    INVENTORY_UPDATE,
    LANDING_PROMO_ARTICLES,
    LANDING_REWARD,
    MESSENGER_CHAT,
    MESSENGER_CHAT_ERROR,
    MESSENGER_FOLLOW_FRIEND_ERROR,
    MESSENGER_FRIENDS,
    MESSENGER_FRIEND_UPDATE,
    MESSENGER_INIT,
    MESSENGER_INVITE,
    MESSENGER_REQUESTS,
    MESSENGER_REQUEST_FRIEND,
    MESSENGER_SEARCH_FRIENDS,
    MISC_BROADCAST_NOTIFICATION,
    MISC_GENERIC_ERROR,
    MISC_MOTD_NOTIFICATION,
    MISC_PING,
    MISC_PONG,
    MISC_SUPER_NOTIFICATION,
    MODERATION_INIT,
    MODERATION_ROOM_INFO,
    MODERATION_TOPICS_INIT,
    MODERATION_USER_INFO,
    NAVIGATOR_COLLAPSED_CATEGORIES,
    NAVIGATOR_CREATE_ROOM,
    NAVIGATOR_EVENT_CATEGORIES,
    NAVIGATOR_FAVORITES,
    NAVIGATOR_LIFTED_ROOMS,
    NAVIGATOR_METADATA,
    NAVIGATOR_PREFERENCES,
    NAVIGATOR_ROOM_CATEGORIES,
    NAVIGATOR_SEARCH,
    NAVIGATOR_UPDATE_FAVORITE_ROOM_STATUS,
    ROOM_BOT_ERROR_NOTIFICATION,
    ROOM_COMPETITION,
    ROOM_COMPETITION_DATA,
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
    ROOM_FORWARD,
    ROOM_HEIGHTMAP,
    ROOM_INFO,
    ROOM_INFO_UPDATED,
    ROOM_INITIAL_INFO,
    ROOM_ITEM_ADDED,
    ROOM_ITEM_ALIASES,
    ROOM_ITEM_OPEN_GIFT_RESULT,
    ROOM_NO_RIGHTS,
    ROOM_OPEN,
    ROOM_OWNER,
    ROOM_OWNERSHIP,
    ROOM_PET_ERROR_NOTIFICATION,
    ROOM_POST_IT,
    ROOM_RIGHTS,
    ROOM_RIGHTS_GIVEN,
    ROOM_RIGHTS_REMOVED,
    ROOM_RIGHT_LEVEL,
    ROOM_ROLLER,
    ROOM_SETTINGS,
    ROOM_SETTINGS_SAVED,
    ROOM_UPDATE_FURNI_STACK,
    ROOM_USERS,
    ROOM_USERS_STATUSES,
    ROOM_USER_ACTION,
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
    ROOM_WORD_FILTER,
    SECRET_KEY,
    SUBSCRIPTION_STATUS,
    TALENT_TRACK,
    UNIQUE_ID,
    USER_ACHIEVEMENT,
    USER_BADGES,
    USER_CLOTHINGS,
    USER_OBJECT,
    USER_PERKS,
    USER_PROFILE,
    USER_RELATIONSHIPS,
    USER_RESPECT_NOTIFICATION,
    USER_RIGHTS,
    USER_SETTINGS,
    USER_TAGS,
    USER_UPDATE,
    USER_UPDATE_FIGURE,
    USER_WARDROBES,
    WIRED_CONDITION_DIALOG,
    WIRED_EFFECT_DIALOG,
    WIRED_REWARD_NOTIFICATION,
    WIRED_SAVED,
    WIRED_TRIGGER_DIALOG,
    GROUP_BADGES,
    GROUP_BADGE_EDITOR,
    GROUP_FURNISHING_SETTINGS,
    GROUP_ID,
    GROUP_INFO,
    GROUP_MANAGE,
    GROUP_MEMBERS,
    GROUP_NEW_REQUEST,
    GROUP_PURCHASED,
    GROUP_PURCHASE_PAGE,
    GROUP_REFRESH_FAVORITE,
    GROUP_UPDATE_FAVORITE,
    GROUP_UPDATE_MEMBER_STATUS,
    GROUP_JOIN_ERROR,
    GROUP_MEMBER_MANAGEMENT_ERROR
}