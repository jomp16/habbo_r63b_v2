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
    const val RELEASE = "PRODUCTION-201702211601-24705679"

    const val ACTIVITY_POINTS_BALANCE = 3339 // 737
    const val AVATAR_EFFECT = 3928 // 3895
    const val CAMERA_PRICE = 880 // 536
    const val CAMPAIGN_OPEN1 = 664 // 3437
    const val CAMPAIGN_OPEN2 = 908 // 2255
    const val CATALOG_CONFIGURATION = 2522 // 3692
    const val CATALOG_GIFT_WRAPPING = 1169 // 3220
    const val CATALOG_HABBO_CLUB_PAGE = 1977 // 3337
    const val CATALOG_INDEX = 3336 // 2903
    const val CATALOG_OFFER = 845 // 1765
    const val CATALOG_PAGE = 3782 // 1481
    const val CATALOG_PRODUCTDATA = 1110 // 625
    const val CATALOG_PURCHASE = 1813 // 3030
    const val CATALOG_RECYCLER_REWARDS = 2510 // 2372
    const val CATALOG_REDEEM_VOUCHER = 3175 // 3542
    const val CLIENT_DEBUG = 2615 // 2057
    const val CLIENT_VARIABLES = 3070 // 2654
    const val CREDITS_BALANCE = 3781 // 1596
    const val EVENT_TRACKER = 1718 // 2330
    const val GAME_LISTING = 3600 // 356
    const val GO_TO_HOTEL_VIEW = 616 // 2324
    const val GROUP_FORUMS = 2405 // 978
    const val GROUP_INFO = 390 // 1717
    const val HABBO_CAMERA_DATA = 3606 // 3665
    const val HABBO_CLUB_GIFTS = 797 // 3823
    const val HABBO_CLUB_INFO = 2361 // 1661
    const val INFO_RETRIEVE = 2176 // 2490
    const val INITIALIZE_GAME_CENTER = 3293 // 3036
    const val INIT_CRYPTO = 2701 // 3396
    const val INVENTORY_BADGES = 2336 // 3221
    const val INVENTORY_BOTS = 3029 // 3263
    const val INVENTORY_ITEMS = 3786 // 2138
    const val INVENTORY_PETS = 2829 // 1983
    const val INVENTORY_UPDATE_BADGES = 3775 // 397
    const val LANDING_PROMO_ARTICLES = 2003 // 385
    const val LANDING_REWARD = 581 // 2553
    const val LOAD_INTERSTITIALS = 2233 // 3759
    const val MESSENGER_ACCEPT_REQUEST = 842 // 892
    const val MESSENGER_CHAT = 606 // 1059
    const val MESSENGER_DECLINE_REQUEST = 1357 // 2210
    const val MESSENGER_FIND_NEW_FRIENDS = 535 // 1396
    const val MESSENGER_FOLLOW_FRIEND = 226 // 2998
    const val MESSENGER_FRIENDS_UPDATE = 3919 // 567
    const val MESSENGER_FRIEND_BAR_STATE = 2327 // 3200
    const val MESSENGER_INIT = 1837 // 3588
    const val MESSENGER_INVITE = 2426 // 1885
    const val MESSENGER_REMOVE_FRIEND = 842 // 892
    const val MESSENGER_REQUESTS = 3602 // 993
    const val MESSENGER_REQUEST_FRIEND = 285 // 2458
    const val MESSENGER_SEARCH_FRIENDS = 411 // 1279
    const val MESSENGER_SET_RELATIONSHIP = 3121 // 2720
    const val MODERATION_BAN_USER = 460 // 2758
    const val MODERATION_CALL_FOR_HELP = 1957 // 1734
    const val MODERATION_CLOSE_TICKET = 571 // 1032
    const val MODERATION_CLOSE_TRADE_USER = 1072 // 1358
    const val MODERATION_KICK_USER = 1460 // 3255
    const val MODERATION_MESSAGE_USER = 32 // 413
    const val MODERATION_MODERATE_ROOM = 342 // 341
    const val MODERATION_MODERATE_USER = 3169 // 3392
    const val MODERATION_MUTE_USER = 2191 // 2629
    const val MODERATION_PICK_TICKET = 703 // 1974
    const val MODERATION_RELEASE_TICKET = 139 // 88
    const val MODERATION_ROOM_CHATLOG = 2782 // 3494
    const val MODERATION_ROOM_INFO = 2976 // 375
    const val MODERATION_TICKET_CHATLOG = 3970 // 1288
    const val MODERATION_TOUR_REQUEST = 158 // 3529
    const val MODERATION_USER_CHATLOG = 457 // 3194
    const val MODERATION_USER_INFO = 2925 // 1716
    const val MODERATION_USER_ROOM_VISITS = 3816 // 3276
    const val MODERATION_WARN_USER = 1023 // 266
    const val NAVIGATOR_CREATE_ROOM = 2178 // 3367
    const val NAVIGATOR_FLAT_CATEGORIES = 815 // 1990
    const val NAVIGATOR_HOME_ROOM = 3332 // 2046
    const val NAVIGATOR_INITIALIZE = 2367 // 832
    const val NAVIGATOR_PROMO_CATEGORIES = 2841 // 1946
    const val NAVIGATOR_SAVE_SETTINGS = 1763 // 1674
    const val NAVIGATOR_SEARCH = 1340 // 2348
    const val PING = 2581 // 953
    const val PONG = 433 // 527
    const val REFRESH_CAMPAIGN = 3693 // 746
    const val RELEASE_CHECK = 4000 // 4000
    const val ROOM_APPLY_DECORATION = 2190 // 3611
    const val ROOM_BANNED_USERS = 2428 // 3570
    const val ROOM_COMPETITION_STATUS = 258 // 2980
    const val ROOM_DIMMER_INFO = 2644 // 409
    const val ROOM_DIMMER_SWITCH = 2197 // 39
    const val ROOM_DIMMER_UPDATE = 1818 // 194
    const val ROOM_DOORBELL = 2363 // 3751
    const val ROOM_ENTRY_DATA = 2452 // 2261
    const val ROOM_GROUP_BADGES = 2347 // 3624
    const val ROOM_INFO = 2092 // 1502
    const val ROOM_ITEM_ALIASES = 1733 // 1158
    const val ROOM_ITEM_USE_CLOTHING = 2512 // 1008
    const val ROOM_LOAD_BY_DOORBELL = 1812 // 3050
    const val ROOM_LOOK_TO = 1463 // 3380
    const val ROOM_MANNEQUIN_CHANGE_FIGURE = 1180 // 3808
    const val ROOM_MANNEQUIN_CHANGE_NAME = 3125 // 1160
    const val ROOM_MOVE = 1611 // 1857
    const val ROOM_MOVE_FLOOR_ITEM = 1051 // 612
    const val ROOM_MOVE_WALL_ITEM = 463 // 3821
    const val ROOM_OPEN_FLAT = 181 // 1539
    const val ROOM_PLACE_BOT = 3061 // 1847
    const val ROOM_PLACE_ITEM = 3278 // 960
    const val ROOM_PLACE_PET = 2025 // 1382
    const val ROOM_PLACE_POST_IT = 1075 // 2544
    const val ROOM_POST_IT = 2117 // 342
    const val ROOM_REEDEM_EXCHANGE_ITEM = 3311 // 605
    const val ROOM_REMOVE_POST_IT = 3195 // 1924
    const val ROOM_RIGHTS = 3968 // 3142
    const val ROOM_SAVE_POST_IT = 3327 // 2201
    const val ROOM_SAVE_SETTINGS = 1109 // 2700
    const val ROOM_SETTINGS = 2075 // 1854
    const val ROOM_TAKE_BOT = 2639 // 1340
    const val ROOM_TAKE_ITEM = 3549 // 2739
    const val ROOM_TAKE_PET = 1061 // 177
    const val ROOM_TRIGGER_CLOSE_DICE = 3144 // 1844
    const val ROOM_TRIGGER_HABBO_WHEEL = 952 // 1708
    const val ROOM_TRIGGER_ITEM = 1908 // 790
    const val ROOM_TRIGGER_ONE_WAY_GATE = 2422 // 913
    const val ROOM_TRIGGER_ROLL_DICE = 2020 // 1426
    const val ROOM_TRIGGER_WALL_ITEM = 2285 // 1086
    const val ROOM_UPDATE_WORD_FILTER = 508 // 292
    const val ROOM_USER_ACTION = 207 // 1888
    const val ROOM_USER_CHAT = 3794 // 1456
    const val ROOM_USER_DANCE = 214 // 3830
    const val ROOM_USER_DROP_HANDITEM = 2965 // 2566
    const val ROOM_USER_GIVE_HANDITEM = 504 // 269
    const val ROOM_USER_SHOUT = 3088 // 2524
    const val ROOM_USER_SIGN = 828 // 278
    const val ROOM_USER_SIT = 157 // 3779
    const val ROOM_USER_START_TYPING = 1223 // 1853
    const val ROOM_USER_STOP_TYPING = 3 // 2080
    const val ROOM_USER_WHISPER = 3399 // 1999
    const val ROOM_WIRED_SAVE_CONDITION = 3322 // 3183
    const val ROOM_WIRED_SAVE_EFFECT = 2582 // 1737
    const val ROOM_WIRED_SAVE_TRIGGER = 2765 // 1753
    const val ROOM_WORD_FILTER = 1094 // 1503
    const val SANCTION_STATUS = 2210 // 3602
    const val SECRET_KEY = 2325 // 1328
    const val SETTINGS_SAVE_CHAT = 2193 // 2158
    const val SETTINGS_SAVE_FOCUS = 1947 // 2618
    const val SETTINGS_SAVE_INVITES = 472 // 2292
    const val SETTINGS_SAVE_VOLUME = 3416 // 3834
    const val SET_USERNAME = 3800 // 3783
    const val SSO_TICKET = 1326 // 2162
    const val SUBSCRIPTION_STATUS = 3905 // 3303
    const val TALENT_STATUS = 1228 // 1311
    const val UNIQUE_ID = 3045 // 3685
    const val USER_ACHIEVEMENT = 1269 // 2865
    const val USER_BADGES = 3839 // 2610
    const val USER_CHANGE_FIGURE = 1617 // 962
    const val USER_CHANGE_MOTTO = 2430 // 2679
    const val USER_PROFILE = 1194 // 2837
    const val USER_RELATIONSHIPS = 3668 // 2879
    const val USER_SETTINGS = 608 // 877
    const val USER_TAGS = 3007 // 1417
    const val USER_WARDROBES = 371 // 148
    const val USER_WARDROBE_SAVE = 2626 // 3915
}