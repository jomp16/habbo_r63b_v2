-- =========================================================================
-- V6: Protocol & R63A Headers Cleanup
-- Correção e renomeação de enums pré-shuffle na tabela R63A:
--   1) ROOM_ITEMS -> ROOM_ADVERTISEMENT (anúncio de quarto)
--   2) ROOM_INTERSITIAL -> LOAD_INTERSTITIALS (incoming) / SHOW_INTERSTITIALS (outgoing)
--   3) MESSENGER_FRIENDS -> MESSENGER_INIT (outgoing: lista de amigos/requests embutida no init pré-shuffle)
-- =========================================================================

UPDATE `releases_incoming_headers_r63a`
SET `name` = 'ROOM_ADVERTISEMENT'
WHERE `name` = 'ROOM_ITEMS';

UPDATE `releases_incoming_headers_r63a`
SET `name` = 'LOAD_INTERSTITIALS'
WHERE `name` = 'ROOM_INTERSITIAL';

UPDATE `releases_outgoing_headers_r63a`
SET `name` = 'SHOW_INTERSTITIALS'
WHERE `name` = 'ROOM_INTERSITIAL';

UPDATE `releases_outgoing_headers_r63a`
SET `name` = 'MESSENGER_INIT'
WHERE `name` = 'MESSENGER_FRIENDS';

-- 4) Remoção de enums redundantes e duplicatas de Game / GameCenter
DELETE FROM `releases_incoming_headers`
WHERE `name` IN ('GAME_2_LEAVE_GAME', 'GAME_CENTER_EVENT', 'GAME_CENTER_LEADERBOARD', 'GAME_GET_ACCOUNT_STATUS');

DELETE FROM `releases_outgoing_headers`
WHERE `name` IN ('GAME_CENTER_LEADERBOARD');
