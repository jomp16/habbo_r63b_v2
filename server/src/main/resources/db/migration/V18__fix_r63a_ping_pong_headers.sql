-- Corrige o mapeamento de Ping e Pong para o Flash Client R63A (pré-shuffle):
--
-- 1. Client -> Server:
--    - PongMessageComposer (resposta ao ping do servidor): header 196 em incoming_r63a.
--    - LatencyPingRequestMessageComposer (ping de latência periódico do client): header 315 em incoming_r63a.
--
-- 2. Server -> Client:
--    - PingMessageEvent (ping periódico/keep-alive do servidor): header 50 em outgoing_r63a.
--    - LatencyPingResponseMessageEvent (resposta com pingId ao ping de latência): header 354 em outgoing_r63a.

INSERT INTO `releases_incoming_headers_r63a` (`name`, `header`, `override_method`)
VALUES ('MISC_PONG', 196, NULL)
ON DUPLICATE KEY UPDATE `header` = 196;

INSERT INTO `releases_outgoing_headers_r63a` (`name`, `header`, `override_method`)
VALUES ('MISC_PING', 50, NULL)
ON DUPLICATE KEY UPDATE `header` = 50;

UPDATE `releases_outgoing_headers_r63a`
SET `header` = 354
WHERE `name` = 'MISC_PONG';
