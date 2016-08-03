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

package tk.jomp16.habbo.communication.outgoing

object Outgoing {
    const val INIT_CRYPTO = 1711 // InitCryptoMessageComposer
    const val SECRET_KEY = 707 // SecretKeyMessageComposer
    const val UNIQUE_ID = 3302 // SetUniqueIdMessageComposer
    const val BROADCAST_NOTIFICATION = 385 // BroadcastMessageAlertMessageComposer
    const val MOTD_NOTIFICATION = 1402 // MOTDNotificationMessageComposer
    const val SUPER_NOTIFICATION = 619 // RoomNotificationMessageComposer
    const val AUTHENTICATION_OK = 3063 // AuthenticationOKMessageComposer
    const val HOME_ROOM = 2469 // NavigatorSettingsMessageComposer
    const val NAVIGATOR_FAVORITES = 2652 // FavouritesMessageComposer
    const val USER_RIGHTS = 71 // UserRightsMessageComposer
    const val AVAILABILITY_STATUS = 1957 // AvailabilityStatusMessageComposer
    const val ACHIEVEMENT_SCORE = 1181 // AchievementScoreMessageComposer
    const val BUILDERS_CLUB_MEMBERSHIP = 1308 // BuildersClubMembershipMessageComposer
    const val USER_SETTINGS = 3960 // SoundSettingsMessageComposer
    const val CAMPAIGN = 2052 // CampaignMessageComposer
    const val SUBSCRIPTION_STATUS = 284 // ScrSendUserInfoMessageComposer
    const val CREDITS_BALANCE = 2866 // CreditBalanceMessageComposer
    const val ACTIVITY_POINTS_BALANCE = 1036 // ActivityPointsMessageComposer
    const val USER_OBJECT = 40 // UserObjectMessageComposer
    const val USER_PERKS = 3722 // UserPerksMessageComposer
    const val NAVIGATOR_METADATA = 2867 // NavigatorMetaDataParserMessageComposer
    const val NAVIGATOR_LIFTED_ROOMS = 2876 // NavigatorLiftedRoomsMessageComposer
    const val NAVIGATOR_PREFERENCES = 2911 // NavigatorPreferencesMessageComposer
    const val NAVIGATOR_COLLAPSED_CATEGORIES = 3928 // NavigatorCollapsedCategoriesMessageComposer
    const val MESSENGER_INIT = 3973 // MessengerInitMessageComposer
    const val MESSENGER_FRIENDS = 650 // BuddyListMessageComposer
    const val MESSENGER_CHAT = 2128 // NewConsoleMessageMessageComposer
    const val MESSENGER_FRIEND_UPDATE = 504 // FriendListUpdateMessageComposer
    const val MESSENGER_REQUESTS = 455 // BuddyRequestsMessageComposer
    const val AVATAR_EFFECTS = 3940 // AvatarEffectsMessageComposer
    const val FIGURE_SETS = 3707 // FigureSetIdsMessageComposer
    const val ROOM_USERS_STATUSES = 2798 // UserUpdateMessageComposer
    const val NAVIGATOR_FLAT_CATEGORIES = 1952 // UserFlatCatsMessageComposer
    const val NAVIGATOR_PROMO_CATEGORIES = 1228 // NavigatorFlatCatsMessageComposer
    const val NAVIGATOR_SEARCH = 328 // NavigatorSearchResultSetMessageComposer
    const val ROOM_INFO = 887 // GetGuestRoomResultMessageComposer
    const val GENERIC_ERROR = 3781 // GenericErrorMessageComposer
    const val ROOM_EXIT = 3422 // CloseConnectionMessageComposer
    const val ROOM_USER_REMOVE = 1925 // UserRemoveMessageComposer
    const val ROOM_ERROR = 2339 // CantConnectMessageComposer
    const val ROOM_OPEN = 224 // OpenConnectionMessageComposer
    const val ROOM_INITIAL_INFO = 729 // RoomReadyMessageComposer
    const val ROOM_DECORATION = 336 // RoomPropertyMessageComposer
    const val ROOM_ITEM_ALIASES = 1688 // FurnitureAliasesMessageComposer
    const val ROOM_HEIGHTMAP = 1801 // HeightMapMessageComposer
    const val ROOM_FLOORMAP = 419 // FloorHeightMapMessageComposer
    const val ROOM_USERS = 779 // UsersMessageComposer
    const val ROOM_VISUALIZATION_THICKNESS = 1180 // RoomVisualizationSettingsMessageComposer
    const val USER_UPDATE = 2662 // UserChangeMessageComposer
    const val ROOM_OWNER = 3588 // YouAreOwnerMessageComposer
    const val ROOM_RIGHT_LEVEL = 2951 // YouAreControllerMessageComposer
    const val ROOM_NO_RIGHTS = 3026 // YouAreNotControllerMessageComposer
    const val ROOM_OWNERSHIP = 2659 // RoomEntryInfoMessageComposer
    const val MESSENGER_FOLLOW_FRIEND_ERROR = 3434 // FollowFriendFailedMessageComposer
    const val ROOM_FORWARD = 2082 // RoomForwardMessageComposer
    const val ROOM_FLOOR_ITEMS = 1495 // ObjectsMessageComposer
    const val ROOM_WALL_ITEMS = 745 // ItemsMessageComposer
    const val LANDING_PROMO_ARTICLES = 2352 // PromoArticlesMessageComposer
    const val ROOM_DOORBELL = 3464 // DoorbellMessageComposer
    const val ROOM_DOORBELL_DENIED = 595 // FlatAccessDeniedMessageComposer
    const val ROOM_DOORBELL_ACCEPT = 237 // FlatAccessibleMessageComposer
    const val INVENTORY_BADGES = 3337 // BadgesMessageComposer
    const val INVENTORY_NEW_OBJECTS = 519 // FurniListNotificationMessageComposer
    const val USER_BADGES = 959 // HabboUserBadgesMessageComposer
    const val USER_TAGS = 446 // UserTagsMessageComposer
    const val MESSENGER_SEARCH_FRIENDS = 3102 // HabboSearchResultMessageComposer
    const val MESSENGER_REQUEST_FRIEND = 1185 // NewBuddyRequestMessageComposer
    const val CATALOG_INDEX = 2596 // CatalogIndexMessageComposer
    const val CATALOG_PAGE = 472 // CatalogPageMessageComposer
    const val CATALOG_CONFIGURATION = 866 // MarketplaceConfigurationMessageComposer
    const val CATALOG_GIFT_WRAPPING = 3976 // GiftWrappingConfigurationMessageComposer
    const val CATALOG_RECYCLER_REWARDS = 2704 // RecyclerRewardsMessageComposer
    const val CATALOG_OFFER_CONFIGURATION = 1008 // CatalogItemDiscountMessageComposer
    const val ROOM_WALL_ITEM_UPDATE = 3671 // ItemUpdateMessageComposer
    const val ROOM_FLOOR_ITEM_UPDATE = 3902 // ObjectUpdateMessageComposer
    const val INVENTORY_ITEMS = 1814 // FurniListMessageComposer
    const val USER_WARDROBES = 1533 // WardrobeMessageComposer
    const val ROOM_ITEM_ADDED = 459 // ObjectAddMessageComposer
    const val INVENTORY_REMOVE_OBJECT = 2648 // FurniListRemoveMessageComposer
    const val NAVIGATOR_CREATE_ROOM = 362 // FlatCreatedMessageComposer
    const val ROOM_USER_IDLE = 1059 // SleepMessageComposer
    const val ROOM_USER_CHAT = 3816 // ChatMessageComposer
    const val ROOM_USER_SHOUT = 139 // ShoutMessageComposer
    const val ROOM_USER_ACTION = 2165 // ActionMessageComposer
    const val CATALOG_HABBO_CLUB_PAGE = 2387 // HabboClubOffersMessageComposer
    const val ROOM_USER_TYPING = 3991 // UserTypingMessageComposer
    const val ROOM_USER_DANCE = 1707 // DanceMessageComposer
}