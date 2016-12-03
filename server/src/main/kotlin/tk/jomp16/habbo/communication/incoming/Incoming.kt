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

package tk.jomp16.habbo.communication.incoming

object Incoming {
    const val RELEASE = "PRODUCTION-201611161402-534608340"

    const val RELEASE_CHECK = 4000 // GetClientVersionMessageEvent
    const val INIT_CRYPTO = 1630 // InitCryptoMessageEvent
    const val SECRET_KEY = 1243 // GenerateSecretKeyMessageEvent
    const val CLIENT_VARIABLES = 46 // ClientVariablesMessageEvent
    const val UNIQUE_ID = 3221 // UniqueIDMessageEvent
    const val SSO_TICKET = 2895 // SSOTicketMessageEvent
    const val CLIENT_DEBUG = 3508 // MemoryPerformanceMessageEvent
    const val INFO_RETRIEVE = 472 // InfoRetrieveMessageEvent
    const val EVENT_TRACKER = 2501 // EventTrackerMessageEvent
    const val GAME_LISTING = 2040 // GetGameListingMessageEvent
    const val INITIALIZE_GAME_CENTER = 3661 // InitializeGameCenterMessageEvent
    const val MESSENGER_INIT = 117 // MessengerInitMessageEvent
    const val NAVIGATOR_INITIALIZE = 932 // InitializeNewNavigatorMessageEvent
    const val CREDITS_BALANCE = 876 // GetCreditsInfoMessageEvent
    const val SUBSCRIPTION_STATUS = 2756 // ScrGetUserInfoMessageEvent
    const val USER_WARDROBES = 344 // GetWardrobeMessageEvent
    const val ROOM_OPEN_FLAT = 463 // OpenFlatConnectionMessageEvent
    const val ROOM_LOAD_BY_DOORBELL = 1134 // GoToFlatMessageEvent
    const val LATENCY_TEST = 2909 // LatencyTestMessageEvent
    const val GO_TO_HOTEL_VIEW = 1207 // GoToHotelViewMessageEvent
    const val REFRESH_CAMPAIGN = 1350 // RefreshCampaignMessageEvent
    const val CATALOG_ROOM_PROMOTION = 722 // GetCatalogRoomPromotionMessageEvent
    const val SET_USERNAME = 3324 // SetUsernameMessageEvent
    const val NAVIGATOR_FLAT_CATEGORIES = 1900 // GetUserFlatCatsMessageEvent
    const val NAVIGATOR_PROMO_CATEGORIES = 2887 // GetEventCategoriesMessageEvent
    const val NAVIGATOR_SEARCH = 753 // NewNavigatorSearchMessageEvent
    const val AVATAR_EFFECT = 2244 // AvatarEffectSelectedMessageEvent
    const val CATALOG_RECYCLER_REWARDS = 1873 // GetRecyclerRewardsMessageEvent
    const val CATALOG_CONFIGURATION = 1429 // GetMarketplaceConfigurationMessageEvent
    const val CATALOG_HABBO_CLUB_PAGE = 3513 // GetHabboClubWindowMessageEvent
    const val CATALOG_GIFT_WRAPPING = 2844 // GetGiftWrappingConfigurationMessageEvent
    const val CATALOG_INDEX = 1344 // GetCatalogIndexMessageEvent
    const val CATALOG_PAGE = 3520 // GetCatalogPageMessageEvent
    const val MESSENGER_REQUESTS = 2913 // GetBuddyRequestsMessageEvent
    const val MESSENGER_FRIENDS_UPDATE = 17 // FriendListUpdateMessageEvent
    const val MESSENGER_CHAT = 1442 // SendMsgMessageEvent
    const val ROOM_INFO = 1563 // GetGuestRoomMessageEvent
    const val ROOM_GROUP_BADGES = 593 // GetHabboGroupBadgesMessageEvent
    const val ROOM_ITEM_ALIASES = 3724 // GetFurnitureAliasesMessageEvent
    const val ROOM_ENTRY_DATA = 3736 // GetRoomEntryDataMessageEvent
    const val USER_ACHIEVEMENT = 2211 // GetAchievementsMessageEvent
    const val ROOM_MOVE = 3534 // MoveAvatarMessageEvent
    const val ROOM_LOOK_TO = 3814 // LookToMessageEvent
    const val USER_BADGES = 3482 // GetSelectedBadgesMessageEvent
    const val USER_TAGS = 199 // GetUserTagsMessageEvent
    const val USER_RELATIONSHIPS = 1832 // GetRelationshipsMessageEvent
    const val ROOM_DOORBELL = 3344 // LetUserInMessageEvent
    const val MESSENGER_FRIEND_BAR_STATE = 2162 // SetFriendBarStateMessageEvent
    const val MESSENGER_FOLLOW_FRIEND = 3901 // FollowFriendMessageEvent
    const val NAVIGATOR_HOME_ROOM = 1104 // UpdateNavigatorSettingsMessageEvent
    const val LANDING_PROMO_ARTICLES = 1853 // GetPromoArticlesMessageEvent
    const val INVENTORY_ITEMS = 2923 // RequestFurniInventoryMessageEvent
    const val INVENTORY_BADGES = 1805 // GetBadgesMessageEvent
    const val INVENTORY_UPDATE_BADGES = 1146 // SetActivatedBadgesMessageEvent
    const val MESSENGER_SEARCH_FRIENDS = 3004 // HabboSearchMessageEvent
    const val MESSENGER_REQUEST_FRIEND = 345 // RequestBuddyMessageEvent
    const val MESSENGER_ACCEPT_REQUEST = 810 // AcceptBuddyMessageEvent
    const val MESSENGER_DECLINE_REQUEST = 1120 // DeclineBuddyMessageEvent
    const val MESSENGER_REMOVE_FRIEND = 987 // RemoveBuddyMessageEvent
    const val ROOM_TRIGGER_ITEM = 2898 // UseFurnitureMessageEvent
    const val ROOM_TRIGGER_WALL_ITEM = 3781 // UseWallItemMessageEvent
    const val USER_PROFILE = 3399 // OpenPlayerProfileMessageEvent
    const val INVENTORY_PETS = 837 // GetPetInventoryMessageEvent
    const val INVENTORY_BOTS = 1981 // GetBotInventoryMessageEvent
    const val ROOM_PLACE_ITEM = 411 // PlaceObjectMessageEvent
    const val ROOM_MOVE_FLOOR_ITEM = 1182 // MoveObjectMessageEvent
    const val ROOM_MOVE_WALL_ITEM = 2440 // MoveWallItemMessageEvent
    const val USER_CHANGE_FIGURE = 3688 // UpdateFigureDataMessageEvent
    const val USER_WARDROBE_SAVE = 2820 // SaveWardrobeOutfitMessageEvent
    const val ROOM_USER_ACTION = 2815 // ActionMessageEvent
    const val ROOM_USER_SIGN = 2549 // ApplySignMessageEvent
    const val ROOM_USER_CHAT = 2995 // ChatMessageEvent
    const val ROOM_USER_SHOUT = 3431 // ShoutMessageEvent
    const val ROOM_USER_WHISPER = 2763 // WhisperMessageEvent
    const val ROOM_USER_START_TYPING = 2564 // StartTypingMessageEvent
    const val ROOM_USER_STOP_TYPING = 2856 // CancelTypingMessageEvent
    const val NAVIGATOR_CREATE_ROOM = 3598 // CreateFlatMessageEvent
    const val CATALOG_PURCHASE = 2571 // PurchaseFromCatalogMessageEvent
    const val ROOM_USER_DANCE = 2953 // DanceMessageEvent
    const val USER_SETTINGS = 257 // GetSoundSettingsMessageEvent
    const val CATALOG_REDEEM_VOUCHER = 2411 // RedeemVoucherMessageEvent
    const val HABBO_CLUB_GIFTS = 3266 // GetClubGiftsMessageEvent
    const val HABBO_CLUB_INFO = 3049 // GetHabboClubCenterInfoMessageEvent
    const val ROOM_APPLY_DECORATION = 2199 // ApplyDecorationMessageEvent
    const val SANCTION_STATUS = 112 // GetSanctionStatusMessageEvent
}