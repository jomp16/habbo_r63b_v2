-- =========================================================================
-- Correções nos headers de RELEASE63-201305221112-286302280 (V7), achadas ao comparar a build com as vizinhas
-- e confirmadas no bytecode AS3 do cliente (composers de membros de grupo: gêmeos de mesma forma no fio, trocados).
--
--   GROUP_ADD_ADMIN         137 -> 62    (62 = dar direitos de admin, chave 'group.members.giverights')
--   GROUP_REMOVE_ADMIN       62 -> 1318  (1318 = remover direitos, chave 'group.members.removerights')
--   GROUP_ACCEPT_MEMBERSHIP 3120 -> 137  (137 = aceitar pedido de entrada, chave 'group.members.accept';
--                                         3120 era o composer de expulsar, que não tem nome no emulador)
--   PET_INFO               1171 -> NULL  (1171 é um composer de estado de mobília, não o pedido de info do pet;
--                                         sem header provado para o pet: sem mapeamento)
--
-- A tabela tem UNIQUE (release_name, header): em duas fases, para nunca haver dois nomes no mesmo header (a chave
-- aceita vários NULL). Fase 1: os quatro viram NULL (só se ainda têm o header antigo). Fase 2: os novos valores.
-- =========================================================================

-- Fase 1
UPDATE `releases_incoming_headers`
SET `header` = NULL
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND ((`name` = 'GROUP_REMOVE_ADMIN' AND `header` = 62)
    OR (`name` = 'GROUP_ADD_ADMIN' AND `header` = 137)
    OR (`name` = 'GROUP_ACCEPT_MEMBERSHIP' AND `header` = 3120)
    OR (`name` = 'PET_INFO' AND `header` = 1171));

-- Fase 2 (PET_INFO fica NULL)
UPDATE `releases_incoming_headers`
SET `header` = 62
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `name` = 'GROUP_ADD_ADMIN'
  AND `header` IS NULL;

UPDATE `releases_incoming_headers`
SET `header` = 1318
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `name` = 'GROUP_REMOVE_ADMIN'
  AND `header` IS NULL;

UPDATE `releases_incoming_headers`
SET `header` = 137
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `name` = 'GROUP_ACCEPT_MEMBERSHIP'
  AND `header` IS NULL;
