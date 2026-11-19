-- =========================================================================
-- V36: conquistas do BaseJump (Fast Food) e a configuração de conquistas por jogo da 2013
--
-- 1) 5 grupos `ACH_BaseJump*` (categoria `games`, 20 níveis cada, emblema `ACH_BaseJump<Grupo><nível>`; os textos
--    `badge_name_ACH_BaseJump*` já existem no gamedata). Limites da wiki, que lista só parte dos níveis (os demais são
--    "x"): os conhecidos entram como estão e os "x" são interpolados entre os vizinhos, sempre estritamente crescentes.
--      ACH_BaseJumpWins         (Fast Food Winner)       vitórias. Conhecidos: 1-7, 9, 10, 12, 15-17, 20 (1000).
--      ACH_BaseJumpMissile      (Fast Food Rocketeer)    mísseis lançados. Conhecidos: 1, 3-7, 11, 17, 18, 20 (2000).
--      ACH_BaseJumpShield       (Fast Food Protector)    mísseis desviados. Conhecidos: 1, 5, 6, 15, 16, 20 (350).
--      ACH_BaseJumpBigParachute (Last Minute Life Saver) pousos perfeitos com paraquedas grande. Conhecidos: 1-3, 6, 7,
--                                                        16, 17; o resto segue a curva do Winner (nível 20 = 1000).
--      ACH_BaseJumpDaysPlayed   (Fast Food Regular)      dias distintos jogados. Conhecidos: 1-6, 10, 11; o nível 20
--                                                        não tem limite na wiki: 400.
--    Prêmio: 5 pontos de conquista por nível, sem pixels.
--
-- 2) O 997 da RELEASE63-201305221112 (`_-0-e()`, sem corpo, enviado ao abrir o Game Center) é o PEDIDO da configuração
--    de conquistas por jogo, e não "inicializar": renomeado para GET_GAME_ACHIEVEMENTS. A resposta é o Incoming[1825]
--    GameAchievementsMessageEvent, o novo Outgoing GAME_ACHIEVEMENTS (linha em TODAS as releases: DISABLED onde o
--    pacote não existe).
-- Idempotente.
-- =========================================================================

INSERT INTO `achievements_group` (`name`, `category`, `badge_append_level`)
SELECT v.`name`, 'games', 1
FROM (SELECT 'ACH_BaseJumpWins' AS `name`
      UNION ALL SELECT 'ACH_BaseJumpMissile'
      UNION ALL SELECT 'ACH_BaseJumpShield'
      UNION ALL SELECT 'ACH_BaseJumpBigParachute'
      UNION ALL SELECT 'ACH_BaseJumpDaysPlayed') v
WHERE NOT EXISTS (SELECT 1 FROM `achievements_group` g WHERE g.`name` = v.`name`);

INSERT INTO `achievements` (`achievement_group_id`, `level`, `reward_activity_points`, `reward_achievement_points`,
                            `progress_requirement`, `enabled`)
WITH RECURSIVE `levels` (`n`) AS (SELECT 1 UNION ALL SELECT `n` + 1 FROM `levels` WHERE `n` < 20)
SELECT g.`id`,
       l.`n`,
       0,
       l.`n` * 5,
       CASE g.`name`
           WHEN 'ACH_BaseJumpWins' THEN ELT(l.`n`, 2, 5, 17, 20, 35, 50, 65, 80, 100, 150, 200, 250, 300, 375, 450, 550, 650, 750, 875, 1000)
           WHEN 'ACH_BaseJumpMissile' THEN ELT(l.`n`, 2, 5, 10, 20, 35, 60, 95, 160, 230, 310, 400, 500, 620, 760, 920, 1100, 1300, 1500, 1750, 2000)
           WHEN 'ACH_BaseJumpShield' THEN ELT(l.`n`, 2, 5, 10, 16, 25, 35, 48, 62, 80, 100, 122, 146, 170, 190, 205, 230, 260, 290, 320, 350)
           WHEN 'ACH_BaseJumpBigParachute' THEN ELT(l.`n`, 2, 5, 10, 20, 35, 50, 65, 80, 100, 150, 200, 250, 300, 375, 450, 550, 650, 750, 875, 1000)
           WHEN 'ACH_BaseJumpDaysPlayed' THEN ELT(l.`n`, 2, 5, 10, 15, 25, 35, 50, 65, 80, 100, 120, 145, 170, 195, 220, 250, 280, 315, 355, 400)
       END,
       1
FROM `achievements_group` g
         CROSS JOIN `levels` l
WHERE g.`name` IN ('ACH_BaseJumpWins', 'ACH_BaseJumpMissile', 'ACH_BaseJumpShield', 'ACH_BaseJumpBigParachute',
                   'ACH_BaseJumpDaysPlayed')
ON DUPLICATE KEY UPDATE `progress_requirement` = VALUES(`progress_requirement`);

UPDATE `releases_incoming_headers`
SET `name` = 'GET_GAME_ACHIEVEMENTS'
WHERE `name` = 'INITIALIZE_GAME_CENTER';

INSERT INTO `releases_outgoing_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT t.`release_name`, t.`name`, t.`header`, IF(t.`header` IS NULL, 'DISABLED', NULL)
FROM (SELECT r.`release_name`,
             'GAME_ACHIEVEMENTS' AS `name`,
             CASE r.`release_name` WHEN 'RELEASE63-201305221112-286302280' THEN 1825 END AS `header`
      FROM `releases` r
      WHERE NOT EXISTS (SELECT 1
                        FROM `releases_outgoing_headers` h
                        WHERE h.`release_name` = r.`release_name` AND h.`name` = 'GAME_ACHIEVEMENTS')) t;
