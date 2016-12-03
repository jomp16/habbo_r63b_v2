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
    const val INIT_CRYPTO = 2607 // InitCryptoMessageComposer
    const val SECRET_KEY = 2899 // SecretKeyMessageComposer
    const val UNIQUE_ID = 2918 // SetUniqueIdMessageComposer
    const val BROADCAST_NOTIFICATION = 3423 // BroadcastMessageAlertMessageComposer
    const val MOTD_NOTIFICATION = 3854 // MOTDNotificationMessageComposer
    const val SUPER_NOTIFICATION = 312 // RoomNotificationMessageComposer
    const val AUTHENTICATION_OK = 3115 // AuthenticationOKMessageComposer
    const val HOME_ROOM = 2459 // NavigatorSettingsMessageComposer
    const val NAVIGATOR_FAVORITES = 1740 // FavouritesMessageComposer
    const val USER_RIGHTS = 386 // UserRightsMessageComposer
    const val AVAILABILITY_STATUS = 3413 // AvailabilityStatusMessageComposer
    const val ACHIEVEMENT_SCORE = 3823 // AchievementScoreMessageComposer
    const val BUILDERS_CLUB_MEMBERSHIP = 1963 // BuildersClubMembershipMessageComposer
    const val USER_SETTINGS = 2483 // SoundSettingsMessageComposer
    const val CAMPAIGN = 3051 // CampaignMessageComposer
    const val SUBSCRIPTION_STATUS = 78 // ScrSendUserInfoMessageComposer
    const val CREDITS_BALANCE = 1608 // CreditBalanceMessageComposer
    const val ACTIVITY_POINTS_BALANCE = 574 // ActivityPointsMessageComposer
    const val USER_OBJECT = 1758 // UserObjectMessageComposer
    const val USER_PERKS = 3122 // UserPerksMessageComposer
    const val NAVIGATOR_METADATA = 2974 // NavigatorMetaDataParserMessageComposer
    const val NAVIGATOR_LIFTED_ROOMS = 8 // NavigatorLiftedRoomsMessageComposer
    const val NAVIGATOR_PREFERENCES = 1785 // NavigatorPreferencesMessageComposer
    const val NAVIGATOR_COLLAPSED_CATEGORIES = 2194 // NavigatorCollapsedCategoriesMessageComposer
    const val MESSENGER_INIT = 586 // MessengerInitMessageComposer
    const val MESSENGER_FRIENDS = 356 // BuddyListMessageComposer
    const val MESSENGER_CHAT = 1030 // NewConsoleMessageMessageComposer
    const val MESSENGER_FRIEND_UPDATE = 2665 // FriendListUpdateMessageComposer
    const val MESSENGER_REQUESTS = 3163 // BuddyRequestsMessageComposer
    const val AVATAR_EFFECTS = 1199 // AvatarEffectsMessageComposer
    const val FIGURE_SETS = 663 // FigureSetIdsMessageComposer
    const val ROOM_USERS_STATUSES = 2404 // UserUpdateMessageComposer
    const val NAVIGATOR_FLAT_CATEGORIES = 911 // UserFlatCatsMessageComposer
    const val NAVIGATOR_PROMO_CATEGORIES = 1885 // NavigatorFlatCatsMessageComposer
    const val NAVIGATOR_SEARCH = 2561 // NavigatorSearchResultSetMessageComposer
    const val ROOM_INFO = 3897 // GetGuestRoomResultMessageComposer
    const val GENERIC_ERROR = 545 // GenericErrorMessageComposer
    const val ROOM_EXIT = 2968 // CloseConnectionMessageComposer
    const val ROOM_USER_REMOVE = 3843 // UserRemoveMessageComposer
    const val ROOM_ERROR = 1034 // CantConnectMessageComposer
    const val ROOM_OPEN = 1821 // OpenConnectionMessageComposer
    const val ROOM_INITIAL_INFO = 3646 // RoomReadyMessageComposer
    const val ROOM_DECORATION = 3838 // RoomPropertyMessageComposer
    const val ROOM_ITEM_ALIASES = 3388 // FurnitureAliasesMessageComposer
    const val ROOM_HEIGHTMAP = 1921 // HeightMapMessageComposer
    const val ROOM_FLOORMAP = 3494 // FloorHeightMapMessageComposer
    const val ROOM_USERS = 2954 // UsersMessageComposer
    const val ROOM_VISUALIZATION_THICKNESS = 134 // RoomVisualizationSettingsMessageComposer
    const val USER_UPDATE = 2447 // UserChangeMessageComposer
    const val ROOM_OWNER = 466 // YouAreOwnerMessageComposer
    const val ROOM_RIGHT_LEVEL = 1829 // YouAreControllerMessageComposer
    const val ROOM_NO_RIGHTS = 3070 // YouAreNotControllerMessageComposer
    const val ROOM_OWNERSHIP = 1376 // RoomEntryInfoMessageComposer
    const val MESSENGER_FOLLOW_FRIEND_ERROR = 3235 // FollowFriendFailedMessageComposer
    const val ROOM_FORWARD = 1484 // RoomForwardMessageComposer
    const val ROOM_FLOOR_ITEMS = 3536 // ObjectsMessageComposer
    const val ROOM_WALL_ITEMS = 606 // ItemsMessageComposer
    const val LANDING_PROMO_ARTICLES = 2738 // PromoArticlesMessageComposer
    const val ROOM_DOORBELL = 601 // DoorbellMessageComposer
    const val ROOM_DOORBELL_DENIED = 2000 // FlatAccessDeniedMessageComposer
    const val ROOM_DOORBELL_ACCEPT = 383 // FlatAccessibleMessageComposer
    const val INVENTORY_BADGES = 2705 // BadgesMessageComposer
    const val INVENTORY_NEW_OBJECTS = 1045 // FurniListNotificationMessageComposer
    const val USER_BADGES = 1855 // HabboUserBadgesMessageComposer
    const val USER_TAGS = 2416 // UserTagsMessageComposer
    const val MESSENGER_SEARCH_FRIENDS = 2252 // HabboSearchResultMessageComposer
    const val MESSENGER_REQUEST_FRIEND = 963 // NewBuddyRequestMessageComposer
    const val CATALOG_INDEX = 3259 // CatalogIndexMessageComposer
    const val CATALOG_PAGE = 3901 // CatalogPageMessageComposer
    const val CATALOG_CONFIGURATION = 2648 // MarketplaceConfigurationMessageComposer
    const val CATALOG_GIFT_WRAPPING = 1651 // GiftWrappingConfigurationMessageComposer
    const val CATALOG_RECYCLER_REWARDS = 459 // RecyclerRewardsMessageComposer
    const val CATALOG_OFFER_CONFIGURATION = 2769 // CatalogItemDiscountMessageComposer
    const val ROOM_WALL_ITEM_UPDATE = 2672 // ItemUpdateMessageComposer
    const val ROOM_FLOOR_ITEM_UPDATE = 1543 // ObjectUpdateMessageComposer
    const val INVENTORY_ITEMS = 1168 // FurniListMessageComposer
    const val USER_WARDROBES = 2698 // WardrobeMessageComposer
    const val ROOM_ITEM_ADDED = 1037 // ObjectAddMessageComposer
    const val ROOM_WALL_ITEM_ADDED = 2085 // ItemAddMessageComposer
    const val INVENTORY_REMOVE_OBJECT = 41 // FurniListRemoveMessageComposer
    const val NAVIGATOR_CREATE_ROOM = 1771 // FlatCreatedMessageComposer
    const val ROOM_USER_IDLE = 3929 // SleepMessageComposer
    const val ROOM_USER_CHAT = 2563 // ChatMessageComposer
    const val ROOM_USER_SHOUT = 2858 // ShoutMessageComposer
    const val ROOM_USER_WHISPER = 1332 // WhisperMessageComposer
    const val ROOM_USER_ACTION = 3783 // ActionMessageComposer
    const val CATALOG_HABBO_CLUB_PAGE = 2916 // HabboClubOffersMessageComposer
    const val ROOM_USER_TYPING = 3554 // UserTypingMessageComposer
    const val ROOM_USER_DANCE = 2083 // DanceMessageComposer
    const val CATALOG_PURCHASE_ERROR = 2946 // PurchaseErrorMessageComposer
    const val INVENTORY_UPDATE = 1139 // FurniListUpdateMessageComposer
    const val CATALOG_PURCHASE_OK = 3013 // PurchaseOKMessageComposer
    const val CATALOG_VOUCHER_REDEEMED = 2011 // VoucherRedeemOkMessageComposer
    const val CATALOG_VOUCHER_REDEEM_ERROR = 3811 // VoucherRedeemErrorMessageComposer
    const val USER_UPDATE_FIGURE = 3331 // FigureUpdateMessageComposer
    const val HABBO_CLUB_INFO = 2505 // HabboClubCenterInfoMessageComposer

    // custom
    const val ROOM_UPDATE_FURNI_STACK = 3493 // jomp16!
    const val CATALOG_LIMITED_SOLD_OUT = 506 // jomp16!
    const val CATALOG_PURCHASE_NOT_ALLOWED = 2237 // jomp16!
}