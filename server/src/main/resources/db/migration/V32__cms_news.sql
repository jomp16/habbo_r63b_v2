-- =========================================================================
-- V32: tabela do CMS (habbo-cms) para as Notícias do hotel
--
-- cms_news: uma notícia (título, resumo, corpo em Markdown restrito que o CMS renderiza com lista branca, imagem de
--   capa opcional). published_at NULL = rascunho (só a equipe vê); no futuro = agendada (aparece sozinha quando a hora
--   chega); author_id fica NULL se o autor for apagado (a notícia continua). Os horários são gravados pelo CMS (UTC).
-- O client pede as notícias ao site: FlashExternalInterface.openHabblet("news") (2009 →) e openNews()/closeNews()
--   (2015 →), ao sair de um quarto (news.auto_popup.enabled), no :news do chat (client.news.embed.enabled), e fecha ao
--   entrar num quarto. Nada disso passa pelo emulador: aqui fica só a tabela.
-- Idempotente.
-- =========================================================================

CREATE TABLE IF NOT EXISTS `cms_news` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(150) NOT NULL DEFAULT '',
  `summary` varchar(500) NOT NULL DEFAULT '',
  `body` mediumtext NOT NULL,
  `image_url` varchar(500) NOT NULL DEFAULT '',
  `author_id` int(11) DEFAULT NULL,
  `published_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `published_at` (`published_at`),
  KEY `author_id` (`author_id`),
  CONSTRAINT `cms_news_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
