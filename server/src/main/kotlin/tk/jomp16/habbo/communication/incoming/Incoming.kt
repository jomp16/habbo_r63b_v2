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
    const val RELEASE = "PRODUCTION-201702131207-594174784"

    const val ACTIVITY_POINTS_BALANCE = 737 // 3461
    const val AVATAR_EFFECT = 3895 // 1693
    const val CAMERA_PRICE = 536 // 3737
    const val CAMPAIGN_OPEN1 = 3437 // 906
    const val CAMPAIGN_OPEN2 = 2255 // 121
    const val CATALOG_CONFIGURATION = 3692 // 3355
    const val CATALOG_GIFT_WRAPPING = 3220 // 1925
    const val CATALOG_HABBO_CLUB_PAGE = 3337 // 2557
    const val CATALOG_INDEX = 2903 // 2684
    const val CATALOG_OFFER = 1765 // 1642
    const val CATALOG_PAGE = 1481 // 1903
    const val CATALOG_PRODUCTDATA = 625 // 93
    const val CATALOG_PURCHASE = 3030 // 2645
    const val CATALOG_RECYCLER_REWARDS = 2372 // 3605
    const val CATALOG_REDEEM_VOUCHER = 3542 // 1557
    const val CLIENT_DEBUG = 2057 // 3236
    const val CLIENT_VARIABLES = 2654 // 2756
    const val CREDITS_BALANCE = 1596 // 1829
    const val EVENT_TRACKER = 2330 // 2827
    const val GAME_LISTING = 356 // 1344
    const val GO_TO_HOTEL_VIEW = 2324 // 2916
    const val GROUP_FORUMS = 978 // 969
    const val GROUP_INFO = 1717 // 1420
    const val HABBO_CAMERA_DATA = 3665 // 2878
    const val HABBO_CLUB_GIFTS = 3823 // 1761
    const val HABBO_CLUB_INFO = 1661 // 364
    const val INFO_RETRIEVE = 2490 // 873
    const val INITIALIZE_GAME_CENTER = 3036 // 1184
    const val INIT_CRYPTO = 3396 // 2686
    const val INVENTORY_BADGES = 3221 // 32
    const val INVENTORY_BOTS = 3263 // 2592
    const val INVENTORY_ITEMS = 2138 // 1006
    const val INVENTORY_PETS = 1983 // 388
    const val INVENTORY_UPDATE_BADGES = 397 // 181
    const val LANDING_PROMO_ARTICLES = 385 // 1644
    const val LANDING_REWARD = 2553 // 2772
    const val LOAD_INTERSTITIALS = 3759 // 1911
    const val MESSENGER_ACCEPT_REQUEST = 892 // 3705
    const val MESSENGER_CHAT = 1059 // 1381
    const val MESSENGER_DECLINE_REQUEST = 2210 // 488
    const val MESSENGER_FIND_NEW_FRIENDS = 1396 // 3623
    const val MESSENGER_FOLLOW_FRIEND = 2998 // 3445
    const val MESSENGER_FRIENDS_UPDATE = 567 // 1415
    const val MESSENGER_FRIEND_BAR_STATE = 3200 // 1174
    const val MESSENGER_INIT = 3588 // 1738
    const val MESSENGER_INVITE = 1885 // 665
    const val MESSENGER_REMOVE_FRIEND = 892 // 3705
    const val MESSENGER_REQUESTS = 993 // 3746
    const val MESSENGER_REQUEST_FRIEND = 2458 // 3258
    const val MESSENGER_SEARCH_FRIENDS = 1279 // 3887
    const val MESSENGER_SET_RELATIONSHIP = 2720 // 2539
    const val MODERATION_BAN_USER = 2758 // 1837
    const val MODERATION_CALL_FOR_HELP = 1734 // 1467
    const val MODERATION_CLOSE_TICKET = 1032 // 76
    const val MODERATION_CLOSE_TRADE_USER = 1358 // 57
    const val MODERATION_KICK_USER = 3255 // 1308
    const val MODERATION_MESSAGE_USER = 413 // 3
    const val MODERATION_MODERATE_ROOM = 341 // 2346
    const val MODERATION_MODERATE_USER = 3392 // 3441
    const val MODERATION_MUTE_USER = 2629 // 2724
    const val MODERATION_PICK_TICKET = 1974 // 1750
    const val MODERATION_RELEASE_TICKET = 88 // 512
    const val MODERATION_ROOM_CHATLOG = 3494 // 1410
    const val MODERATION_ROOM_INFO = 375 // 1554
    const val MODERATION_TICKET_CHATLOG = 1288 // 2934
    const val MODERATION_TOUR_REQUEST = 3529 // 1650
    const val MODERATION_USER_CHATLOG = 3194 // 882
    const val MODERATION_USER_INFO = 1716 // 411
    const val MODERATION_USER_ROOM_VISITS = 3276 // 3721
    const val MODERATION_WARN_USER = 266 // 1060
    const val NAVIGATOR_CREATE_ROOM = 3367 // 103
    const val NAVIGATOR_FLAT_CATEGORIES = 1990 // 2591
    const val NAVIGATOR_HOME_ROOM = 2046 // 3198
    const val NAVIGATOR_INITIALIZE = 832 // 415
    const val NAVIGATOR_PROMO_CATEGORIES = 1946 // 3081
    const val NAVIGATOR_SAVE_SETTINGS = 1674 // 485
    const val NAVIGATOR_SEARCH = 2348 // 1740
    const val PING = 953 // 3906
    const val PONG = 527 // 2270
    const val REFRESH_CAMPAIGN = 746 // 3182
    const val RELEASE_CHECK = 4000 // 4000
    const val ROOM_APPLY_DECORATION = 3611 // 2005
    const val ROOM_BANNED_USERS = 3570 // 2587
    const val ROOM_COMPETITION_STATUS = 2980 // 453
    const val ROOM_DIMMER_INFO = 409 // 3859
    const val ROOM_DIMMER_SWITCH = 39 // 216
    const val ROOM_DIMMER_UPDATE = 194 // 856
    const val ROOM_DOORBELL = 3751 // 2500
    const val ROOM_ENTRY_DATA = 2261 // 1831
    const val ROOM_GROUP_BADGES = 3624 // 2503
    const val ROOM_INFO = 1502 // 417
    const val ROOM_ITEM_ALIASES = 1158 // 138
    const val ROOM_ITEM_USE_CLOTHING = 1008 // 2478
    const val ROOM_LOAD_BY_DOORBELL = 3050 // 3550
    const val ROOM_LOOK_TO = 3380 // 3972
    const val ROOM_MANNEQUIN_CHANGE_FIGURE = 3808 // 2622
    const val ROOM_MANNEQUIN_CHANGE_NAME = 1160 // 2414
    const val ROOM_MOVE = 1857 // 2133
    const val ROOM_MOVE_FLOOR_ITEM = 612 // 1600
    const val ROOM_MOVE_WALL_ITEM = 3821 // 850
    const val ROOM_OPEN_FLAT = 1539 // 274
    const val ROOM_PLACE_BOT = 1847 // 169
    const val ROOM_PLACE_ITEM = 960 // 3781
    const val ROOM_PLACE_PET = 1382 // 2192
    const val ROOM_PLACE_POST_IT = 2544 // 3240
    const val ROOM_POST_IT = 342 // 441
    const val ROOM_REEDEM_EXCHANGE_ITEM = 605 // 2676
    const val ROOM_REMOVE_POST_IT = 1924 // 3535
    const val ROOM_RIGHTS = 3142 // 1280
    const val ROOM_SAVE_POST_IT = 2201 // 881
    const val ROOM_SAVE_SETTINGS = 2700 // 1625
    const val ROOM_SETTINGS = 1854 // 1991
    const val ROOM_TAKE_BOT = 1340 // 1127
    const val ROOM_TAKE_ITEM = 2739 // 2046
    const val ROOM_TAKE_PET = 177 // 3223
    const val ROOM_TRIGGER_CLOSE_DICE = 1844 // 1666
    const val ROOM_TRIGGER_HABBO_WHEEL = 1708 // 2575
    const val ROOM_TRIGGER_ITEM = 790 // 1699
    const val ROOM_TRIGGER_ONE_WAY_GATE = 913 // 600
    const val ROOM_TRIGGER_ROLL_DICE = 1426 // 2129
    const val ROOM_TRIGGER_WALL_ITEM = 1086 // 3227
    const val ROOM_UPDATE_WORD_FILTER = 292 // 89
    const val ROOM_USER_ACTION = 1888 // 1540
    const val ROOM_USER_CHAT = 1456 // 2235
    const val ROOM_USER_DANCE = 3830 // 2769
    const val ROOM_USER_DROP_HANDITEM = 2566 // 3207
    const val ROOM_USER_GIVE_HANDITEM = 269 // 1054
    const val ROOM_USER_SHOUT = 2524 // 2868
    const val ROOM_USER_SIGN = 278 // 2426
    const val ROOM_USER_SIT = 3779 // 2151
    const val ROOM_USER_START_TYPING = 1853 // 2901
    const val ROOM_USER_STOP_TYPING = 2080 // 3078
    const val ROOM_USER_WHISPER = 1999 // 2294
    const val ROOM_WIRED_SAVE_CONDITION = 3183 // 1918
    const val ROOM_WIRED_SAVE_EFFECT = 1737 // 1869
    const val ROOM_WIRED_SAVE_TRIGGER = 1753 // 899
    const val ROOM_WORD_FILTER = 1503 // 3314
    const val SANCTION_STATUS = 3602 // 3772
    const val SECRET_KEY = 1328 // 1377
    const val SETTINGS_SAVE_CHAT = 2158 // 554
    const val SETTINGS_SAVE_FOCUS = 2618 // 633
    const val SETTINGS_SAVE_INVITES = 2292 // 3985
    const val SETTINGS_SAVE_VOLUME = 3834 // 3178
    const val SET_USERNAME = 3783 // 2834
    const val SSO_TICKET = 2162 // 1376
    const val SUBSCRIPTION_STATUS = 3303 // 1892
    const val TALENT_STATUS = 1311 // 2734
    const val UNIQUE_ID = 3685 // 613
    const val USER_ACHIEVEMENT = 2865 // 2333
    const val USER_BADGES = 2610 // 1895
    const val USER_CHANGE_FIGURE = 962 // 66
    const val USER_CHANGE_MOTTO = 2679 // 1170
    const val USER_PROFILE = 2837 // 1364
    const val USER_RELATIONSHIPS = 2879 // 2312
    const val USER_SETTINGS = 877 // 3994
    const val USER_TAGS = 1417 // 1890
    const val USER_WARDROBES = 148 // 2024
    const val USER_WARDROBE_SAVE = 3915 // 2752
}