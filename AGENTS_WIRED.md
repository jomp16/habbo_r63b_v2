# AGENTS_WIRED.md

Comprehensive guide to the **Wired and Wired 2.0 System** in the Habbo server codebase (`server/src/main/kotlin/ovh/rwx/habbo/game/item/wired/`).

---

## 1. Architecture & Execution Lifecycle

Wired items are placed on room tiles and grouped into stacks based on their coordinate position (`Vector2`).

### Stack Execution Flow (`WiredHandler.triggerWired`)
When a trigger event occurs, `WiredHandler` finds all matching triggers on the tile, sets up a `WiredContext`, and executes the stack in strict physical Z-order (`sortedBy { it.roomItem.position.z }`):

```
1. Selectors (onSelect) -> 2. Addons (onAddon) -> 3. Triggers (onTrigger) -> 4. Conditions (onCondition) -> 5. Effects (onEffect) -> 6. flushWiredMovements
```

1. **Selectors (`WiredSelector`)**: Populates `wiredContext.targetFurnis` and `wiredContext.targetUsers`.
2. **Addons (`WiredAddon`)**: Applies execution limits, randomizers (`WiredAddonRandom`), unseen selectors, order modifiers, and placeholders.
3. **Triggers (`WiredTrigger`)**: Evaluates `onTrigger`. Sets `wiredContext.sourceItem` (the triggering mobi) and `wiredContext.triggererUser`.
   - **Self-Click Guard**: Triggers (such as `WiredTriggerClickFurni`, `WiredTriggerStuffState`, `WiredTriggerStateChanged`) **must ignore self-activation** (`if (clickedItem.id == roomItem.id) return false`) to prevent recursive feedback loops or accidental firings during item configuration.
4. **Conditions (`WiredCondition`)**: Validates criteria (supports AND evaluation by default, OR evaluation via `WiredAddonOrEval`, and quantifier rules).
5. **Effects (`WiredEffect`)**: Executes actions on effective furnis / users. If delay > 0, schedules a `WiredDelayTask`.
6. **Movement Flushing (`flushWiredMovements`)**: Dispatches all smooth sliding animations collected in `wiredContext.batchedMovements` via `WIRED_MOVEMENT` (modern) or `ROOM_OBJECT_SLIDE` (legacy).

---

## 2. Context & Effective Sources (`WiredContext`)

`WiredContext` carries the execution state through the stack lifecycle.

### Furni & User Sources
Wired effects and conditions resolve their target objects using `getEffectiveFurnis` and `getEffectiveUsers`:

- **Furni Sources (`WiredFurniSource`)**:
  - `TRIGGERING_ITEM (100)`: Uses `wiredContext.sourceItem` (with fallback to `targetFurnis`).
  - `SELECTED_ITEMS (0)`: Uses items explicitly selected in the wired box (`wiredData.items` or `stuffIds2`).
  - `SELECTOR_ITEMS (200)`: Uses `wiredContext.targetFurnis` provided by a selector in the stack.
  - `SIGNAL_ITEMS (201)`: Uses items transmitted through wired signal antennas.
  - `ALL_ROOM_ITEMS (900)`: Operates on all room items.

- **User Sources (`WiredUserSource`)**:
  - `TRIGGERING_USER (0)`: The user that caused the event (`wiredContext.triggererUser`).
  - `SELECTOR_USERS (200)`: Users collected by a user selector (`wiredContext.targetUsers`).
  - `SIGNAL_USERS (201)`: Users received via wired signals.
  - `USER_BY_NAME (101)`: Matches a specific user in the room by username (`wiredData.message`).
  - `ALL_ROOM_USERS (900)`: All humanoid users currently in the room.

---

## 3. Wired Variables (Wired 2.0)

Wired 2.0 introduces first-class variables supporting integer/string state storage, arithmetic, placeholders, and live object properties.

### Target Scopes (`WiredVariableTarget`)
**Never use raw magic numbers (`0`, `1`, `-10`, `-20`).** Always use `WiredVariableTarget`:

| Scope | Code | Enum | Description |
|---|---|---|---|
| **Furni** | `0` | `WiredVariableTarget.FURNI` | Bound to a specific `RoomItem` ID. |
| **User** | `1` | `WiredVariableTarget.USER` | Bound to a user ID. |
| **Global (Room)** | `-10` | `WiredVariableTarget.GLOBAL` | Room-wide variable bound to `room.roomData.id`. |
| **Context** | `-20` | `WiredVariableTarget.CONTEXT` | Ephemeral variable bound only to the executing `WiredContext`. |

### Internal Variables (`InternalVariableDefinition`)
Internal variables (`variableType = 1`) expose real-time game properties to Wired logic:
- **Furni**: `@furni.id`, `@furni.class_id`, `@furni.state` (writable), `@furni.position_x` (writable), `@furni.position_y` (writable), `@furni.altitude` (writable), `@furni.rotation` (writable), `@furni.is_invisible`, `@furni.is_stackable`, `@furni.can_stand_on`, etc.
- **User**: `@user.index`, `@user.type`, `@user.gender`, `@user.level`, `@user.achievement_score`, `@user.is_hc`, `@user.has_rights`, `@user.position_x` (writable), `@user.position_y` (writable), `@user.altitude` (writable), `@user.direction` (writable), `@user.handitem_id` (writable), `@user.effect_id` (writable), `@user.dance` (writable), etc.
- **Room**: `@room.furni_count`, `@room.user_count`, `@room.wired_timer`, `@room.team_red_score` (writable), `@room.team_blue_score` (writable), etc.
- **Context**: `@context.selector_furni_count`, `@context.selector_user_count`, `@context.signal_furni_count`, etc.

### Client-Side Hierarchy & Text Connectors
1. **No AS3 Hardcoding**: The AS3 client does not hardcode internal variables. It expects the server to send definitions dynamically via `WiredVariableDialogResponse` / `WiredAllVariablesDiffsResponse`.
2. **Category Tree**: The AS3 client parses dot-notation in variable names (e.g. `@furni.position_x`) into visual tree folders (`@furni` → `position_x`).
3. **Text Connectors (`textConnectors`)**: Integer values are mapped to user-facing labels on the server (e.g., rotation `0 -> "North"`, `2 -> "East"`; gender `0 -> "Male"`, `1 -> "Female"`).

---

## 4. Movement Animations vs Instant Updates

When modifying physical object properties (`@furni.position_x`, `@furni.position_y`, `@furni.altitude`, `@user.position_x`, etc.):
- Calling `setFloorItem` alone only emits `ROOM_FLOOR_ITEM_UPDATE` (teleports without animation).
- To achieve smooth sliding animations, compare initial and modified coordinates and add `WiredFurniMove` or `WiredUserMove` to `wiredContext.batchedMovements`.
- The stack runner automatically executes `room.flushWiredMovements(batchedMovements)` at the end of the stack execution.

---

## 5. Architectural & Database Rules

1. **Strict DAO Encapsulation**: Domain classes like `WiredVariableManager` and `WiredHandler` **must never run raw SQL queries**. All persistence belongs in `WiredVariableDao` or `ItemDao`.
2. **Variable Diff Hashing**: `WiredVariableManager.getAllVariablesHash()` produces an incremental rolling hash of variable definitions to avoid re-sending unchanged variable metadata across the network.
3. **Persistence Isolation**: Temporary variables are wiped on room unload or user exit (`onUserLeave`), while persistent variables (`VariableAvailabilityType.isPersistent`) are saved to the database.
