-- =========================================================================
-- V22: Mapeamento de pacotes para releases R63 (20111005, 20111213, 20120402, 20120704, 20130522)
-- =========================================================================

-- 1) RELEASE63-201110051250-692472661
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `header` IN (
    2774, 3747, 3500, 2901, 686, 1079, 1923, 651, 2587, 442, 2689, 1433, 681, 2130, 1274, 1249, 1973, 1676, 648, 1113, 3050, 2601, 1021,
    274, 1393, 2468, 3052, 74, 2324, 2723, 1385, 3633, 2942, 3003, 2301, 2435, 2947, 756, 1144, 58, 257, 2595, 310,
    3708, 2747, 1204, 1257, 2687, 825, 3776, 2528, 3771, 3800, 2928, 151, 3393, 2244, 3362, 3070, 2250, 1528, 1600, 2304,
    2841, 2684, 2703
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
-- CATALOG_GIFT_WRAPPING: o pedido da config de embrulho de presentes e `_-0tQ()` = Outgoing[2841] (HabboCatalog.init -> _SafeStr_5370; a resposta
-- 3039 monta a config em `_SafeStr_5407`). Mesmo hash c6b82e1e do header estatico 473 (CATALOG_GIFT_WRAPPING no R63A) ate RELEASE63-201111031102.
-- O 1973 (`_-3bY()`, hash e6485695 = estatico 3019) e um pedido do Marketplace (`_-0pn._SafeStr_1285._SafeStr_5298`, chamado ao montar as janelas marketplace_*).
UPDATE `releases_incoming_headers` SET `header` = 2841, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_GIFT_WRAPPING';
-- (STATUS e REWARDS trocam de header entre si: o bloco de pre-anulacao no topo da secao 1 ja libera 648 e 1676.)
-- CATALOG_RECYCLER_STATUS = 648 (`_-vZ()`, chamado no init() do controlador do reciclador; a resposta 280 vai para `_SafeStr_5487` = status/tempo) e
-- CATALOG_RECYCLER_REWARDS = 1676 (`_-1ZH()`, pedido por `_SafeStr_6380()` quando a lista de premios e null; a resposta 3347 vai para `_SafeStr_5492`).
-- Casa com o pre-shuffle (hash do 648 = estatico 413 RECYCLER_STATUS; hash do 1676 = estatico 412 RECYCLER_REWARDS).
UPDATE `releases_incoming_headers` SET `header` = 648, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_RECYCLER_STATUS';
UPDATE `releases_incoming_headers` SET `header` = 1676, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_RECYCLER_REWARDS';
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
UPDATE `releases_incoming_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'ROOM_BANNED_USERS';
UPDATE `releases_incoming_headers` SET `header` = 3362, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_IGNORE_USER';
UPDATE `releases_incoming_headers` SET `header` = 3070, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_LOAD_BY_DOORBELL';
UPDATE `releases_incoming_headers` SET `header` = 2250, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_MANNEQUIN_CHANGE_NAME';
UPDATE `releases_incoming_headers` SET `header` = 1528, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'SETTINGS_SAVE_VOLUME';
UPDATE `releases_incoming_headers` SET `header` = 1600, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_SETTINGS';
-- CATALOG_HABBO_CLUB_PAGE: o pedido das ofertas do HC em 20111005 e `_-1yq()` = Outgoing[2684], SEM parametros
-- (ClubBuyCatalogWidget -> HabboCatalog._SafeStr_5304; resposta 3681). O 2304 (`_-1vj(int offerId)`) e o `buy_button` de um popup
-- de oferta, nao o pedido de ofertas. Sem parametros ate RELEASE63-201111031102; de 201111071158 em diante e `_-1GA(int windowId)`.
UPDATE `releases_incoming_headers` SET `header` = 2684, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_HABBO_CLUB_PAGE';

-- Pacotes inexistentes na era de outubro/2011 (DISABLED)
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' 
WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` IN (
    'ROOM_USER_DROP_HANDITEM', 'ROOM_USER_GIVE_HANDITEM', 'ROOM_USER_SIT',
    'CATALOG_PURCHASE_RENTAL_DIALOG', 'CATALOG_OFFER', 'CATALOG_OFFER_CONFIGURATION',
    'GAME_CENTER_ACHIEVEMENTS', 'GAME_CENTER_CAN_PLAY_GAME', 'GAME_CENTER_JOIN_GAME', 'GAME_CENTER_LUCKY_LOSERS', 'GAME_CENTER_PRIZE', 'GAME_CENTER_QUIT_GAME', 'GAME_LISTING', 'INITIALIZE_GAME_CENTER',
    'GROUP_ACCEPT_MEMBERSHIP', 'GROUP_ADD_ADMIN', 'GROUP_DECLINE_MEMBERSHIP', 'GROUP_FAVORITE', 'GROUP_FURNISHING_SETTINGS', 'GROUP_FURNISHINGS', 'GROUP_MANAGE', 'GROUP_MEMBERS', 'GROUP_PURCHASE_PAGE', 'GROUP_REMOVE_ADMIN', 'GROUP_REMOVE_FAVORITE', 'GROUP_REQUEST_JOIN', 'GROUP_UPDATE_COLORS', 'GROUP_UPDATE_SETTINGS',
    'GUIDE_TOOL_OPEN', 'INVENTORY_BOTS',
    'LANDING_IS_BADGE_REQUEST_FULFILLED', 'LANDING_PROMO_ARTICLES', 'LANDING_REQUEST_BADGE', 'LANDING_REWARD', 'LANDING_SECONDS_UNTIL',
    'MESSENGER_SET_RELATIONSHIP', 'MISC_UNSEEN_ITEMS_TRACKER',
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
    2161, 1129, 2526, 3879, 3058, 1445, 1045, 3848, 1079, 2024, 3061, 162, 1495, 895, 1311, 3836, 2610, 882, 1712, 1345, 3727
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
-- ACTIVITY_POINTS_BALANCE existe na 20111213 (header 2052 -> parser com Dictionary points: int count, [int type, int amount]); estava DISABLED por engano na V16
UPDATE `releases_outgoing_headers` SET `header` = 2052, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ACTIVITY_POINTS_BALANCE';
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'SETTINGS_SAVE_FOCUS';

UPDATE `releases_incoming_headers` SET `header` = 1040, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'CATALOG_PURCHASE';

-- CATALOG_PAGE (GetCatalogPage int,int; MD5 c03cc33890b7ce1eb24d01b5e277559b, o mesmo da 20130522/1732) = 2913 na 20111213 (Messages.txt);
-- a V16 atribuiu 2913 a MISC_UNSEEN_ITEMS_TRACKER (inexistente na era) e deixou CATALOG_PAGE DISABLED: o catalogo nao carregava
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'MISC_UNSEEN_ITEMS_TRACKER';
UPDATE `releases_incoming_headers` SET `header` = 2913, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'CATALOG_PAGE';

-- 3) RELEASE63-201204021456-654375178
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `header` IN (
    2484, 1580, 1592, 2486, 2801, 2650, 226, 3697, 1483, 636, 729, 2767, 1166, 741, 3672, 3848, 3111, 3950, 3918
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

-- CATALOG_PAGE = 3125 na 20120402 (mesmo MD5 c03cc338...); V16 o atribuiu a MISC_UNSEEN_ITEMS_TRACKER (inexistente na era)
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'MISC_UNSEEN_ITEMS_TRACKER';
UPDATE `releases_incoming_headers` SET `header` = 3125, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'CATALOG_PAGE';

-- 4) RELEASE63-201207040943-767851649
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `header` IN (
    1871, 720, 30, 3897, 2030, 1874, 315, 2223, 1264, 2599, 3575, 2468, 3436, 2144, 2591, 2578, 2504, 305, 682, 632, 3854,
    1530, 2370, 1305, 1682, 1221, 3689, 2703, 867, 338, 208, 763, 3631, 858, 2264, 2899, 3271, 1783, 610, 62, 2134,
    2252, 1749, 1163, 2165, 3144, 2770, 2103, 1128, 405, 50, 1704, 424, 3841, 3622
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
-- CATALOG_GIFT_WRAPPING 20120704 = 3841 (`_-1UQ()`, enviado no HabboCatalog.init como 2o pedido sem parametros depois do indice; mesmo hash 083228a5 do 1540 de 20111213 e do 984 de 20120402).
-- O 2578 e um pedido do Marketplace (enviado por `_-2r9._SafeStr_1619`, a mesma classe que pede a config do Marketplace 1163).
UPDATE `releases_incoming_headers` SET `header` = 3841, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_GIFT_WRAPPING';
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
UPDATE `releases_incoming_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'ROOM_BANNED_USERS';
UPDATE `releases_incoming_headers` SET `header` = 405, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_IGNORE_USER';
UPDATE `releases_incoming_headers` SET `header` = 50, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_LOAD_BY_DOORBELL';
UPDATE `releases_incoming_headers` SET `header` = 1704, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'SETTINGS_SAVE_VOLUME';
-- CATALOG_HABBO_CLUB_PAGE 20120704 = 3622 (`_-Fw(int)`: ClubBuyCatalogWidget.init -> controlador `_SafeStr_12446` -> HabboCatalog._SafeStr_6765).
-- O 424 (`_-3ht(int)`) e a compra no Marketplace (confirmacao `catalog.marketplace.confirm_*`).
UPDATE `releases_incoming_headers` SET `header` = 3622, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_HABBO_CLUB_PAGE';

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
-- GROUP_BADGE_EDITOR (GuildEditorDataMessageEvent, parser _-395) existe na 20120704 (Messages.txt: Incoming[2286], MD5 3dac01ab... = o da 20130522/3285;
-- habbo-timeline: sem mudanca estrutural desde 201108251106); estava DISABLED por engano e o editor de emblema do grupo nao recebia dados
UPDATE `releases_outgoing_headers` SET `header` = 2286, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'GROUP_BADGE_EDITOR';
-- idem nas builds anteriores (o hash muda entre builds, entao o parser foi achado pela forma no AS3: parser que faz `new <GuildEditorData>(k)`,
-- cujo construtor le 5 listas: 2x (int, string, string) = bases/simbolos e 3x (int, string hex) = cores; Messages.txt: Incoming[h] ... [Parser: x])
UPDATE `releases_outgoing_headers` SET `header` = 2450, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'GROUP_BADGE_EDITOR';
UPDATE `releases_outgoing_headers` SET `header` = 1929, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'GROUP_BADGE_EDITOR';
UPDATE `releases_outgoing_headers` SET `header` = 2337, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GROUP_BADGE_EDITOR';

-- ROOM_ERROR (CantConnectMessageEvent: int reason [+ string parameter se reason == 3]). Achado pela ancora `navigator.guestroomfull.title`
-- (handler do CantConnect no navigator) -> evento -> Messages.txt: 20111005=455, 20111213=3482 (ok), 20120402=3205 (ok), 20120704=1783, 20130522=1014.
-- O 2979 da 20130522 era o MD5 69d268... = GuildEditFailedMessageEvent (int), nao o erro de entrada em quarto; 20111005/20120704 estavam DISABLED por engano
UPDATE `releases_outgoing_headers` SET `header` = 455, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 1783, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 1014, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_ERROR';

-- ROOM_DECORATION (RoomPropertyMessageEvent: string tipo floor/wallpaper/landscape/landscapeanim + string valor) = 3854 na 20111213
-- (mesmo MD5 8cc2e1b9... do 3522 da 20111005; parser _-0T8 le 2 strings e faz switch em floor/wallpaper/landscape); estava DISABLED e o quarto
-- entrava sem a cor do piso/parede/paisagem
UPDATE `releases_outgoing_headers` SET `header` = 3854, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_DECORATION';

-- Pacotes DISABLED por engano numa release mas ligados nas vizinhas (auditoria cruzada 2011-2013). Em cada caso o MD5 do pacote da release vizinha
-- reaparece no Messages.txt da release alvo, com a mesma assinatura e o header livre (flash Incoming = outgoing do emulador, Outgoing = incoming).
-- 20111213 (outgoing): MESSENGER_INVITE, MISC_BROADCAST_NOTIFICATION, MISC_MOTD_NOTIFICATION, ROOM_ITEM_ALIASES (MD5 da 20111005)
UPDATE `releases_outgoing_headers` SET `header` = 1591, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'MESSENGER_INVITE';
UPDATE `releases_outgoing_headers` SET `header` = 1386, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'MISC_BROADCAST_NOTIFICATION';
UPDATE `releases_outgoing_headers` SET `header` = 6, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'MISC_MOTD_NOTIFICATION';
UPDATE `releases_outgoing_headers` SET `header` = 3954, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_ITEM_ALIASES';
-- NAVIGATOR_CAN_CREATE_ROOM (outgoing) em 20111213 e 20120402 (MD5 da 20120704, header 1526)
UPDATE `releases_outgoing_headers` SET `header` = 3617, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';
UPDATE `releases_outgoing_headers` SET `header` = 518, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';
-- 20120704 (incoming): ROOM_ITEM_USE_CLOTHING (MD5 da 20120402 e da 20130522, int+int) e SANCTION_STATUS (MD5 da 20111213 e da 20120402, string)
UPDATE `releases_incoming_headers` SET `header` = 1204, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_ITEM_USE_CLOTHING';
UPDATE `releases_incoming_headers` SET `header` = 2069, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'SANCTION_STATUS';
-- CATALOG_PURCHASE_OK (PurchaseOKMessageEvent) = 1134 na 20111213: o unico parser "embrulho" de oferta (new <Offer>(k), como 1021 na 20111005 e 883 na 20120402);
-- o handler do evento loga "[HabboNotifications] purchase ok" e usa `notifications.text.purchase.ok` (HabboNotifications); estava DISABLED
UPDATE `releases_outgoing_headers` SET `header` = 1134, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'CATALOG_PURCHASE_OK';
-- GUIDE_TOOLS nao existe na 20120402 (sem literais `guide.help.guide.tool*` no AS3, como ja provado para 2011-10 e 2012-07): o 2412 era o evento (int, string) do
-- mensageiro (RoomInviteEvent, MD5 e022fbe0...), nao a contagem de guias; o Kotlin escreve bool+3 int
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'GUIDE_TOOLS';

-- MESSENGER_CHAT (NewConsoleMessageEvent: int senderId + string texto) estava num header errado em 2011-10..2012-07 (so a 20130522 acertava).
-- Ancora: o handler do HabboMessenger loga "Received console msg: " e o evento e registrado logo antes do RoomInvite (handler com o literal
-- `messenger.invitation`, que ja bate com o MESSENGER_INVITE do banco em todas as builds) e do InstantMessageError (MESSENGER_CHAT_ERROR):
-- 20111005 = 3993 (era 182), 20111213 = 1166 (era 1323), 20120402 = 2412 (era 1130 = "Got public space cast libs" do navegador; antes GUIDE_TOOLS), 20120704 = 1746 (era 1409)
UPDATE `releases_outgoing_headers` SET `header` = NULL WHERE `release_name` IN ('RELEASE63-201110051250-692472661', 'RELEASE63-201112130905-713192101', 'RELEASE63-201204021456-654375178', 'RELEASE63-201207040943-767851649') AND `name` = 'MESSENGER_CHAT';
UPDATE `releases_outgoing_headers` SET `header` = 3993, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MESSENGER_CHAT';
UPDATE `releases_outgoing_headers` SET `header` = 1166, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'MESSENGER_CHAT';
UPDATE `releases_outgoing_headers` SET `header` = 2412, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'MESSENGER_CHAT';
UPDATE `releases_outgoing_headers` SET `header` = 1746, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'MESSENGER_CHAT';

-- ROOM_RIGHTS_REMOVED (FlatControllerRemoved): o handler do cliente loga "Flat controller removed: " logo apos o de "Flat controller added: " (ROOM_RIGHTS_GIVEN, que ja bate
-- com o banco: 495 e 880). Header real: 20111213 = 75 (era 2616), 20120402 = 3465 (era 3403); 20111005/20120704/20130522 ja estavam certos
UPDATE `releases_outgoing_headers` SET `header` = NULL WHERE `release_name` IN ('RELEASE63-201112130905-713192101', 'RELEASE63-201204021456-654375178') AND `name` = 'ROOM_RIGHTS_REMOVED';
UPDATE `releases_outgoing_headers` SET `header` = 75, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_RIGHTS_REMOVED';
UPDATE `releases_outgoing_headers` SET `header` = 3465, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_RIGHTS_REMOVED';
UPDATE `releases_outgoing_headers` SET `header` = 1715, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_NOT_OPEN';
UPDATE `releases_outgoing_headers` SET `header` = 1765, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'TRADING_OPEN_FAILED';
UPDATE `releases_outgoing_headers` SET `header` = 3771, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'INVENTORY_REMOVE_OBJECT';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_UPDATE_FURNI_STACK';

-- 5) RELEASE63-201305221112-286302280
-- Nula previamente os headers alvos nesta release para evitar colisao de unicidade (release_name, header)
UPDATE `releases_incoming_headers`
SET `header` = NULL
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `header` IN (2177, 241, 2375, 1171);
UPDATE `releases_incoming_headers` SET `header` = 2177, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'TRADING_OPEN';
UPDATE `releases_incoming_headers`
SET `header`          = 241,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `name` = 'ROOM_TRIGGER_ITEM';
-- PET_INFO 20130522 = 2375: composer (int) com md5 d876d416... identico a 2011-12/2012-04/2012-07 (2928/1712/3950/2770); 241/1171 sao (int,int=0) de RoomObjectEventHandler (uso de furni de chao)
UPDATE `releases_incoming_headers`
SET `header`          = 2375,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `name` = 'PET_INFO';
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
-- PET_REMOVED_FROM_INVENTORY: o 1355 e TradingCloseEvent. Real = parser de 1 Int cujo handler faz remove(petId) no mapa de pets do inventario
-- (par do PET_ADDED: add(pet) no mesmo mapa); mesma forma do 670 de 2012-07 (md5 5abcf941...). 2011-12: _SafeStr_2607 = 1849; 2011-10: _SafeStr_2440 = 2319.
UPDATE `releases_outgoing_headers`
SET `header`          = 1849,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201112130905-713192101'
  AND `name` = 'PET_REMOVED_FROM_INVENTORY';
UPDATE `releases_outgoing_headers`
SET `header`          = 2319,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'PET_REMOVED_FROM_INVENTORY';
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

-- 6e) Correcoes de propagacao (2016->2013->2011/2012): evidencias no AS3 de cada release
-- ROOM_OWNER = md5 e6bf0b4c... (flag owner: `owner || level >= controller`, igual a 2013). O md5 6200c929... (antigo 1318/2663/3836) e outro evento.
UPDATE `releases_outgoing_headers`
SET `header`          = 3601,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'ROOM_OWNER';
UPDATE `releases_outgoing_headers`
SET `header`          = 3421,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201112130905-713192101'
  AND `name` = 'ROOM_OWNER';
UPDATE `releases_outgoing_headers`
SET `header`          = 1843,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201204021456-654375178'
  AND `name` = 'ROOM_OWNER';
UPDATE `releases_outgoing_headers`
SET `header`          = 1500,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'ROOM_OWNER';
-- PET_EXPERIENCE: md5 f3bdd4a6... continuo 2011-12 -> 2012-04 -> 2012-07 (mesmo consumidor do widget de pet: roomId, petId, tipo, amount).
UPDATE `releases_outgoing_headers`
SET `header`          = 1694,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'PET_EXPERIENCE';
UPDATE `releases_outgoing_headers`
SET `header`          = 1125,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'PET_EXPERIENCE';
-- GAME_2_WEEKLY_COMPETITIVE_LEADERBOARD 20120704: 4 eventos com Game2WeeklyLeaderboardParser (438/2269/2651 ja nomeados); o 2530 (_-hT) e o restante.
UPDATE `releases_outgoing_headers`
SET `header`          = 2530,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'GAME_2_WEEKLY_COMPETITIVE_LEADERBOARD';
-- GROUP_ID: md5 1f05cb... (parser groupId) igual ao de 20120704 (1665); registrado no HabboGroupsManager de 2011-12 e 2012-04.
UPDATE `releases_outgoing_headers`
SET `header`          = 3496,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201112130905-713192101'
  AND `name` = 'GROUP_ID';
UPDATE `releases_outgoing_headers`
SET `header`          = 1243,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201204021456-654375178'
  AND `name` = 'GROUP_ID';
-- Lista de banidos da sala + unban (RoomSettingsCtrl 'moderation_banned_users', ROOM_UNBAN_USER) so existem a partir de 2013: ausentes em 2011-2012.
UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'ROOM_BANNED_USERS';
UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'ROOM_BANNED_USERS';
UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'ROOM_USER_UNBANNED';
-- Mystery box: nenhuma referencia no dump de 20120704 (existe so em 2013).
UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'MYSTERY_BOX_CHALLENGE';
-- ROOM_BANNED_USERS (cliente->servidor) 2011-2012: o md5 95839b... e o clique 'remover todos' (navigator.flatctrls.removeconfirm), nao a lista de banidos.
UPDATE `releases_incoming_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201112130905-713192101'
  AND `name` = 'ROOM_BANNED_USERS';
UPDATE `releases_incoming_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201204021456-654375178'
  AND `name` = 'ROOM_BANNED_USERS';
-- INVENTORY_BOTS (servidor->cliente) 2011-10: bots so existem a partir de 2012 (sem classe de inventario de bots nos dumps de 2011-10/2011-12; confirmado pelo usuario).
UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'INVENTORY_BOTS';

-- 6f) Grupos e catalogo em 2011-10 (outgoing). Grupos foram redesenhados no fim de 2011: em 2011-10 ainda vale o sistema antigo.
-- Alinhamento do bloco de registro do gerenciador de grupos 2011-10 x 2011-12 (ordem + assinatura do parser) e handlers/literais iguais:
-- GROUP_INFO 1121 (objeto de grupo groupId/groupName, mesmos 13 campos iniciais do 1764); GROUP_MANAGE 2499 (janela edit_guild_tab_context, mesmos arrays/flags do 2528);
-- GROUP_MEMBERS 304 (janela guild_members_item_list; lista de membros do 2314); GROUP_UPDATE_MEMBER_STATUS 1200 (handler `groupId == x.groupId; update(data)` identico ao do 1219; o `data` e exatamente a classe do ELEMENTO da lista de membros nas duas releases: 2011-10 [Int,String,Bool,Bool], 2011-12 [Int,Int,String,String,String] - mesmo evento, membro versionado por data);
-- GROUP_MEMBER_MANAGEMENT_ERROR 3389 (alerta group.membermgmt.fail.<codigo>, igual ao 1223); GROUP_PURCHASED 3872 (mostra group_created_window, fecha a janela de criacao e manda o navegador para a sala: wire 2011-10 = [roomId Int]; 2011-12 = [roomId Int, groupId Int] - groupId e campo novo, versionar por data);
-- CATALOG_HABBO_CLUB_PAGE 3681 (assinatura identica a do 3008: Int, lista de elementos de 10 campos).
UPDATE `releases_outgoing_headers`
SET `header`          = 1121,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'GROUP_INFO';
UPDATE `releases_outgoing_headers`
SET `header`          = 2499,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'GROUP_MANAGE';
UPDATE `releases_outgoing_headers`
SET `header`          = 304,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'GROUP_MEMBERS';
UPDATE `releases_outgoing_headers`
SET `header`          = 1200,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'GROUP_UPDATE_MEMBER_STATUS';
UPDATE `releases_outgoing_headers`
SET `header`          = 3389,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'GROUP_MEMBER_MANAGEMENT_ERROR';
UPDATE `releases_outgoing_headers`
SET `header`          = 3872,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'GROUP_PURCHASED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3681,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` = 'CATALOG_HABBO_CLUB_PAGE';
-- Sem evento correspondente em 2011-10 (ausentes tambem em 2011-12 no banco; sem assinatura/literal nos dumps de 2011-10/12):
-- GROUP_ID (refetch de grupo, sistema novo), GROUP_FURNISHING_SETTINGS (so aparece em 2012-07), CATALOG_OFFER (2012-04), CATALOG_OFFER_CONFIGURATION (2012-07), GUIDE_TOOLS (sem literais de guia ate 2012-04), CAMPAIGN (so 2013).
UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201110051250-692472661'
  AND `name` IN ('GROUP_ID', 'GROUP_FURNISHING_SETTINGS', 'CATALOG_OFFER', 'CATALOG_OFFER_CONFIGURATION', 'GUIDE_TOOLS',
                 'CAMPAIGN');

-- 6g) 2012-07 (outgoing = servidor->cliente). Provas no AS3 do dump de 2012-07:
-- ROOM_PET/BOT_ERROR_NOTIFICATION: handlers com switch(errorCode) que referenciam RSEME_PETS_FORBIDDEN_IN_HOTEL (1896) e RSEME_BOTS_FORBIDDEN_IN_HOTEL (408).
-- GAME_CENTER_CAN_PLAY_GAME 3412: handler identico ao do 2424 de 2013 (gameTypeId == jogo atual; habilita/desabilita os botoes de jogar), registrado logo apos Game2AccountGameStatus.
-- ROOM_COMPETITION 972: RoomCompetitionController trata exatamente 972, ROOM_COMPETITION_DATA (237) e GUEST_ROOM_RESULT; handler com o mesmo corpo do 2006 de 2013.
-- REFRESH_CAMPAIGN (incoming) 1546: refresh() do widget do Hall da Fama da meta comunitaria (UserListWidget; CommunityGoalHallOfFameWidget em 2013) envia 1 String (em 2012-07 `landing.view.communitygoalhof.goalcode`; em 2013 o slot `landing.view.dynamic.slot.N.conf`, composer 573) e recebe o Hall da Fama - mesmo composer, semantica do argumento generalizada.
-- GET_SNOWWAR_GAME_TOKENS_OFFER (incoming) 2701: composer sem parametros enviado no clique do icone de jogos do HabboGameManager (iguais 1169 em 2012-04 e 876 em 2013).
UPDATE `releases_outgoing_headers`
SET `header`          = 408,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'ROOM_BOT_ERROR_NOTIFICATION';
UPDATE `releases_outgoing_headers`
SET `header`          = 1896,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'ROOM_PET_ERROR_NOTIFICATION';
UPDATE `releases_outgoing_headers`
SET `header`          = 3412,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'GAME_CENTER_CAN_PLAY_GAME';
UPDATE `releases_outgoing_headers`
SET `header`          = 972,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'ROOM_COMPETITION';
UPDATE `releases_incoming_headers`
SET `header`          = 1546,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'REFRESH_CAMPAIGN';
UPDATE `releases_incoming_headers`
SET `header`          = 2701,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'GET_SNOWWAR_GAME_TOKENS_OFFER';
/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
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

-- Inexistentes em 2012-07 (provado no AS3): GAME_CENTER_ACHIEVEMENTS (a janela do Game Center reaproveita ACHIEVEMENT_LIST 2157; o evento proprio so em 2013), GUIDE_TOOLS (contagem de guias/ajudantes/guardioes em servico:
-- sem registerParameter de guide.help.guide.tool.*onduty; guardioes entram no fim de 2012), TALENT_STATUS (TalentTrackController registra so TALENT_TRACK e 1 evento de grupo),
-- LANDING_IS_BADGE_REQUEST_FULFILLED (RequestBadgeButtonElementHandler sem eventos nem pedido), CAMPAIGN (resposta do widget de meta comunitaria: so 2013).
UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` IN
      ('GAME_CENTER_ACHIEVEMENTS', 'GUIDE_TOOLS', 'TALENT_STATUS', 'LANDING_IS_BADGE_REQUEST_FULFILLED', 'CAMPAIGN');

-- 6g) Pedidos do catalogo (cliente -> servidor) confirmados no AS3 (quem envia + quem chama) nas demais releases de 2011 a 2013.
-- CATALOG_HABBO_CLUB_PAGE 20111213 = 1345 (`_-49A()`, sem parametros: ClubBuyCatalogWidget.init -> controlador `_SafeStr_8089` -> HabboCatalog._SafeStr_8090).
-- O 1811 (`_-1GA(int)`) e a compra no Marketplace (confirmacao `catalog.marketplace.confirm_*`, buy_button).
UPDATE `releases_incoming_headers` SET `header` = 1345, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'CATALOG_HABBO_CLUB_PAGE';
-- CATALOG_GIFT_WRAPPING 20130522 = 1287 (`_-2Q2()`, HabboCatalog.init, 2o pedido sem parametros depois do indice 2170, como em 2011/2012).
-- O 2947 e enviado por toggleCatalog() quando o aviso de itens novos esta ativo (CATALOG_NEW_ITEMS_HIDE), nao e a config de embrulho.
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `header` IN (1287);
UPDATE `releases_incoming_headers` SET `header` = 1287, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'CATALOG_GIFT_WRAPPING';

-- 6h) MISC_GET_MOTD (pedido da mensagem do dia): existe em TODAS as releases, nao e DISABLED. E o unico pedido do componente
-- HabboNotifications (no AS3 da WIN63 2026 e `GetMOTDMessageComposer`, enviado por HabboNotifications.activate(); a resposta e o
-- `onMOTD` -> MOTDNotification, que o servidor ja atende com MISC_MOTD_NOTIFICATION). Em 20120704 (851) e 20130522 (3170) o banco ja estava certo.
-- 20111005 = 2703 (`_-294()`, HabboNotifications `_-9l._SafeStr_54`); 20111213 = 3727 (`_-39Q()`, `_-1Sk._SafeStr_82`; mesmo hash 32252773 do 2703);
-- 20120402 = 3918 (`_-0HG()`, `_-0li._SafeStr_97.activate()`, a classe que instancia as telas de notificacao `_-kN`).
UPDATE `releases_incoming_headers` SET `header` = 2703, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'MISC_GET_MOTD';
UPDATE `releases_incoming_headers` SET `header` = 3727, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'MISC_GET_MOTD';
UPDATE `releases_incoming_headers` SET `header` = 3918, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'MISC_GET_MOTD';

-- 6i) ROOM_IGNORE_USER (pedido de ignorar usuario): o banco guardava o header do UNIGNORE nestas 3 releases.
-- O servico de ignorar do cliente envia o composer de ignore e o de unignore, cada um com seu header:
-- 20111213 ignore = 1822 (`_-nn(String)`, md5 5e0bb8a8 = o mesmo do 3362 da 20111005); unignore = 1610 (md5 aa15483d = 1629 da 20111005).
-- 20120402 ignore = 3662 (`_-4KV(String,String)`); unignore = 1129 (md5 aa15483d).
-- 20130522 ignore = 2406 (`_-00q(String)`, RWUAM_IGNORE_USER); unignore = 3934 (md5 17c2a249 = o 256 da 20120704).
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `header` IN (1822);
UPDATE `releases_incoming_headers` SET `header` = 1822, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_IGNORE_USER';
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `header` IN (3662);
UPDATE `releases_incoming_headers` SET `header` = 3662, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_IGNORE_USER';
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `header` IN (2406);
UPDATE `releases_incoming_headers` SET `header` = 2406, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_IGNORE_USER';

-- 6j) MESSENGER_BLOCKED_USERS (pedido da lista de bloqueados) nao existe nas releases PRODUCTION: o AS3 delas nao tem
-- BlockedUsersManager / isBlocked / blockedUsers. O recurso de bloquear usuarios so aparece na WIN63-202512101753.
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED'
WHERE `name` = 'MESSENGER_BLOCKED_USERS' AND `release_name` IN (
'PRODUCTION-201611291003-338511768',
'PRODUCTION-202104091531-593308026',
'PRODUCTION-202111191349-636893688',
'PRODUCTION-202311200305-397722618');

-- 6k) Presentes: ROOM_ITEM_OPEN_GIFT (abrir presente) em 20111005.
-- `_SafeStr_13929[2016] = _SafeStr_1939;` enviado pelo metodo de abrir presente da sessao da sala; md5 84650fcb35dba724ac1dd82296301763
-- = o 78 (OpenPresent) estatico do R63A. O banco tinha 1433.
-- (O CATALOG_GIFT_WRAPPING da WIN63-202610271147 foi corrigido na propria V24, que cria a linha e roda depois desta.)
UPDATE `releases_incoming_headers` SET `header` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `header` IN (2016);
UPDATE `releases_incoming_headers` SET `header` = 2016, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_ITEM_OPEN_GIFT';
