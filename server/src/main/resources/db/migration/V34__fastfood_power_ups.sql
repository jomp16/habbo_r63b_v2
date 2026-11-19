-- =========================================================================
-- V34: os turbos do Fast Food (BaseJump): estoque por usuário e ofertas da loja do lobby
--
-- fastfood_power_ups: quantos turbos de cada tipo o usuário tem (type = o nome do cliente, §_-DQ§: missile,
--   bigparachute, shield). Uma partida libera até 3 de cada ("Podem ser usados TRÊS ... por jogo"), e cada turbo
--   usado sai do estoque. Quem joga pela primeira vez ganha 3 mísseis e 5 escudos (as linhas nascem então, inclusive
--   com 0: a presença delas marca que o presente já foi dado).
-- fastfood_power_up_offers: as ofertas do "Comprar mais" (POWER_UPS_PRICE: id, tipo, quantidade, preço em
--   créditos); o cliente mostra lado a lado as ofertas de um tipo e compra pelo id. Valores da época: 25 mísseis por
--   1 crédito, 15 paraquedas grandes por 1, 5 escudos por 5.
-- Idempotente.
-- =========================================================================

CREATE TABLE IF NOT EXISTS `fastfood_power_ups` (
  `user_id` int(11) NOT NULL,
  `type` varchar(16) NOT NULL,
  `amount` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`, `type`),
  CONSTRAINT `fastfood_power_ups_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

CREATE TABLE IF NOT EXISTS `fastfood_power_up_offers` (
  `id` varchar(32) NOT NULL,
  `type` varchar(16) NOT NULL,
  `amount` int(11) NOT NULL,
  `price_credits` int(11) NOT NULL,
  `position` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

INSERT INTO `fastfood_power_up_offers` (`id`, `type`, `amount`, `price_credits`, `position`)
VALUES ('missile_25', 'missile', 25, 1, 0),
       ('bigparachute_15', 'bigparachute', 15, 1, 0),
       ('shield_5', 'shield', 5, 5, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
