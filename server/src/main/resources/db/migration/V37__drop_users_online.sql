-- =========================================================================
-- V37: sai a coluna `users.online`
--
-- Quem está no hotel é estado em memória do emulador (HabboSessionManager.onlineUserIds), exposto em
-- GET /api/v1/status/online. O flag gravado no banco ficava preso em 1 quando o servidor era desligado (deploy) ou
-- caía com gente conectada, e a CMS mostrava "no hotel agora" para quem não estava. O emulador não lê nem grava mais a
-- coluna e a CMS consulta a API.
-- Idempotente.
-- =========================================================================

ALTER TABLE `users` DROP COLUMN IF EXISTS `online`;
