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
    const val INIT_CRYPTO = 3855 // InitCryptoMessageComposer
    const val SECRET_KEY = 1312 // SecretKeyMessageComposer
    const val UNIQUE_ID = 31 // SetUniqueIdMessageComposer
    const val BROADCAST_NOTIFICATION = 3452 // BroadcastMessageAlertMessageComposer
    const val MOTD_NOTIFICATION = 1731 // MOTDNotificationMessageComposer
    const val SUPER_NOTIFICATION = 3538 // RoomNotificationMessageComposer
    const val AUTHENTICATION_OK = 1338 // AuthenticationOKMessageComposer
    const val HOME_ROOM = 3775 // NavigatorSettingsMessageComposer
    const val NAVIGATOR_FAVORITES = 772 // FavouritesMessageComposer
    const val USER_RIGHTS = 1455 // UserRightsMessageComposer
    const val AVAILABILITY_STATUS = 2786 // AvailabilityStatusMessageComposer
    const val ACHIEVEMENT_SCORE = 692 // AchievementScoreMessageComposer
    const val BUILDERS_CLUB_MEMBERSHIP = 3980 // BuildersClubMembershipMessageComposer
    const val USER_SETTINGS = 145 // SoundSettingsMessageComposer
    const val CAMPAIGN = 3617 // CampaignMessageComposer
    const val SUBSCRIPTION_STATUS = 2117 // ScrSendUserInfoMessageComposer
    const val CREDITS_BALANCE = 1490 // CreditBalanceMessageComposer
    const val ACTIVITY_POINTS_BALANCE = 1677 // ActivityPointsMessageComposer
    const val USER_OBJECT = 1325 // UserObjectMessageComposer
    const val USER_PERKS = 2193 // UserPerksMessageComposer
    const val NAVIGATOR_METADATA = 544 // NavigatorMetaDataParserMessageComposer
    const val NAVIGATOR_LIFTED_ROOMS = 1501 // NavigatorLiftedRoomsMessageComposer
    const val NAVIGATOR_PREFERENCES = 1067 // NavigatorPreferencesMessageComposer
    const val NAVIGATOR_COLLAPSED_CATEGORIES = 155 // NavigatorCollapsedCategoriesMessageComposer
    const val MESSENGER_INIT = 2179 // MessengerInitMessageComposer
    const val MESSENGER_FRIENDS = 368 // BuddyListMessageComposer
    const val MESSENGER_CHAT = 1850 // NewConsoleMessageMessageComposer
    const val MESSENGER_FRIEND_UPDATE = 577 // FriendListUpdateMessageComposer
    const val MESSENGER_REQUESTS = 3096 // BuddyRequestsMessageComposer
    const val AVATAR_EFFECTS = 823 // AvatarEffectsMessageComposer
    const val FIGURE_SETS = 1695 // FigureSetIdsMessageComposer
    const val ROOM_USERS_STATUSES = 1179 // UserUpdateMessageComposer
    const val NAVIGATOR_FLAT_CATEGORIES = 1382 // UserFlatCatsMessageComposer
    const val NAVIGATOR_PROMO_CATEGORIES = 1766 // NavigatorFlatCatsMessageComposer
    const val NAVIGATOR_SEARCH = 1909 // NavigatorSearchResultSetMessageComposer
    const val ROOM_INFO = 3817 // GetGuestRoomResultMessageComposer
    const val GENERIC_ERROR = 3514 // GenericErrorMessageComposer
    const val ROOM_EXIT = 73 // CloseConnectionMessageComposer
    const val ROOM_USER_REMOVE = 1566 // UserRemoveMessageComposer
    const val ROOM_ERROR = 598 // CantConnectMessageComposer
    const val ROOM_OPEN = 684 // OpenConnectionMessageComposer
    const val ROOM_INITIAL_INFO = 2669 // RoomReadyMessageComposer
    const val ROOM_DECORATION = 542 // RoomPropertyMessageComposer
    const val ROOM_ITEM_ALIASES = 2106 // FurnitureAliasesMessageComposer
    const val ROOM_HEIGHTMAP = 504 // HeightMapMessageComposer
    const val ROOM_FLOORMAP = 3902 // FloorHeightMapMessageComposer
    const val ROOM_USERS = 2797 // UsersMessageComposer
    const val ROOM_VISUALIZATION_THICKNESS = 2836 // RoomVisualizationSettingsMessageComposer
    const val USER_UPDATE = 1304 // UserChangeMessageComposer
    const val ROOM_OWNER = 3161 // YouAreOwnerMessageComposer
    const val ROOM_RIGHT_LEVEL = 302 // YouAreControllerMessageComposer
    const val ROOM_NO_RIGHTS = 1329 // YouAreNotControllerMessageComposer
    const val ROOM_OWNERSHIP = 2440 // RoomEntryInfoMessageComposer
    const val MESSENGER_FOLLOW_FRIEND_ERROR = 3765 // FollowFriendFailedMessageComposer
    const val ROOM_FORWARD = 3691 // RoomForwardMessageComposer
    const val ROOM_FLOOR_ITEMS = 3171 // ObjectsMessageComposer
    const val ROOM_WALL_ITEMS = 1235 // ItemsMessageComposer
    const val LANDING_PROMO_ARTICLES = 2772 // PromoArticlesMessageComposer
    const val ROOM_DOORBELL = 2658 // DoorbellMessageComposer
    const val ROOM_DOORBELL_DENIED = 1556 // FlatAccessDeniedMessageComposer
    const val ROOM_DOORBELL_ACCEPT = 2886 // FlatAccessibleMessageComposer
    const val INVENTORY_BADGES = 1963 // BadgesMessageComposer
    const val INVENTORY_NEW_OBJECTS = 2130 // FurniListNotificationMessageComposer
    const val USER_BADGES = 382 // HabboUserBadgesMessageComposer
    const val USER_TAGS = 2492 // UserTagsMessageComposer
    const val MESSENGER_SEARCH_FRIENDS = 880 // HabboSearchResultMessageComposer
    const val MESSENGER_REQUEST_FRIEND = 179 // NewBuddyRequestMessageComposer
    const val CATALOG_INDEX = 1275 // CatalogIndexMessageComposer
    const val CATALOG_PAGE = 3195 // CatalogPageMessageComposer
    const val CATALOG_CONFIGURATION = 1933 // MarketplaceConfigurationMessageComposer
    const val CATALOG_GIFT_WRAPPING = 1712 // GiftWrappingConfigurationMessageComposer
    const val CATALOG_RECYCLER_REWARDS = 3524 // RecyclerRewardsMessageComposer
    const val CATALOG_OFFER_CONFIGURATION = 835 // CatalogItemDiscountMessageComposer
    const val ROOM_WALL_ITEM_UPDATE = 2218 // ItemUpdateMessageComposer
    const val ROOM_FLOOR_ITEM_UPDATE = 1280 // ObjectUpdateMessageComposer
    const val INVENTORY_ITEMS = 2042 // FurniListMessageComposer
    const val USER_WARDROBES = 1352 // WardrobeMessageComposer
    const val ROOM_ITEM_ADDED = 671 // ObjectAddMessageComposer
    const val INVENTORY_REMOVE_OBJECT = 1171 // FurniListRemoveMessageComposer
    const val NAVIGATOR_CREATE_ROOM = 2413 // FlatCreatedMessageComposer
}