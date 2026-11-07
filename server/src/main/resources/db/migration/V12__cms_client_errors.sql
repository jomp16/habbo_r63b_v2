-- =========================================================================
-- V12: tabelas do CMS (habbo-cms) para os erros e crashes dos clients
--
-- cms_sso_tickets: cada ticket SSO que o CMS emite (POST /api/client/ticket), com o usuário e a release.
--   O emulador apaga o users.auth_ticket no login; é por esta tabela que um relatório de crash
--   (POST /flash_client_error?ticket=…) diz quem estava jogando.
-- cms_client_errors: os crashes (kind = crash, do /flash_client_error) e os erros não fatais
--   (kind = error, FlashExternalInterface.logError) que a equipe vê na página "Erros" do CMS.
--   payload: tudo o que chegou (JSON), para os campos sem coluna própria.
-- =========================================================================

CREATE TABLE IF NOT EXISTS `cms_sso_tickets` (
  `ticket` varchar(64) NOT NULL,
  `user_id` int(11) NOT NULL,
  `release` varchar(191) NOT NULL DEFAULT '',
  `ip` varchar(64) NOT NULL DEFAULT '',
  `user_agent` varchar(512) NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ticket`),
  KEY `user_id` (`user_id`),
  KEY `created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

CREATE TABLE IF NOT EXISTS `cms_client_errors` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `user_id` int(11) DEFAULT NULL,
  `release` varchar(191) NOT NULL DEFAULT '',
  `kind` enum('crash','error') NOT NULL,
  `fatal` tinyint(1) NOT NULL DEFAULT 0,
  `error_desc` text NOT NULL,
  `error_cat` varchar(64) NOT NULL DEFAULT '',
  `error_ctx` varchar(255) NOT NULL DEFAULT '',
  `error_data` mediumtext NOT NULL,
  `debug` mediumtext NOT NULL,
  `in_room` varchar(64) NOT NULL DEFAULT '',
  `last_room` varchar(255) NOT NULL DEFAULT '',
  `sent_msg_data` text NOT NULL,
  `rece_msg_data` text NOT NULL,
  `flash_version` varchar(255) NOT NULL DEFAULT '',
  `agent` varchar(512) NOT NULL DEFAULT '',
  `ip` varchar(64) NOT NULL DEFAULT '',
  `user_agent` varchar(512) NOT NULL DEFAULT '',
  `payload` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `created_at` (`created_at`),
  KEY `user_id` (`user_id`),
  KEY `release` (`release`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
