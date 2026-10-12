-- =========================================================================
-- V9: Game Center Feature
-- Feature Game Center (era AS3 / Flash):
--   1) Tabela DDL game_center_games e jogos base (snowwar, basejump).
--   2) Registro de todos os enums do Game Center em todas as releases.
--   3) Headers concretos para PRODUCTION-201611291003-338511768.
--   4) Marcação de DISABLED nas releases onde Game Center é extinto (era AIR 2021+).
-- =========================================================================

-- 1) DDL da tabela game_center_games
CREATE TABLE IF NOT EXISTS `game_center_games` (
    `game_type_id` INT NOT NULL,
    `name`         VARCHAR(64) NOT NULL,
    `bg_color`     VARCHAR(16) NOT NULL DEFAULT '68bbd2',
    `text_color`   VARCHAR(16) NOT NULL DEFAULT '',
    `dir`          VARCHAR(128) NOT NULL,
    `swf`          VARCHAR(128) NOT NULL,
    `assets_swf`   VARCHAR(128) NULL,
    `play_url`     VARCHAR(255) NOT NULL DEFAULT '',
    PRIMARY KEY (`game_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

-- 2) Jogos base
DELETE FROM `game_center_games`;
INSERT INTO `game_center_games` (`game_type_id`, `name`, `bg_color`, `text_color`, `dir`, `swf`, `assets_swf`, `play_url`)
VALUES
    (1, 'snowwar', '93d4f3', '', 'gamecenter_snowwar', '', NULL, ''),
    (2, 'basejump', '68bbd2', '', 'gamecenter_basejump', 'BaseJump.swf', 'BasicAssets.swf', '');

-- 3) Registro de enums Incoming do Game Center em todas as releases
INSERT INTO `releases_incoming_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT r.`release_name`, n.`name`, NULL, NULL
FROM `releases` r
CROSS JOIN (
    SELECT 'GAME_CENTER_ACHIEVEMENTS' AS `name`
    UNION ALL SELECT 'GAME_CENTER_CAN_PLAY_GAME'
    UNION ALL SELECT 'GAME_CENTER_JOIN_GAME'
    UNION ALL SELECT 'GAME_CENTER_LUCKY_LOSERS'
    UNION ALL SELECT 'GAME_CENTER_PRIZE'
    UNION ALL SELECT 'GAME_CENTER_QUIT_GAME'
    UNION ALL SELECT 'GAME_2_GET_WEEKLY_COMPETITIVE_FRIENDS_LEADERBOARD'
    UNION ALL SELECT 'GAME_2_GET_WEEKLY_COMPETITIVE_LEADERBOARD'
    UNION ALL SELECT 'GAME_LISTING'
    UNION ALL SELECT 'INITIALIZE_GAME_CENTER'
) n
WHERE NOT EXISTS (
    SELECT 1 FROM `releases_incoming_headers` h
    WHERE h.`release_name` = r.`release_name` AND h.`name` = n.`name`
);

-- 4) Registro de enums Outgoing do Game Center em todas as releases
INSERT INTO `releases_outgoing_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT r.`release_name`, n.`name`, NULL, NULL
FROM `releases` r
CROSS JOIN (
    SELECT 'GAME_CENTER_ACHIEVEMENTS' AS `name`
    UNION ALL SELECT 'GAME_CENTER_CAN_PLAY_GAME'
    UNION ALL SELECT 'GAME_CENTER_FEATURED_LUCKY_LOSER_OF_THE_WEEK'
    UNION ALL SELECT 'GAME_CENTER_LIST'
    UNION ALL SELECT 'GAME_CENTER_LOAD_GAME'
    UNION ALL SELECT 'GAME_CENTER_PRIZE'
    UNION ALL SELECT 'GAME_2_WEEKLY_COMPETITIVE_FRIENDS_LEADERBOARD'
    UNION ALL SELECT 'GAME_2_WEEKLY_COMPETITIVE_LEADERBOARD'
    UNION ALL SELECT 'GAME_2_WEEKLY_FRIENDS_LEADERBOARD'
) n
WHERE NOT EXISTS (
    SELECT 1 FROM `releases_outgoing_headers` h
    WHERE h.`release_name` = r.`release_name` AND h.`name` = n.`name`
);

-- 5) Headers concretos PRODUCTION-201611291003-338511768 (dump AS3)
UPDATE `releases_incoming_headers` SET `header` = 389   WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_ACHIEVEMENTS';
UPDATE `releases_incoming_headers` SET `header` = 3171  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_CAN_PLAY_GAME';
UPDATE `releases_incoming_headers` SET `header` = 1458  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_JOIN_GAME';
UPDATE `releases_incoming_headers` SET `header` = 1054, `override_method` = NULL WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_LUCKY_LOSERS';

DELETE FROM `releases_incoming_headers` WHERE `name` = 'GAME_CENTER_LEADERBOARD_WINNER';
UPDATE `releases_incoming_headers` SET `header` = 2565  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_GET_WEEKLY_LEADERBOARD';
UPDATE `releases_incoming_headers` SET `header` = 654   WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_GET_WEEKLY_COMPETITIVE_FRIENDS_LEADERBOARD';
UPDATE `releases_incoming_headers` SET `header` = 1081  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_GET_WEEKLY_COMPETITIVE_LEADERBOARD';
UPDATE `releases_incoming_headers` SET `header` = 11    WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_GET_ACCOUNT_GAME_STATUS';

UPDATE `releases_outgoing_headers` SET `header` = 2265  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_ACHIEVEMENTS';
UPDATE `releases_outgoing_headers` SET `header` = 3805  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_CAN_PLAY_GAME';
UPDATE `releases_outgoing_headers` SET `header` = 2893, `override_method` = NULL WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_ACCOUNT_GAME_STATUS';
UPDATE `releases_outgoing_headers` SET `header` = 3097  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_FEATURED_LUCKY_LOSER_OF_THE_WEEK';
UPDATE `releases_outgoing_headers` SET `header` = 222   WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_LIST';
UPDATE `releases_outgoing_headers` SET `header` = 3654  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_CENTER_LOAD_GAME';
UPDATE `releases_outgoing_headers` SET `header` = 2196  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_WEEKLY_LEADERBOARD';
UPDATE `releases_outgoing_headers` SET `header` = 3560  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_WEEKLY_COMPETITIVE_LEADERBOARD';
UPDATE `releases_outgoing_headers` SET `header` = 3512  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_WEEKLY_COMPETITIVE_FRIENDS_LEADERBOARD';
UPDATE `releases_outgoing_headers` SET `header` = 2270  WHERE `release_name` = 'PRODUCTION-201611291003-338511768' AND `name` = 'GAME_2_WEEKLY_FRIENDS_LEADERBOARD';

-- 6) Marca DISABLED nas releases onde Game Center é extinto e sem header mapeado
UPDATE `releases_incoming_headers`
SET `override_method` = 'DISABLED'
WHERE `name` IN ('GAME_CENTER_ACHIEVEMENTS', 'GAME_CENTER_CAN_PLAY_GAME', 'GAME_CENTER_JOIN_GAME',
                 'GAME_CENTER_LUCKY_LOSERS',
                 'GAME_CENTER_PRIZE', 'GAME_CENTER_QUIT_GAME')
  AND `release_name` NOT IN ('RELEASE63-201305221112-286302280', 'PRODUCTION-201611291003-338511768')
  AND `header` IS NULL;

UPDATE `releases_outgoing_headers`
SET `override_method` = 'DISABLED'
WHERE `name` IN ('GAME_CENTER_FEATURED_LUCKY_LOSER_OF_THE_WEEK', 'GAME_CENTER_LIST',
                 'GAME_CENTER_LOAD_GAME', 'GAME_CENTER_PRIZE')
  AND `release_name` NOT IN ('RELEASE63-201305221112-286302280', 'PRODUCTION-201611291003-338511768')
  AND `header` IS NULL;
