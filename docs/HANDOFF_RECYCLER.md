# Handoff — Reciclador (Ecotron / Furni-Matic): implementação futura

Estado: **bloqueado por packet log** do client oficial (Furni-Matic 2025+). Não implementar temporadas/odds sem ele.
Escrito em 2026-11-13. Nada commitado.

## Premissa (do usuário)

- As odds **não estão chumbadas no client**: o servidor as envia no pacote da lista de prêmios
  (`CATALOG_RECYCLER_REWARDS`: `level` + `odds` int por nível). Confirmar o formato de 2025 (int "1 em N"? porcentagem?
  base 10000? quantos níveis?) **exige packet log do hotel oficial**, indisponível agora.
- Não derivar odds de fã-sites; eles são só pista.

## Estado atual do emulador (após a sessão de 2026-11-13)

- Lista de prêmios: `communication/outgoing/catalog/recycler/CatalogRecyclerRewardsResponse.kt`.
  - `odds` vêm de `config.yaml` `recycler.odds` (exemplo: `1:1, 2:25, 3:50, 4:75, 5:100`).
  - Nome/quantidade só `isVersionAtLeast(2012, 5, 2)`.
  - Corrigido em 2026-11-13: níveis resolvidos antes de escrever (`resolveLevels`); a contagem é a dos prêmios com
    `furnishing` (antes contava nomes inexistentes e desalinhava o buffer) e falta de odds falha antes do 1º byte.
- Sorteio: `game/catalog/RecyclerRewards.kt` (puro, testado em `RecyclerRewardsTest`).
  - Modelo "1 em N" de 2012/2014: do mais raro ao mais comum, cada nível acerta com `1/odds`, cai no nível 1. Pula
    níveis vazios e mobis inexistentes.
  - Chamado por `CatalogRecyclerService.getRandomRecyclerReward`.
- Reciclagem: `communication/incoming/catalog/recycler/CatalogRecycleItemsHandler.kt`.
  - Um só handler (`@Handler` + `@HandlerR63A`); slots por data em `game/catalog/RecyclerSlots.kt`: 5 fixo no
    client até `RELEASE63-201204132305` (const `= 5` no `RecyclerLogic`); a partir de `RELEASE63-201204162302` o
    client lê `catalog.recycler.number.of.slots` / depois `recycler.number_of_slots` (padrão 5) do
    `external_variables` → servidor usa `recycler.slots` do config, que DEVE ser igual ao valor servido.
  - O client só tem um código de falha (2 = alerta `${recycler.info.closed}`): quantidade errada aparece como "fechado".
  - Corrigido nesta sessão: ids distintos, do dono, `allowRecycle`, fora de troca (`HabboInventory.takeItemsExactly`);
    prêmio + caixa num `CreatedItems` (rollback); itens reciclados só são apagados após o sucesso e voltam se falhar;
    `recycler.open=false` recusa no servidor.
  - Sem taxa, sem cooldown.
  - Conquistas ligadas (regras puras em `game/catalog/RecyclerAchievements.kt`, teste `RecyclerAchievementsTest`; os 5
    grupos já existem no banco): `ACH_RecycledItems` (+N itens), `ACH_RecycleEcoprizes` (+itens reciclados que são
    prêmio), `ACH_RecyclerRarePrize` (nível >= 3) e `ACH_RecyclerGrandPrize` (maior nível povoado, se houver > 1) no
    sucesso do `CatalogRecycleItemsHandler`; `ACH_RecycleBoxOpener` (+1) no sucesso do `GiftOpener` para a caixa do
    reciclador. Raro/grande/ecoprize têm `TODO(2025)`: refazer o mapeamento nível → faixa com o packet log.
  - Dado do banco: `ACH_RecycledItems` nível 19 = 5000 < nível 18 = 7500 (igual à notícia oficial; não alterado).
- Caixa: `recycler.gift_box` (exemplo `matic_box`); abre por `GiftOpener` (caminho sem animação), mesmos ids.
- Status: `CatalogRecyclerStatusHandler` respeita `recycler.open` (`todo` de timeout).

## Fatos públicos por era (fã-sites, conferir)

| | 2012 | 2014 | 2025 |
|---|---|---|---|
| Prêmios | sci-fi (passarelas, holo trees, alien/frank/space duck, holo dragon) | CCTV, command chair, brain in a jar, duck vendor | `recycler_*` patchwork/vime + `clothing_recycler_patchlongscarf` + balão de fala |
| Odds | comum garantido, incomum 1:10, arcano 1:25 | idem | 62,23 / 25 / 10 / 2,5 / 0,25 / 0,02 % (6 níveis, soma 100) |
| Custo | 8 mobis | 8 mobis | 8 mobis + 10 duckets |
| Cooldown | ? | ? | 10 s clássico, 40 s Beta (anúncio prévio: 60 s) |
| Caixa | caixa Furni-Matic | idem | `matic_box` |

- Os prêmios **giram**: semanal em 2012. Em 2025 a coleção é trocada e os prêmios antigos deixam de ser recicláveis
  (afeta `ACH_RecycleEcoprizes`).
- Conquistas de 2025 (categoria "Explore o Hotel"), ausentes no emulador:
  - `ACH_RecycleBoxOpener1..10`: caixas abertas (1, 10, 25, 50, 100, 150, 250, 350, 600, 1000).
  - `ACH_RecyclerRarePrize1..10`: prêmio arcano ou superior (1, 3, 7, 12, 20, 30, 45, 60, 80, 100).
  - `ACH_RecycledItems1..20`: itens reciclados (5 … 10000; a notícia lista XIX = 5000, provável erro).
  - `ACH_RecycleEcoprizes1..10`: prêmios do reciclador reciclados de novo.
  - `ACH_RecyclerGrandPrize1..5`: prêmio do nível máximo (2025: lâmpada do dragão, 0,02%).

## Pistas do `external_variables` atual (servido)

- Base `gamedata/external_variables.txt`: `recycler.ducket_cost=10`, `recycler.enabled=true`,
  `recycler.number_of_slots=8`, `recycler.timeout_seconds=10` (taxa e cooldown de 2025 vêm do client config; o
  servidor precisa aplicar os mesmos valores).
- Override `gamedata/override_rwx/external_override_variables{,_fastfood}.txt` (2026-11-13):
  `catalog.recycler.number.of.slots=8` + `recycler.number_of_slots=8`. Local → servidor: ver a memória
  `swf-gamedata-location-and-sync`.
- O base já servia `recycler.number_of_slots=8`, então os clients de 2012-07+ já batiam com o servidor; só a
  `RELEASE63-201204162302` (que lê o nome antigo `catalog.recycler.number.of.slots`) dependia da linha nova. Os clients
  anteriores a 2012-04-16 têm 5 fixo e dependem só do emulador (`RecyclerSlots`).

## Ideia futura: o emulador ler o reciclador do `external_variables` (fonte única)

Hoje `recycler.slots/open/odds` ficam no `config.yaml` e podem divergir do que o client lê (foi a causa do alerta
"fechado"). O emulador já lê o `external_variables` em dois lugares, cada um com seu parser `chave=valor` inline:
`game/figure/FigureManager.resolveFigureDataUrl` (`external.figurepartlist.txt`) e
`game/habbicon/HabbiconAssetSynchronizer.syncAssets` (`habbicons.hash`, `habbicons.url`), ambos via
`habboConfig.externalVariablesTxt`.

Desenho sugerido (não implementado):
- Um `ExternalVariables` único (busca + parse + cache com recarga, ex. no `reload` do servidor), substituindo os dois
  parsers inline. Usar a URL **com o override** que os clients recebem (`external_variables.php` mescla
  `override_rwx/`), senão o servidor lê valores diferentes dos do client.
- Ler `recycler.number_of_slots` (fallback `catalog.recycler.number.of.slots`, depois padrão 5 = padrão do client),
  `recycler.enabled`, `recycler.ducket_cost`, `recycler.timeout_seconds`; o `config.yaml` vira só fallback quando a
  URL falhar.
- Atenção: o client lê UM `external_variables` por release (`MiscClientVariablesHandler` recebe a URL que o client
  usou); se algum dia releases diferentes receberem variables diferentes, a leitura precisa ser por URL/release, não
  global.

## O que capturar no packet log (hotel oficial, AIR/Flash 2025+)

1. Status do reciclador (aberto/fechado, campo de timeout/cooldown?).
2. Lista de prêmios completa: níveis, valor bruto de `odds` por nível, tipo de produto (mobi / visual / balão), nomes,
   quantidade.
3. Pedido de reciclagem (quantidade de ids, campos extras) e resposta (resultado, id da caixa, códigos de
   cooldown/erro ao reciclar dentro do cooldown ou sem duckets).
4. Abertura da `matic_box`: pacote e campos do prêmio revelado (visual/balão não são mobi).
5. Saldo após reciclar (taxa de 10 duckets).

## Desenho de implementação (depois do log)

- Dado por era/temporada, no padrão de `activity_point_types` (`available_from/until`): tabela (migration Flyway)
  `recycler_seasons` (prêmios, modelo de odds, taxa, cooldown, slots, caixa) + prêmios por nível e tipo. Escolha pela
  data da release (`isVersionBetween`). Substitui `recycler.odds/slots/gift_box` do `config.yaml`.
- Sorteio por modelo: manter `drawLevel` "1 em N" para 2012/2014; adicionar o modelo ponderado/percentual de 2025
  (puro + teste com seed, ex.: 1M sorteios dentro da tolerância).
- Taxa via `CurrencyService.tryCharge` (duckets, `pointType` da temporada), estorno no rollback do `CreatedItems`.
  Cooldown por usuário no servidor.
- Prêmios não-mobi (visual/balão): o modelo de conteúdo da caixa (`items_gift_contents`) só entrega itens; precisa de
  um tipo de conteúdo novo.
- Conquistas: já ligadas; resolver os `TODO(2025)` de `RecyclerAchievements.kt` (faixa arcano+ e mítica) com as
  odds/níveis reais do log.
- Slots já conferidos no AS3 (ver acima); conferir os campos dos pacotes de 2025 no AS3 (skill `timeline-as3`) antes
  de mudar o wire.
