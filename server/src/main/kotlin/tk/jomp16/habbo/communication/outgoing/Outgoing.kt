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

@Suppress("unused")
object Outgoing {
    const val INIT_CRYPTO = 3578 // InitCryptoMessageComposer
    const val SECRET_KEY = 1236 // SecretKeyMessageComposer
    const val UNIQUE_ID = 2371 // SetUniqueIdMessageComposer
    const val BROADCAST_NOTIFICATION = 591 // BroadcastMessageAlertMessageComposer
    const val MOTD_NOTIFICATION = 1141 // MOTDNotificationMessageComposer
    const val SUPER_NOTIFICATION = 413 // RoomNotificationMessageComposer
    const val AUTHENTICATION_OK = 3536 // AuthenticationOKMessageComposer
    const val HOME_ROOM = 2341 // NavigatorSettingsMessageComposer
    const val NAVIGATOR_FAVORITES = 3505 // FavouritesMessageComposer
    const val USER_RIGHTS = 2466 // UserRightsMessageComposer
    const val AVAILABILITY_STATUS = 919 // AvailabilityStatusMessageComposer
    const val ACHIEVEMENT_SCORE = 2435 // AchievementScoreMessageComposer
    const val BUILDERS_CLUB_MEMBERSHIP = 257 // BuildersClubMembershipMessageComposer
    const val USER_SETTINGS = 654 // SoundSettingsMessageComposer
    const val CAMPAIGN = 3799 // CampaignMessageComposer
    const val SUBSCRIPTION_STATUS = 269 // ScrSendUserInfoMessageComposer
    const val CREDITS_BALANCE = 2203 // CreditBalanceMessageComposer
    const val ACTIVITY_POINTS_BALANCE = 188 // ActivityPointsMessageComposer
    const val USER_OBJECT = 3716 // UserObjectMessageComposer
    const val USER_PERKS = 3893 // UserPerksMessageComposer
    const val NAVIGATOR_METADATA = 1713 // NavigatorMetaDataParserMessageComposer
    const val NAVIGATOR_LIFTED_ROOMS = 1063 // NavigatorLiftedRoomsMessageComposer
    const val NAVIGATOR_PREFERENCES = 2445 // NavigatorPreferencesMessageComposer
    const val NAVIGATOR_COLLAPSED_CATEGORIES = 2333 // NavigatorCollapsedCategoriesMessageComposer
    const val MESSENGER_INIT = 944 // MessengerInitMessageComposer
    const val MESSENGER_FRIENDS = 3265 // BuddyListMessageComposer
    const val MESSENGER_CHAT = 731 // NewConsoleMessageMessageComposer
    const val MESSENGER_FRIEND_UPDATE = 3155 // FriendListUpdateMessageComposer
    const val MESSENGER_REQUESTS = 1666 // BuddyRequestsMessageComposer
    const val AVATAR_EFFECTS = 628 // AvatarEffectsMessageComposer
    const val FIGURE_SETS = 3778 // FigureSetIdsMessageComposer
    const val ROOM_USERS_STATUSES = 1083 // UserUpdateMessageComposer
    const val NAVIGATOR_FLAT_CATEGORIES = 943 // UserFlatCatsMessageComposer
    const val NAVIGATOR_PROMO_CATEGORIES = 462 // NavigatorFlatCatsMessageComposer
    const val NAVIGATOR_SEARCH = 2149 // NavigatorSearchResultSetMessageComposer
    const val ROOM_INFO = 2940 // GetGuestRoomResultMessageComposer
    const val GENERIC_ERROR = 1530 // GenericErrorMessageComposer
    const val ROOM_EXIT = 2299 // CloseConnectionMessageComposer
    const val ROOM_USER_REMOVE = 2477 // UserRemoveMessageComposer
    const val ROOM_ERROR = 2083 // CantConnectMessageComposer
    const val ROOM_OPEN = 3388 // OpenConnectionMessageComposer
    const val ROOM_INITIAL_INFO = 3291 // RoomReadyMessageComposer
    const val ROOM_DECORATION = 1603 // RoomPropertyMessageComposer
    const val ROOM_ITEM_ALIASES = 839 // FurnitureAliasesMessageComposer
    const val ROOM_HEIGHTMAP = 2222 // HeightMapMessageComposer
    const val ROOM_FLOORMAP = 1833 // FloorHeightMapMessageComposer
    const val ROOM_USERS = 3315 // UsersMessageComposer
    const val ROOM_VISUALIZATION_THICKNESS = 1509 // RoomVisualizationSettingsMessageComposer
    const val USER_UPDATE = 1149 // UserChangeMessageComposer
    const val ROOM_OWNER = 2394 // YouAreOwnerMessageComposer
    const val ROOM_RIGHT_LEVEL = 757 // YouAreControllerMessageComposer
    const val ROOM_NO_RIGHTS = 3406 // YouAreNotControllerMessageComposer
    const val ROOM_OWNERSHIP = 889 // RoomEntryInfoMessageComposer
    const val MESSENGER_FOLLOW_FRIEND_ERROR = 2191 // FollowFriendFailedMessageComposer
    const val ROOM_FORWARD = 3529 // RoomForwardMessageComposer
    const val ROOM_FLOOR_ITEMS = 945 // ObjectsMessageComposer
    const val ROOM_WALL_ITEMS = 747 // ItemsMessageComposer
    const val LANDING_PROMO_ARTICLES = 1484 // PromoArticlesMessageComposer
    const val ROOM_DOORBELL = 384 // DoorbellMessageComposer
    const val ROOM_DOORBELL_DENIED = 256 // FlatAccessDeniedMessageComposer
    const val ROOM_DOORBELL_ACCEPT = 931 // FlatAccessibleMessageComposer
    const val INVENTORY_BADGES = 3608 // BadgesMessageComposer
    const val INVENTORY_NEW_OBJECTS = 3353 // FurniListNotificationMessageComposer
    const val USER_BADGES = 682 // HabboUserBadgesMessageComposer
    const val USER_TAGS = 1883 // UserTagsMessageComposer
    const val MESSENGER_SEARCH_FRIENDS = 1095 // HabboSearchResultMessageComposer
    const val MESSENGER_REQUEST_FRIEND = 3034 // NewBuddyRequestMessageComposer
    const val CATALOG_INDEX = 1834 // CatalogIndexMessageComposer
    const val CATALOG_PAGE = 2554 // CatalogPageMessageComposer
    const val CATALOG_CONFIGURATION = 2018 // MarketplaceConfigurationMessageComposer
    const val CATALOG_GIFT_WRAPPING = 2504 // GiftWrappingConfigurationMessageComposer
    const val CATALOG_RECYCLER_REWARDS = 46 // RecyclerRewardsMessageComposer
    const val CATALOG_OFFER_CONFIGURATION = 2551 // CatalogItemDiscountMessageComposer
    const val ROOM_WALL_ITEM_UPDATE = 3949 // ItemUpdateMessageComposer
    const val ROOM_FLOOR_ITEM_UPDATE = 1001 // ObjectUpdateMessageComposer
    const val INVENTORY_ITEMS = 1793 // FurniListMessageComposer
    const val USER_WARDROBES = 980 // WardrobeMessageComposer
    const val ROOM_ITEM_ADDED = 1216 // ObjectAddMessageComposer
    const val ROOM_WALL_ITEM_ADDED = 584 // ItemAddMessageComposer
    const val INVENTORY_REMOVE_OBJECT = 489 // FurniListRemoveMessageComposer
    const val NAVIGATOR_CREATE_ROOM = 673 // FlatCreatedMessageComposer
    const val ROOM_USER_IDLE = 742 // SleepMessageComposer
    const val ROOM_USER_CHAT = 619 // ChatMessageComposer
    const val ROOM_USER_SHOUT = 1047 // ShoutMessageComposer
    const val ROOM_USER_ACTION = 1333 // ActionMessageComposer
    const val CATALOG_HABBO_CLUB_PAGE = 1128 // HabboClubOffersMessageComposer
    const val ROOM_USER_TYPING = 3532 // UserTypingMessageComposer
    const val ROOM_USER_DANCE = 3080 // DanceMessageComposer
    const val CATALOG_PURCHASE_ERROR = 451 // PurchaseErrorMessageComposer
    const val INVENTORY_UPDATE = 3514 // FurniListUpdateMessageComposer
    const val CATALOG_PURCHASE_OK = 3576 // PurchaseOKMessageComposer
    const val CATALOG_VOUCHER_REDEEMED = 1 // VoucherRedeemOkMessageComposer
    const val CATALOG_VOUCHER_REDEEM_ERROR = 2213 // VoucherRedeemErrorMessageComposer
}