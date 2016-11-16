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

@Suppress("unused")
object Incoming {
    const val RELEASE = "PRODUCTION-201610052203-260805057"

    const val RELEASE_CHECK = 4000 // GetClientVersionMessageEvent
    const val INIT_CRYPTO = 3091 // InitCryptoMessageEvent
    const val SECRET_KEY = 239 // GenerateSecretKeyMessageEvent
    const val CLIENT_VARIABLES = 2956 // ClientVariablesMessageEvent
    const val UNIQUE_ID = 1666 // UniqueIDMessageEvent
    const val SSO_TICKET = 2176 // SSOTicketMessageEvent
    const val CLIENT_DEBUG = 458 // MemoryPerformanceMessageEvent
    const val INFO_RETRIEVE = 3980 // InfoRetrieveMessageEvent
    const val EVENT_TRACKER = 1751 // EventTrackerMessageEvent
    const val GAME_LISTING = 3507 // GetGameListingMessageEvent
    const val INITIALIZE_GAME_CENTER = 3667 // InitializeGameCenterMessageEvent
    const val MESSENGER_INIT = 1552 // MessengerInitMessageEvent
    const val NAVIGATOR_INITIALIZE = 3956 // InitializeNewNavigatorMessageEvent
    const val CREDITS_BALANCE = 3355 // GetCreditsInfoMessageEvent
    const val SUBSCRIPTION_STATUS = 444 // ScrGetUserInfoMessageEvent
    const val USER_WARDROBES = 2870 // GetWardrobeMessageEvent
    const val ROOM_OPEN_FLAT = 3451 // OpenFlatConnectionMessageEvent
    const val ROOM_LOAD_BY_DOORBELL = 3938 // GoToFlatMessageEvent
    const val LATENCY_TEST = 2194 // LatencyTestMessageEvent
    const val GO_TO_HOTEL_VIEW = 2988 // GoToHotelViewMessageEvent
    const val REFRESH_CAMPAIGN = 244 // RefreshCampaignMessageEvent
    const val CATALOG_ROOM_PROMOTION = 540 // GetCatalogRoomPromotionMessageEvent
    const val SET_USERNAME = 1709 // SetUsernameMessageEvent
    const val NAVIGATOR_FLAT_CATEGORIES = 2612 // GetUserFlatCatsMessageEvent
    const val NAVIGATOR_PROMO_CATEGORIES = 671 // GetEventCategoriesMessageEvent
    const val NAVIGATOR_SEARCH = 462 // NewNavigatorSearchMessageEvent
    const val AVATAR_EFFECT = 2460 // AvatarEffectSelectedMessageEvent
    const val CATALOG_RECYCLER_REWARDS = 996 // GetRecyclerRewardsMessageEvent
    const val CATALOG_CONFIGURATION = 3838 // GetMarketplaceConfigurationMessageEvent
    const val CATALOG_HABBO_CLUB_PAGE = 1139 // GetHabboClubWindowMessageEvent
    const val CATALOG_GIFT_WRAPPING = 661 // GetGiftWrappingConfigurationMessageEvent
    const val CATALOG_INDEX = 3058 // GetCatalogIndexMessageEvent
    const val CATALOG_PAGE = 2065 // GetCatalogPageMessageEvent
    const val MESSENGER_REQUESTS = 3740 // GetBuddyRequestsMessageEvent
    const val MESSENGER_FRIENDS_UPDATE = 1113 // FriendListUpdateMessageEvent
    const val MESSENGER_CHAT = 2893 // SendMsgMessageEvent
    const val ROOM_INFO = 1327 // GetGuestRoomMessageEvent
    const val ROOM_GROUP_BADGES = 379 // GetHabboGroupBadgesMessageEvent
    const val ROOM_ITEM_ALIASES = 3975 // GetFurnitureAliasesMessageEvent
    const val ROOM_ENTRY_DATA = 3994 // GetRoomEntryDataMessageEvent
    const val USER_ACHIEVEMENT = 710 // GetAchievementsMessageEvent
    const val ROOM_MOVE = 1660 // MoveAvatarMessageEvent
    const val ROOM_LOOK_TO = 2168 // LookToMessageEvent
    const val USER_BADGES = 2981 // GetSelectedBadgesMessageEvent
    const val USER_TAGS = 2743 // GetUserTagsMessageEvent
    const val USER_RELATIONSHIPS = 1228 // GetRelationshipsMessageEvent
    const val ROOM_DOORBELL = 3278 // LetUserInMessageEvent
    const val MESSENGER_FRIEND_BAR_STATE = 3707 // SetFriendBarStateMessageEvent
    const val MESSENGER_FOLLOW_FRIEND = 524 // FollowFriendMessageEvent
    const val NAVIGATOR_HOME_ROOM = 3307 // UpdateNavigatorSettingsMessageEvent
    const val LANDING_PROMO_ARTICLES = 1392 // GetPromoArticlesMessageEvent
    const val INVENTORY_ITEMS = 2591 // RequestFurniInventoryMessageEvent
    const val INVENTORY_BADGES = 1214 // GetBadgesMessageEvent
    const val INVENTORY_UPDATE_BADGES = 3345 // SetActivatedBadgesMessageEvent
    const val MESSENGER_SEARCH_FRIENDS = 609 // HabboSearchMessageEvent
    const val MESSENGER_REQUEST_FRIEND = 498 // RequestBuddyMessageEvent
    const val MESSENGER_ACCEPT_REQUEST = 3349 // AcceptBuddyMessageEvent
    const val MESSENGER_DECLINE_REQUEST = 2285 // DeclineBuddyMessageEvent
    const val MESSENGER_REMOVE_FRIEND = 3372 // RemoveBuddyMessageEvent
    const val ROOM_TRIGGER_ITEM = 2984 // UseFurnitureMessageEvent
    const val USER_PROFILE = 1289 // OpenPlayerProfileMessageEvent
    const val INVENTORY_PETS = 2135 // GetPetInventoryMessageEvent
    const val INVENTORY_BOTS = 3222 // GetBotInventoryMessageEvent
    const val ROOM_PLACE_ITEM = 1852 // PlaceObjectMessageEvent
    const val ROOM_MOVE_FLOOR_ITEM = 1322 // MoveObjectMessageEvent
    const val ROOM_MOVE_WALL_ITEM = 1293 // MoveWallItemMessageEvent
    const val USER_CHANGE_FIGURE = 1475 // UpdateFigureDataMessageEvent
    const val USER_WARDROBE_SAVE = 3605 // SaveWardrobeOutfitMessageEvent
    const val ROOM_USER_ACTION = 2924 // ActionMessageEvent
    const val ROOM_USER_SIGN = 3646 // ApplySignMessageEvent
    const val ROOM_USER_CHAT = 2141 // ChatMessageEvent
    const val ROOM_USER_SHOUT = 2236 // ShoutMessageEvent
    const val ROOM_USER_START_TYPING = 2735 // StartTypingMessageEvent
    const val ROOM_USER_STOP_TYPING = 2778 // CancelTypingMessageEvent
    const val NAVIGATOR_CREATE_ROOM = 3560 // CreateFlatMessageEvent
    const val CATALOG_PURCHASE = 1943 // PurchaseFromCatalogMessageEvent
    const val ROOM_USER_DANCE = 3421 // DanceMessageEvent
    const val USER_SETTINGS = 1423 // GetSoundSettingsMessageEvent
    const val CATALOG_REDEEM_VOUCHER = 2664 // RedeemVoucherMessageEvent
    const val HABBO_CLUB_GIFTS = 3724 // GetClubGiftsMessageEvent
    const val HABBO_CLUB_INFO = 42 // GetHabboClubCenterInfoMessageEvent
}