-- =========================================================================
-- V17: Mapeamento de ROOM_MOVE, ROOM_GROUP_BADGES e ROOM_ADVERTISEMENT
-- para releases R63A (20111005, 20111213, 20120402, 20120704)
-- =========================================================================

-- 1) Registra ROOM_ADVERTISEMENT para todas as releases existentes (desabilitado por default)
INSERT INTO `releases_incoming_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT `release_name`, 'ROOM_ADVERTISEMENT', NULL, 'DISABLED' FROM `releases`
ON DUPLICATE KEY UPDATE `release_name` = VALUES(`release_name`);

INSERT INTO `releases_outgoing_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT `release_name`, 'ROOM_ADVERTISEMENT', NULL, 'DISABLED' FROM `releases`
ON DUPLICATE KEY UPDATE `release_name` = VALUES(`release_name`);

-- 2) RELEASE63-201110051250-692472661
UPDATE `releases_incoming_headers` SET `header` = 1604, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_outgoing_headers` SET `header` = 2599, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_incoming_headers` SET `header` = 2991, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 922, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 2659, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'AVATAR_EFFECTS';
UPDATE `releases_outgoing_headers` SET `header` = 3793, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_INFO_UPDATED';

-- 3) RELEASE63-201112130905-713192101
UPDATE `releases_incoming_headers` SET `header` = 2723, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_MOVE';
UPDATE `releases_incoming_headers` SET `header` = 437, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_outgoing_headers` SET `header` = 2722, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_incoming_headers` SET `header` = 962, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 2043, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 497, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'SHOW_INTERSTITIALS';
UPDATE `releases_outgoing_headers` SET `header` = 1859, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'HOME_ROOM';
UPDATE `releases_outgoing_headers` SET `header` = 1174, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_USER_CHAT';
UPDATE `releases_outgoing_headers` SET `header` = 3363, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_USER_SHOUT';
UPDATE `releases_outgoing_headers` SET `header` = 765, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_USER_WHISPER';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_USER_UNBANNED';
UPDATE `releases_outgoing_headers` SET `header` = 1917, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'INVENTORY_ITEMS';
UPDATE `releases_outgoing_headers` SET `header` = 2197, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'INVENTORY_FURNI_ADD_OR_UPDATE';
UPDATE `releases_incoming_headers` SET `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'INVENTORY_ITEMS';
UPDATE `releases_outgoing_headers` SET `header` = 1074, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'INVENTORY_UNSEEN_ITEMS';
UPDATE `releases_outgoing_headers` SET `header` = 2663, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_OWNER';
UPDATE `releases_outgoing_headers` SET `header` = 1572, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_NO_RIGHTS';
UPDATE `releases_outgoing_headers` SET `header` = 244, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_INFO_UPDATED';
UPDATE `releases_outgoing_headers` SET `header` = 3482, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'INVENTORY_BOTS';
UPDATE `releases_outgoing_headers` SET `header` = 1140, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'AVATAR_EFFECTS';
UPDATE `releases_incoming_headers` SET `header` = 3154, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'USER_TAGS';
UPDATE `releases_outgoing_headers` SET `header` = 1344, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'USER_TAGS';

-- 4) RELEASE63-201204021456-654375178
UPDATE `releases_incoming_headers` SET `header` = 3355, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_MOVE';
UPDATE `releases_incoming_headers` SET `header` = 3257, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_outgoing_headers` SET `header` = 693, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_incoming_headers` SET `header` = 2526, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 1576, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 3467, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'HOME_ROOM';
UPDATE `releases_outgoing_headers` SET `header` = 3314, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_USER_CHAT';
UPDATE `releases_outgoing_headers` SET `header` = 964, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_USER_SHOUT';
UPDATE `releases_outgoing_headers` SET `header` = 1957, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_USER_WHISPER';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_USER_UNBANNED';
UPDATE `releases_outgoing_headers` SET `header` = 1220, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'INVENTORY_ITEMS';
UPDATE `releases_outgoing_headers` SET `header` = 848, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'INVENTORY_FURNI_ADD_OR_UPDATE';
UPDATE `releases_incoming_headers` SET `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'INVENTORY_ITEMS';
UPDATE `releases_outgoing_headers` SET `header` = 2375, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'INVENTORY_UNSEEN_ITEMS';
UPDATE `releases_outgoing_headers` SET `header` = 3836, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_OWNER';
UPDATE `releases_outgoing_headers` SET `header` = 373, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_NO_RIGHTS';
UPDATE `releases_outgoing_headers` SET `header` = 628, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_INFO_UPDATED';
UPDATE `releases_outgoing_headers` SET `header` = 3205, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'INVENTORY_BOTS';
UPDATE `releases_outgoing_headers` SET `header` = 1710, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'AVATAR_EFFECTS';
UPDATE `releases_incoming_headers` SET `header` = 2282, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'USER_TAGS';
UPDATE `releases_outgoing_headers` SET `header` = 3264, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'USER_TAGS';

-- 5) RELEASE63-201207040943-767851649
UPDATE `releases_incoming_headers` SET `header` = 348, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_outgoing_headers` SET `header` = 348, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_GROUP_BADGES';
UPDATE `releases_incoming_headers` SET `header` = 3732, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 208, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 586, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'AVATAR_EFFECTS';
UPDATE `releases_outgoing_headers` SET `header` = 3052, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_INFO_UPDATED';
UPDATE `releases_incoming_headers` SET `header` = 1201, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'USER_TAGS';
UPDATE `releases_outgoing_headers` SET `header` = 602, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'USER_TAGS';
UPDATE `releases_incoming_headers` SET `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'INVENTORY_ITEMS';
UPDATE `releases_outgoing_headers` SET `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'INVENTORY_ITEMS';

-- 6) RELEASE63-201305221112-286302280
UPDATE `releases_outgoing_headers` SET `header` = 1281, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'AVATAR_EFFECTS';
UPDATE `releases_outgoing_headers` SET `header` = 3285, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'GROUP_BADGE_EDITOR';
UPDATE `releases_incoming_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'LOAD_INTERSTITIALS';
UPDATE `releases_incoming_headers` SET `header` = 2200, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = 'DISABLED' WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'SHOW_INTERSTITIALS';
UPDATE `releases_outgoing_headers` SET `header` = 2941, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_ADVERTISEMENT';
UPDATE `releases_outgoing_headers` SET `header` = 2979, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_ERROR';
UPDATE `releases_outgoing_headers` SET `header` = 1920, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_INFO_UPDATED';
UPDATE `releases_outgoing_headers` SET `header` = 2842, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_SETTINGS_SAVED';
UPDATE `releases_outgoing_headers` SET `header` = 1624, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_NO_RIGHTS';
UPDATE `releases_incoming_headers` SET `header` = 1234, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'USER_TAGS';
UPDATE `releases_outgoing_headers` SET `header` = 1795, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'USER_TAGS';
UPDATE `releases_incoming_headers` SET `header` = 1723, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'ROOM_GROUP_BADGES';

-- 7) CATALOG_UPDATE (CatalogPublishedMessageEvent, parser vazio, alerta catalog.alert.published) nas releases R63
UPDATE `releases_outgoing_headers` SET `header` = 3896, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'CATALOG_UPDATE';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_EXIT' AND `header` = 610;
UPDATE `releases_outgoing_headers` SET `header` = 610, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'CATALOG_UPDATE';
UPDATE `releases_outgoing_headers` SET `header` = NULL, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_EXIT' AND `header` = 1638;
UPDATE `releases_outgoing_headers` SET `header` = 1638, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'CATALOG_UPDATE';
UPDATE `releases_outgoing_headers` SET `header` = 329, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'CATALOG_UPDATE';
UPDATE `releases_outgoing_headers` SET `header` = 540, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201305221112-286302280' AND `name` = 'CATALOG_UPDATE';

-- 8) INVENTORY_FURNI_ADD_OR_UPDATE (FurniListAddOrUpdate, 1 item por pacote, formato R63A) nas releases R63 de 2011-2012
UPDATE `releases_outgoing_headers` SET `header` = 1283, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'INVENTORY_FURNI_ADD_OR_UPDATE';
UPDATE `releases_outgoing_headers` SET `header` = 3020, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'INVENTORY_FURNI_ADD_OR_UPDATE';

-- 9) ROOM_OPEN (handler RS_CONNECTED) e ROOM_EXIT (handler RS_DISCONNECTED, parser vazio) nas releases R63 de 2011-2012
UPDATE `releases_outgoing_headers` SET `header` = 450, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_OPEN';
UPDATE `releases_outgoing_headers` SET `header` = 392, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_EXIT';
UPDATE `releases_outgoing_headers` SET `header` = 835, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_EXIT';
UPDATE `releases_outgoing_headers` SET `header` = 2313, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_EXIT';
UPDATE `releases_outgoing_headers` SET `header` = 2648, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_EXIT';

-- 10) INVENTORY_ITEMS unificado por versão (sem override_method): remove os overrides da 20111005 (V15)
UPDATE `releases_incoming_headers` SET `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'INVENTORY_ITEMS';
UPDATE `releases_outgoing_headers` SET `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'INVENTORY_ITEMS';

-- 11) AUTHENTICATION_OK da RELEASE63-201112130905 (o handler cria a toolbar do client; estava DISABLED)
UPDATE `releases_outgoing_headers` SET `header` = 3231, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'AUTHENTICATION_OK';

-- 12) ROOM_TRIGGER_ITEM (floor) e ROOM_TRIGGER_WALL_ITEM (wall) nas releases R63 de 2011-2012 (a 201305221112 já estava certa: 241 e 1878)
UPDATE `releases_incoming_headers` SET `header` = 828, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TRIGGER_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 878, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_TRIGGER_WALL_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3307, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_TRIGGER_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 114, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_TRIGGER_WALL_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3401, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_TRIGGER_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 178, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_TRIGGER_WALL_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 3567, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TRIGGER_ITEM';
UPDATE `releases_incoming_headers` SET `header` = 821, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_TRIGGER_WALL_ITEM';

-- 13) Pacotes do client que estavam DISABLED/NULL nas releases R63 de 2011-2012 (typing, figura, perfil, guarda-roupa, expulsar)
UPDATE `releases_incoming_headers` SET `header` = 795, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'ROOM_KICK_USER';
UPDATE `releases_incoming_headers` SET `header` = 1866, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201110051250-692472661' AND `name` = 'USER_WARDROBES';
UPDATE `releases_incoming_headers` SET `header` = 1091, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_USER_START_TYPING';
UPDATE `releases_incoming_headers` SET `header` = 3439, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_USER_STOP_TYPING';
UPDATE `releases_incoming_headers` SET `header` = 3258, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'USER_CHANGE_FIGURE';
UPDATE `releases_incoming_headers` SET `header` = 3223, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'USER_PROFILE';
UPDATE `releases_incoming_headers` SET `header` = 1092, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'USER_WARDROBES';
UPDATE `releases_incoming_headers` SET `header` = 2049, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201112130905-713192101' AND `name` = 'ROOM_KICK_USER';
UPDATE `releases_incoming_headers` SET `header` = 3313, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_USER_START_TYPING';
UPDATE `releases_incoming_headers` SET `header` = 2833, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_USER_STOP_TYPING';
UPDATE `releases_incoming_headers` SET `header` = 1693, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'USER_CHANGE_FIGURE';
UPDATE `releases_incoming_headers` SET `header` = 297, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'USER_WARDROBES';
UPDATE `releases_incoming_headers` SET `header` = 3158, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201204021456-654375178' AND `name` = 'ROOM_KICK_USER';
UPDATE `releases_incoming_headers` SET `header` = 321, `override_method` = NULL WHERE `release_name` = 'RELEASE63-201207040943-767851649' AND `name` = 'ROOM_KICK_USER';
