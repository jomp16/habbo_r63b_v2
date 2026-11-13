# Multi-currency (activity points)

## Modelo

- **Preço de oferta** = credits + **uma** moeda alternativa (tipo + quantidade). É o que o client lê
  (`priceInCredits`, `priceInActivityPoints`, `activityPointType`); não existem duas moedas alternativas no mesmo item.
  `catalog_items.price_points` + `point_type` (V25). `priceInSilver` (AIR 2024+) é campo à parte: o emulador manda 0.
- **Saldo** = `UserInformation.activityPointsCurrencies` (`UserCurrencies`, chave = código do tipo). Cada alteração suja
  só o tipo mexido e o write-behind grava apenas esses; `users_currencies` tem `UNIQUE(user_id, type)`.
- **Cobrança** = `CurrencyService.check/charge` (catálogo, pets, club, câmera, habbicon). Não mexer em saldo direto.
- **Tipos e janelas por release** = tabela `activity_point_types` (`available_from/until`, `enabled`), lida por
  `ActivityPointTypes`. O pacote `ACTIVITY_POINTS_BALANCE` e as ofertas do catálogo só levam tipos que o client daquela
  release entende; oferta com moeda não suportada some do catálogo e a compra é recusada. Pixels (0) vale sempre.
- Reward periódico de outras moedas: `reward.extra_currencies` no `config.yaml`.

## O que o AS3 prova (1349 releases, 2009-06 a 2026-11)

O client monta as chaves de texto **pelo tipo** (`"catalog.purchase.youractivitypoints." + tipo`), então é genérico; o que
muda por release é a família de textos que ele referencia:

| Builds | Releases | Comportamento |
|---|---|---|
| 200906161327 – 201011120041 | 122 | só `catalog.alert.notenough.credits`: sem moeda alternativa na UI do catálogo |
| 201011160241 – 201201111103 | ~200 | `catalog.purchase.price(.credits_and)_activitypoints.<tipo>` + `youractivitypoints`: textos só existem p/ tipos 0, 1, 2, 4 |
| 201201130937 – 202611041140 | ~1030 | só `youractivitypoints` + `notenough.activitypoints`: qualquer tipo com texto (1, 2, 4, 5, 101, 103, 104, 105) |

A oferta só carrega o tipo a partir de 2010-12-03 (antes é sempre pixels).

## Quando cada tipo existiu (flash texts originais da Sulake)

O início de cada tipo em `activity_point_types` é o do primeiro snapshot de texto original que o traz (limite superior;
entre parênteses o snapshot anterior, sem o tipo): 1 = 2010-12-03 (2010-11-16), 2 = 2011-02-04 (2010-12-03),
3 e 4 = 2011-08-30 (2011-02-04), 101 e 102 = 2012-04-02 (2011-12-13), 103 = 2012-07-04 (2012-04-02),
5, 104 e 105 = 2013-05-22 (2012-07-04). Os tipos 3 (GIFT_POINTS) e 102 (NUTS) existiram de fato (têm textos
oficiais de saldo insuficiente/preço); só o `youractivitypoints.102` nunca existiu no original.

## Pacotes (deals)

Uma oferta com `catalog_items.deal_id` entrega vários mobis de uma vez. Antes da V26, `catalog_deals` era uma linha por
mobi (o mesmo que `catalog_items.amount`) e o DAO esperava listas "a,b" que a FK impedia: um pacote de vários mobis não
existia. Agora (V26): `catalog_deals (id, name)` e `catalog_deal_items (deal_id, position, item_name, amount)`, na ordem
em que os produtos saem na oferta. O emulador lê `CatalogItem.dealProducts` (ordenado; mobi que não existe mais fica
de fora) e a oferta sem nenhum mobi válido some do catálogo. Comprar `N` pacotes entrega `quantidade × N` de cada produto.
`catalog_items.deal_id` é `ON DELETE RESTRICT`: um pacote em uso não se apaga. O CMS cria e edita os pacotes.

## Textos

O `external_flash_texts.php` mescla o texto base com `gamedata/override_rwx/external_flash_override_texts.txt`.
Ali estão as chaves que faltavam no texto base: tipo 3 (notenough, preço, saldo, notificação),
`catalog.purchase.confirmation.dialog.price.(credits_and_)activitypoints.<tipo>` e `youractivitypoints.102`.
Atenção: o client lê `credits_and_activitypoints`; as linhas `credits+activitypoints` antigas do override não são
usadas por nenhum client.

Cada tipo precisa de ícone `icon_activity_point_<tipo>` nos assets do client (sem ele o client só não mostra o ícone).
