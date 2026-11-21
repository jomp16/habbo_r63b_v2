-- =========================================================================
-- V39: `users_preferences.friend_bar_open` (boolean) vira `ui_flags` (bitmask)
--
-- O cliente moderno guarda UM int de flags de UI: bit 1 = barra de amigos aberta, bit 2 = room tools aberto, e reenvia o
-- int inteiro (MESSENGER_FRIEND_BAR_STATE) a cada mudança. Gravar `readInt() == 1` tratava `3` (barra + room tools)
-- como "barra fechada", e o login seguinte abria a barra recolhida. Os valores atuais (0/1) já são bitmasks válidos.
-- Idempotente.
-- =========================================================================

ALTER TABLE `users_preferences` ADD COLUMN IF NOT EXISTS `ui_flags` int(11) NOT NULL DEFAULT 1 AFTER `chat_color`;

UPDATE `users_preferences` SET `ui_flags` = `friend_bar_open`
WHERE EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users_preferences' AND COLUMN_NAME = 'friend_bar_open');

ALTER TABLE `users_preferences` DROP COLUMN IF EXISTS `friend_bar_open`;
