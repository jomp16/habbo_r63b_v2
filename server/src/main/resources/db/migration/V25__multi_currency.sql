-- =========================================================================
-- V25: Multi-currency
--  1) users_currencies: remove duplicatas acumuladas e cria UNIQUE(user_id, type), sem o qual o
--     ON DUPLICATE KEY UPDATE de update_user_currencies.sql nunca disparava (uma linha nova por flush)
--  2) activity_point_types: tipos de moeda conhecidos e a janela de releases em que o client os suporta
--  3) catalog_items: cost_pixels/cost_vip viram (price_points, point_type): o client le por oferta
--     priceInCredits + UMA moeda alternativa (priceInActivityPoints + activityPointType)
-- =========================================================================

-- 1) users_currencies -------------------------------------------------------------------------
-- Sem UNIQUE cada flush inseria uma linha nova; a de maior id e a mais recente (o flush grava o valor atual).
DELETE uc
FROM `users_currencies` uc
         JOIN (SELECT `user_id`, `type`, MAX(`id`) AS `keep_id`
               FROM `users_currencies`
               GROUP BY `user_id`, `type`) k ON k.`user_id` = uc.`user_id` AND k.`type` = uc.`type`
WHERE uc.`id` < k.`keep_id`;

-- Reinicia o indice: as linhas que sobraram sao renumeradas de 1 em diante (ninguem referencia esse id) e o
-- AUTO_INCREMENT volta ao menor valor possivel (o InnoDB o ajusta para MAX(id) + 1).
SET @uc_seq = 0;
UPDATE `users_currencies` SET `id` = (@uc_seq := @uc_seq + 1) ORDER BY `id`;
ALTER TABLE `users_currencies` AUTO_INCREMENT = 1;

ALTER TABLE `users_currencies`
    ADD UNIQUE KEY IF NOT EXISTS `uq_users_currencies_user_type` (`user_id`, `type`);

-- O indice simples vira redundante (a UNIQUE tem user_id como prefixo e continua servindo a FK).
-- Nem todo banco o tem (depende do DDL com que a tabela foi criada), por isso IF EXISTS.
ALTER TABLE `users_currencies`
    DROP KEY IF EXISTS `user_id`;

-- 2) activity_point_types ---------------------------------------------------------------------
-- available_from/available_until: janela de data de build do client em que o tipo pode ser enviado.
-- Provado no AS3 (1349 releases): o client monta as chaves de texto por tipo ("catalog.purchase.youractivitypoints." + tipo),
-- logo e generico a partir de 2010-11-16 (primeira release com catalog.alert.notenough.activitypoints); antes so ha credits.
-- A oferta do catalogo so carrega o tipo da moeda a partir de 2010-12-03 (antes e sempre pixels).
-- O inicio de cada tipo e o do primeiro flash texts ORIGINAL da Sulake que traz textos dele (snapshots em
-- swf.habbo.rwx.ovh/gordon e HabBit; a data e o limite superior da introducao, o anterior sem o tipo vem entre parenteses):
--   1 = 2010-12-03 (2010-11-16)   2 = 2011-02-04 (2010-12-03)   3 e 4 = 2011-08-30 (2011-02-04)
--   101 e 102 = 2012-04-02 (2011-12-13)   103 = 2012-07-04 (2012-04-02)   5, 104 e 105 = 2013-05-22 (2012-07-04)
-- Os tipos 3 (gift points) e 102 (nuts) existiram de fato: tem textos oficiais de notenough/preco/saldo. So o
-- "catalog.purchase.youractivitypoints.102" nunca existiu no original; o override_rwx do swf.habbo.rwx.ovh cobre ele.
CREATE TABLE IF NOT EXISTS `activity_point_types`
(
    `code`            int(11)      NOT NULL,
    `name`            varchar(64)  NOT NULL,
    `available_from`  datetime     NULL,
    `available_until` datetime     NULL,
    `enabled`         tinyint(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_520_ci;

INSERT INTO `activity_point_types` (`code`, `name`, `available_from`, `available_until`, `enabled`)
VALUES (0, 'PIXELS', NULL, NULL, 1),
       (1, 'SNOWFLAKES', '2010-12-03 00:00:00', NULL, 1),
       (2, 'HEARTS', '2011-02-04 00:00:00', NULL, 1),
       (3, 'GIFT_POINTS', '2011-08-30 00:00:00', NULL, 1),
       (4, 'SHELLS', '2011-08-30 00:00:00', NULL, 1),
       (5, 'DIAMONDS', '2013-05-22 00:00:00', NULL, 1),
       (101, 'SEASHELLS_2', '2012-04-02 00:00:00', NULL, 1),
       (102, 'NUTS', '2012-04-02 00:00:00', NULL, 1),
       (103, 'STARS', '2012-07-04 00:00:00', NULL, 1),
       (104, 'CLOUDS', '2013-05-22 00:00:00', NULL, 1),
       (105, 'DIAMONDS_2', '2013-05-22 00:00:00', NULL, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`),
                        `available_from` = VALUES(`available_from`),
                        `available_until` = VALUES(`available_until`),
                        `enabled` = VALUES(`enabled`);

-- 3) catalog_items ----------------------------------------------------------------------------
ALTER TABLE `catalog_items`
    ADD COLUMN IF NOT EXISTS `price_points` int(11) NOT NULL DEFAULT 0 AFTER `cost_credits`,
    ADD COLUMN IF NOT EXISTS `point_type`   int(11) NOT NULL DEFAULT 0 AFTER `price_points`;

-- O client so le UMA moeda alternativa por oferta. Quando cost_vip e cost_pixels coexistiam, o serializador ja enviava so
-- a vip, entao ela vence (DIAMONDS = 5); os itens com as duas colunas > 0 perdem o cost_pixels.
-- Os dois UPDATEs so rodam enquanto as colunas antigas existem (re-execucao depois do DROP nao os reaplica).
SET @has_old_prices = (SELECT COUNT(*)
                       FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE()
                         AND TABLE_NAME = 'catalog_items'
                         AND COLUMN_NAME IN ('cost_pixels', 'cost_vip')) = 2;

SET @sql = IF(@has_old_prices,
              'UPDATE `catalog_items` SET `price_points` = `cost_vip`, `point_type` = 5 WHERE `cost_vip` > 0',
              'DO 0');
PREPARE migrate_vip FROM @sql;
EXECUTE migrate_vip;
DEALLOCATE PREPARE migrate_vip;

SET @sql = IF(@has_old_prices,
              'UPDATE `catalog_items` SET `price_points` = `cost_pixels`, `point_type` = 0 WHERE `cost_vip` = 0 AND `cost_pixels` > 0',
              'DO 0');
PREPARE migrate_pixels FROM @sql;
EXECUTE migrate_pixels;
DEALLOCATE PREPARE migrate_pixels;

ALTER TABLE `catalog_items`
    DROP COLUMN IF EXISTS `cost_pixels`,
    DROP COLUMN IF EXISTS `cost_vip`;
