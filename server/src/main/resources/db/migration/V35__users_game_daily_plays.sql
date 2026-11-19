-- =========================================================================
-- V35: as partidas grátis do dia, por jogo do Game Center
--
-- O "Jogue agora! N" do Game Center (GAME_2_ACCOUNT_GAME_STATUS: freeGamesLeft; -1 = ilimitado) mostra quantas
-- partidas o usuário ainda tem HOJE naquele jogo: game.snowwar.free_games_daily_regular / _hc menos as já jogadas.
-- Com 0 o cliente troca o botão pela compra do HC. Antes o servidor contava só as partidas de SnowWar e da SEMANA
-- (users_snowwar_stats), então o Fast Food nunca descontava e o "diário" não virava.
-- Uma linha por usuário e jogo: o dia da última contagem e quantas naquele dia (de outro dia = 0).
-- Idempotente.
-- =========================================================================

CREATE TABLE IF NOT EXISTS `users_game_daily_plays` (
  `user_id` int(11) NOT NULL,
  `game_type_id` int(11) NOT NULL,
  `play_date` date NOT NULL,
  `plays` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`, `game_type_id`),
  CONSTRAINT `users_game_daily_plays_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
