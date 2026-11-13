# AGENTS.md

Habbo R63B retro server in Kotlin (Java 25 toolchain, Kotlin 2.4.21). Multi-module Gradle build; Netty + MySQL/MariaDB +
hot-load plugin system.

## Build & run

- **Init submodules first**: `git submodule update --init --recursive` (`pathfinding`, `habbo_imaging`,
  `plugin_manager`, `camera_renderer`, `plugins_src/plugin_webapp`).
- Build: `./gradlew clean assembleDist` → `launcher/build/distributions/launcher-0.1.0-SNAPSHOT.{jar,tbz2}`.
- Run: `cd launcher/build/distributions && java -jar launcher-0.1.0-SNAPSHOT.jar` (`ovh.rwx.MainKt`,
  `launcher/src/main/kotlin/ovh/rwx/Main.kt`).
- CI: `gradle assemble check`. Only `pathfinding` has tests (`GridTest.kt`).
- No root `run` task. `camera_renderer/` is a **separate Gradle build** (own `settings.gradle`): don't run root Gradle
  inside it; use `gradle :camera:run` from root.

## Runtime configuration

- `config.yaml` is **required** in the CWD (gitignored; copy `config.example.yaml`). Real RSA keys
  (`encryption.rsa.N/D/E`) go only in `config.yaml`.
- Dist unpacks `config.example.yaml`, `fastfood_localization.json`, `plugins/` relative to CWD.
- `port` (TCP) and `ws_port` (WebSocket) are bound; `web_port` is parsed but **not bound** (add any HTTP listener in
  `HabboServer.start()` beside the TCP/WS bootstraps).
- MOTD file (`motd_enabled`, `motd_file_path`): lines starting with `#` are stripped.

## Deploy (`scripts/deploy.sh`)

- systemd `habbo.service` as user `habbo:habbo`; app in `/opt/habbo/`; camera assets in
  `/mnt/HDD_EXTERNO2/Habbo/camera_data` (symlinked to `/opt/habbo/camera_data`); MariaDB is external.
- `./scripts/deploy.sh` builds locally, rsyncs jars/plugins, generates `deploy/config.prod.yaml` if missing
  (`debug: false`), restarts the service. `DEPLOY_TARGET` overrides the target.
- Flags: `--skip-build`, `--sync-camera` (camera_data changed), `--status` (+ `--tail [N]` to follow logs).

## Local dev DB (`scripts/dev_db.sh`)

Docker MariaDB `habbo-dev-mariadb` at `127.0.0.1:3306`. `sync` (prod dump → local), `sync --baseline` (schema + static
tables only), `reset` (from cached dump), `up|down|status|cli`.

## Modules

`:launcher`, `:server` (bulk of the code), `:plugin_manager`/`:pathfinding`/`:habbo_imaging` (submodules), `:camera`
(`camera_renderer/camera/`), `:plugin_impl1` (sample plugin), `:plugin_webapp` (submodule, Javalin).

- Plugin JARs build into `$rootDir/plugins/` (libs in `plugins/lib/<name>/`, see `plugins_src/common_plugins.gradle`);
  they are not in the launcher's lib/ — loaded at runtime by `PluginManager.loadPluginsFromDir`.

## Architecture

- `HabboServer.kt` is the singleton root: HikariCP → Kwery → Netty → handlers → game state → plugin manager.
- **Class discovery is static, generated at build time** by `:server:generateClassIndex`
  (`build-logic/.../GenerateClassIndexTask.kt`, ASM) into `META-INF/habbo/class-index.txt`, read by
  `ovh.rwx.habbo.util.ClassIndex`. No classpath scan (ClassGraph is gone). Covers `@Handler`/`@HandlerR63A`/`@Response`/
  `@ResponseR63A`, `@FFHandler`/`@FFResponse`, `@WiredItemInteractor`, concrete `ItemInteractor`/`FurnitureLogic`
  subclasses, and built-in `PluginListener`s. **A new kind of discoverable class must be added to the task registration
  in `server/build.gradle.kts` AND `ClassIndex.Kind`.** Re-read on every call: Gradle compile + `reload_handlers` picks
  up new handlers; IDE-native builds don't regenerate it.
- Plugin JARs: `habbo.plugin-conventions` runs `generatePluginDescriptor` (ASM), producing the final `plugin.json` =
  hand-written `src/main/plugin/plugin.json` (kept OUTSIDE `resources/` on purpose: JRebel would serve the un-generated
  copy) + `entryPoints` (concrete `PluginListener` subclasses). `PluginManager.addPluginJar` loads only `entryPoints`
  and rejects JARs without them (rebuild the plugin).
- Packets: `communication/incoming/` (`@Handler(Incoming.X)`) and `communication/outgoing/` (`@Response(Outgoing.X)`). A
  new client message needs an `Incoming` enum entry and a handler.
- DB: DAOs in `server/.../database/` load `.sql` from `server/src/main/resources/sql/` with `:name` params.
- Game state: `game/{room,item,user,achievement,camera,wired}/`. `IRoomTask`s run on the room scheduler pool
  (`room_task.threads`).

## Wired

- 19 triggers under `game/item/wire/trigger/triggers/`. A new trigger needs `WiredTriggerType`, the class, and any
  client UI mapping.
- Rewards: `WiredRewardNotificationResponse`; reasons `0` BADGE_REWARDED, `-7` ITEM_REWARDED, errors 1–5, 8 (see
  `DEVELOPMENT_GUIDE.md`).
- Don't bypass the per-session throttle on wired handlers.
- Execution order, sources, Wired 2.0 variables and DAO rules: read `AGENTS_WIRED.md` before touching wired code.

## SnowStorm (Game2 / SnowWar)

**Read `AGENTS_SNOWWAR.md` before touching any SnowWar code** (lifecycle, AS3-parity invariants, collisions, checksum
diagnosis). Typed enums only, never raw ints.

## Packet responses & MethodHandles

- `HabboHandler.invokeResponse` calls `@Response` methods via `MethodHandle.invokeWithArguments`: **no Kotlin default
  parameters**; the signature must match exactly what `session.sendHabboResponse(...)` passes, else
  `WrongMethodTypeException`.
- Composite payloads: one `data class` DTO, signature `(habboResponse: HabboResponse, data: MyData)`.
- **Era branching**: in unified `@Response` methods NEVER inspect transport properties like
  `habboResponse.outgoingR63A != null`. Use date predicates: `isVersionBetween(...)`, `isVersionBefore(y, m, d)`,
  `isVersionAtLeast(y, m, d)`.
- **Merge multi-release packets** with the same semantic intent (e.g. `GetInterstitialMessageComposer`): unify enum
  names (`LOAD_INTERSTITIALS`, `SHOW_INTERSTITIALS`) and merge `@Handler`+`@HandlerR63A` / `@Response`+`@ResponseR63A`
  into single files with date branching; no duplicates or stubs.

## AS3 defaults & UI

- **Keep classic Habbo semantics** for new flags from modern releases (`idleSleepEnabled`, `leaveOnDoorTileEnabled`):
  DB/entity default preserves the old behavior (sleep on idle = `true`, leave on door tile = `true`) unless the game
  always did the opposite.
- **Negated checkboxes**: check the AS3 controller (`*Ctrl.as`/`*View.as`); `_doNot...`/`_hide...` boxes often serialize
  `data.flag = !checkBox.isSelected`.
- **Defaults come from the AS3 DTO field initializers** (`private var _SafeStr_xxxx:Boolean = true/false;`), never from
  stubs/mocks in `@Response`.

## Protocol alignment vs persistence

- **Ephemeral/abandoned protocol fields** (e.g. Beta 2009 flags dropped in R44, the 6 moderation ints from the
  2012-11-14 24h test) MUST be drained in the incoming handler (`readInt()`/`readBoolean()`) to keep the Netty buffer
  aligned, and NEVER persisted (no DB columns/entity properties; YAGNI).
- **Absent optional fields** (e.g. Beta thicknesses, AIR 2026 dynamic categories): the save handler MUST preserve
  existing state (`room.roomData.property`), never reset to 0/defaults.
- **Modern AIR (WIN63 2026-05-18+)**: chat only sends/receives `chatFloodSensitivity` (`chatFloodProtection`) plus 6 new
  AFK/Sleep/Door/Pets flags. The other 4 chat fields and `allowNavigatorDynCats` are gone; sending `chatType` corrupts
  the AIR UI.
- **`ROOM_UPDATE_FURNI_STACK` / `UpdateStackingHeightComposer`** exists only from 2013-05-16
  (`RELEASE63-201305162304-499805981`). For all earlier releases (`20111005`, `20111213`, `20120402`, `20120704`) it
  MUST stay `header = NULL, override_method = 'DISABLED'` in the DB: sending it corrupts the buffer (in `20111005` it
  collided with `ObjectsDataUpdateMessageEvent` 3887 → fatal parse error). Legacy clients recompute stack height from
  `ROOM_FLOOR_ITEM_REMOVE`/`ROOM_FLOOR_ITEM_UPDATE`.

## Database

- **NEVER read, search, edit, stage or commit `sql_dump/database.sql`**, and never use DB dumps to derive packet
  structures, headers or release differences. Use `habbo-timeline` (`~/.local/bin/habbo-timeline`, via the
  `timeline-as3` skill) and the AS3 decompilations.
- MariaDB/InnoDB, `utf8mb4_unicode_520_ci`; Kwery `MysqlDialect`, Hikari config from `HabboConfig`.
- `sql_dump/dump_sql.sh` (needs `yq`, `mariadb-dump`; `-s` = schema only) regenerates the gitignored dump.

## Flyway migrations

- **Migrations are the single source of truth**: every schema change, table, multi-release/R63A header goes in
  `server/src/main/resources/db/migration/V{N}__<descricao>.sql`. Flyway runs at boot (`FlywayMigration.migrate()` in
  `HabboServer.start()`, before `HabboHandler`).
- **NEVER run manual DDL/DML** (`INSERT`/`UPDATE`/`ALTER TABLE`) on the DB: it diverges from `flyway_schema_history`. To
  validate SQL syntax, only use `START TRANSACTION; SOURCE ...; ROLLBACK;`.
- No `INSERT IGNORE` on PKs (MariaDB logs warning 1062 at boot): use `ON DUPLICATE KEY UPDATE` or `WHERE NOT EXISTS`.
- One batched `INSERT` per table for headers; no commented throwaway queries (`-- SELECT ...`).
- Renames (e.g. `releases_incoming_headers_r63a`/`releases_outgoing_headers_r63a`):
  `UPDATE ... SET name = 'NEW' WHERE name = 'OLD'`, never `DELETE` + `INSERT`.

## Persistence & write-behind

- **No JPA/Hibernate** (state lives in RAM for hours under Netty event loops; dirty-checking thousands of furni is too
  costly). Use **Jdbi 3** + HikariCP with explicit queries. `bindKotlin` placeholders use camelCase property names
  (`:userId`) unless `@ColumnName`.
- **NEVER run blocking JDBC on Netty/game threads** for deferrable work.
- No throwaway insert DTOs (`NewPetInsert`, `NewRoomInsert`) or `KClass` queues/maps in `WriteBehindManager`. One-off
  async writes: `WriteBehindManager.queue(WriteKey.x(id), ...) { db { ... } }` with the SQL kept in the DAO. JDBC
  batching (`batchInsert`/`batchUpdate`) only for natural collections (e.g. `ItemDao.addItems`).
- **`WriteKey` rules** (`KeyedWriteScheduler` runs up to 4 writes in parallel; writes sharing a key run in enqueue
  order, disjoint keys run concurrently):
  - Declare EVERY row the write creates/changes/deletes (`WriteKey.item/.room/.user/.group/.pet(id)`), plus the FK
    parent row when it may have been created in the same window (e.g. `items_wired` declares the item; moving an item to
    a new room declares item + room; `createGroup` declares group + room). Too few keys can reorder rows or break FKs;
    too many only costs parallelism.
  - Rows without a natural key: `WriteKey("tipo", id)`, the SAME key on every operation of that row. If one operation
    only knows the row id and another only the owner/pair, the INSERT declares both (see `MessengerDao`).
  - `queue(emptyList())` = independent (result doesn't depend on order). Keyless `queue { }` (global FIFO) exists but
    NOT for new code.
  - Direct `db { }` writes and the dirty set (`AbstractDirtyEntity.flush()`) are outside the keys; the dirty set runs
    only after the round's queued writes finish.
- **Dirty entities**: continuously mutated entities (`UserInformation`, `UserStats`, `UserPreferences`, `RoomData`,
  `RoomItem`) extend `AbstractDirtyEntity` and implement `flush()`. `WriteBehindManager` only collects the dirty set and
  calls `flush()` polymorphically (no type maps, no `when`, no DAO imports).

## Packet reverse engineering (2011–2013)

- Always consult `AGENTS_FINDING_PACKET.md` (4-step pipeline + anchor table).
- NEVER run regex/`re.DOTALL` scripts over `Habbo_codigo_completo.txt` (30MB+, hangs): use `rg` on `Messages.txt` or
  exact anchors (`inventory.trading.warning`, `ROFCAE_DICE_OFF`, `RWFCRM_REDEEM`, `deleteroom.confirm.message`).
- Flash `Outgoing[...]` = client→server = emulator `releases_incoming_headers`; `Incoming[...]` = server→client =
  `releases_outgoing_headers`.
- AS3 indices are hex (`[0x0600] = 1536`): search `\[(0x[0-9a-fA-F]+|\d+)\]\s*=\s*<Class>;`.

## Conventions

- Kotlin style `official`. License headers: GPLv3 for server/launcher/plugin_impl1/plugin_webapp/camera; LGPLv3 for
  submodules (`pathfinding`, `habbo_imaging`, `plugin_manager`). Match existing headers.
- Package roots: `ovh.rwx` (launcher), `ovh.rwx.habbo` (server), `ovh.rwx.utils.plugin`, `ovh.rwx.utils.pathfinding`,
  `ovh.rwx.habbo.imaging`.
- Achievements: `ACH_<SnakeName>` (e.g. `ACH_BuildersClub`).
- Plugin commands: `@Command(["name"], rank = N, permissionName = "...")` in `PluginListener` subclasses (example:
  `plugins_src/plugin_impl1/.../PluginImpl1Listener.kt`).

## Gotchas

- `BuildConfig` (`GIT_COMMIT_FULL`, `GIT_COMMIT_SHORT`, `BUILD_INSTANT`) is generated at build time; don't read those
  from elsewhere.
- `rebel.xml` (JRebel) is unrelated to the build; it's excluded from the JAR in root `build.gradle`.
- Never commit runtime/reference artifacts: `packet_infos/`, `MISSING_ITEMS.txt`, `camera_data/`, `motd/`, `cache/`.
- `fastFoodServerBootstrap` is commented out in `HabboServer.start()`: keep it so unless you restore
  `FastFoodNettyEncoder`/`Decoder`/`Handler`.
- `DEVELOPMENT_GUIDE.md` (Portuguese, dated): verify against code before relying on it.
