# Handoff — micro-otimização e stress do emulador (para uma sessão limpa)

Estado: TUDO ABAIXO ESTÁ **NÃO COMMITADO** (emulador `habbo_r63b_v2` e SDK `habbo_client_sdk`). Nada foi commitado; o usuário ainda não decidiu.

## Objetivo em curso
Escalar o emulador para muitos usuários numa VPS fraca, validando que o tick de sala respeita 500 ms. "VPS fraca" = emulador fixado em 2 núcleos / 1 GB de heap (`CPUS=0-1`, `HEAP=1g`); o notebook do usuário (12 núcleos, 15,6 GB) NÃO é fraco.

## O que já foi feito (resumo)
- JDK 25 / Kotlin 2.4.20; ClassGraph removido (índice de classes gerado no build: `GenerateClassIndexTask`, `GeneratePluginDescriptorTask`, `ClassIndex.kt`). `reload_handlers` continua funcionando.
- Salas virtuais em memória (`virtual_rooms` no config; ids 2_100_000_001+; nunca persistidas; aparecem no navigator). Bots do SDK usam sessões 2_000_000_000+ marcadas `ephemeral` (write-behind nunca grava).
- Performance: RC4 próprio com decifragem in-place, RSA com CRT (p,q recuperados de N,D,E no boot) e cache de assinaturas, handshake em `handshakeDispatcher`, outbox por sessão drenado uma vez por tick (`OutboxBatch`), broadcast com payload compartilhado (`HabboResponse.shared`), `UserChatTask`/`RoomNetworkDispatcher` com menos alocação, `ServerMetrics` com `LongAdder`, `debug` não monta string por pacote.
- Bugs achados: DH secret com BigInteger com sinal (1/256 handshakes) no SDK; awaiter de login registrado depois do envio; corrida na partida da sala (usuários descartados); backlog ilimitado da fila de tarefas do Netty.
- Resultados (12 núcleos): 5000 bots (10x500, 1 ação/s) intervalo médio 615→504 ms, pior 2732→539 ms, Full GC 4→0, RSS 5,3→2,8 GB, alocação 912→266 MB/s. Escada L5 (200/300/500 por sala): 0 janelas >600 ms, 0 falhas.
- VPS simulada (CPUS=0-1, heap 1 GB, 100 salas, 0,2 ações/s, rampa 80–100/s): 1000–3000 usuários perfeitos; 4000: só o balde final de desconexão em massa teve janelas ruins.
- Escada V2 (6000/8000/10000 usuários, 200 s, RAMP=100): 
  - 6000: 0 falhas, RSS 1319 MB, 101/2300 janelas >600 ms (máx 711 ms), intervalo médio 503,9 ms.
  - 8000: 0 falhas, RSS 1331 MB, 133/2577 janelas >600 ms (máx 678 ms), intervalo médio 505,3 ms.
  - 10000: **pendente** no momento do handoff (ver `build/stress-runs` ou o scratchpad `V2_100`). Falta confirmar se as janelas >600 ms estão só no burst de desconexão final ou também no regime.

### Rodadas posteriores (VPS simulada 2 cores / 1 GB, janelas >600 ms em 10 s; 0 falhas em todas)
| Usuários | V2 (antes) | V3 | V4 (repetição de V3) | ZGC (Z1) |
|---|---|---|---|---|
| 6000 | 101/2300 | 12/2300 | 61/2300 | 1/2300 |
| 8000 | 133/2577 | 172/2500 | 0/2501 | 124/2500 |
| 10000 | 125/2693 | 0/2701 | 67/2700 | 166/2700 (máx 743 ms) |
Conclusões: a variação entre rodadas do mesmo coletor supera a diferença entre G1 e ZGC e entre V2 e V3; as mudanças de `handshakeDispatcher` (mín. 1) e das flags de GC não têm efeito comprovado (mantidas por serem coerentes). Os baldes ruins concentram-se em poucos instantes que atingem muitas salas ao mesmo tempo (paradas globais) e no burst de desconexão do fim. Padrão continua G1. O emulador sustenta 10000 usuários em 2 vCPU usando ~0,8 core e ~1,4 GB RSS.
Diagnóstico com JFR de 185 s (rodada J1, 10000 usuários, G1): os baldes de 10 s em que quase todas as salas estouram 600 ms coincidem com pausas do Young GC ≥ ~64 ms (pausas crescem de 20 para 76 ms ao longo do teste; pico de 265 ms no início); pausas ≤ 53 ms quase não geram janelas lentas; não houve sincronização de safepoint longa. Teste `GC=g1low` (MaxGCPauseMillis=30): 230 young GCs de ~30 ms, mas PIOR (intervalo médio 517,9 ms, 247/2639 janelas >600 ms) — GC mais frequente consome CPU nos 2 núcleos. ZGC também sem vantagem. Logo, pausa de GC é parte da causa, mas encurtar pausas via flags não resolve; o caminho é reduzir a taxa de alocação/live-set por tick (A*, ServerMetrics, HabboResponse) ou dar mais heap/núcleos. Obs.: scale_run.sh aceita `JFR_SECONDS` e o perfil `g1low` existe só como experimento negativo.
Micro-otimizações de alocação aplicadas depois (todas com testes; NENHUMA moveu a cauda do tick): `Utils.round` sem BigDecimal (UtilsRoundTest compara com a versão exata em 500 mil valores), `RoomGamemap.isBlocked(x,y)` sem Vector2 no hot path do A*, A* com array de registros reutilizado por thread (`AStarFinder`), `OutboxBatch` com estado reutilizado por thread, `ServerMetrics.roomTickReservoir` (sem boxing de Int por tick). Rodadas M1–M3 (10000 usuários, 1 GB): 314–416/2700 janelas >600 ms; JFR de alocação total NÃO serve para comparar (mistura a rampa de handshakes com o regime; a atribuição de 35% a `delay` do loop é implausível em volume). Heap 2 GB (H2): 10000 → 165/2700, 8000 → 121/2500 (máx 758 ms), RSS ~2 GB; ganho pequeno e dentro do ruído entre rodadas.
Estado: o emulador sustenta 10000 usuários em 2 vCPU sem falhas, intervalo médio 503–512 ms, mas 0–15% das janelas de 10 s têm algum tick >600 ms; a causa não foi isolada (GC contribui mas flags/coletor/heap não resolvem). Ideias não testadas: loop da sala em ScheduledThreadPool dedicado em vez de `delay` de coroutine no `cachedExecutorDispatcher`; custo do burst de desconexão; mais núcleos.
Próximo passo útil: investigar o burst de desconexão em massa e a latência de agendamento (sched-lag), depois o A*.

### VPS de 4 vCPU / heap 4 GB (B4, bots sintéticos, 0 falhas)
| Usuários | Intervalo médio | Pior janela | Janelas >600 ms | RSS |
|---|---|---|---|---|
| 10000 | 502,2 ms | 573 ms | 0/2400 | 1537 MB |
| 15000 | 505,4 ms | 620 ms | 7/2700 | 3383 MB |
| 20000 | 511,2 ms | 648 ms | 73/3000 | 3546 MB |
Heap vivo (pós-GC): ≈ 0,18 GB + 20 KB × usuários (10k: ~375 MB, 15k: ~457 MB, 20k: ~570 MB). Fórmulas de dimensionamento: ver resposta na conversa (vCPU = max(2, ceil(U/2500)); heap = max(1 GB, 3×vivo); RAM = heap + 1 GB + banco); margem para DB/cliente real é HEURÍSTICA, não medida.

## Teste de write-behind com banco real descartável (Docker)
- ATENÇÃO: o `config.yaml` aponta para o banco REMOTO de prod; as rodadas de stress anteriores (bots efêmeros, só leitura de catálogo) conectaram nele. Para testes com escrita use SEMPRE o banco local abaixo.
- `scripts/stress/db/dump_baseline.sh` (uma vez; somente leitura em prod): estrutura de todas as tabelas + dados só das estáticas (catálogo, furnishings, modelos, headers, achievements...) → `build/stress-db/baseline.sql.gz`. Nenhum dado de jogador sai de prod.
- `scripts/stress/db/db.sh up|down|jvm-props|sql` (container `habbo-stress-db`, `mariadb:13.0` = versão de prod, datadir em tmpfs, porta 33306); `seed_users.sh N` cria usuários `dbbot<i>` com ticket `dbt-<i>`; `run_db_test.sh N [seg]` faz tudo (banco novo → usuários → emulador → bots → confere o banco).
- O emulador aceita `-Dhabbo.db.host/port/name/user/password` (override do `DatabaseConfig`); `run-stress-server.sh` repassa via `EXTRA_JVM`.
- Validado com 100 bots (login real): 0 falhas, 0 erros, `online`→0 após logout, `online_seconds`, badges/achievements/currencies gravados.
- Log de stress do write-behind/banco (`WriteBehindStats` + `WriteBehindStressLog`, linhas `[write-behind]` a cada 10 s em `stress-test.log` com `stress_test: true`): escritas feitas/falhas, fila pendente (atual/pico), simultâneas, espera na fila (0–2000 ms é normal: o worker drena a cada 2 s), execução, ciclos de flush, entidades sujas, queries do banco e pool Hikari.
- Cenário `player` do SDK (feito pelo subagente Haiku; não commitado): `CPUS=0-3 ACTIONS=1 SDK_SCENARIO=player SDK_ENV="SCENARIO_VERBOSE=true" scripts/stress/db/run_db_test.sh 20 90` (compra no catálogo, inventário, criar quarto, colocar/mover/pegar item, andar/falar). Validação independente: 20 bots/90 s → 121 compras, 0 falhas inesperadas, quartos criados = linhas em `rooms` (54), ids das compras ⊂ `items`.
- CORRIGIDO (validado, não commitado): comprar teletransporte gravava o DOBRO de itens (esperado: 1 comprado = 2 itens ligados em par). Correção: `ItemDao.addItems` cria o parceiro (`addTeleportPartner`) e o devolve no fim da lista; `CatalogPurchaseService` não duplica mais a entrada. Validação: 20 bots, 16 teletransportes comprados → 32 itens, 32 linhas em `items_teleport` (16 pares, os dois sentidos), cliente 489 = banco 489 itens. Descrição original do bug: `CatalogPurchaseService.buildPurchaseDataList` duplica a entrada (`result += result.last()`, 2 pontos) E `ItemDao.addSpecialItemData` (caso TELEPORT) cria o par de cada item → 4 linhas e 2 pares em `items_teleport` por unidade comprada, enquanto o `PurchaseOK` informa 2. Evidência: 20 bots, cliente 384 itens vs banco 398 (+14 = exatamente os teletransportes). Corrigir mantendo UM dos dois mecanismos.
- Testes do par de teletransporte (142 testes na suíte, 0 falhas): `NewItemPlanTest` (regra pura `planNewItems`, sem banco), `CatalogPurchaseDataListTest` (a compra gera 1 entrada por unidade; mutação que reintroduz a duplicação faz 2 de 4 falharem) e `ItemPersistenceContainerTest` (Testcontainers `mariadb:13.0`; carrega o V1 como "banco existente" e roda o `FlywayMigration` real; grava itens e pares com o SQL do `ItemDao`; PULADO sem Docker). Deps: `org.testcontainers:testcontainers(-mariadb):2.0.2`.
- ACHADO: o `V1__initial_schema.sql` (dump) NÃO cria um banco vazio — sem `SET FOREIGN_KEY_CHECKS=0` (FKs para tabelas ainda inexistentes) e traz a própria `flyway_schema_history`. Produção só funciona porque parte de um banco existente (baseline na versão 1). Instalação do zero exige o dump; não alterei o V1 (mudaria o checksum de quem já o aplicou).
- Próximo: estender o SDK (abrir catálogo → página aleatória do índice → comprar 1..n móveis → inventário/colocar) e conferir `items`/`users_currencies` no banco; medir fila/latência do write-behind sob carga.

## Como rodar
1. `./run-stress-server.sh start` (variáveis `CPUS`, `HEAP`, `GC`, `BUILD`; pid em `.stress-server.pid`; `./run-stress-server.sh stop`). Usa `config.yaml` com `stress_test: true`, `virtual_rooms.enabled: true`, `debug: false`.
2. Um degrau: `ROOMS=100 ACTIONS=0.2 RAMP=100 CLIENT_HEAP=3g PIN=1 EMU_CPUS=0-1 EMU_RESTORE=0-1 CLIENT_CPUS=4-11 scripts/stress/scale_run.sh <dir> <botsPorSala> <segundos>`.
3. Escada: `ROOMS=100 ACTIONS=0.2 RAMP=100 RUN=200 ... scripts/stress/ladder.sh V2 60 80 100` (saída em `build/stress-runs/`).
4. Análise: `python3 -I scripts/stress/analyze.py <dir> [--jfr]` (lê `stress-test.log`, `gc.log*`, JFR).
5. Cenário do SDK direto: `cd ../habbo_client_sdk && ./run.sh hotel` (env `RUN_SECONDS`, `CHURN_SECONDS`, `ACTIONS_PER_SEC`, `SCENARIO_VIRTUAL_ROOMS`, `SCENARIO_BOTS_PER_ROOM`, `SCENARIO_RAMP_UP_PER_SECOND`; o `run.sh` sobrescreve `SCENARIO_RUN_SECONDS` etc.).
- A guarda de memória aborta os bots se `MemAvailable` < 700 MB.

## Restrições (do usuário)
- NUNCA editar arquivos com python/sed/scripts: só Edit/Write.
- Nunca mexer no banco manualmente nem ler `sql_dump/database.sql`; `FlywayRepairTest` está `@Ignore` de propósito (batia no banco real).
- Não usar `pgrep -f` com padrão presente na própria linha de comando (use o pid file).
- `config.yaml` local foi alterado (gitignored): `virtual_rooms.count` 100 (original 10), `stress_test: true`, `debug: false` — reverter conforme preferência.

## Candidatos a próxima otimização
1. A* (~16–20% da CPU em sala pequena): `AStarFinder` com `HashMap<Int,NodeRecord>`, `getNeighbors` alocando `ArrayList`/`Vector2` e lookups em `ConcurrentHashMap` — submódulo `pathfinding` (tem `GridTest`).
2. ~~`handshakeDispatcher` mín. 2~~ FEITO (agora `max(1, cores/3)`; falta rebuild + medir na escada V2 após o V2_100 terminar).
3. Custo do burst de desconexão em massa (`UserPartRoomTask`, fechamento de sessão).
4. ~~`ParallelGCThreads=4` fixo~~ FEITO (flags removidas do perfil g1; a JVM dimensiona pelos núcleos visíveis).
5. `DefaultChannelPromise` por escrita; alocações em `ServerMetrics.recordHandler` e `HabboResponse.<init>`; A/B com ZGC (pausas G1 Young ~25–45 ms não são o gargalo).
6. Recomendado antes de commitar: smoke test com cliente real (RC4/encoder/outbox tocam todo pacote).

## Estado dos repositórios
- Emulador: ~43 arquivos modificados/novos (build-logic, server, launcher/log4j2.xml, AGENTS.md, config.example.yaml, run-stress-server.sh, scripts/stress/, docs/). Suíte: 124 testes, 1 ignorado.
- Submódulo `plugin_manager`: `PluginInfo.entryPoints` e carregamento por entryPoints (repo separado, não commitado).
- SDK: `SharedEventLoop.kt`, `HotelScenario.kt` novos + 10 modificados (ClientOptions, ImplRoom, ImplHabboClient, HabboClientDecoder, ImplClientSession, ClientCrypto, Main, StressConfig, Bot, Reporter).
