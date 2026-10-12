-- =========================================================================
-- V11: Old Navigator aba "Quartos Oficial" (RELEASE63-201305221112)
-- Incoming 504 = GetGuestRoomMessage (_-45W, 1 int roomId) — no 2013 o ÚNICO caller
--   é a aba official (MainViewCtrl._SafeStr_7667=4); roomId = quarto atual (0 se nenhum).
--   Renomeado p/ NAVIGATOR_OFFICIAL_ROOMS por refletir o uso real da era 2013.
-- A resposta (2721 GuestRoomResult / RoomEntryInfo) JÁ EXISTE no emulador como
--   GUEST_ROOM_RESULT (GuestRoomResultResponse, nome Sulek GetGuestRoomResultEvent)
--   — nenhum outgoing novo necessário.
-- Outros releases reciclam 504 para pacotes distintos — só o 2013.
--
-- Outgoing 1263 = OfficialRoomsEvent (_SafeStr_2776, parser _SafeStr_2757) —
--   enviado junto com o GUEST_ROOM_RESULT no fluxo do 504 (aba official
--   só manda o 504; o 1263 seta data._SafeStr_7792 e encerra o loading).

INSERT INTO `releases_outgoing_headers` (`release_name`, `name`, `header`, `override_method`)
VALUES ('RELEASE63-201305221112-286302280', 'NAVIGATOR_OFFICIAL_ROOMS_RESULT', 1263, NULL)
ON DUPLICATE KEY UPDATE `header` = 1263, `override_method` = NULL;

-- DISABLED nas outras releases: o OfficialRoomsEvent só existe no navigator 2013
INSERT INTO `releases_outgoing_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT r.`release_name`, 'NAVIGATOR_OFFICIAL_ROOMS_RESULT', NULL, 'DISABLED'
FROM `releases` r
WHERE r.`release_name` <> 'RELEASE63-201305221112-286302280'
  AND NOT EXISTS (
    SELECT 1 FROM `releases_outgoing_headers` h
    WHERE h.`release_name` = r.`release_name` AND h.`name` = 'NAVIGATOR_OFFICIAL_ROOMS_RESULT'
);
-- =========================================================================

-- Renomeia o antigo 509 NAVIGATOR_OFFICIAL_ROOMS (na verdade GuildBase search) → NAVIGATOR_GUILD_BASE_SEARCH.
-- Guarda NOT EXISTS via LEFT JOIN: idempotente se o rename já foi aplicado em execução anterior falha.
UPDATE `releases_incoming_headers` h
LEFT JOIN `releases_incoming_headers` g
  ON g.`release_name` = h.`release_name` AND g.`name` = 'NAVIGATOR_GUILD_BASE_SEARCH'
SET h.`name` = 'NAVIGATOR_GUILD_BASE_SEARCH'
WHERE h.`name` = 'NAVIGATOR_OFFICIAL_ROOMS' AND h.`header` = 509 AND g.`release_name` IS NULL;

-- Renomeia 504: NAVIGATOR_GET_GUEST_ROOM → NAVIGATOR_OFFICIAL_ROOMS (uso real 2013).
-- DELETE antes do rename: nas outras releases o DISABLED insert abaixo já criou a linha,
-- deixando um duplicado GET_GUEST_ROOM que causaria 1062 no UPDATE.
DELETE h FROM `releases_incoming_headers` h
WHERE h.`name` = 'NAVIGATOR_GET_GUEST_ROOM'
  AND EXISTS (
    SELECT 1 FROM `releases_incoming_headers` o
    WHERE o.`release_name` = h.`release_name` AND o.`name` = 'NAVIGATOR_OFFICIAL_ROOMS'
  );

UPDATE `releases_incoming_headers` h
LEFT JOIN `releases_incoming_headers` o
  ON o.`release_name` = h.`release_name` AND o.`name` = 'NAVIGATOR_OFFICIAL_ROOMS'
SET h.`name` = 'NAVIGATOR_OFFICIAL_ROOMS'
WHERE h.`name` = 'NAVIGATOR_GET_GUEST_ROOM' AND o.`release_name` IS NULL;

-- Marca o 504 como DISABLED nas outras releases (ausência comprovada: ID reciclado)
INSERT INTO `releases_incoming_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT r.`release_name`, 'NAVIGATOR_OFFICIAL_ROOMS', NULL, 'DISABLED'
FROM `releases` r
WHERE r.`release_name` <> 'RELEASE63-201305221112-286302280'
  AND NOT EXISTS (
    SELECT 1 FROM `releases_incoming_headers` h
    WHERE h.`release_name` = r.`release_name` AND h.`name` = 'NAVIGATOR_OFFICIAL_ROOMS'
  );

-- 4) Renomeia o typo histórico NAGIVATOR -> NAVIGATOR (in-place, preserva header 380)
UPDATE `releases_incoming_headers_r63a`
SET `name` = 'NAVIGATOR_OFFICIAL_ROOMS'
WHERE `name` = 'NAGIVATOR_OFFICIAL_ROOMS';

-- 5) Renomeia ROOM_OWNERSHIP -> GUEST_ROOM_RESULT (nome Sulek do 2721@2013;
--    o parser _SafeStr_1070 atende tanto o fluxo navigator quanto o join)
UPDATE `releases_outgoing_headers`
SET `name` = 'GUEST_ROOM_RESULT'
WHERE `name` = 'ROOM_OWNERSHIP';

UPDATE `releases_outgoing_headers_r63a`
SET `name` = 'GUEST_ROOM_RESULT'
WHERE `name` = 'ROOM_OWNERSHIP';

-- 6) Outgoing r63a 450 = OfficialRoomsEvent (resposta ao incoming 380 GetOfficialRooms)
INSERT INTO `releases_outgoing_headers_r63a` (`name`, `header`, `override_method`)
VALUES ('NAVIGATOR_OFFICIAL_ROOMS_RESULT', 450, NULL)
ON DUPLICATE KEY UPDATE `header` = 450, `override_method` = NULL;
