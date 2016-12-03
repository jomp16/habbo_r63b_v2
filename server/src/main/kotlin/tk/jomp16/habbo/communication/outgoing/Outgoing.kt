/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.communication.outgoing

object Outgoing {
    const val INIT_CRYPTO = 1347 // 2607
    const val SECRET_KEY = 3885 // 2899
    const val UNIQUE_ID = 1488 // 2918
    const val BROADCAST_NOTIFICATION = 3801 // 3423
    const val MOTD_NOTIFICATION = 2035 // 3854
    const val SUPER_NOTIFICATION = 1992 // 312
    const val AUTHENTICATION_OK = 2491 // 3115
    const val HOME_ROOM = 2875 // 2459
    const val NAVIGATOR_FAVORITES = 151 // 1740
    const val USER_RIGHTS = 411 // 386
    const val AVAILABILITY_STATUS = 2033 // 3413
    const val ACHIEVEMENT_SCORE = 1968 // 3823
    const val BUILDERS_CLUB_MEMBERSHIP = 1452 // 1963
    const val USER_SETTINGS = 513 // 2483
    const val CAMPAIGN = 1745 // 3051
    const val SUBSCRIPTION_STATUS = 954 // 78
    const val CREDITS_BALANCE = 3475 // 1608
    const val ACTIVITY_POINTS_BALANCE = 2018 // 574
    const val USER_OBJECT = 2725 // 1758
    const val USER_PERKS = 2586 // 3122
    const val NAVIGATOR_METADATA = 3052 // 2974
    const val NAVIGATOR_LIFTED_ROOMS = 3104 // 8
    const val NAVIGATOR_PREFERENCES = 518 // 1785
    const val NAVIGATOR_COLLAPSED_CATEGORIES = 1543 // 2194
    const val MESSENGER_INIT = 1605 // 586
    const val MESSENGER_FRIENDS = 3130 // 356
    const val MESSENGER_CHAT = 1587 // 1030
    const val MESSENGER_FRIEND_UPDATE = 2800 // 2665
    const val MESSENGER_REQUESTS = 280 // 3163
    const val AVATAR_EFFECTS = 340 // 1199
    const val FIGURE_SETS = 1450 // 663
    const val ROOM_USERS_STATUSES = 1640 // 2404
    const val NAVIGATOR_FLAT_CATEGORIES = 1562 // 911
    const val NAVIGATOR_PROMO_CATEGORIES = 3244 // 1885
    const val NAVIGATOR_SEARCH = 2690 // 2561
    const val ROOM_INFO = 687 // 3897
    const val GENERIC_ERROR = 1600 // 545
    const val ROOM_EXIT = 122 // 2968
    const val ROOM_USER_REMOVE = 2661 // 3843
    const val ROOM_ERROR = 899 // 1034
    const val ROOM_OPEN = 758 // 1821
    const val ROOM_INITIAL_INFO = 2031 // 3646
    const val ROOM_DECORATION = 2454 // 3838
    const val ROOM_ITEM_ALIASES = 1723 // 3388
    const val ROOM_HEIGHTMAP = 2753 // 1921
    const val ROOM_FLOORMAP = 1301 // 3494
    const val ROOM_USERS = 374 // 2954
    const val ROOM_VISUALIZATION_THICKNESS = 3547 // 134
    const val USER_UPDATE = 3920 // 2447
    const val ROOM_OWNER = 339 // 466
    const val ROOM_RIGHT_LEVEL = 780 // 1829
    const val ROOM_NO_RIGHTS = 2392 // 3070
    const val ROOM_OWNERSHIP = 749 // 1376
    const val MESSENGER_FOLLOW_FRIEND_ERROR = 3048 // 3235
    const val ROOM_FORWARD = 160 // 1484
    const val ROOM_FLOOR_ITEMS = 1778 // 3536
    const val ROOM_WALL_ITEMS = 1369 // 606
    const val LANDING_PROMO_ARTICLES = 286 // 2738
    const val ROOM_DOORBELL = 2309 // 601
    const val ROOM_DOORBELL_DENIED = 878 // 2000
    const val ROOM_DOORBELL_ACCEPT = 3783 // 383
    const val INVENTORY_BADGES = 717 // 2705
    const val INVENTORY_NEW_OBJECTS = 2103 // 1045
    const val USER_BADGES = 1087 // 1855
    const val USER_TAGS = 1255 // 2416
    const val MESSENGER_SEARCH_FRIENDS = 973 // 2252
    const val MESSENGER_REQUEST_FRIEND = 2219 // 963
    const val CATALOG_INDEX = 1032 // 3259
    const val CATALOG_PAGE = 804 // 3901
    const val CATALOG_CONFIGURATION = 1823 // 2648
    const val CATALOG_GIFT_WRAPPING = 2234 // 1651
    const val CATALOG_RECYCLER_REWARDS = 3164 // 459
    const val CATALOG_OFFER_CONFIGURATION = 2347 // 2769
    const val ROOM_WALL_ITEM_UPDATE = 2009 // 2672
    const val ROOM_FLOOR_ITEM_UPDATE = 3776 // 1543
    const val INVENTORY_ITEMS = 994 // 1168
    const val USER_WARDROBES = 3315 // 2698
    const val ROOM_ITEM_ADDED = 1534 // 1037
    const val ROOM_WALL_ITEM_ADDED = 2187 // 2085
    const val INVENTORY_REMOVE_OBJECT = 159 // 41
    const val NAVIGATOR_CREATE_ROOM = 1304 // 1771
    const val ROOM_USER_IDLE = 1797 // 3929
    const val ROOM_USER_CHAT = 1446 // 2563
    const val ROOM_USER_SHOUT = 1036 // 2858
    const val ROOM_USER_WHISPER = 2704 // 1332
    const val ROOM_USER_ACTION = 1631 // 3783
    const val CATALOG_HABBO_CLUB_PAGE = 2405 // 2916
    const val ROOM_USER_TYPING = 1717 // 3554
    const val ROOM_USER_DANCE = 2233 // 2083
    const val CATALOG_PURCHASE_ERROR = 1404 // 2946
    const val INVENTORY_UPDATE = 3151 // 1139
    const val CATALOG_PURCHASE_OK = 869 // 3013
    const val CATALOG_VOUCHER_REDEEMED = 3336 // 2011
    const val CATALOG_VOUCHER_REDEEM_ERROR = 714 // 3811
    const val USER_UPDATE_FIGURE = 2429 // 3331
    const val HABBO_CLUB_INFO = 3277 // 2505
    const val ROOM_UPDATE_FURNI_STACK = 558 // 3493
    const val CATALOG_LIMITED_SOLD_OUT = 377 // 506
    const val CATALOG_PURCHASE_NOT_ALLOWED = 3770 // 2237
}