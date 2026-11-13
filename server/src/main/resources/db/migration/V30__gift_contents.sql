-- =========================================================================
-- V30: Presentes (gifts) com o conteudo real dentro da caixa
--  Tabela items_gift_contents (caixa -> itens de conteudo, mesmos ids), headers estaticos do R63A para
--  CATALOG_PURCHASE_GIFT / CATALOG_RECEIVER_NOT_FOUND e limiares oficiais de ACH_GiftGiver / ACH_GiftReceiver.
--  Idempotente (zero warnings 1062 no boot). Os headers das 24 releases ja estavam corretos (exceto os 2 corrigidos na V22, secao 6k).
--  As linhas legadas de items_gift NAO sao migradas: o servidor mantem o fallback que abre essas caixas do jeito antigo.
-- =========================================================================

-- 1) Vinculo caixa -> conteudo. Os itens de conteudo sao linhas normais de items (dono = destinatario, sem sala) e ficam
-- escondidos de inventario/sala enquanto existirem aqui. Apagar a caixa ou o conteudo apaga o vinculo.
CREATE TABLE IF NOT EXISTS `items_gift_contents` (
  `gift_item_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  PRIMARY KEY (`gift_item_id`, `item_id`),
  UNIQUE KEY `item_id` (`item_id`),
  CONSTRAINT `items_gift_contents_ibfk_1` FOREIGN KEY (`gift_item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `items_gift_contents_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

-- 2) R63A (headers estaticos, PK header + UNIQUE name). Idempotente.
-- Incoming 472 = CATALOG_PURCHASE_GIFT; outgoing 76 = CATALOG_RECEIVER_NOT_FOUND (corpo vazio).
INSERT INTO `releases_incoming_headers_r63a` (`name`, `header`, `override_method`)
SELECT v.`name`, v.`header`, NULL
FROM (SELECT 'CATALOG_PURCHASE_GIFT' AS `name`, 472 AS `header`) v
WHERE NOT EXISTS (SELECT 1 FROM `releases_incoming_headers_r63a` h WHERE h.`name` = v.`name` OR h.`header` = v.`header`);

INSERT INTO `releases_outgoing_headers_r63a` (`name`, `header`, `override_method`)
SELECT v.`name`, v.`header`, NULL
FROM (SELECT 'CATALOG_RECEIVER_NOT_FOUND' AS `name`, 76 AS `header`) v
WHERE NOT EXISTS (SELECT 1 FROM `releases_outgoing_headers_r63a` h WHERE h.`name` = v.`name` OR h.`header` = v.`header`);

-- 3) Limiares oficiais dos emblemas de presente (flash texts badge_desc_ACH_GiftGiver<N>_HHCA / ACH_GiftReceiver<N>_HHCA: 1, 6, 14, 26, 46, 86, 146, 236, 366, 566;
-- niveis 11 a 15 do GiftGiver = wiki: 816, 1141, 1541, 2041, 2591). Juncao por nome do grupo + nivel.
UPDATE `achievements` a
JOIN `achievements_group` g ON g.`id` = a.`achievement_group_id`
JOIN (SELECT 'ACH_GiftGiver' AS `name`, 1 AS `level`, 1 AS `req` UNION ALL SELECT 'ACH_GiftGiver', 2, 6 UNION ALL SELECT 'ACH_GiftGiver', 3, 14
      UNION ALL SELECT 'ACH_GiftGiver', 4, 26 UNION ALL SELECT 'ACH_GiftGiver', 5, 46 UNION ALL SELECT 'ACH_GiftGiver', 6, 86
      UNION ALL SELECT 'ACH_GiftGiver', 7, 146 UNION ALL SELECT 'ACH_GiftGiver', 8, 236 UNION ALL SELECT 'ACH_GiftGiver', 9, 366
      UNION ALL SELECT 'ACH_GiftGiver', 10, 566 UNION ALL SELECT 'ACH_GiftGiver', 11, 816 UNION ALL SELECT 'ACH_GiftGiver', 12, 1141
      UNION ALL SELECT 'ACH_GiftGiver', 13, 1541 UNION ALL SELECT 'ACH_GiftGiver', 14, 2041 UNION ALL SELECT 'ACH_GiftGiver', 15, 2591
      UNION ALL SELECT 'ACH_GiftReceiver', 1, 1 UNION ALL SELECT 'ACH_GiftReceiver', 2, 6 UNION ALL SELECT 'ACH_GiftReceiver', 3, 14
      UNION ALL SELECT 'ACH_GiftReceiver', 4, 26 UNION ALL SELECT 'ACH_GiftReceiver', 5, 46 UNION ALL SELECT 'ACH_GiftReceiver', 6, 86
      UNION ALL SELECT 'ACH_GiftReceiver', 7, 146 UNION ALL SELECT 'ACH_GiftReceiver', 8, 236 UNION ALL SELECT 'ACH_GiftReceiver', 9, 366
      UNION ALL SELECT 'ACH_GiftReceiver', 10, 566) v ON v.`name` = g.`name` AND v.`level` = a.`level`
SET a.`progress_requirement` = v.`req`
WHERE a.`progress_requirement` <> v.`req`;

-- 4) Caixas e lacos do embrulho de presente (boxTypes / ribbonTypes da GiftWrappingConfiguration), por faixa de release
-- como activity_point_types (NULL = sem limite). Textos catalog.gift_wrapping_new.box.N / ribbon.N: RELEASE63-201305221112
-- tem caixas 0-6 e lacos 0-10; os textos atuais (WIN63) acrescentam a caixa 8 ("Caixinha do amor"). Nao existe caixa 7.
-- Sem textos das PRODUCTION 2016-2023, a caixa 8 so aparece a partir da primeira WIN63 (2025-12-10).
CREATE TABLE IF NOT EXISTS `catalog_gift_wrapping_types` (
  `kind` enum('box','ribbon') NOT NULL,
  `type_id` int(11) NOT NULL,
  `available_from` datetime NULL,
  `available_until` datetime NULL,
  PRIMARY KEY (`kind`, `type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

INSERT INTO `catalog_gift_wrapping_types` (`kind`, `type_id`, `available_from`, `available_until`) VALUES
('box', 0, NULL, NULL), ('box', 1, NULL, NULL), ('box', 2, NULL, NULL), ('box', 3, NULL, NULL),
('box', 4, NULL, NULL), ('box', 5, NULL, NULL), ('box', 6, NULL, NULL), ('box', 8, '2025-12-10 00:00:00', NULL),
('ribbon', 0, NULL, NULL), ('ribbon', 1, NULL, NULL), ('ribbon', 2, NULL, NULL), ('ribbon', 3, NULL, NULL),
('ribbon', 4, NULL, NULL), ('ribbon', 5, NULL, NULL), ('ribbon', 6, NULL, NULL), ('ribbon', 7, NULL, NULL),
('ribbon', 8, NULL, NULL), ('ribbon', 9, NULL, NULL), ('ribbon', 10, NULL, NULL)
ON DUPLICATE KEY UPDATE `available_from` = VALUES(`available_from`), `available_until` = VALUES(`available_until`);
