# AGENTS_WIRED.md

Guia do **Wired / Wired 2.0** (`server/src/main/kotlin/ovh/rwx/habbo/game/item/wired/`).

## 1. Execução

Itens Wired ficam em tiles e são agrupados em stacks por coordenada (`Vector2`). Em `WiredHandler.triggerWired`, ao
ocorrer um trigger, o handler acha os triggers do tile, monta um `WiredContext` e executa a stack em ordem física de Z
(`sortedBy { it.roomItem.position.z }`):

```
Selectors (onSelect) -> Addons (onAddon) -> Triggers (onTrigger) -> Conditions (onCondition) -> Effects (onEffect) -> flushWiredMovements
```

1. **Selectors (`WiredSelector`)**: preenchem `wiredContext.targetFurnis` e `targetUsers`.
2. **Addons (`WiredAddon`)**: limites de execução, randomizers (`WiredAddonRandom`), unseen selectors, modificadores de
   ordem, placeholders.
3. **Triggers (`WiredTrigger`)**: avaliam `onTrigger` e definem `sourceItem` (mobi que disparou) e `triggererUser`.
   **Self-click guard**: triggers como `WiredTriggerClickFurni`, `WiredTriggerStuffState`, `WiredTriggerStateChanged`
   devem ignorar a auto-ativação (`if (clickedItem.id == roomItem.id) return false`), evitando loops de feedback e
   disparos ao configurar o item.
4. **Conditions (`WiredCondition`)**: AND por padrão, OR via `WiredAddonOrEval`, mais regras de quantificador.
5. **Effects (`WiredEffect`)**: agem sobre furnis/usuários efetivos; com delay > 0 agendam um `WiredDelayTask`.
6. **`flushWiredMovements`**: despacha as animações acumuladas em `wiredContext.batchedMovements` via `WIRED_MOVEMENT`
   (moderno) ou `ROOM_OBJECT_SLIDE` (legado).

## 2. Fontes efetivas (`getEffectiveFurnis` / `getEffectiveUsers`)

- **`WiredFurniSource`**: `TRIGGERING_ITEM (100)` (`sourceItem`, fallback `targetFurnis`), `SELECTED_ITEMS (0)`
  (`wiredData.items` ou `stuffIds2`), `SELECTOR_ITEMS (200)` (`targetFurnis`), `SIGNAL_ITEMS (201)` (antenas de signal),
  `ALL_ROOM_ITEMS (900)`.
- **`WiredUserSource`**: `TRIGGERING_USER (0)`, `SELECTOR_USERS (200)` (`targetUsers`), `SIGNAL_USERS (201)`,
  `USER_BY_NAME (101)` (`wiredData.message`), `ALL_ROOM_USERS (900)`.

## 3. Variáveis (Wired 2.0)

Variáveis de primeira classe (int/string, aritmética, placeholders, propriedades vivas). **Nunca use números mágicos**
(`0`, `1`, `-10`, `-20`): use `WiredVariableTarget`:

| Enum      | Código | Escopo                                     |
|-----------|--------|--------------------------------------------|
| `FURNI`   | `0`    | Ligada a um `RoomItem` ID.                 |
| `USER`    | `1`    | Ligada a um user ID.                       |
| `GLOBAL`  | `-10`  | Da sala, ligada a `room.roomData.id`.      |
| `CONTEXT` | `-20`  | Efêmera, só no `WiredContext` em execução. |

**Internas (`InternalVariableDefinition`, `variableType = 1`)** expõem propriedades do jogo em tempo real, com prefixos
`@furni.`, `@user.`, `@room.`, `@context.` (ex: `@furni.state/position_x/position_y/altitude/rotation`,
`@user.position_x/direction/handitem_id/effect_id/dance`, `@room.team_red_score`, `@context.selector_furni_count`).
Várias são graváveis; consulte `InternalVariableDefinition` para a lista e as flags de escrita.

**Cliente**:

1. O AS3 não tem variáveis internas hardcoded: o servidor envia as definições via `WiredVariableDialogResponse` /
   `WiredAllVariablesDiffsResponse`.
2. A notação com ponto vira pastas na UI (`@furni.position_x` → `@furni` → `position_x`).
3. `textConnectors` mapeiam inteiros para rótulos, no servidor (rotation `0 -> "North"`, `2 -> "East"`; gender
   `0 -> "Male"`, `1 -> "Female"`).

## 4. Movimento animado vs instantâneo

Ao alterar propriedades físicas (`@furni.position_x/y`, `@furni.altitude`, `@user.position_x`, ...), `setFloorItem`
sozinho só emite `ROOM_FLOOR_ITEM_UPDATE` (teleporta sem animação). Para deslizar suavemente, compare as coordenadas
inicial e final e adicione `WiredFurniMove` / `WiredUserMove` a `wiredContext.batchedMovements`; o runner chama
`room.flushWiredMovements(batchedMovements)` ao fim da stack.

## 5. Regras de arquitetura e banco

1. **DAO encapsulado**: `WiredVariableManager` e `WiredHandler` **nunca executam SQL direto**; toda persistência fica em
   `WiredVariableDao` ou `ItemDao`.
2. **Hash de diff**: `WiredVariableManager.getAllVariablesHash()` gera um hash incremental das definições, para não
   reenviar metadados inalterados.
3. **Isolamento de persistência**: variáveis temporárias são apagadas ao descarregar a sala ou sair do usuário
   (`onUserLeave`); as persistentes (`VariableAvailabilityType.isPersistent`) vão ao banco.
