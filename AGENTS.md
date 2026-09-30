# AGENTS.md

Habbo R63B retro server written in Kotlin (Java 17 toolchain, Kotlin 2.3.0). Multi-module Gradle build; runs on Netty with a MySQL/MariaDB backend and a hot-load plugin system.

## Build & run

- **Init submodules first** (this is the #1 thing agents miss): `git submodule update --init --recursive`. The submodules are `pathfinding`, `habbo_imaging`, `plugin_manager`, `camera_renderer`, and `plugins_src/plugin_webapp`.
- Build: `./gradlew clean assembleDist` (output: `launcher/build/distributions/launcher-0.1.0-SNAPSHOT.{jar,tbz2}`).
- Run: `cd launcher/build/distributions && java -jar launcher-0.1.0-SNAPSHOT.jar`. The launcher uses `Main-Class: ovh.rwx.MainKt` (entrypoint: `launcher/src/main/kotlin/ovh/rwx/Main.kt`).
- CI runs `gradle assemble check --stacktrace` (Drone) and `gradle --build-cache assemble` (GitLab). `check` only matters for the `pathfinding` module — it's the only subproject with tests (`pathfinding/src/test/.../GridTest.kt`).
- No application Gradle `run` task exists at the root. The `:camera` subproject has one (`gradle :camera:run`) but it is a separate nested build rooted at `camera_renderer/`.

## Runtime configuration

- `config.yaml` is **required at runtime** and is parsed from the working directory (`File("config.yaml")` in `server/.../HabboServer.kt`). It is gitignored — copy `config.example.yaml` to `config.yaml` and edit before first run.
- The dist archive unpacks `config.example.yaml`, `fastfood_localization.json`, and the `plugins/` directory at the root. The launcher expects these files relative to CWD.
- **Port bindings**: `port` (TCP game socket) and `ws_port` (WebSocket) are bound; `web_port` is in the config schema but currently **not bound** by `HabboServer.start()`.
- Encryption keys (`encryption.rsa.N/D/E`) in `config.example.yaml` are sample values. Real values belong in `config.yaml` only.
- `motd_enabled` + `motd_file_path`: lines starting with `#` are stripped from the MOTD file.

## Module layout (`settings.gradle`)

| Project | Real path | Notes |
|---|---|---|
| `:launcher` | `launcher/` | Distribution assembly, `Main-Class` host. |
| `:server` | `server/` | Core game logic, the bulk of the codebase. |
| `:plugin_manager` | `plugin_manager/` (submodule) | Plugin loader API; API dep, used by `:server`. |
| `:pathfinding` | `pathfinding/` (submodule) | Tile grid pathfinding (only module with tests). |
| `:habbo_imaging` | `habbo_imaging/` (submodule) | Figure/avatar imaging helpers. |
| `:camera` | `camera_renderer/camera/` | CLI camera renderer (separate composition). |
| `:plugin_impl1` | `plugins_src/plugin_impl1/` | Sample plugin (room commands, `@Command`). |
| `:plugin_webapp` | `plugins_src/plugin_webapp/` (submodule) | Javalin webapp plugin (HTTP API on a worker thread). |

- `camera_renderer/` is **a separate Gradle build** with its own `settings.gradle` and `build.gradle` — do not run root Gradle commands inside it. Run `gradle :camera:run` from the root, or build inside `camera_renderer/` directly.
- Plugin JARs are built into `$rootDir/plugins/` (see `plugins_src/common_plugins.gradle`) along with their libs in `plugins/lib/<plugin-name>/`. They are NOT bundled into the launcher JAR's lib/ — they're loaded from the `plugins/` directory at runtime by `PluginManager.loadPluginsFromDir(File("plugins"))`.

## Architecture

- `server/src/main/kotlin/ovh/rwx/habbo/HabboServer.kt` is the singleton root. It wires HikariCP → Kwery (MySQL) → Netty → handlers → game state → plugin manager.
- Handler discovery uses **Reflections8** (`HabboServer.reflections`) to scan the classpath for `@Handler`, `@Response`, `@Task`, etc. — keep annotations on classes; reflection caches won't pick up Kotlin metadata-only files.
- Packets: `communication/incoming/` (handlers, registered by `@Handler(Incoming.X)`) and `communication/outgoing/` (responses, registered by `@Response(Outgoing.X)`). New client messages require both an `Incoming` enum entry and a handler.
- DB: DAOs in `server/.../database/` use `.sql` files from `server/src/main/resources/sql/`. Kwery (`com.github.andrewoma.kwery`) is the session layer; SQL files are loaded by path with `:name` named parameters.
- Game state: `game/room/`, `game/item/`, `game/user/`, `game/achievement/`, `game/camera/`, `game/wired/`. Room tasks (`IRoomTask`) run on the room scheduler pool (`room_task.threads`).

## Wired system

- 19 trigger types live under `server/.../game/item/wire/trigger/triggers/`. Adding a new trigger requires editing `WiredTriggerType`, the trigger class, and any UI mapping expected by the client.
- Rewards flow through `WiredRewardNotificationResponse` (`communication/outgoing/wired/`). Reason codes used by the protocol: `0` BADGE_REWARDED, `-7` ITEM_REWARDED, plus error codes 1–5 and 8 (see `DEVELOPMENT_GUIDE.md`).
- Per-session throttle on wired handlers is in scope — don't bypass it.

## SnowStorm (Game2 / SnowWar)

- **AS3 Client Source**: The complete decompiled AS3 client source code is located at `/mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/Habbo_codigo_completo.txt` and `/mnt/DADOS/Downloads/Habbo/Sulek_SWF/FlashWindows/<RELEASE>/`. Always search `Habbo_codigo_completo.txt` for exact packet flows, listeners, and controllers.
- **Match Lifecycle & Sequence**:
  1. `GAME_2_GAME_STARTED` (lobby data with players)
  2. `GAME_2_ENTER_ARENA` (arena dimensions, heightmap, fuse objects, players)
  3. `GAME_2_STAGE_LOAD` (gameType) + `GAME_2_STAGE_STILL_LOADING` (0%): **CRITICAL**: The client only invokes `GameArenaView.init()` (which builds 3D room planes, ground tiles, and 97 fuse object furni) upon receiving `GAME_2_STAGE_LOAD`.
  4. Client completes room generation (`REE_OBJECTS_INITIALIZED`) and sends `Game2LoadStageReady(100)` to the server.
  5. Server sends `GAME_2_STAGE_STILL_LOADING` (100%) and `GAME_2_STAGE_STARTING` (countDown: 5s).
  6. After 5s countdown, server sends `GAME_2_STAGE_RUNNING` (duration: 120s), switching client state to `STATE_RUNNING (4)` and starting the timer.
  7. Server runs a scheduled tick pool broadcasting `GAME_2_GAME_STATUS` every 150ms (3 subturns of 50ms).
  8. End match: `GAME_2_STAGE_ENDING` -> `GAME_2_GAME_ENDING`.
- **Avatar Serialization (19 ints)**: `HumanGameObjectData` in AS3 expects exactly 19 variables (indices 0..18) followed by 4 strings (`name`, `mission`, `figure`, `gender`). Use `SnowWarUserSerializeMode.STAGE_HUMAN`.
- **Physics & Synchronized Events Invariants**:
  - **Input Gates vs. Simulation Event Application**: Input validation checks (e.g. `throwTimer < 1`, `canThrowSnowballs()`) only belong at composer dispatch/packet reception. In the simulation loop (`tickGame()`), events (`HumanThrowsSnowballAtPosition`, `HumanThrowsSnowballAtHuman`) MUST NOT gate execution on `throwTimer < 1` or `canMove()`. AS3 `throwSnowball()` unconditionally decrements `snowBallCount` if `snowBallCount > 0`. Gating event execution in simulation leads to silent `snowBallCount` divergence (variable index 8 = checksum delta of 9).
  - **Animation vs Physics Dual-Event Pattern**: Throwing a snowball generates two distinct events. Event ID `4` (`HumanThrowsSnowballAtPosition`) and ID `3` (`HumanThrowsSnowballAtHuman`) only trigger avatar animation and throw timers in AS3. The physical entity `SnowBallGameObject` is ONLY created when receiving Event ID `8` (`CreateSnowball`). The server must emit ID `8` in `subturnEvents` when spawning snowballs so the AS3 arena simulation remains in sync.
  - **Actions Stop Movement and Align**: Both `throwSnowball()` (Events 3 & 4) and `startMakingSnowball()` (Event ID 7) in AS3 explicitly call `stopMovement()`, which immediately stops the avatar, snaps location and moveTarget to the current/next tile, and sets throw direction. The server must call `stopWalking()` on both to keep `moveTarget`, `currentLocation`, and `bodyDirection` synchronized with the client.
  - **Throw Cooldown**: AS3 enforces `SNOWBALL_THROW_INTERVAL = 5` subturns (`_SafeStr_8227`), reset on throw event application.
  - **Ground & Hole Collisions & Object Bypass**: Per AS3 `testCollisionWithGround`, snowballs fly freely over holes/chasms (`'x'` tiles) as long as Z >= 1. Balls only hit the ground if Z < 1 or if a valid ground tile exists at `(tileX, tileY)` with Z < tileHeight. Crucially, in AS3 `SnowBallGameObject.subturn()`, `getTileAt(tileX, tileY)` returns `null` when over a hole (`'x'`). Because `testCollisions` is guarded by `if (_arg_2)`, **all entity and neighbouring tile collisions are bypassed while flying over holes** until the ball re-enters a valid ground tile (`isValidTile`). Neighbouring tiles ahead (`direction8`, `direction8 - 1`, `direction8 + 1`) must also be validated against holes.
  - **Checksum Delta Diagnosis**: Checksum mismatches can be mathematically isolated by calculating `clientChecksum - serverChecksum`. The delta corresponds to the entity variable multiplier `(index + 1)` (e.g. delta `11` = index 10 `activityTimer`, delta `9` = index 8 `snowBallCount`, delta `8` = index 7 `hitPoints`). This immediately pinpoints which variable and subturn (50ms) diverged.
  - **Tile Entity Collisions (Trees, Machines, Piles, Humans)**: Snowballs test collision against all 4 tile-occupying object types:
    - **Trees**: `height = 3200`, `radius = 1200`. Absorbs hits up to `maxHits` (then cleared from tile).
    - **Machines**: `height = 1200`, `radius = 1200`. Absorbs hits (destroys snowball, machine unchanged).
    - **Piles**: `height = snowballCount * 100`, `radius = snowballCount * 100`. When `snowballCount <= 0`, pile is cleared from tile and does not collide. Absorbs hits (destroys snowball, pile unchanged).
    - **Humans**: `height = 5000`, `radius = 1600`. Knocks down / damages human and destroys snowball.
  - **Machine & Pile Pickup**: Users can pick up snowballs from all 8 adjacent tiles around a machine or pile when stationary (`!isWalking`), at a cadence of `MACHINE_PICKUP_INTERVAL = 6` subturns (~300ms).
- **Strongly-Typed Enums Over Magic Numbers**:
  - Always use typed enums instead of raw ints for game entities and protocol IDs: `SnowWarEventType` (IDs 1, 2, 3, 4, 7, 8, 11, 12), `SnowWarGameObjectType` (1=Snowball, 2=Tree, 3=Pile, 4=Machine, 5=Human), `SnowWarTrajectory` (0..3), and `Direction` (0..7).

## Packet Responses & MethodHandles

- `HabboHandler.invokeResponse` invokes methods annotated with `@Response(Outgoing.X)` via Java `MethodHandle.invokeWithArguments`.
- **No Default Parameters**: `MethodHandle` does not support Kotlin default parameter values (`foo: Int = 0`). The `@Response` method signature must match the exact number and types of arguments passed into `session.sendHabboResponse(...)`. If parameter count differs, `WrongMethodTypeException` is thrown at runtime.
- **DTO Pattern**: Use a single typed `data class` for composite response payloads (e.g. `Game2EnterArenaData`, `Game2StageStartingData`) to keep response signatures clean `(habboResponse: HabboResponse, data: MyDataClass)`.

## AS3 Defaults & UI Checkbox Inversion

- **Preservação de Semântica Histórica**: Ao introduzir flags comportamentais novas vindas de releases modernas (ex: `idleSleepEnabled`, `leaveOnDoorTileEnabled`), o default do banco e das entidades deve SEMPRE manter o comportamento clássico do Habbo (avatares dormem por inatividade = `true`, pisar na porta sai da sala = `true`), a menos que o jogo sempre tenha tido o oposto.
- **Checkboxes Negativas na UI**: Sempre verifique o controller de tela do AS3 (`*Ctrl.as` / `*View.as`). Checkboxes como `_doNot...` ou `_hide...` costumam inverter o booleano na serialização do composer (`data.flag = !checkBox.isSelected`). Não confunda a desmarcação da caixa com valor `false` no protocolo.
- **Inicializadores de DTOs no AS3**: Nunca confie em stubs ou mocks numéricos temporários colocados em `@Response`. A fonte da verdade dos defaults do cliente está na declaração de variáveis dos DTOs AS3 (`private var _SafeStr_xxxx:Boolean = true/false;`).

## Protocol Alignment vs Database Persistence

- **Campos Efêmeros de Protocolo**: Experimentos históricos da Sulake de curta duração (ex: flags do Beta 2009 descartadas na R44, testes de 24h como os 6 inteiros de moderação em 2012-11-14) DEVEM ser lidos/drenados no incoming handler (`habboRequest.readInt()` / `readBoolean()`) para manter o byte buffer do Netty estritamente alinhado.
- **NÃO Persistir no Banco**: NUNCA crie colunas no MariaDB ou propriedades em entidades de domínio para campos efêmeros/abandonados em 2009/2012. Persistir experimentos de curta duração viola YAGNI, gera colunas mortas no schema e onera o dirty-checking desnecessariamente.
- **Preservação de Estado em Campos Opcionais Ausentes**: Quando uma compilação cliente não envia certos campos (ex: espessuras no Beta, dynamic categories no AIR 2026), o handler de gravação DEVE preservar o estado existente na entidade (`room.roomData.property`), nunca resetando para 0 ou defaults destrutivos.
- **AIR Moderno (WIN63 2026-05-18+)**: No chat, apenas `chatFloodSensitivity` (`chatFloodProtection`) é enviado/recebido, acompanhado das 6 novas flags AFK/Sleep/Porta/Pets. Os outros 4 campos de chat e `allowNavigatorDynCats` foram extintos; enviar `chatType` no response corrompe a UI do AIR.

## Database

- **NEVER consult, read, search, or edit `sql_dump/database.sql`**: Agents must completely ignore `sql_dump/database.sql`. Never use `database.sql` or DB dumps to analyze, deduce or verify packet structures, headers, incoming/outgoing payloads, or release differences. Always use the timeline tool `habbo-timeline` (available at `~/.local/bin/habbo-timeline` and `/home/jomp16/IdeaProjects/Habbo/habbo-timeline/bin/habbo-timeline`, invoked via the `timeline-as3` skill) and actual client AS3 bytecode decompilations. Do not stage or commit `sql_dump/database.sql`.
- `migrations/` holds incremental SQL files (`NNN_description.sql`). The base schema is `sql_dump/database.sql` (regenerated by `sql_dump/dump_sql.sh`).
- `sql_dump/dump_sql.sh` requires `yq` (from `go-yq`) and `mariadb-dump`. It reads `config.yaml` from the repo root. Pass `-s` for schema-only. Output goes to `sql_dump/database.sql` (gitignored).
- MariaDB/MySQL with InnoDB and `utf8mb4_unicode_520_ci` (see `migrations/001_create_users_pets.sql`). Kwery uses `MysqlDialect` and Hikari config from `HabboConfig`.

## Database Migrations & Flyway Lifecycle

- **Migrations São a Única Fonte de Verdade**: Toda e qualquer alteração de schema, criação de tabelas, inclusão de headers multi-release ou headers R63A deve ser escrita no arquivo Flyway correspondente em `server/src/main/resources/db/migration/V{N}__<descricao>.sql`.
- **NUNCA executar DDL/DML manual no banco**: É terminantemente proibido executar `INSERT`, `UPDATE` ou `ALTER TABLE` manualmente no MariaDB para aplicar features em desenvolvimento. O Flyway executa `FlywayMigration.migrate()` automaticamente no boot do servidor (`HabboServer.start()`) antes de inicializar o `HabboHandler`. Execuções manuais causam divergências com o `flyway_schema_history` e quebram a previsibilidade do ambiente do usuário.
- **Testes de Migration Apenas com Rollback**: Caso precise validar a sintaxe de um SQL gerado, utilize estritamente blocos com rollback: `START TRANSACTION; SOURCE ...; ROLLBACK;`.
- **Idempotência Sem Warnings (Evitar INSERT IGNORE em PKs)**: O MariaDB emite avisos SQL 1062 em `INSERT IGNORE` quando há colisão de chave primária, poluindo o boot do Flyway com logs `[WARN ] Duplicate entry`. Utilize sempre `ON DUPLICATE KEY UPDATE` ou `WHERE NOT EXISTS`.
- **Migrations Limpas e Sem Ruído**: Agrupe inserts de headers em um único batch `INSERT` por tabela e NUNCA inclua queries descartáveis comentadas (`-- SELECT ...`) dentro dos arquivos de migration.

## Database & Write-Behind Architecture

- **No JPA / Hibernate**: O servidor mantém estado ativo na RAM por horas sob concorrência de Event Loops do Netty. Não use JPA/Hibernate — o dirty-checking comparando milhares de mobílias na RAM causaria estresse massivo de CPU, memory leaks e conflitos com o modelo de threads do Netty. Usamos **Jdbi 3** + **HikariCP** com queries explícitas.
- **Write-Behind para Zero Lag no Netty**: NUNCA execute JDBC síncrono bloqueante nas threads do Netty/jogo para operações que podem ser diferidas. Qualquer I/O síncrono bloqueia a thread de rede e gera jitter/lag para outros jogadores.
- **Evite Over-engineering com DTOs de Insert**: NUNCA crie DTOs descartáveis (`NewPetInsert`, `NewRoomInsert`) nem filas/mapas de `KClass` no `WriteBehindManager` apenas para transportar parâmetros para o banco:
  - Para escritas/inserts pontuais assíncronos, use `WriteBehindManager.queue { db { ... } }`. Isso garante execução não-bloqueante no pool `Dispatchers.IO` enquanto mantém o SQL e a lógica coesos dentro do próprio DAO.
  - Reserve batching JDBC (`batchInsert` / `batchUpdate`) apenas para coleções reais e naturais (ex: `ItemDao.addItems` comprando catálogo ou salvando mobílias de um quarto).
- **Polimorfismo em Entidades Sujas (`AbstractDirtyEntity`)**:
  - Entidades com mutação contínua (`UserInformation`, `UserStats`, `UserPreferences`, `RoomData`, `RoomItem`) estendem `AbstractDirtyEntity` e implementam `abstract fun flush()`.
  - O `WriteBehindManager` apenas coleta o set de entidades sujas e invoca `flush()` polimorficamente — sem mapas de tipos, sem `when` de classes e sem importar todos os DAOs do universo no manager.
- **Jdbi 3 `batchInsert` e `bindKotlin`**: Placeholders em queries com `bindKotlin` usam os nomes das propriedades Kotlin em camelCase (ex: `:userId`, `:itemId`), não snake_case, a menos que anotados com `@ColumnName`.

## Conventions

- Kotlin code style is `official` (set in `gradle.properties`).
- License headers: GPLv3 for server/launcher/plugin_impl1/plugin_webapp/camera, LGPLv3 for submodules (`pathfinding`, `habbo_imaging`, `plugin_manager`). Match the existing header when adding new files.
- Package roots: `ovh.rwx` (launcher), `ovh.rwx.habbo` (server), `ovh.rwx.utils.plugin` (plugin_manager), `ovh.rwx.utils.pathfinding`, `ovh.rwx.habbo.imaging`.
- Achievement names: `ACH_<SnakeName>` (e.g. `ACH_BuildersClub`, `ACH_RoomDecoHosting`).
- Plugin commands: `@Command(["name"], rank = N, permissionName = "...")` on functions in classes extending `PluginListener`. See `plugins_src/plugin_impl1/.../PluginImpl1Listener.kt` for the contract.

## Gotchas

- **Generated code**: `BuildConfig` is produced by `com.github.gmazzo.buildconfig` and `org.ajoberstar.grgit` — `GIT_COMMIT_FULL`, `GIT_COMMIT_SHORT`, `BUILD_INSTANT` are computed at build time. Don't read them at runtime from another source.
- `rebel.xml` is in `server/src/main/resources/` but JRebel is unrelated to the build (it's just excluded from the JAR alongside `META-INF/*.SF` etc. in the root `build.gradle`).
- `.stignore` (Syncthing) ignores `build/`, `out/`, `.gradle/`, and `camera_data/assets_renderer_v2/`. The `camera_data/` directory is gitignored output for the camera renderer.
- `packet_infos/`, `MISSING_ITEMS.txt`, `camera_data/`, `motd/`, `cache/` are all runtime artifacts or reference data — never commit.
- `WEB_PORT` is parsed but unused; if you add an HTTP listener, do it in `HabboServer.start()` next to the existing TCP/WS bootstraps.
- `fastFoodServerBootstrap` is wired up but commented out in `HabboServer.start()` — keep it commented out unless you also restore the matching netty handler (`FastFoodNettyEncoder`/`FastFoodNettyDecoder`/`FastFoodNettyHandler`).

## Useful starting points

- Entry point: `launcher/src/main/kotlin/ovh/rwx/Main.kt`
- Server root: `server/src/main/kotlin/ovh/rwx/habbo/HabboServer.kt`
- Config schema: `server/src/main/kotlin/ovh/rwx/habbo/config/HabboConfig.kt`
- SQL queries: `server/src/main/resources/sql/`
- Packet handlers: `server/src/main/kotlin/ovh/rwx/habbo/communication/incoming/`
- Plugin example: `plugins_src/plugin_impl1/src/main/kotlin/ovh/rwx/plugin/impl1/PluginImpl1Listener.kt`
- Long-form architecture notes: `DEVELOPMENT_GUIDE.md` (Portuguese, dated — verify against code before relying on it).
