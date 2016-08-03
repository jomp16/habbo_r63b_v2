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
    const val RELEASE = "PRODUCTION-201603071224-880429594"

    const val RELEASE_CHECK = 4000 // GetClientVersionMessageEvent
    const val INIT_CRYPTO = 1393 // InitCryptoMessageEvent
    const val SECRET_KEY = 1060 // GenerateSecretKeyMessageEvent
    const val CLIENT_VARIABLES = 639 // ClientVariablesMessageEvent
    const val UNIQUE_ID = 1446 // UniqueIDMessageEvent
    const val SSO_TICKET = 1136 // SSOTicketMessageEvent
    const val CLIENT_DEBUG = 2003 // MemoryPerformanceMessageEvent
    const val INFO_RETRIEVE = 3676 // InfoRetrieveMessageEvent
    const val EVENT_TRACKER = 164 // EventTrackerMessageEvent
    const val GAME_LISTING = 1143 // GetGameListingMessageEvent
    const val INITIALIZE_GAME_CENTER = 2500 // InitializeGameCenterMessageEvent
    const val MESSENGER_INIT = 2450 // MessengerInitMessageEvent
    const val NAVIGATOR_INITIALIZE = 1692 // InitializeNewNavigatorMessageEvent
    const val CREDITS_BALANCE = 3411 // GetCreditsInfoMessageEvent
    const val SUBSCRIPTION_STATUS = 1068 // ScrGetUserInfoMessageEvent
    const val USER_WARDROBES = 53 // GetWardrobeMessageEvent
    const val ROOM_OPEN_FLAT = 2744 // OpenFlatConnectionMessageEvent
    const val ROOM_LOAD_BY_DOORBELL = 3117 // GoToFlatMessageEvent
    const val LATENCY_TEST = 844 // LatencyTestMessageEvent
    const val GO_TO_HOTEL_VIEW = 1189 // GoToHotelViewMessageEvent
    const val REFRESH_CAMPAIGN = 1380 // RefreshCampaignMessageEvent
    const val CATALOG_ROOM_PROMOTION = 905 // GetCatalogRoomPromotionMessageEvent
    const val SET_USERNAME = 2811 // SetUsernameMessageEvent
    const val NAVIGATOR_FLAT_CATEGORIES = 1825 // GetUserFlatCatsMessageEvent
    const val NAVIGATOR_PROMO_CATEGORIES = 1406 // GetEventCategoriesMessageEvent
    const val NAVIGATOR_SEARCH = 2543 // NewNavigatorSearchMessageEvent
    const val AVATAR_EFFECT = 1856 // AvatarEffectSelectedMessageEvent
    const val CATALOG_RECYCLER_REWARDS = 1221 // GetRecyclerRewardsMessageEvent
    const val CATALOG_CONFIGURATION = 2527 // GetMarketplaceConfigurationMessageEvent
    const val CATALOG_HABBO_CLUB_PAGE = 155 // GetHabboClubWindowMessageEvent
    const val CATALOG_GIFT_WRAPPING = 3663 // GetGiftWrappingConfigurationMessageEvent
    const val CATALOG_INDEX = 3357 // GetCatalogIndexMessageEvent
    const val CATALOG_PAGE = 3889 // GetCatalogPageMessageEvent
    const val MESSENGER_REQUESTS = 3542 // GetBuddyRequestsMessageEvent
    const val MESSENGER_FRIENDS_UPDATE = 330 // FriendListUpdateMessageEvent
    const val MESSENGER_CHAT = 2483 // SendMsgMessageEvent
    const val ROOM_INFO = 3127 // GetGuestRoomMessageEvent
    const val ROOM_GROUP_BADGES = 2597 // GetHabboGroupBadgesMessageEvent
    const val ROOM_ITEM_ALIASES = 3918 // GetFurnitureAliasesMessageEvent
    const val ROOM_ENTRY_DATA = 3381 // GetRoomEntryDataMessageEvent
    const val USER_ACHIEVEMENT = 732 // GetAchievementsMessageEvent
    const val ROOM_MOVE = 3993 // MoveAvatarMessageEvent
    const val ROOM_LOOK_TO = 361 // LookToMessageEvent
    const val USER_BADGES = 1819 // GetSelectedBadgesMessageEvent
    const val USER_TAGS = 3241 // GetUserTagsMessageEvent
    const val USER_RELATIONSHIPS = 3152 // GetRelationshipsMessageEvent
    const val ROOM_DOORBELL = 3517 // LetUserInMessageEvent
    const val MESSENGER_FRIEND_BAR_STATE = 3256 // SetFriendBarStateMessageEvent
    const val MESSENGER_FOLLOW_FRIEND = 3066 // FollowFriendMessageEvent
    const val NAVIGATOR_HOME_ROOM = 2408 // UpdateNavigatorSettingsMessageEvent
    const val LANDING_PROMO_ARTICLES = 995 // GetPromoArticlesMessageEvent
    const val INVENTORY_ITEMS = 46 // RequestFurniInventoryMessageEvent
    const val INVENTORY_BADGES = 602 // GetBadgesMessageEvent
    const val INVENTORY_UPDATE_BADGES = 2684 // SetActivatedBadgesMessageEvent
    const val MESSENGER_SEARCH_FRIENDS = 3438 // HabboSearchMessageEvent
    const val MESSENGER_REQUEST_FRIEND = 3462 // RequestBuddyMessageEvent
    const val MESSENGER_ACCEPT_REQUEST = 3935 // AcceptBuddyMessageEvent
    const val MESSENGER_DECLINE_REQUEST = 3016 // DeclineBuddyMessageEvent
    const val MESSENGER_REMOVE_FRIEND = 512 // RemoveBuddyMessageEvent
    const val ROOM_TRIGGER_ITEM = 3678 // UseFurnitureMessageEvent
    const val USER_PROFILE = 1481 // OpenPlayerProfileMessageEvent
    const val INVENTORY_PETS = 731 // GetPetInventoryMessageEvent
    const val INVENTORY_BOTS = 987 // GetBotInventoryMessageEvent
    const val ROOM_PLACE_ITEM = 1861 // PlaceObjectMessageEvent
    const val ROOM_MOVE_FLOOR_ITEM = 556 // MoveObjectMessageEvent
    const val ROOM_MOVE_WALL_ITEM = 428 // MoveWallItemMessageEvent
    const val USER_CHANGE_FIGURE = 1175 // UpdateFigureDataMessageEvent
    const val USER_WARDROBE_SAVE = 1093 // SaveWardrobeOutfitMessageEvent
    const val ROOM_USER_ACTION = 212 // ActionMessageEvent
    const val ROOM_USER_SIGN = 1600 // ApplySignMessageEvent
    const val ROOM_USER_CHAT = 2252 // ChatMessageEvent
    const val ROOM_USER_SHOUT = 1471 // ShoutMessageEvent
    const val ROOM_USER_START_TYPING = 2170 // StartTypingMessageEvent
    const val ROOM_USER_STOP_TYPING = 2751 // CancelTypingMessageEvent
    const val NAVIGATOR_CREATE_ROOM = 1822 // CreateFlatMessageEvent
    const val CATALOG_PURCHASE = 730 // PurchaseFromCatalogMessageEvent
    const val ROOM_USER_DANCE = 3688 // DanceMessageEvent
    const val PING = 1006 // PingMessageEvent
}