---
name: habbo-feature-implement
description: "Playbook para implementar uma feature nova de pacotes Habbo no emulador habbo_r63b_v2 (ex: Habbicons, Chests, novas mobílias Wired). Use quando for adicionar handlers/responses novos, headers multi-release por migration, manager/DAO de game feature, ou interactor/FurnitureLogic para mobília nova — do RE do AS3 até o build verde."
---

# Habbo Feature Implement — de pacote novo a feature completa

Playbook para implementar uma feature de rede nova no emulador. Para busca/RE de pacotes
individuais (achar header por classe ofuscada, structure-diff, corrigir dessincronização),
use a skill **`habbo-re`** primeiro — esta skill assume os pacotes já identificados.

## Projetos e artefatos

| Artefato | Caminho |
|---|---|
| Emulador (alvo) | `~/IdeaProjects/Habbo/habbo_r63b_v2` |
| Timeline & RE Global | `~/IdeaProjects/Habbo/habbo-timeline` (`habbo-timeline <PACOTE>`, 1.330+ releases 2009-2026, gerador de Kotlin) |
| Patcher (headers, caches Sulek) | `~/IdeaProjects/Habbo/habboair-patcher` |
| Caches Sulek por release | `habboair-patcher/cache/flash-{windows|osx}-<RELEASE>.json` |
| Gerador de Migrations | `habbo_r63b_v2/.agents/skills/habbo-feature-implement/generate_headers.py` |
| AS3 decompilado (Fonte Primária) | `/mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/Habbo_codigo_completo.txt` |
| SWFs & Scripts | `~/Downloads/HabboWin/Habbo_SWF_Collection/<RELEASE>/HabboAir.cracked_scripts.txt` |
| External texts | `curl -sL https://www.habbo.com.br/gamedata/external_flash_texts/0` (3.7 MB — sempre grep, nunca carregar inteiro) |
| Migrations de headers de referência | `habbo_r63b_v2/migrations/006_habbicon_headers.sql`, `009_chest_headers.sql` |

## Direções (o bug #1 — sempre re-verificar)

```
Banco releases_incoming_headers  = client->server  = Sulek cache .messages.outgoing (Composers)
Banco releases_outgoing_headers  = server->client  = Sulek cache .messages.incoming  (Events)
```

Inverter isso faz um header colidir com outro pacote real na mesma tabela
(unique key `(release_name, header)`). O `HeaderMapper` do patcher e o
`release.sh search-class` usam essa convenção — é a prova de verdade.

## Fase 1 — RE no AS3 (estrutura dos pacotes)

1. **Estrutura e Nomes com `habbo-timeline` (Recomendado)**:
   Execute o `habbo-timeline` para o evento/composer alvo:
   ```bash
   habbo-timeline NomeDoMessageEvent
   # ou
   habbo-timeline NomeDoMessageComposer
   ```
   Isso extrai a sequência de tipos (`Int`, `String`, `Boolean`, `Long`), desofusca os nomes reais de variáveis analisando getters, logs e construtores DTOs, mapeia delegações (ex: `ItemDataParser`, `parseObjectData`) e gera o esqueleto de código Kotlin pronto.

2. **Inspeção no AS3 decompilado (`HabboAir.cracked_scripts.txt` ou `habbo-timeline inspect`)**:
   - **Events** (server→client): classe do evento `extends MessageEvent` → parser `IMessageParser`.
     A ordem dos `read*` no `parse()` = ordem no wire. Getters do parser = nomes dos campos.
   - **Composers** (client→server): `implements IMessageComposer`; os `push()` do construtor
     = campos que o **handler do emulador lê, nessa ordem**. `new _SafeStr_XXXX(a, b, c)`
     no código de uso do client mostra o significado de cada argumento.
3. Ache quem **envia** e quem **escuta** cada mensagem (ex: `WiredChestController` registra
   os events e dispara os composers) — isso define o fluxo da feature, não só os pacotes.
4. Dados derivados do **modelo do objeto** (ex: `object.getModel().getNumber("furniture_...")`)
   indicam chaves que o servidor precisa mandar dentro do stuff data / serialização do item.
5. Constantes do parser (ex: `SUCCESS = 0`) mapeiam os result codes que o emulador devolve.
6. External texts (`wiredchests.*`, `notification.*`) revelam regras de negócio, limites e
   códigos de erro nomeados (`wiredchests.upgrade.result.error.N`).
7. **Defaults reais no AS3**: Para novos campos persistíveis, inspecione a declaração dos atributos privados da classe DTO no AS3 (`private var _field:Type = default;`). Mocks estáticos em handlers/responses antigos não representam o default real do protocolo.
8. **Mapeamento de UI e Inversões**: Inspecione o controller de interface correspondente (`*Ctrl.as`). Verifique se checkboxes têm prefixo negativo (`_doNot...`, `_disable...`) que invertem o valor na atribuição do composer (`data.featureEnabled = !checkBox.isSelected`). Garanta que o default adotado na migration preserve o comportamento histórico esperado do Habbo.

## Fase 2 — Headers multi-release

**O emulador não sobe se faltar header para qualquer release** (`HabboHandler.isMissing...Headers`
→ `exitProcess(1)`). Todo enum novo em `Incoming.kt`/`Outgoing.kt` precisa de linha na
tabela para TODAS as releases.

Gere a migration usando o script incluído nesta skill (`generate_headers.py`):

```bash
python3 .agents/skills/habbo-feature-implement/generate_headers.py \
    --feature wired_chest \
    --incoming CHEST_OPEN_GET_CONTENTS=OpenChestAndGetContentsMessageComposer \
    --incoming CHEST_CLOSE=CloseChestMessageComposer \
    --outgoing CHEST_OPEN=OpenChestMessageEvent \
    --incoming-r63a ROOM_BAN_USER=320 \
    --patcher-dir ../habboair-patcher \
    --releases-from-config \
    -o server/src/main/resources/db/migration/V{N}__feature_headers.sql
```

Regras do gerador de migration:
1. Lista de releases = `SELECT release_name FROM releases` (ou do `releases_config.json`).
   No cache Sulek o nome tem prefixo de plataforma: `windows-WIN63-...`/`osx-MAC63-...` — o script remove
   o prefixo antes de casar com o banco.
2. Para cada release, extrai os IDs **só se TODOS os pacotes da feature estiverem nomeados
   no cache** — release sem a feature (releases antigas) fica com `NULL` (nunca invente ID:
   um header NULL é inofensivo; um ID errado colide com outro pacote real).
3. Estrutura da migration (idempotente e sem warnings):
   - Releases: `INSERT ... ON DUPLICATE KEY UPDATE release_name = VALUES(release_name)` (evita os avisos de warning 1062 no MariaDB/Flyway gerados por `INSERT IGNORE`).
   - NULLs: `INSERT IGNORE ... SELECT ... WHERE NOT EXISTS`.
   - Concretos: Agrupados em um único batch `INSERT ... ON DUPLICATE KEY UPDATE \`header\` = VALUES(\`header\`)` por tabela (conciso, legível).
   - R63A: Headers pré-shuffle são gerados automaticamente se detectados ou passados via `--incoming-r63a`/`--outgoing-r63a` (ou sufixo `:R63A_ID`).
   - Sem SELECTs comentados no arquivo final (migração limpa).
4. Entenda o ciclo de vida do NULL: `ReleaseDao` converte `NULL → -1`; `-1` nunca despacha;
   `HabboResponse` para header inexistente loga e não envia. Ou seja: `NULL` = feature
   silenciosamente desligada naquela release.

## Fase 3 — Implementação no emulador

Layout (replicar o commit do Habbicon `c61a8f3`):

```
communication/incoming/Incoming.kt     (+ enums, ordem alfabética)
communication/outgoing/Outgoing.kt     (+ enums)
communication/incoming/<feature>/      1 ARQUIVO POR HEADER (XxxHandler.kt, @Handler)
communication/outgoing/<feature>/      1 ARQUIVO POR HEADER (XxxResponse.kt, @Response)
game/<feature>/FeatureManager.kt       lógica de negócio + estado em memória
game/<feature>/FeatureData.kt          data classes do domínio
database/<feature>/FeatureDao.kt       Kwery (sql/resources/sql/<feature>/*.sql)
migrations/NNN_create_feature.sql      tabelas + achievements
migrations/NNN_feature_headers.sql     headers multi-release
game/HabboGame.kt                      + val featureManager = FeatureManager()
```

Regras do código:

- Handler: `@Handler(Incoming.XXX)` → `fun handle(habboSession, habboRequest)`; leia na
  ordem do composer do AS3 (ou template do `habbo-timeline`); delegue ao manager; **sem lógica de negócio no handler**.
- Response: `@Response(Outgoing.XXX)` → `fun response(habboResponse, ...)`: escreva na
  ordem do parser do AS3. Use o gerador do `habbo-timeline` para obter o esqueleto de `write*` com os nomes de variáveis reais e as condicionais de versão `isVersionAtLeast(...)`. `HabboResponse` tem writeInt/writeUTF/writeBoolean/writeLong.
- **Invocação de Response via MethodHandle**: `HabboHandler.invokeResponse` despacha via Java `MethodHandle`. **Não use default parameters do Kotlin (`arg: Int = 0`)** em métodos `@Response` — a aridade deve casar exatamente com os parâmetros passados no `sendHabboResponse(...)`. Para pacotes com múltiplos campos, crie e use um DTO (`data class`) unificado como único argumento de payload.
- **Serialização Multi-Mode**: Entidades enviadas em múltiplos formatos/pacotes (ex: `SnowWarUser`) devem usar serialização polimórfica ou modos explícitos (`SnowWarUserSerializeMode`), centralizando o protocolo.
- Busca de sessão por userId: `HabboServer.habboSessionManager.getHabboSessionById(id)`
  (o map `habboSessions` é por `ChannelId`, não por userId).
- Moedas: `userInformation.credits` / `activityPointsCurrencies[ActivityPointType.DIAMONDS]`
  (persistidos no close da sessão); responda `CREDITS_BALANCE` + `ACTIVITY_POINTS_BALANCE`
  (variante `OutgoingR63A` quando `release == "R63A"`).
- Achievement: `HabboServer.habboGame.achievementManager.progress(session, "ACH_Nome", n, accumulate)`
  + INSERT na migration (`achievements_group` + `achievements`).
- **Compatibilidade Pré-Shuffle (R63A)**:
  - **Verificação de UI na Fronteira**: Nem todas as sub-funcionalidades modernas existiam no R63A. Verifique o controller AS3 correspondente na release de fronteira (`RELEASE63-201109301501-135705747`). Se a tela não possuía determinada aba ou botão (ex: aba de banidos no Room Settings), os pacotes correspondentes do cliente não existiam no protocolo AS3 da época.
  - **Ações Pontuais e Semântica Clássica**: Se a ação existia apenas via menu contextual ou comando (ex: `ROOM_BAN_USER` no Header 320 com apenas `userId: Int`), adicione o `@HandlerR63A(IncomingR63A.XXX)` com leitura condicional (`isR63A`) e adote o comportamento clássico esperado (ex: ban permanente = `RWUAM_BAN_USER_PERM`). Se a feature não existia de forma alguma, apenas enums nas listas modernas — headers NULL de R63A ficam na tabela `_r63a` com o mesmo padrão se necessário.
  - **Headers R63A na Migration**: Headers de R63A devem ser inseridos em `releases_incoming_headers_r63a` ou `releases_outgoing_headers_r63a` no mesmo arquivo `V{N}__*.sql` da feature.
  - **Não executar DDL no banco**: Deixe que o Flyway aplique a migration no próximo boot do emulador.

## Fase 4 — Feature com mobília (furni)

1. **InteractionType**: adicione o valor (`CHEST("chest")`) — o `fromString` casa com a
   coluna `furnishings.interaction_type`. Migration `UPDATE furnishings SET
   interaction_type=... WHERE item_name LIKE 'wf_...%'` (não espere re-register de mobi).
2. **Interactor** (`game/item/interactors/XxxItemInteractor.kt`, 1 por tipo): é
   descoberto por Reflections e indexado por `interactionType`. É o ponto de entrada do
   duplo clique (`RoomTriggerItemHandler`). Use `HabboServer.habboGame.xxxManager`
   (nunca instancie o manager dentro do interactor — cache diverge).
3. **FurnitureLogic** (`game/item/logic/`, 1 arquivo por logic): cada família de mobília
   converte `extraData` (string do banco) → `StuffData` (protocolo) e define o extra
   inicial no catálogo (`correctCatalogExtraData`). Registrados por Reflections
   (`interactionTypes` + opcional `itemNames` p/ wallpaper/floor/landscape).
   **Nada de `when (interactionType)` central** — cada logic se responsabiliza pelo seu.
4. **StuffData** (`game/item/stuff/`): espelho de `com.sulake.habbo.room.object.data` do
   client: `LegacyStuffData(0)`, `MapStuffData(1)`, `StringArrayStuffData(2)`,
   `IntArrayStuffData(3)`, `LimitedStuffData(256)`. Wire: `[roomExtra:Int (só quarto)] [formatKey] [payload]`.
   Para cada mobília, crie uma **data class nomeada** (ex: `MannequinData`) com
   `parse(extraData)` / `toStuffData()` / `toExtraData()` e a correlação AS3 no KDoc.
   `legacyValue` = state visual (para Map é a key `"state"`).
5. `extra_data` no banco: mobílias antigas usam separador `\x07` (legado, manter);
   mobílias novas (chest) usam **JSON**. O client NUNCA vê o formato do banco — só o
   StuffData estruturado.

## Fase 5 — Build e verificação

```bash
cd ~/IdeaProjects/Habbo/habbo_r63b_v2 && ./gradlew :server:compileKotlin   # iterativo
./gradlew assembleDist                                                     # final
```

Checklist final:

- [ ] Todos os enums novos têm header (mesmo NULL) em TODAS as releases — senão o boot mata
- [ ] 1 arquivo por header em incoming/ e outgoing/
- [ ] Migration idempotente (rodou 2x sem 1062)
- [ ] Fluxo do client validado contra o controller/AS3 (quem envia o quê, em que ordem)
- [ ] Strings de UI/notification deixadas para o client resolver (`$$"${wiredchests.xxx}"`)
      exceto placeholders (`%user_name%` → replace no server)
- [ ] `MISSING_ITEMS.txt` é só log de register de mobi — a ação pendente é interactor/logic
