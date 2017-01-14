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

object Incoming {
    const val RELEASE = "PRODUCTION-201612201205-462162202"

    const val ACTIVITY_POINTS_BALANCE = 509
    const val AVATAR_EFFECT = 1298
    const val CAMERA_PRICE = 2470
    const val CAMPAIGN_OPEN1 = 711
    const val CAMPAIGN_OPEN2 = 3280
    const val CATALOG_CONFIGURATION = 1457
    const val CATALOG_GIFT_WRAPPING = 2448
    const val CATALOG_HABBO_CLUB_PAGE = 1826
    const val CATALOG_INDEX = 1528
    const val CATALOG_OFFER = 2367
    const val CATALOG_PAGE = 1977
    const val CATALOG_PRODUCTDATA = 2184 // todo: is that correct?
    const val CATALOG_PURCHASE = 2095
    const val CATALOG_RECYCLER_REWARDS = 256
    const val CATALOG_REDEEM_VOUCHER = 324
    const val CLIENT_DEBUG = 3888
    const val CLIENT_VARIABLES = 3173
    const val CREDITS_BALANCE = 2978
    const val EVENT_TRACKER = 703
    const val GAME_LISTING = 393
    const val GO_TO_HOTEL_VIEW = 887
    const val GROUP_FORUMS = 2651 // todo: is that correct?
    const val GROUP_INFO = 3374
    const val HABBO_CAMERA_DATA = 3847
    const val HABBO_CLUB_GIFTS = 2007
    const val HABBO_CLUB_INFO = 1720
    const val INFO_RETRIEVE = 3211
    const val INITIALIZE_GAME_CENTER = 795
    const val INIT_CRYPTO = 3087
    const val INVENTORY_BADGES = 3412
    const val INVENTORY_BOTS = 3757
    const val INVENTORY_ITEMS = 140
    const val INVENTORY_PETS = 3897
    const val INVENTORY_UPDATE_BADGES = 3048
    const val LANDING_PROMO_ARTICLES = 1202
    const val LANDING_REWARD = 1696
    const val LOAD_INTERSTITIALS = 1755
    const val MESSENGER_ACCEPT_REQUEST = 402
    const val MESSENGER_CHAT = 3371
    const val MESSENGER_DECLINE_REQUEST = 668
    const val MESSENGER_FOLLOW_FRIEND = 2151
    const val MESSENGER_FRIENDS_UPDATE = 2881
    const val MESSENGER_FRIEND_BAR_STATE = 1342
    const val MESSENGER_INIT = 2134
    const val MESSENGER_REMOVE_FRIEND = 402
    const val MESSENGER_REQUESTS = 343
    const val MESSENGER_REQUEST_FRIEND = 1823
    const val MESSENGER_SEARCH_FRIENDS = 553
    const val MODERATION_BAN_USER = 2912
    const val MODERATION_CALL_FOR_HELP = 716
    const val MODERATION_CLOSE_TICKET = 3615
    const val MODERATION_CLOSE_TRADE_USER = 3084
    const val MODERATION_KICK_USER = 910
    const val MODERATION_MESSAGE_USER = 1292
    const val MODERATION_MODERATE_ROOM = 3803
    const val MODERATION_MODERATE_USER = 2805
    const val MODERATION_MUTE_USER = 2525
    const val MODERATION_PICK_TICKET = 769
    const val MODERATION_RELEASE_TICKET = 2401
    const val MODERATION_ROOM_CHATLOG = 197
    const val MODERATION_ROOM_INFO = 351
    const val MODERATION_TICKET_CHATLOG = 3056
    const val MODERATION_TOUR_REQUEST = 781
    const val MODERATION_USER_CHATLOG = 3377
    const val MODERATION_USER_INFO = 2917
    const val MODERATION_USER_ROOM_VISITS = 599
    const val MODERATION_WARN_USER = 1809
    const val NAVIGATOR_CREATE_ROOM = 3777
    const val NAVIGATOR_FLAT_CATEGORIES = 2590
    const val NAVIGATOR_HOME_ROOM = 3796
    const val NAVIGATOR_INITIALIZE = 3883
    const val NAVIGATOR_PROMO_CATEGORIES = 86
    const val NAVIGATOR_SAVE_SETTINGS = 2522
    const val NAVIGATOR_SEARCH = 696
    const val PING = 3379
    const val PONG = 455
    const val REFRESH_CAMPAIGN = 1572
    const val RELEASE_CHECK = 4000
    const val ROOM_APPLY_DECORATION = 1304
    const val ROOM_BANNED_USERS = 361
    const val ROOM_COMPETITION_STATUS = 382 // todo: is that correct?
    const val ROOM_DIMMER_INFO = 1677
    const val ROOM_DIMMER_SWITCH = 1599
    const val ROOM_DIMMER_UPDATE = 2498
    const val ROOM_DOORBELL = 1344
    const val ROOM_ENTRY_DATA = 1273
    const val ROOM_GROUP_BADGES = 3275
    const val ROOM_INFO = 1573
    const val ROOM_ITEM_ALIASES = 2862
    const val ROOM_ITEM_USE_CLOTHING = 1253
    const val ROOM_LOAD_BY_DOORBELL = 1163
    const val ROOM_LOOK_TO = 1431
    const val ROOM_MANNEQUIN_CHANGE_FIGURE = 2166
    const val ROOM_MANNEQUIN_CHANGE_NAME = 2527
    const val ROOM_MOVE = 284
    const val ROOM_MOVE_FLOOR_ITEM = 1093
    const val ROOM_MOVE_WALL_ITEM = 1873
    const val ROOM_OPEN_FLAT = 3462
    const val ROOM_PLACE_ITEM = 3221
    const val ROOM_PLACE_POST_IT = 3675
    const val ROOM_POST_IT = 1769
    const val ROOM_REEDEM_EXCHANGE_ITEM = 2
    const val ROOM_REMOVE_POST_IT = 2636
    const val ROOM_RIGHTS = 123
    const val ROOM_SAVE_POST_IT = 2653
    const val ROOM_SAVE_SETTINGS = 115
    const val ROOM_SETTINGS = 2460
    const val ROOM_TAKE_ITEM = 2368
    const val ROOM_TRIGGER_CLOSE_DICE = 905
    const val ROOM_TRIGGER_HABBO_WHEEL = 855
    const val ROOM_TRIGGER_ITEM = 1225
    const val ROOM_TRIGGER_ONE_WAY_GATE = 3787
    const val ROOM_TRIGGER_ROLL_DICE = 948
    const val ROOM_TRIGGER_WALL_ITEM = 1057
    const val ROOM_UPDATE_WORD_FILTER = 1048
    const val ROOM_USER_ACTION = 347
    const val ROOM_USER_CHAT = 2026
    const val ROOM_USER_DANCE = 2051
    const val ROOM_USER_SHOUT = 1489
    const val ROOM_USER_SIGN = 3362
    const val ROOM_USER_START_TYPING = 625
    const val ROOM_USER_STOP_TYPING = 282
    const val ROOM_USER_WHISPER = 1687
    const val ROOM_WORD_FILTER = 322
    const val SANCTION_STATUS = 591
    const val SECRET_KEY = 1180
    const val SETTINGS_SAVE_CHAT = 1367
    const val SETTINGS_SAVE_FOCUS = 1279
    const val SETTINGS_SAVE_INVITES = 1193
    const val SETTINGS_SAVE_VOLUME = 68
    const val SET_USERNAME = 600
    const val SSO_TICKET = 3429
    const val SUBSCRIPTION_STATUS = 339
    const val TALENT_STATUS = 2062 // todo: is that correct?
    const val UNIQUE_ID = 2236
    const val USER_ACHIEVEMENT = 3002
    const val USER_BADGES = 3583
    const val USER_CHANGE_FIGURE = 1708
    const val USER_CHANGE_MOTTO = 3042
    const val USER_PROFILE = 2526
    const val USER_RELATIONSHIPS = 3128
    const val USER_SETTINGS = 3174
    const val USER_TAGS = 2393
    const val USER_WARDROBES = 384
    const val USER_WARDROBE_SAVE = 1860
}