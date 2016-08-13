/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.communication.incoming

object Incoming {
    const val RELEASE = "PRODUCTION-201607262204-86871104"

    const val RELEASE_CHECK = 4000 // GetClientVersionMessageEvent
    const val INIT_CRYPTO = 340 // InitCryptoMessageEvent
    const val SECRET_KEY = 460 // GenerateSecretKeyMessageEvent
    const val CLIENT_VARIABLES = 2463 // ClientVariablesMessageEvent
    const val UNIQUE_ID = 2220 // UniqueIDMessageEvent
    const val SSO_TICKET = 127 // SSOTicketMessageEvent
    const val CLIENT_DEBUG = 1873 // MemoryPerformanceMessageEvent
    const val INFO_RETRIEVE = 2139 // InfoRetrieveMessageEvent
    const val EVENT_TRACKER = 2544 // EventTrackerMessageEvent
    const val GAME_LISTING = 1242 // GetGameListingMessageEvent
    const val INITIALIZE_GAME_CENTER = 1011 // InitializeGameCenterMessageEvent
    const val MESSENGER_INIT = 3058 // MessengerInitMessageEvent
    const val NAVIGATOR_INITIALIZE = 199 // InitializeNewNavigatorMessageEvent
    const val CREDITS_BALANCE = 1543 // GetCreditsInfoMessageEvent
    const val SUBSCRIPTION_STATUS = 2100 // ScrGetUserInfoMessageEvent
    const val USER_WARDROBES = 277 // GetWardrobeMessageEvent
    const val ROOM_OPEN_FLAT = 3785 // OpenFlatConnectionMessageEvent
    const val ROOM_LOAD_BY_DOORBELL = 745 // GoToFlatMessageEvent
    const val LATENCY_TEST = 1717 // LatencyTestMessageEvent
    const val GO_TO_HOTEL_VIEW = 1794 // GoToHotelViewMessageEvent
    const val REFRESH_CAMPAIGN = 3388 // RefreshCampaignMessageEvent
    const val CATALOG_ROOM_PROMOTION = 1079 // GetCatalogRoomPromotionMessageEvent
    const val SET_USERNAME = 3853 // SetUsernameMessageEvent
    const val NAVIGATOR_FLAT_CATEGORIES = 187 // GetUserFlatCatsMessageEvent
    const val NAVIGATOR_PROMO_CATEGORIES = 2919 // GetEventCategoriesMessageEvent
    const val NAVIGATOR_SEARCH = 105 // NewNavigatorSearchMessageEvent
    const val AVATAR_EFFECT = 728 // AvatarEffectSelectedMessageEvent
    const val CATALOG_RECYCLER_REWARDS = 3052 // GetRecyclerRewardsMessageEvent
    const val CATALOG_CONFIGURATION = 95 // GetMarketplaceConfigurationMessageEvent
    const val CATALOG_HABBO_CLUB_PAGE = 2277 // GetHabboClubWindowMessageEvent
    const val CATALOG_GIFT_WRAPPING = 2570 // GetGiftWrappingConfigurationMessageEvent
    const val CATALOG_INDEX = 3048 // GetCatalogIndexMessageEvent
    const val CATALOG_PAGE = 878 // GetCatalogPageMessageEvent
    const val MESSENGER_REQUESTS = 2492 // GetBuddyRequestsMessageEvent
    const val MESSENGER_FRIENDS_UPDATE = 2531 // FriendListUpdateMessageEvent
    const val MESSENGER_CHAT = 3653 // SendMsgMessageEvent
    const val ROOM_INFO = 3933 // GetGuestRoomMessageEvent
    const val ROOM_GROUP_BADGES = 1766 // GetHabboGroupBadgesMessageEvent
    const val ROOM_ITEM_ALIASES = 3675 // GetFurnitureAliasesMessageEvent
    const val ROOM_ENTRY_DATA = 764 // GetRoomEntryDataMessageEvent
    const val USER_ACHIEVEMENT = 1749 // GetAchievementsMessageEvent
    const val ROOM_MOVE = 2935 // MoveAvatarMessageEvent
    const val ROOM_LOOK_TO = 2988 // LookToMessageEvent
    const val USER_BADGES = 1594 // GetSelectedBadgesMessageEvent
    const val USER_TAGS = 1069 // GetUserTagsMessageEvent
    const val USER_RELATIONSHIPS = 1044 // GetRelationshipsMessageEvent
    const val ROOM_DOORBELL = 1332 // LetUserInMessageEvent
    const val MESSENGER_FRIEND_BAR_STATE = 1741 // SetFriendBarStateMessageEvent
    const val MESSENGER_FOLLOW_FRIEND = 3906 // FollowFriendMessageEvent
    const val NAVIGATOR_HOME_ROOM = 3753 // UpdateNavigatorSettingsMessageEvent
    const val LANDING_PROMO_ARTICLES = 291 // GetPromoArticlesMessageEvent
    const val INVENTORY_ITEMS = 696 // RequestFurniInventoryMessageEvent
    const val INVENTORY_BADGES = 3315 // GetBadgesMessageEvent
    const val INVENTORY_UPDATE_BADGES = 1447 // SetActivatedBadgesMessageEvent
    const val MESSENGER_SEARCH_FRIENDS = 1021 // HabboSearchMessageEvent
    const val MESSENGER_REQUEST_FRIEND = 2457 // RequestBuddyMessageEvent
    const val MESSENGER_ACCEPT_REQUEST = 109 // AcceptBuddyMessageEvent
    const val MESSENGER_DECLINE_REQUEST = 3875 // DeclineBuddyMessageEvent
    const val MESSENGER_REMOVE_FRIEND = 579 // RemoveBuddyMessageEvent
    const val ROOM_TRIGGER_ITEM = 2475 // UseFurnitureMessageEvent
    const val USER_PROFILE = 3412 // OpenPlayerProfileMessageEvent
    const val INVENTORY_PETS = 194 // GetPetInventoryMessageEvent
    const val INVENTORY_BOTS = 1379 // GetBotInventoryMessageEvent
    const val ROOM_PLACE_ITEM = 1262 // PlaceObjectMessageEvent
    const val ROOM_MOVE_FLOOR_ITEM = 2660 // MoveObjectMessageEvent
    const val ROOM_MOVE_WALL_ITEM = 15 // MoveWallItemMessageEvent
    const val USER_CHANGE_FIGURE = 1476 // UpdateFigureDataMessageEvent
    const val USER_WARDROBE_SAVE = 1377 // SaveWardrobeOutfitMessageEvent
    const val ROOM_USER_ACTION = 3417 // ActionMessageEvent
    const val ROOM_USER_SIGN = 3184 // ApplySignMessageEvent
    const val ROOM_USER_CHAT = 520 // ChatMessageEvent
    const val ROOM_USER_SHOUT = 1134 // ShoutMessageEvent
    const val ROOM_USER_START_TYPING = 1022 // StartTypingMessageEvent
    const val ROOM_USER_STOP_TYPING = 1096 // CancelTypingMessageEvent
    const val NAVIGATOR_CREATE_ROOM = 1674 // CreateFlatMessageEvent
    const val CATALOG_PURCHASE = 1986 // PurchaseFromCatalogMessageEvent
    const val ROOM_USER_DANCE = 1551 // DanceMessageEvent
}