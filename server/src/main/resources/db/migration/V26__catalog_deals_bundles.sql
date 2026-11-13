-- =========================================================================
-- V26: Pacotes (catalog deals) de verdade
--  Antes, catalog_deals tinha (id PK, item_name FK, amount): uma linha = N unidades de UM mobi (o mesmo que
--  catalog_items.amount), e o DAO esperava listas "a,b" que a FK impede. Um pacote com varios mobis nao existia.
--  Agora: catalog_deals(id, name) + catalog_deal_items(deal_id, position, item_name, amount), na ordem em que os
--  produtos aparecem na oferta. Idempotente: pode rodar de novo em qualquer estado.
-- =========================================================================

-- 1) nome do pacote (o que o CMS mostra no seletor e vira o nome da oferta)
ALTER TABLE `catalog_deals`
    ADD COLUMN IF NOT EXISTS `name` varchar(100) NOT NULL DEFAULT '' AFTER `id`;

-- 2) os produtos do pacote
CREATE TABLE IF NOT EXISTS `catalog_deal_items`
(
    `deal_id`   int(11)      NOT NULL,
    `position`  int(11)      NOT NULL,
    `item_name` varchar(100) NOT NULL,
    `amount`    int(11)      NOT NULL DEFAULT 1,
    PRIMARY KEY (`deal_id`, `position`),
    KEY `item_name` (`item_name`),
    CONSTRAINT `catalog_deal_items_deal_fk` FOREIGN KEY (`deal_id`) REFERENCES `catalog_deals` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `catalog_deal_items_item_fk` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_520_ci;

-- 3) as linhas antigas (cada uma era UM mobi) viram pacotes de um produto. So enquanto as colunas antigas existem.
SET @has_old_deal_cols = (SELECT COUNT(*)
                          FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE()
                            AND TABLE_NAME = 'catalog_deals'
                            AND COLUMN_NAME IN ('item_name', 'amount')) = 2;

SET @sql = IF(@has_old_deal_cols,
              'INSERT INTO `catalog_deal_items` (`deal_id`, `position`, `item_name`, `amount`) SELECT d.`id`, 1, d.`item_name`, d.`amount` FROM `catalog_deals` d WHERE NOT EXISTS (SELECT 1 FROM `catalog_deal_items` i WHERE i.`deal_id` = d.`id`)',
              'DO 0');
PREPARE migrate_deal_items FROM @sql;
EXECUTE migrate_deal_items;
DEALLOCATE PREPARE migrate_deal_items;

SET @sql = IF(@has_old_deal_cols,
              'UPDATE `catalog_deals` SET `name` = CONCAT(`amount`, ''x '', `item_name`) WHERE `name` = ''''',
              'DO 0');
PREPARE migrate_deal_names FROM @sql;
EXECUTE migrate_deal_names;
DEALLOCATE PREPARE migrate_deal_names;

-- 4) tira o modelo antigo (a FK e o indice antes das colunas)
ALTER TABLE `catalog_deals`
    DROP FOREIGN KEY IF EXISTS `catalog_deals_ibfk_1`;
ALTER TABLE `catalog_deals`
    DROP KEY IF EXISTS `item_name`;
ALTER TABLE `catalog_deals`
    DROP COLUMN IF EXISTS `item_name`,
    DROP COLUMN IF EXISTS `amount`;

-- 5) apagar um pacote em uso nao pode levar as ofertas junto (era ON DELETE CASCADE): so apaga sem uso
ALTER TABLE `catalog_items`
    DROP FOREIGN KEY IF EXISTS `catalog_items_ibfk_3`;
ALTER TABLE `catalog_items`
    ADD CONSTRAINT `catalog_items_ibfk_3` FOREIGN KEY (`deal_id`) REFERENCES `catalog_deals` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
