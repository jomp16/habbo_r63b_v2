-- =========================================================================
-- V22: Mapeamento de pacotes para releases R63 (20111005, 20111213, 20120402, 20120704, 20130522)
-- =========================================================================

-- 1) RELEASE63-201110051250-692472661
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `header` IN (
    2774, 3747, 3500, 2901, 686, 1079, 1923, 651, 2587, 442, 2689, 1433, 681, 2130, 1274, 1249, 1973, 1676, 648, 1113, 3050, 2601, 1021,
    274, 1393, 2468, 3052, 74, 2324, 2723, 1385, 3633, 2942, 3003, 2301, 2435, 2947, 756, 1144, 58, 257, 2595, 310,
    3708, 2747, 1204, 1257, 2687, 825, 3776, 2528, 3771, 3800, 2928, 151, 3393, 2244, 3362, 3070, 2250, 1528, 1600, 2304
);
UPDATE `releases_outgoing_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `header` IN (
    1781, 1157, 473, 3445, 3701, 2079, 3848, 3979,
    1575, 1419, 2029, 350, 1852, 2628, 1140, 3964, 3887, 2068
);

UPDATE `releases_incoming_headers` SET `header` = 2774, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_PLACE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3747, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TAKE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3500, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_MOVE_WALL_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 2901, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_MOVE_FLOOR_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 686, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';
UPDATE `releases_incoming_headers` SET `header` = 1079, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 1923, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_SAVE_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 651, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_REMOVE_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 2587, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_DIMMER_INFO';
UPDATE `releases_incoming_headers` SET `header` = 442, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_DIMMER_SWITCH';
UPDATE `releases_incoming_headers` SET `header` = 2689, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_DIMMER_UPDATE';
UPDATE `releases_incoming_headers` SET `header` = 1433, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_ITEM_OPEN_GIFT';
UPDATE `releases_incoming_headers` SET `header` = 681, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ACHIEVEMENT_LIST';
UPDATE `releases_incoming_headers` SET `header` = 2130, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'AVATAR_EFFECT';
-- CATALOG_PURCHASE: o header anterior (1274/350/3189/2591) era o GetOffers do Marketplace (estatico 3018,
-- md5 b0f80f.../00f645..., chamado com (-1,-1,query,-1)); o real e o purchaseFromCatalog do HabboCatalog.
UPDATE `releases_incoming_headers` SET `header` = 742, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_PURCHASE';
UPDATE `releases_incoming_headers` SET `header` = 1249, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_PURCHASE_GIFT';
UPDATE `releases_incoming_headers` SET `header` = 1973, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_GIFT_WRAPPING';
UPDATE `releases_incoming_headers` SET `header` = 1676, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_RECYCLER_STATUS';
UPDATE `releases_incoming_headers` SET `header` = 648, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_RECYCLER_REWARDS';
UPDATE `releases_incoming_headers` SET `header` = 1113, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MESSENGER_FOLLOW_FRIEND';
UPDATE `releases_incoming_headers` SET `header` = 3050, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'INVENTORY_UNSEEN_ITEMS_TRACKER';
UPDATE `releases_incoming_headers` SET `header` = 2601, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'WIRED_SAVE_TRIGGER';
UPDATE `releases_incoming_headers` SET `header` = 1021, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'WIRED_SAVE_CONDITION';
UPDATE `releases_incoming_headers` SET `header` = 274, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_BAN_USER';
UPDATE `releases_incoming_headers` SET `header` = 1393, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_DELETE';
UPDATE `releases_incoming_headers` SET `header` = 2468, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_GIVE_RIGHTS';
UPDATE `releases_incoming_headers` SET `header` = 3052, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_MUTE_USER';
UPDATE `releases_incoming_headers` SET `header` = 74, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_PLACE_PET';
UPDATE `releases_incoming_headers` SET `header` = 2324, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_REEDEM_EXCHANGE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 2723, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TAKE_PET';
UPDATE `releases_incoming_headers` SET `header` = 1385, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TRIGGER_CLOSE_DICE';
UPDATE `releases_incoming_headers` SET `header` = 3633, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TRIGGER_HABBO_WHEEL';
UPDATE `releases_incoming_headers` SET `header` = 2942, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TRIGGER_ONE_WAY_GATE';
UPDATE `releases_incoming_headers` SET `header` = 3003, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TRIGGER_ROLL_DICE';
UPDATE `releases_incoming_headers` SET `header` = 2301, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_ACTION';
UPDATE `releases_incoming_headers` SET `header` = 2435, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_CONFIRM_DECLINE';
UPDATE `releases_incoming_headers` SET `header` = 2947, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_OPEN';
UPDATE `releases_incoming_headers` SET `header` = 756, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_REMOVE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 1144, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_UNACCEPT';
UPDATE `releases_incoming_headers` SET `header` = 58, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_CHANGE_MOTTO';
UPDATE `releases_incoming_headers` SET `header` = 257, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_DANCE';
UPDATE `releases_incoming_headers` SET `header` = 2595, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_PROFILE';
UPDATE `releases_incoming_headers` SET `header` = 310, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_ACCEPT';

UPDATE `releases_incoming_headers` SET `header` = 3708, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_MARKETPLACE_CONFIGURATION';
UPDATE `releases_incoming_headers` SET `header` = 2747, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'HABBO_CLUB_GIFTS';
UPDATE `releases_incoming_headers` SET `header` = 1204, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MODERATION_CALL_FOR_HELP';
UPDATE `releases_incoming_headers` SET `header` = 1257, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MODERATION_BAN_USER';
UPDATE `releases_incoming_headers` SET `header` = 2687, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MODERATION_KICK_USER';
UPDATE `releases_incoming_headers` SET `header` = 825, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MODERATION_ROOM_CHATLOG';
UPDATE `releases_incoming_headers` SET `header` = 3776, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MODERATION_TICKET_CHATLOG';
UPDATE `releases_incoming_headers` SET `header` = 2528, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'NAVIGATOR_ADD_FAVORITE_ROOM';
UPDATE `releases_incoming_headers` SET `header` = 3771, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'NAVIGATOR_HOME_ROOM';
UPDATE `releases_incoming_headers` SET `header` = 3800, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'NAVIGATOR_REMOVE_FAVORITE_ROOM';
UPDATE `releases_incoming_headers` SET `header` = 2928, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'PET_INFO';
UPDATE `releases_incoming_headers` SET `header` = 151, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'PET_RESPECT';
UPDATE `releases_incoming_headers` SET `header` = 3393, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'REFRESH_CAMPAIGN';
UPDATE `releases_incoming_headers` SET `header` = 2244, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_BANNED_USERS';
UPDATE `releases_incoming_headers` SET `header` = 3362, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_IGNORE_USER';
UPDATE `releases_incoming_headers` SET `header` = 3070, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_LOAD_BY_DOORBELL';
UPDATE `releases_incoming_headers` SET `header` = 2250, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_MANNEQUIN_CHANGE_NAME';
UPDATE `releases_incoming_headers` SET `header` = 1528, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'SETTINGS_SAVE_VOLUME';
UPDATE `releases_incoming_headers` SET `header` = 1600, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_SETTINGS';
UPDATE `releases_incoming_headers` SET `header` = 2304, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_HABBO_CLUB_PAGE';

-- Pacotes inexistentes na era de outubro/2011 (DISABLED)
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' 
WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` IN (
    'ROOM_USER_DROP_HANDITEM', 'ROOM_USER_GIVE_HANDITEM', 'ROOM_USER_SIT',
    'CATALOG_PURCHASE_RENTAL_DIALOG', 'CATALOG_OFFER', 'CATALOG_OFFER_CONFIGURATION',
    'GAME_CENTER_ACHIEVEMENTS', 'GAME_CENTER_CAN_PLAY_GAME', 'GAME_CENTER_JOIN_GAME', 'GAME_CENTER_LUCKY_LOSERS', 'GAME_CENTER_PRIZE', 'GAME_CENTER_QUIT_GAME', 'GAME_LISTING', 'INITIALIZE_GAME_CENTER',
    'GROUP_ACCEPT_MEMBERSHIP', 'GROUP_ADD_ADMIN', 'GROUP_DECLINE_MEMBERSHIP', 'GROUP_FAVORITE', 'GROUP_FURNISHING_SETTINGS', 'GROUP_FURNISHINGS', 'GROUP_MANAGE', 'GROUP_MEMBERS', 'GROUP_PURCHASE_PAGE', 'GROUP_REMOVE_ADMIN', 'GROUP_REMOVE_FAVORITE', 'GROUP_REQUEST_JOIN', 'GROUP_UPDATE_COLORS', 'GROUP_UPDATE_SETTINGS',
    'GUIDE_TOOL_OPEN', 'INVENTORY_BOTS',
    'LANDING_IS_BADGE_REQUEST_FULFILLED', 'LANDING_PROMO_ARTICLES', 'LANDING_REQUEST_BADGE', 'LANDING_REWARD', 'LANDING_SECONDS_UNTIL',
    'MESSENGER_SET_RELATIONSHIP', 'MISC_GET_MOTD', 'MISC_UNSEEN_ITEMS_TRACKER',
    'MODERATION_MUTE_USER', 'MODERATION_WARN_USER', 'MODERATION_TOUR_REQUEST', 'MODERATION_USER_CHATLOG',
    'NAVIGATOR_GO_TO_BY_NAME', 'PET_COMMANDS', 'PET_SELECTED', 'ROOM_COMPETITION_INIT', 'ROOM_ITEM_USE_CLOTHING', 'ROOM_MANNEQUIN_CHANGE_FIGURE', 'ROOM_MUTE_ALL', 'ROOM_PLACE_BOT', 'ROOM_TAKE_BOT', 'ROOM_UNBAN_USER',
    'SANCTION_STATUS', 'SETTINGS_SAVE_CHAT', 'SETTINGS_SAVE_INVITES', 'SETTINGS_SAVE_FOCUS', 'TALENT_STATUS', 'TALENT_TRACK_OPEN', 'USER_RELATIONSHIPS'
);

UPDATE `releases_outgoing_headers` SET `header` = 1781, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_PROFILE';
UPDATE `releases_outgoing_headers` SET `header` = 1157, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_ACCEPT';
UPDATE `releases_outgoing_headers` SET `header` = 473, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_CONFIRMATION';
UPDATE `releases_outgoing_headers` SET `header` = 3445, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_COMPLETED';
UPDATE `releases_outgoing_headers` SET `header` = 3701, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_CLOSE';
-- Trading 201110 pelos logs do dispatcher do TradingModel: _-al(3666) monta os mapas de itens (ItemList),
-- 2079 loga "TRADING::TradingNotOpenEvent", 3848 = others_account_disabled, 749 = own_account_disabled.
UPDATE `releases_outgoing_headers` SET `header` = 3666, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_ITEM_LIST';
UPDATE `releases_outgoing_headers` SET `header` = 2079, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_NOT_OPEN';
UPDATE `releases_outgoing_headers` SET `header` = 3979, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_OPEN_FAILED';
UPDATE `releases_outgoing_headers` SET `header` = 1575, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_WALL_ITEM_UPDATE';
UPDATE `releases_outgoing_headers` SET `header` = 1419, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_WALL_ITEM_REMOVE';
UPDATE `releases_outgoing_headers` SET `header` = 2029, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_REMOVE';
UPDATE `releases_outgoing_headers` SET `header` = 350, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_DANCE';
UPDATE `releases_outgoing_headers` SET `header` = 1852, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_HANDITEM';
UPDATE `releases_outgoing_headers` SET `header` = 2628, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_EFFECT';
UPDATE `releases_outgoing_headers` SET `header` = 1140, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_ACTION';
UPDATE `releases_outgoing_headers` SET `header` = 3964, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'INVENTORY_REMOVE_OBJECT';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_UPDATE_FURNI_STACK';
UPDATE `releases_outgoing_headers` SET `header` = 2068, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_ITEM_OPEN_GIFT_RESULT';

-- 2) RELEASE63-201112130905-713192101
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `header` IN (
    2161, 1129, 2526, 3879, 3058, 1445, 1045, 3848, 1079, 2024, 3061, 162, 1495, 895, 1311, 3836, 2610, 882, 1712
);
UPDATE `releases_outgoing_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `header` IN (881);

UPDATE `releases_incoming_headers` SET `header` = 2161, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_PLACE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 1129, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_TAKE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 2526, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_MOVE_WALL_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3879, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_MOVE_FLOOR_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3058, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';
UPDATE `releases_incoming_headers` SET `header` = 1445, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 1045, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_SAVE_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 3848, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_REMOVE_POST_IT';
UPDATE `releases_outgoing_headers` SET `header` = 881, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 1079, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_DIMMER_INFO';
UPDATE `releases_incoming_headers` SET `header` = 2024, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_DIMMER_SWITCH';
UPDATE `releases_incoming_headers` SET `header` = 3061, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_DIMMER_UPDATE';
UPDATE `releases_incoming_headers` SET `header` = 162, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'MESSENGER_FOLLOW_FRIEND';
UPDATE `releases_incoming_headers` SET `header` = 1495, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'INVENTORY_UNSEEN_ITEMS_TRACKER';
UPDATE `releases_incoming_headers` SET `header` = 895, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_BAN_USER';
UPDATE `releases_incoming_headers` SET `header` = 1311, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_MUTE_USER';
UPDATE `releases_incoming_headers` SET `header` = 3836, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_PLACE_PET';
UPDATE `releases_incoming_headers` SET `header` = 2610, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_OPEN';
UPDATE `releases_incoming_headers` SET `header` = 882, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_UNACCEPT';
UPDATE `releases_incoming_headers` SET `header` = 1712, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'PET_INFO';
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'SETTINGS_SAVE_FOCUS';

UPDATE `releases_incoming_headers` SET `header` = 1040, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'CATALOG_PURCHASE';

-- 3) RELEASE63-201204021456-654375178
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `header` IN (
    2484, 1580, 1592, 2486, 2801, 2650, 226, 3697, 1483, 636, 729, 2767, 1166, 741, 3672, 3848, 3111, 3950
);

UPDATE `releases_incoming_headers` SET `header` = 2484, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_PLACE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 1580, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_TAKE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 1592, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_MOVE_WALL_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 2486, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_MOVE_FLOOR_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 2801, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';
UPDATE `releases_incoming_headers` SET `header` = 2650, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 226, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_SAVE_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 3697, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_REMOVE_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 1483, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_DIMMER_INFO';
UPDATE `releases_incoming_headers` SET `header` = 636, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_DIMMER_SWITCH';
UPDATE `releases_incoming_headers` SET `header` = 729, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_DIMMER_UPDATE';
UPDATE `releases_incoming_headers` SET `header` = 2767, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'MESSENGER_FOLLOW_FRIEND';
UPDATE `releases_incoming_headers` SET `header` = 1166, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'INVENTORY_UNSEEN_ITEMS_TRACKER';
UPDATE `releases_incoming_headers` SET `header` = 741, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_BAN_USER';
UPDATE `releases_incoming_headers` SET `header` = 3672, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_MUTE_USER';
UPDATE `releases_incoming_headers` SET `header` = 3848, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_PLACE_PET';
UPDATE `releases_incoming_headers` SET `header` = 3111, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'TRADING_OPEN';
UPDATE `releases_incoming_headers` SET `header` = 3950, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'PET_INFO';
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'SETTINGS_SAVE_FOCUS';

UPDATE `releases_incoming_headers` SET `header` = 3163, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'CATALOG_PURCHASE';

-- 4) RELEASE63-201207040943-767851649
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `header` IN (
    1871, 720, 30, 3897, 2030, 1874, 315, 2223, 1264, 2599, 3575, 2468, 3436, 2144, 2591, 2578, 2504, 305, 682, 632, 3854,
    1530, 2370, 1305, 1682, 1221, 3689, 2703, 867, 338, 208, 763, 3631, 858, 2264, 2899, 3271, 1783, 610, 62, 2134,
    2252, 1749, 1163, 2165, 3144, 2770, 2103, 1128, 405, 50, 1704, 424
);
UPDATE `releases_outgoing_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `header` IN (
    105, 3761, 2016, 3734, 661, 3321, 1715, 1765, 3771, 2358
);

UPDATE `releases_incoming_headers` SET `header` = 1871, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_PLACE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 720, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TAKE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 30, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_MOVE_WALL_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3897, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_MOVE_FLOOR_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 2030, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';
UPDATE `releases_incoming_headers` SET `header` = 1874, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 315, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_SAVE_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 2223, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_REMOVE_POST_IT';
UPDATE `releases_incoming_headers` SET `header` = 1264, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_DIMMER_INFO';
UPDATE `releases_incoming_headers` SET `header` = 2599, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_DIMMER_SWITCH';
UPDATE `releases_incoming_headers` SET `header` = 3575, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_DIMMER_UPDATE';
UPDATE `releases_incoming_headers` SET `header` = 2468, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_ITEM_OPEN_GIFT';
UPDATE `releases_incoming_headers` SET `header` = 3436, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ACHIEVEMENT_LIST';
UPDATE `releases_incoming_headers` SET `header` = 2144, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'AVATAR_EFFECT';
UPDATE `releases_incoming_headers` SET `header` = 979, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_PURCHASE';
UPDATE `releases_incoming_headers` SET `header` = 2578, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_GIFT_WRAPPING';
UPDATE `releases_incoming_headers` SET `header` = 2504, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_RECYCLER_STATUS';
UPDATE `releases_incoming_headers` SET `header` = 305, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MESSENGER_FOLLOW_FRIEND';
UPDATE `releases_incoming_headers` SET `header` = 682, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'INVENTORY_UNSEEN_ITEMS_TRACKER';
UPDATE `releases_incoming_headers` SET `header` = 632, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'WIRED_SAVE_TRIGGER';
UPDATE `releases_incoming_headers` SET `header` = 3854, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'WIRED_SAVE_CONDITION';
UPDATE `releases_incoming_headers` SET `header` = 1530, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_BAN_USER';
UPDATE `releases_incoming_headers` SET `header` = 2370, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_DELETE';
UPDATE `releases_incoming_headers` SET `header` = 1305, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_GIVE_RIGHTS';
UPDATE `releases_incoming_headers` SET `header` = 1682, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_MUTE_USER';
UPDATE `releases_incoming_headers` SET `header` = 1221, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_PLACE_PET';
UPDATE `releases_incoming_headers` SET `header` = 3689, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_REEDEM_EXCHANGE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 2703, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TAKE_PET';
UPDATE `releases_incoming_headers` SET `header` = 867, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TRIGGER_CLOSE_DICE';
UPDATE `releases_incoming_headers` SET `header` = 338, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TRIGGER_HABBO_WHEEL';
UPDATE `releases_incoming_headers` SET `header` = 208, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TRIGGER_ONE_WAY_GATE';
UPDATE `releases_incoming_headers` SET `header` = 763, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TRIGGER_ROLL_DICE';
UPDATE `releases_incoming_headers` SET `header` = 3631, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_ACTION';
UPDATE `releases_incoming_headers` SET `header` = 858, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_DROP_HANDITEM';
UPDATE `releases_incoming_headers` SET `header` = 2264, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_GIVE_HANDITEM';
UPDATE `releases_incoming_headers` SET `header` = 2899, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_SIT';
UPDATE `releases_incoming_headers` SET `header` = 3271, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_CONFIRM_DECLINE';
UPDATE `releases_incoming_headers` SET `header` = 1783, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_OPEN';
UPDATE `releases_incoming_headers` SET `header` = 610, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_REMOVE_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 62, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_UNACCEPT';
UPDATE `releases_incoming_headers` SET `header` = 2134, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'USER_CHANGE_MOTTO';
UPDATE `releases_incoming_headers` SET `header` = 2252, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'USER_BADGES';
UPDATE `releases_incoming_headers` SET `header` = 1749, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_ACCEPT';

UPDATE `releases_incoming_headers` SET `header` = 1163, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_MARKETPLACE_CONFIGURATION';
UPDATE `releases_incoming_headers` SET `header` = 2165, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_CALL_FOR_HELP';
UPDATE `releases_incoming_headers` SET `header` = 3144, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_ROOM_CHATLOG';
UPDATE `releases_incoming_headers` SET `header` = 2770, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'PET_INFO';
UPDATE `releases_incoming_headers` SET `header` = 2103, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'PET_RESPECT';
UPDATE `releases_incoming_headers` SET `header` = 1128, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_BANNED_USERS';
UPDATE `releases_incoming_headers` SET `header` = 405, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_IGNORE_USER';
UPDATE `releases_incoming_headers` SET `header` = 50, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_LOAD_BY_DOORBELL';
UPDATE `releases_incoming_headers` SET `header` = 1704, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'SETTINGS_SAVE_VOLUME';
UPDATE `releases_incoming_headers` SET `header` = 424, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_HABBO_CLUB_PAGE';

-- Pacotes inexistentes na era de julho/2012 (DISABLED)
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' 
WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` IN (
    'CATALOG_PURCHASE_RENTAL_DIALOG',
    'GAME_CENTER_ACHIEVEMENTS', 'GAME_CENTER_CAN_PLAY_GAME', 'GAME_CENTER_JOIN_GAME', 'GAME_CENTER_LUCKY_LOSERS', 'GAME_CENTER_QUIT_GAME', 'GAME_LISTING', 'INITIALIZE_GAME_CENTER',
    'GUIDE_TOOL_OPEN',
    'LANDING_IS_BADGE_REQUEST_FULFILLED', 'LANDING_PROMO_ARTICLES',
    'MESSENGER_SET_RELATIONSHIP', 'MISC_UNSEEN_ITEMS_TRACKER', 'MODERATION_USER_CHATLOG',
    'NAVIGATOR_GO_TO_BY_NAME', 'PET_COMMANDS', 'ROOM_COMPETITION_INIT', 'ROOM_ITEM_USE_CLOTHING', 'ROOM_MUTE_ALL', 'ROOM_PLACE_BOT', 'ROOM_TAKE_BOT', 'ROOM_UNBAN_USER',
    'SANCTION_STATUS', 'SETTINGS_SAVE_FOCUS', 'TALENT_STATUS', 'TALENT_TRACK_OPEN', 'USER_RELATIONSHIPS'
);

UPDATE `releases_outgoing_headers` SET `header` = 105, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'USER_PROFILE';
UPDATE `releases_outgoing_headers` SET `header` = 3761, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_ACCEPT';
UPDATE `releases_outgoing_headers` SET `header` = 2016, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_CONFIRMATION';
UPDATE `releases_outgoing_headers` SET `header` = 3734, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_COMPLETED';
UPDATE `releases_outgoing_headers` SET `header` = 661, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_CLOSE';
UPDATE `releases_outgoing_headers` SET `header` = 3321, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_ITEM_LIST';
UPDATE `releases_outgoing_headers` SET `header` = 1715, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_NOT_OPEN';
UPDATE `releases_outgoing_headers` SET `header` = 1765, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_OPEN_FAILED';
UPDATE `releases_outgoing_headers` SET `header` = 3771, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'INVENTORY_REMOVE_OBJECT';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_UPDATE_FURNI_STACK';

-- 5) RELEASE63-201305221112-286302280
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `header` IN (2177, 241);
UPDATE `releases_incoming_headers` SET `header` = 2177, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'TRADING_OPEN';
UPDATE `releases_incoming_headers` SET `header` = 241, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'PET_INFO';
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'SETTINGS_SAVE_FOCUS';

-- =========================================================================
-- 6) Correcoes auditadas (NULLs mapeados + erros comprovados nas vizinhas)
-- Cada item tem evidencia no AS3 do proprio dump (registro/handler/literais de UI/continuidade md5).
-- Pacote ausente na release (feature nasceu depois, provado por literal/md5): header NULL + DISABLED.
-- =========================================================================

-- 6a) releases_outgoing_headers: NULLs de 20111005 e 20120704
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_LIMITED_SOLD_OUT';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_PURCHASE_RENTAL_DIALOG';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GAME_CENTER_ACHIEVEMENTS';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GAME_CENTER_CAN_PLAY_GAME';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GAME_CENTER_FEATURED_LUCKY_LOSER_OF_THE_WEEK';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GAME_CENTER_LIST';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GAME_CENTER_LOAD_GAME';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GAME_CENTER_PRIZE';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'LANDING_IS_BADGE_REQUEST_FULFILLED';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'LANDING_PROMO_ARTICLES';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'LANDING_REWARD';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'LANDING_SECONDS_UNTIL';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_BOT_ERROR_NOTIFICATION';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_COMPETITION';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_COMPETITION_DATA';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_USER_UNBANNED';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TALENT_STATUS';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TALENT_TRACK';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_PERKS';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_RELATIONSHIPS';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'WIRED_REWARD_NOTIFICATION';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'USER_RELATIONSHIPS';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_PURCHASE_RENTAL_DIALOG';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_CENTER_LIST';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'LANDING_PROMO_ARTICLES';
UPDATE `releases_outgoing_headers` SET `header` = 3554, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_PURCHASE_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 2113, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MESSENGER_CHAT_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 500, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MODERATION_ROOM_INFO';
UPDATE `releases_outgoing_headers` SET `header` = 2840, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'PET_ADDED_TO_INVENTORY';
UPDATE `releases_outgoing_headers` SET `header` = 2017, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'PET_INFO';
UPDATE `releases_outgoing_headers` SET `header` = 3534, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GROUP_PURCHASE_PAGE';
UPDATE `releases_outgoing_headers` SET `header` = 2837, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MISC_BROADCAST_NOTIFICATION';
UPDATE `releases_outgoing_headers` SET `header` = 1664, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MISC_GENERIC_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 190, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_DOORBELL_ACCEPT';
UPDATE `releases_outgoing_headers` SET `header` = 1539, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_PET_ERROR_NOTIFICATION';
UPDATE `releases_outgoing_headers` SET `header` = 3848, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers` SET `header` = 749, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';
UPDATE `releases_outgoing_headers` SET `header` = 1884, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_SETTINGS';
UPDATE `releases_outgoing_headers` SET `header` = 3119, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'WIRED_CONDITION_DIALOG';
UPDATE `releases_outgoing_headers` SET `header` = 2574, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'WIRED_EFFECT_DIALOG';
-- WIRED_SAVED (corpo vazio b761f097, handler fecha a UI) nasce em 201112151103; c72ca16d (3756/2479) e o OpenEvent (estatico 653, le int).
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'WIRED_SAVED';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'WIRED_SAVED';
UPDATE `releases_outgoing_headers` SET `header` = 3856, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'WIRED_TRIGGER_DIALOG';
UPDATE `releases_outgoing_headers` SET `header` = 2039, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MESSENGER_CHAT_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 1777, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_DOORBELL_ACCEPT';
UPDATE `releases_outgoing_headers` SET `header` = 3000, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_ACTION';
UPDATE `releases_outgoing_headers` SET `header` = 2387, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_DANCE';
UPDATE `releases_outgoing_headers` SET `header` = 1096, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_EFFECT';
UPDATE `releases_outgoing_headers` SET `header` = 1763, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_USER_HANDITEM';
UPDATE `releases_outgoing_headers` SET `header` = 2550, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'WIRED_CONDITION_DIALOG';
UPDATE `releases_outgoing_headers` SET `header` = 181, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'WIRED_TRIGGER_DIALOG';
UPDATE `releases_outgoing_headers` SET `header` = 2133, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'USER_SETTINGS';
UPDATE `releases_outgoing_headers` SET `header` = 2613, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MISC_GENERIC_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 3183, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_PURCHASE_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 1602, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_FRIENDS_LEADERBOARD';
UPDATE `releases_outgoing_headers` SET `header` = 3597, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_GAME_CREATED';
UPDATE `releases_outgoing_headers` SET `header` = 1485, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_IN_ARENA_QUEUE';
UPDATE `releases_outgoing_headers` SET `header` = 3219, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_STAGE_LOAD';
UPDATE `releases_outgoing_headers` SET `header` = 943, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_STOP_COUNTER';
UPDATE `releases_outgoing_headers` SET `header` = 3766, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_TOTAL_LEADERBOARD';
UPDATE `releases_outgoing_headers` SET `header` = 1259, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_USER_BLOCKED';
UPDATE `releases_outgoing_headers` SET `header` = 1967, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_INFO';
UPDATE `releases_outgoing_headers` SET `header` = 711, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_MANAGE';
UPDATE `releases_outgoing_headers` SET `header` = 1925, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_PURCHASE_PAGE';

-- 6b) releases_outgoing_headers: trading e Game2 de 20111213 / 20120402
-- Correcoes RELEASE63-201112130905-713192101: Game2 pelos nomes reais de classe no Messages.txt; trading pelos logs do dispatcher do TradingModel
-- (TRADING::*Event, others/own_account_disabled), ItemList/Open pelos handlers (mapas de itens / "Trading open - inventory is null!").
UPDATE `releases_outgoing_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `header` IN (911, 879, 1355);
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'PET_REMOVED_FROM_INVENTORY';  -- 1355 e TradingCloseEvent; header real nao provado
UPDATE `releases_outgoing_headers` SET `header` = 1198, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GAME_2_IN_ARENA_QUEUE';
UPDATE `releases_outgoing_headers` SET `header` = 911, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GAME_2_USER_BLOCKED';
UPDATE `releases_outgoing_headers` SET `header` = 17, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GAME_2_GAME_CANCELLED';
UPDATE `releases_outgoing_headers` SET `header` = 179, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GAME_2_USER_LEFT_GAME';
UPDATE `releases_outgoing_headers` SET `header` = 1389, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GAME_2_PLAYER_REMATCHES';
UPDATE `releases_outgoing_headers` SET `header` = 2497, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GAME_2_ENTER_ARENA_FAILED';
UPDATE `releases_outgoing_headers` SET `header` = 3220, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GAME_2_STOP_COUNTER';
UPDATE `releases_outgoing_headers` SET `header` = 3319, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_OPEN';
UPDATE `releases_outgoing_headers` SET `header` = 1045, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_ITEM_LIST';
UPDATE `releases_outgoing_headers` SET `header` = 3885, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_ACCEPT';
UPDATE `releases_outgoing_headers` SET `header` = 3564, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_CONFIRMATION';
UPDATE `releases_outgoing_headers` SET `header` = 1032, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_COMPLETED';
UPDATE `releases_outgoing_headers` SET `header` = 1355, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_CLOSE';
UPDATE `releases_outgoing_headers` SET `header` = 2872, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_NOT_OPEN';
UPDATE `releases_outgoing_headers` SET `header` = 2873, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_OPEN_FAILED';
UPDATE `releases_outgoing_headers` SET `header` = 879, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers` SET `header` = 1709, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';
-- Correcoes RELEASE63-201204021456-654375178: idem.
UPDATE `releases_outgoing_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `header` IN (1439, 2258, 3995, 1786);
UPDATE `releases_outgoing_headers` SET `header` = 1439, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_FRIENDS_LEADERBOARD';
UPDATE `releases_outgoing_headers` SET `header` = 2258, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_TOTAL_LEADERBOARD';
UPDATE `releases_outgoing_headers` SET `header` = 3250, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_IN_ARENA_QUEUE';
UPDATE `releases_outgoing_headers` SET `header` = 3995, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_USER_BLOCKED';
UPDATE `releases_outgoing_headers` SET `header` = 456, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_PLAYER_REMATCHES';
UPDATE `releases_outgoing_headers` SET `header` = 3256, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_GAME_CANCELLED';
UPDATE `releases_outgoing_headers` SET `header` = 3456, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_USER_LEFT_GAME';
UPDATE `releases_outgoing_headers` SET `header` = 3599, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_ENTER_ARENA_FAILED';
UPDATE `releases_outgoing_headers` SET `header` = 1838, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GAME_2_STOP_COUNTER';
UPDATE `releases_outgoing_headers` SET `header` = 1878, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'TRADING_ITEM_LIST';
UPDATE `releases_outgoing_headers` SET `header` = 2102, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'TRADING_CLOSE';
UPDATE `releases_outgoing_headers` SET `header` = 1139, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'TRADING_OPEN_FAILED';
UPDATE `releases_outgoing_headers` SET `header` = 944, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers` SET `header` = 3778, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- 6c) releases_incoming_headers: NULLs de 20120704 + moderacao kick/caution (20120704, 20130522)
-- Moderacao: literais do switch de UI ('Kick...' / 'Sending caution...'): KICK = kick, WARN = caution.
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `header` IN (357);
UPDATE `releases_incoming_headers` SET `header` = 3838, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_WARN_USER';
UPDATE `releases_incoming_headers` SET `header` = 1965, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_OFFER';
UPDATE `releases_incoming_headers` SET `header` = 2710, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_GAME_CHAT';
UPDATE `releases_incoming_headers` SET `header` = 1419, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_GET_FRIENDS_LEADERBOARD';
UPDATE `releases_incoming_headers` SET `header` = 3620, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_GET_TOTAL_LEADERBOARD';
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_GET_WEEKLY_COMPETITIVE_LEADERBOARD';
UPDATE `releases_incoming_headers` SET `header` = 3551, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GAME_2_GET_WEEKLY_LEADERBOARD';
UPDATE `releases_incoming_headers` SET `header` = 3447, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_ACCEPT_MEMBERSHIP';
UPDATE `releases_incoming_headers` SET `header` = 3957, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_FAVORITE';
UPDATE `releases_incoming_headers` SET `header` = 12, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_FURNISHING_SETTINGS';
UPDATE `releases_incoming_headers` SET `header` = 2974, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_REMOVE_FAVORITE';
UPDATE `releases_incoming_headers` SET `header` = 2448, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_REQUEST_JOIN';
UPDATE `releases_incoming_headers` SET `header` = 75, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_BAN_USER';
UPDATE `releases_incoming_headers` SET `header` = 357, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_KICK_USER';
UPDATE `releases_incoming_headers` SET `header` = 3636, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_MUTE_USER';
UPDATE `releases_incoming_headers` SET `header` = 1078, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_TICKET_CHATLOG';
UPDATE `releases_incoming_headers` SET `header` = 1495, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_USER_INFO';
UPDATE `releases_incoming_headers` SET `header` = 2391, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_ITEM_ALIASES';
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `header` IN (410, 2691);
UPDATE `releases_incoming_headers` SET `header` = 410, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'MODERATION_KICK_USER';
UPDATE `releases_incoming_headers` SET `header` = 2691, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'MODERATION_WARN_USER';

-- 6d) -- MODERATION_INIT 20120704: 3242, handler le ModeratorInitData (issues + templates) e mostra a ferramenta de moderacao.
UPDATE `releases_outgoing_headers` SET `header` = 3242, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MODERATION_INIT';
