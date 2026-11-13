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

-- =========================================================================
-- V23: Limpeza de pacotes cruzados de baú (CHEST_*) inseridos incorretamente
-- na V5 e correção/desativação de headers para WIN63-202610121709-785561319
-- =========================================================================

-- 1) Remove entradas de Outgoing (MessageEvent) inseridas indevidamente em releases_incoming_headers
-- e duplicatas legadas que não pertencem ao enum Incoming.kt
DELETE
FROM `releases_incoming_headers`
WHERE `name` IN (
                 'CHEST_COINS',
                 'CHEST_ITEMS_CHUNK',
                 'CHEST_ITEMS_UPDATED',
                 'CHEST_OPEN',
                 'CHEST_PREFERENCES_UPDATE_SUCCESS',
                 'CHEST_TRANSACTION_LOG_DETAILS',
                 'CHEST_TRANSACTION_LOG_LIST',
                 'CHEST_UPGRADE_RESULT',
                 'WIRED_TRANSACTION_GET_CHEST_LOGS',
                 'WIRED_TRANSACTION_GET_LOG_DETAILS'
    );

-- 2) Remove entradas de Incoming (MessageComposer) inseridas indevidamente em releases_outgoing_headers
-- e duplicatas legadas que não pertencem ao enum Outgoing.kt
DELETE
FROM `releases_outgoing_headers`
WHERE `name` IN (
                 'CHEST_CLOSE',
                 'CHEST_GET_TRANSACTION_LOGS',
                 'CHEST_GET_TRANSACTION_LOG_DETAILS',
                 'CHEST_LOCK_ALL',
                 'CHEST_OPEN_GET_CONTENTS',
                 'CHEST_SET_NOTIFICATION_PREFERENCES',
                 'CHEST_SET_OPTIONS',
                 'CHEST_SET_PREFERENCES',
                 'CHEST_START_ADDING',
                 'CHEST_UPGRADE',
                 'CHEST_WITHDRAW_ALL',
                 'CHEST_WITHDRAW_COINS',
                 'CHEST_WITHDRAW_ITEMS',
                 'WIRED_TRANSACTION_LOG_DETAILS',
                 'WIRED_TRANSACTION_LOG_LIST'
    );

-- 3) Aplica headers confirmados do cliente AIR para WIN63-202610121709-785561319
UPDATE `releases_incoming_headers`
SET `header`          = 924,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';

UPDATE `releases_incoming_headers`
SET `header`          = 3282,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` = 'NAVIGATOR_POPULAR_TAGS';

UPDATE `releases_outgoing_headers`
SET `header`          = 3489,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` = 'NAVIGATOR_CAN_CREATE_ROOM';

UPDATE `releases_outgoing_headers`
SET `header`          = 27,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` = 'NAVIGATOR_POPULAR_TAGS';

-- 4) Marca como DISABLED no WIN63 os composres do navegador antigo e do Game Center extinto
UPDATE `releases_incoming_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` IN (
                 'NAVIGATOR_POPULAR_ROOMS',
                 'NAVIGATOR_MY_ROOMS',
                 'NAVIGATOR_MY_FAVORITES',
                 'NAVIGATOR_MY_HISTORY',
                 'NAVIGATOR_FRIENDS_ROOMS',
                 'NAVIGATOR_FRIENDS_IN_ROOMS',
                 'NAVIGATOR_GUILD_BASE_SEARCH',
                 'NAVIGATOR_HIGHEST_SCORE_ROOMS',
                 'NAVIGATOR_PROMO_ROOMS',
                 'NAVIGATOR_TAG_SEARCH',
                 'NAVIGATOR_LATEST_EVENTS',
                 'NAVIGATOR_LIST_ROOMS',
                 'ROOM_ENTRY_DATA',
                 'GAME_2_GET_WEEKLY_COMPETITIVE_FRIENDS_LEADERBOARD',
                 'GAME_2_GET_WEEKLY_COMPETITIVE_LEADERBOARD'
    );

UPDATE `releases_outgoing_headers`
SET `header`          = NULL,
    `override_method` = 'DISABLED'
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` IN (
                 'NAVIGATOR_LIST_ROOMS',
                 'GAME_2_WEEKLY_COMPETITIVE_FRIENDS_LEADERBOARD'
    );
