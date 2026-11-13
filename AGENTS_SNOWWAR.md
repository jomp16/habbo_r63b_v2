# AGENTS_SNOWWAR.md

Guia do **SnowStorm (Game2 / SnowWar)**. Leia antes de mexer em qualquer código de SnowWar. Troubleshooting de desync:
`docs/snowwar_desync_manual.md` e a skill `snowwar-desync`.

## Fonte AS3

- Código AS3 completo descompilado: `/mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/Habbo_codigo_completo.txt`
  e `/mnt/DADOS/Downloads/Habbo/Sulek_SWF/FlashWindows/<RELEASE>/`.
- Busque sempre em `Habbo_codigo_completo.txt` (com `rg`) os fluxos de pacotes, listeners e controllers exatos.

## Ciclo de vida da partida

1. `GAME_2_GAME_STARTED` (dados do lobby com jogadores)
2. `GAME_2_ENTER_ARENA` (dimensões, heightmap, fuse objects, jogadores)
3. `GAME_2_STAGE_LOAD` (gameType) + `GAME_2_STAGE_STILL_LOADING` (0%). **CRÍTICO**: o cliente só chama
   `GameArenaView.init()` (constrói planos 3D, tiles e 97 furnis fuse) ao receber `GAME_2_STAGE_LOAD`.
4. O cliente termina a geração (`REE_OBJECTS_INITIALIZED`) e envia `Game2LoadStageReady(100)`.
5. Servidor envia `GAME_2_STAGE_STILL_LOADING` (100%) e `GAME_2_STAGE_STARTING` (countDown 5s).
6. Após 5s, `GAME_2_STAGE_RUNNING` (duração 120s): cliente vai para `STATE_RUNNING (4)` e inicia o timer.
7. Pool de ticks transmite `GAME_2_GAME_STATUS` a cada 150ms (3 subturns de 50ms).
8. Fim: `GAME_2_STAGE_ENDING` -> `GAME_2_GAME_ENDING`.

## Serialização do avatar (19 ints)

`HumanGameObjectData` no AS3 espera exatamente 19 variáveis (índices 0..18) seguidas de 4 strings (`name`, `mission`,
`figure`, `gender`). Use `SnowWarUserSerializeMode.STAGE_HUMAN`.

## Invariantes de física e eventos sincronizados

- **Input gates vs. aplicação de eventos**: validações de entrada (`throwTimer < 1`, `canThrowSnowballs()`) só existem
  no despacho do composer / recepção do pacote. No loop de simulação (`tickGame()`), os eventos
  `HumanThrowsSnowballAtPosition` e `HumanThrowsSnowballAtHuman` NÃO PODEM ser condicionados a `throwTimer < 1` nem
  `canMove()`. O AS3 `throwSnowball()` decrementa `snowBallCount` incondicionalmente se `> 0`; condicionar causa
  divergência silenciosa (índice 8 = delta de checksum 9).
- **Padrão dual animação/física**: o arremesso gera eventos distintos. ID `4` (`HumanThrowsSnowballAtPosition`) e ID `3`
  (`HumanThrowsSnowballAtHuman`) só disparam animação e timers de arremesso. A entidade `SnowBallGameObject` é criada
  SOMENTE pelo Event ID `8` (`CreateSnowball`): o servidor deve emitir ID `8` em `subturnEvents` ao criar bolas.
- **Ações param e alinham o movimento**: `throwSnowball()` (Events 3 e 4) e `startMakingSnowball()` (Event 7) chamam
  `stopMovement()` no AS3 (para o avatar, ajusta location e moveTarget para o tile atual/próximo e define a direção). O
  servidor deve chamar `stopWalking()` em ambos para manter `moveTarget`, `currentLocation` e `bodyDirection`
  sincronizados.
- **Cooldown de arremesso**: `SNOWBALL_THROW_INTERVAL = 5` subturns (`_SafeStr_8227`), resetado na aplicação do evento.
- **Buracos (`'x'`) e bypass de colisão**: pelo `testCollisionWithGround`, bolas voam livres sobre buracos enquanto Z >=
  1. Só atingem o chão se Z < 1 ou se existe tile válido em `(tileX, tileY)` com Z < tileHeight. Em
  `SnowBallGameObject.subturn()`, `getTileAt(tileX, tileY)` retorna `null` sobre buraco e `testCollisions` é guardado
  por `if (_arg_2)`: **todas as colisões com entidades e tiles vizinhos são ignoradas sobre buracos** até a bola
  reentrar num tile válido (`isValidTile`). Os vizinhos à frente (`direction8`, `direction8 - 1`, `direction8 + 1`)
  também precisam ser validados contra buracos.
- **Diagnóstico por delta de checksum**: `clientChecksum - serverChecksum` = multiplicador `(index + 1)` da variável
  divergente: delta `3` = índice 2 `currentLocation.x`/`locH`; `4` = índice 3 `currentLocation.y`/`locV`; `6` = índice 5
  `snowballCount` da pilha; `8` = índice 7 `hitPoints`; `9` = índice 8 `snowBallCount` do usuário / `hits` da árvore;
  `11` = índice 10 `activityTimer`.
- **Colisões e walkability**:
    - **Árvores**: `height = 3200`, `radius = 1200`. Absorvem hits até `maxHits`. Tile permanentemente bloqueado
      (`_tile.blocked = true`).
    - **Máquinas**: `height = 1200`, `radius = 1200`. Absorvem hits (destroem a bola, máquina inalterada). Bloqueiam
      avatares permanentemente.
    - **Pilhas**: `height = snowballCount * 100`, `radius = snowballCount * 100`. Com `snowballCount <= 0` a pilha sai
      do tile (`removeGameObject()`) e NÃO colide; **pilhas vazias ficam 100% andáveis**. Pilhas NUNCA entram no
      `blockedTiles` estático; cheque dinamicamente
      `piles.any { it.snowballCount > 0 && it.x == tileX && it.y == tileY }` em `isTileWalkable`.
    - **Humanos**: `height = 5000`, `radius = 1600`. Derruba/danifica o humano e destrói a bola.
- **Concorrência do FullGameStatus**: `onFullStatusRequest` deve ser `synchronized(this)` com `tickGame()`, usar
  `lastCompletedTurn`/`lastCompletedChecksum` (evita drift de turno) e serializar os `deferredEvents` pendentes em
  `subTurns`, para o cliente não perder eventos em voo do próximo turno.
- **Pickup em máquina/pilha**: o usuário pega bolas dos 8 tiles adjacentes quando parado (`!isWalking`), a cada
  `MACHINE_PICKUP_INTERVAL = 6` subturns (~300ms).

## Enums tipados, sem números mágicos

`SnowWarEventType` (IDs 1, 2, 3, 4, 7, 8, 11, 12), `SnowWarGameObjectType` (1=Snowball, 2=Tree, 3=Pile, 4=Machine,
5=Human), `SnowWarTrajectory` (0..3) e `Direction` (0..7).
