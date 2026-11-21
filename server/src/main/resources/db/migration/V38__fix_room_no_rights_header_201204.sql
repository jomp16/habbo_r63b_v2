-- =========================================================================
-- V38: ROOM_NO_RIGHTS (YouAreNotController) da RELEASE63-201204021456 sai do header 373 e vai para o 2860
--
-- A V17 gravou 373, mas no AS3 dessa release `Incoming[373]` (_-3tN) é POLL_ERROR: o cliente abria o alerta
-- "Erro! ???" (win_error) ao entrar em qualquer sala. O 373 e o 2860 têm o mesmo hash (colisão no Messages.txt); o
-- 2860 (_-Xv) é o YouAreNotController, cujo handler zera o nível de controle da sessão.
-- Idempotente.
-- =========================================================================

UPDATE `releases_outgoing_headers`
SET `header` = 2860
WHERE `release_name` = 'RELEASE63-201204021456-654375178'
  AND `name` = 'ROOM_NO_RIGHTS';
