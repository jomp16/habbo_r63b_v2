-- =========================================================================
-- V33: o Game Center da RELEASE63-201207040943-767851649 e os ids de jogo da Sulake
--
-- 1) Pacotes que a V22 deu como inexistentes em julho/2012, mas que o cliente manda (casados com a 2013 pelo pacote
--    AS3 de cada classe e pela resposta que o cliente espera):
--    - 3054 `_-bZ(int gameId)`: "Jogue agora!" (HabboGameManager.joinPlayerQueue, todo jogo que não é o SnowWar;
--      este vai por Game2QuickJoinGame). Resposta: LoadGame (3791, GAME_CENTER_LOAD_GAME), no mesmo formato da 2013.
--      Sem ele mapeado o emulador descartava o pedido e o BaseJump nunca carregava.
--    - 3388 `_-2Er(int gameId)`: pode jogar? (pacote do joinPlayerQueue; resposta 3412 = CAN_PLAY_GAME)
--    - 2019 `_-1w1(int gameId)`: sorteados da semana (pacote do prêmio/1193; resposta 805 = lucky_losers_list)
--    - 1790 `_-0ti(int gameId, int)`: ranking competitivo da semana (resposta 2530, Game2WeeklyLeaderboardParser)
--    As conquistas do jogo vêm do ACHIEVEMENT_LIST do hotel (3436 → 2157): GAME_CENTER_ACHIEVEMENTS segue DISABLED.
--
-- 2) Os ids de jogo passam a ser os da Sulake: snowwar 0, basejump 3 (slotcar 4, onedraw 5). Até
--    RELEASE63-201208142302 o cliente os tem fixos (classe `_-4QC` em 2012-07; só mostra um jogo com
--    game.center.<nome>.enabled=true nas variáveis) e manda/espera esses ids no JOIN_GAME, CAN_PLAY_GAME e
--    LOAD_GAME; e até o cliente de 2013 trata o 0 como SnowWar (joinPlayerQueue: 0 vai pelo Game2, qualquer outro
--    pelo JOIN_GAME). Com os mesmos ids no banco, a GAME_CENTER_LIST e as versões de lista fixa falam a mesma língua.
-- Idempotente.
-- =========================================================================

UPDATE `releases_incoming_headers`
SET `header` = CASE `name`
        WHEN 'GAME_CENTER_JOIN_GAME' THEN 3054
        WHEN 'GAME_CENTER_CAN_PLAY_GAME' THEN 3388
        WHEN 'GAME_CENTER_LUCKY_LOSERS' THEN 2019
        WHEN 'GAME_2_GET_WEEKLY_COMPETITIVE_LEADERBOARD' THEN 1790
    END,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` IN ('GAME_CENTER_JOIN_GAME', 'GAME_CENTER_CAN_PLAY_GAME', 'GAME_CENTER_LUCKY_LOSERS',
                 'GAME_2_GET_WEEKLY_COMPETITIVE_LEADERBOARD');

UPDATE `game_center_games`
SET `game_type_id` = CASE `name` WHEN 'snowwar' THEN 0 WHEN 'basejump' THEN 3 END
WHERE `name` IN ('snowwar', 'basejump');
