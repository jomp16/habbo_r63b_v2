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

package tk.jomp16.habbo.communication.incoming

enum class Incoming {
    ACTIVITY_POINTS_BALANCE,
    AVATAR_EFFECT,
    CAMERA_PRICE,
    CAMPAIGN_OPEN1,
    CAMPAIGN_OPEN2,
    CATALOG_CONFIGURATION,
    CATALOG_GIFT_WRAPPING,
    CATALOG_HABBO_CLUB_PAGE,
    CATALOG_INDEX,
    CATALOG_INITIALIZED,
    CATALOG_OFFER,
    CATALOG_PAGE,
    CATALOG_PRODUCTDATA,
    CATALOG_PURCHASE,
    CATALOG_PURCHASE_RENTAL_DIALOG,
    CATALOG_RECYCLER_REWARDS,
    CATALOG_REDEEM_VOUCHER,
    CATALOG_SWITCH_MODE,
    CLIENT_DEBUG,
    CLIENT_VARIABLES,
    CREDITS_BALANCE,
    ENABLE_NOTIFICATIONS,
    EVENT_TRACKER,
    FORUM_LIST,
    FLOOR_PLAN_OPEN,
    FLOOR_PLAN_PROPERTIES,
    FLOOR_PLAN_SAVE,
    GAME_LISTING,
    GO_TO_HOTEL_VIEW,
    GROUP_FORUMS,
    GROUP_INFO,
    GUIDE_TOOL_OPEN,
    HABBO_CAMERA_DATA,
    HABBO_CLUB_GIFTS,
    HABBO_CLUB_INFO,
    INFO_RETRIEVE,
    INITIALIZE_GAME_CENTER,
    INIT_CRYPTO,
    INVENTORY_BADGES,
    INVENTORY_BOTS,
    INVENTORY_ITEMS,
    INVENTORY_PETS,
    INVENTORY_UPDATE_BADGES,
    LANDING_PROMO_ARTICLES,
    LANDING_REWARD,
    LOAD_INTERSTITIALS,
    MESSENGER_ACCEPT_REQUEST,
    MESSENGER_CHAT,
    MESSENGER_DECLINE_REQUEST,
    MESSENGER_FIND_NEW_FRIENDS,
    MESSENGER_FOLLOW_FRIEND,
    MESSENGER_FRIENDS_UPDATE,
    MESSENGER_FRIEND_BAR_STATE,
    MESSENGER_INIT,
    MESSENGER_INVITE,
    MESSENGER_REMOVE_FRIEND,
    MESSENGER_REQUESTS,
    MESSENGER_REQUEST_FRIEND,
    MESSENGER_SEARCH_FRIENDS,
    MESSENGER_SET_RELATIONSHIP,
    MODERATION_BAN_USER,
    MODERATION_CALL_FOR_HELP,
    MODERATION_CLOSE_TICKET,
    MODERATION_CLOSE_TRADE_USER,
    MODERATION_KICK_USER,
    MODERATION_MESSAGE_USER,
    MODERATION_MODERATE_ROOM,
    MODERATION_MODERATE_USER,
    MODERATION_MUTE_USER,
    MODERATION_PICK_TICKET,
    MODERATION_RELEASE_TICKET,
    MODERATION_ROOM_CHATLOG,
    MODERATION_ROOM_INFO,
    MODERATION_TICKET_CHATLOG,
    MODERATION_TOUR_REQUEST,
    MODERATION_USER_CHATLOG,
    MODERATION_USER_INFO,
    MODERATION_USER_ROOM_VISITS,
    MODERATION_WARN_USER,
    NAVIGATOR_CREATE_ROOM,
    NAVIGATOR_FLAT_CATEGORIES,
    NAVIGATOR_HOME_ROOM,
    NAVIGATOR_INITIALIZE,
    NAVIGATOR_PROMO_CATEGORIES,
    NAVIGATOR_SAVE_SETTINGS,
    NAVIGATOR_SEARCH,
    PING,
    PONG,
    REFRESH_CAMPAIGN,
    RELEASE_CHECK,
    ROOM_APPLY_DECORATION,
    ROOM_BANNED_USERS,
    ROOM_COMPETITION_STATUS,
    ROOM_DELETE,
    ROOM_DIMMER_INFO,
    ROOM_DIMMER_SWITCH,
    ROOM_DIMMER_UPDATE,
    ROOM_DOORBELL,
    ROOM_ENTRY_DATA,
    ROOM_GIVE_RIGHTS,
    ROOM_GROUP_BADGES,
    ROOM_IGNORE_USER,
    ROOM_INFO,
    ROOM_ITEM_ALIASES,
    ROOM_ITEM_USE_CLOTHING,
    ROOM_KICK_USER,
    ROOM_LOAD_BY_DOORBELL,
    ROOM_LOOK_TO,
    ROOM_MANNEQUIN_CHANGE_FIGURE,
    ROOM_MANNEQUIN_CHANGE_NAME,
    ROOM_MOVE,
    ROOM_MOVE_FLOOR_ITEM,
    ROOM_MOVE_WALL_ITEM,
    ROOM_MUTE_USER,
    ROOM_OPEN_FLAT,
    ROOM_PLACE_BOT,
    ROOM_PLACE_ITEM,
    ROOM_PLACE_PET,
    ROOM_PLACE_POST_IT,
    ROOM_POST_IT,
    ROOM_REEDEM_EXCHANGE_ITEM,
    ROOM_REMOVE_POST_IT,
    ROOM_RIGHTS,
    ROOM_SAVE_POST_IT,
    ROOM_SAVE_SETTINGS,
    ROOM_SETTINGS,
    ROOM_TAKE_BOT,
    ROOM_TAKE_ITEM,
    ROOM_TAKE_OWN_RIGHTS,
    ROOM_TAKE_PET,
    ROOM_TRIGGER_CLOSE_DICE,
    ROOM_TRIGGER_HABBO_WHEEL,
    ROOM_TRIGGER_ITEM,
    ROOM_TRIGGER_ONE_WAY_GATE,
    ROOM_TRIGGER_ROLL_DICE,
    ROOM_TRIGGER_WALL_ITEM,
    ROOM_UPDATE_WORD_FILTER,
    ROOM_USER_ACTION,
    ROOM_USER_CHAT,
    ROOM_USER_DANCE,
    ROOM_USER_DROP_HANDITEM,
    ROOM_USER_GIVE_HANDITEM,
    ROOM_USER_INIT_TRADE,
    ROOM_USER_RESPECT,
    ROOM_USER_SHOUT,
    ROOM_USER_SIGN,
    ROOM_USER_SIT,
    ROOM_USER_START_TYPING,
    ROOM_USER_STOP_TYPING,
    ROOM_USER_WHISPER,
    ROOM_WIRED_SAVE_CONDITION,
    ROOM_WIRED_SAVE_EFFECT,
    ROOM_WIRED_SAVE_TRIGGER,
    ROOM_WORD_FILTER,
    SANCTION_STATUS,
    SECRET_KEY,
    SETTINGS_SAVE_CHAT,
    SETTINGS_SAVE_FOCUS,
    SETTINGS_SAVE_INVITES,
    SETTINGS_SAVE_VOLUME,
    SET_USERNAME,
    SSO_TICKET,
    SUBSCRIPTION_STATUS,
    TALENT_STATUS,
    TALENT_TRACK_OPEN,
    UNIQUE_ID,
    USER_ACHIEVEMENT,
    USER_BADGES,
    USER_CHANGE_FIGURE,
    USER_CHANGE_MOTTO,
    USER_PROFILE,
    USER_RELATIONSHIPS,
    USER_SETTINGS,
    USER_TAGS,
    USER_WARDROBES,
    USER_WARDROBE_SAVE
}