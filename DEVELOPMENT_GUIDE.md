# Habbo R63B v2 - Guia de Desenvolvimento (VIVO)

Última atualização: 02/03/2026
Projetos em andamento: **Wired Rewards System** e sistema completo de Wired triggers

## Estrutura do Projeto Atual

### Arquitetura Geral
```
server/src/main/kotlin/ovh/rwx/habbo/
├── communication/              # Sistema de comunicação cliente-servidor
│   ├── incoming/              # Handlers de pacotes recebidos
│   │   ├── achievement/       # Handlers de achievement progress e list
│   │   ├── catalog/           # Handlers de compra/catalogo
│   │   ├── camera/            # Handler para revelar fotos (R63B)
│   │   ├── gamecenter/        # Game Center handlers
│   │   ├── group/forum/       # Forum handlers
│   │   ├── guide/             # Guides/Bugs handlers
│   │   ├── inventory/         # Inventário handlers
│   │   ├── landing/           # Landing page handlers (badge rewards)
│   │   ├── misc/              # Pacotes genéricos
│   │   ├── messenger/         # Messenger/friends handlers
│   │   ├── moderation/        # Moderation tools
│   │   ├── navigator/         # Room navigation helpers
│   │   ├── room/              # Quarto handlers (aplicar decoração é o atual)
│   │   ├── setting/           # Preferências
│   │   ├── subscription/      # Subscription management
│   │   ├── user/              # User changes (figure, motto)
│   │   └── wired/             # Wired system handlers
│   ├── outgoing/              # Responses ao cliente
│   │   ├── achievement/       # Achievement progress & unlocks
│   │   ├── camera/            # Camera responses (R63B only)
│   │   ├── catalog/           # Catalog responses
│   │   ├── wired/             # Wired notification responses (REWARDS)
│   └── [outros]/
├── database/                  # DAOs
│   ├── achievement/           # Achievement queries (batch updates)
│   ├── item/                  # Item queries
╱─ user/item/user_items.sql, etc.
├── game/                      # Lógica do jogo
│   ├── achievement/           # AchievementManager, Achievements, Groups
│   ├── catalog/               # Compra LTDs, Camera photos via purchase
│   ├── camera/                # R63B: renderização de fotos (thumbnail/métade)
│   │   └── ... (achievevement via revealPhoto)
│   ├── gamecenter/            # Game Center lógica (score/medalhas)
│   ├── item/                  # FURNISHINGS, interactores, wired triggers
│   │   ├── wire/              # R63B: Wired system completo
│   │   │   └── trigger/triggers/    # 19 tipos de triggers + CLOCK_REACH_TIME
│   │   └── interactors/       # Roller, WiredItemInteractor (recaptha)
│   ├── room/                  # Quarto, tasks, dimmer management
│   │   ├── dimmer             # Dimming da área do quarto
│   │   ├── wired/              # Wired triggers no quarto
│   │   └── games/             # Battle Banzai (3 achievements)
│   │       └── tasks          # ACH_RoomEntry, ACH_BuildersClub
│   └── user/                  # User, badges, subscribe system
├── server/                    # Configuração e inicialização do servidor
╱— config.yaml, init, etc.
```

### Recursos SQL (Atualizado)
```
server/src/main/resources/sql/achievement/
├── select_achievement_groups.sql      # Lista todos os grupos de achievements
├── select_achievements.sql            # Lista achievements ativos
├── insert_user_achievement.sql        # Insere progresso no database (batch)
└── update_user_achievement.sql        # Update batch do progresso

server/src/main/resources/sql/items/   # Itens + achievement herdado
user/items/user_items_with_room.sql     # Seleção de items do usuário+quarto

server/src/main/resources/sql/builders/furni_ids.sql
# IDs dos móveis Builders Club para detecção em decoration
```

## Sistema de Comunicação

### 1. Achievement Handlers (R63B)

**Progress Response:** Envia progresso atual de um grupo específico
```kotlin
class AchievementProgressResponse {
    @Response(Incoming.PROGRESS_REQUEST)
    fun response(session: HabboSession, groupId: Int, level: Int, progress: Int) {
        session.sendHabboResponse(Outgoing.PROGRESS_RESPONSE) {
            writeInt(groupId)
            writeInt(level)
            writeInt(progress)
        }
    }
}
```

**Unlock Response:** Notifica unlock de nivel (último nível do grupo, não todos intermediários)
```kotlin
class AchievementUnlockedResponse {
    @Response(Incoming.UNLOCK_CONFIRMATION)
    fun response(session: HabboSession, groupId: Int, level: Int) {
        session.sendHabboResponse(Outgoing.UNLOCK_RESPONSE) {
            writeInt(groupId)
            writeInt(level) // último nível (ex: 3 para unlock completo)
            
            rewards = listOf(
                Reward(badgeCode, rewardActivityPoints), // Pixels/Duckets (1-2x levels restantes)
                Reward(badgeCode, rewardAchievementPoints), // Score/Topázios (bonus points)
            )
        }
    }
}
```

### 2. Wired Notification Responses **(R63B - Novo)**

O **WiredRewardNotificationResponse** responde a wired triggers que entregam recompensas:

```kotlin
class WiredRewardNotificationResponse {
    @Response(Incoming.WIRED_REWARD) // Trigger de recompensa
    fun notification(session: HabboSession, reward: RewardType?) {
        val reason = when(reward) {
            null -> null  // Erro genérico
            // Código baseado em tipo de wired trigger
            is Badge -> Awarded.badgeCode.toLong() + 0   // BADGE_REWARDED
            is Item -> Awarded.itemId.toLong() - 7      // ITEM_REWARDED
            // ... outros tipos
        }
        
        session.sendHabboResponse(Outgoing.WIRED_REWARD_NOTIFICATION) {
            writeInt(reason ?: ERROR_GENERIC)
            if (reward != null) {
                writeBoolean(true)     // success = true
                writeLong(reward.id)   // ID do prêmio
            } else {
                writeBoolean(false)    // success = false  
                writeString(errorReason)  // reason codes explained below
            }
        }
    }
}
```

**Reason Codes:** Error reasons sent on failure:
- `ERROR_GENERIC` - Generic wired reward error (default)
- `0` (`BADGE_REWARDED`) - Badge rewarded successfully through plugin/system
- `-7` (`ITEM_REWARDED`) - Item rewarded successfully through system
- `1` - "Limite de prêmios atingido" (max rewards per account reached)
- `2` - "Prêmio já ganhado por esta conta" (already awarded to this user)
- `3` - "Limite de prêmios por dia" (max rewards per day reached)
- `4` - "Não teve sorte na rodada" (bad luck on this round)
- `5` - "Prêmios máximos do dia já entregues" (daily limit exhausted)
- `8` - "Limite de prêmio por minuto atingido" (minute-based throttling)

### 3. Handlers by Type (Communication Module)

**Catalog:**
```kotlin
class CatalogOfferHandler {
    fun handle(session: HabboSession, request: CatalogRequest): Result {
        val offerId = request.offerId
        
        when(offerType) {
            CATALOG_OFFER_TYPE_LTD -> checkLTDPurchase(session, offerId) {
                // ACH_LTDPurchaser + ACH_LTDEarlyBird (herdado)
            }
            CATALOG_OFFER_TYPE_PHOTO -> checkCameraPhotoAccess(session, offerId) {
                // ACH_CameraPhotoCount (quando revela foto)
            }
        }
        
        // Verifica se user já tem todos os prêmios das ofertas passadas
        if (!hasAllRewards(session.offeredRewardIds)) {
            return Result.success(
                Offered(id = offerId, hasReward = true)  // User has all rewards already
            )
        }
    }
}
```

**Builders Club (R63B):**
```kotlin
room.applyDecorationHandler(session, newFurniList) {
    val isDecoratedWithBCDeco = checkBCDecoration(furniList) // Verifica IDs de móveis BC
    
    if (isDecoratedWithBCDeco && hasNotWonACH_BuildersClub(session)) {
        progress(ACH_BuildersClub, amount, accumulate = true)
    }
}
```

**Landing/Badge Rewards:**
```kotlin
class LandingRequestBadgeHandler {
    fun handle(session: HabboSession): Result {
        // Verifica badges via session.badges e plugins como RoomBadgeCommandsListener
        val newRewards = calculateRewards(badges)
        
        for (reward in newRewards) {
            when(reward) {
                is Badge -> sendWiredReward(session, reward, Rewarded.BADGE_REWARDED)  // Wired trigger notification via plugin
                is Item -> sendWiredReward(session, reward, Rewarded.ITEM_REWARDED)  // System-wide item rewards via plugin
            }
        }
        
        return Result.success(Awarded(id = requestId, success = true))
    }
}
```

### 4. Wired Triggers (R63B - Otimização de Performance)

**Interface:**
```kotlin
class WiredTrigger : IRoomTask {
    override fun executeTask(room: Room)?   // Returns task to be scheduled for next cycle
    private val triggerId: Int          // ID for wired system tracking in room.wiredTriggers
    
    @Suppress("UNUSED_PARAMETER")
    private fun onTaskScheduled(room: Room, task: IRoomTask) {
        // Wired trigger logic runs every tick when executed
        this.executeTask(room)      // Schedule new task (called during current cycle execution)
    }
}
```

**Trigger Types (19 total in WiredTriggerType):**
- `TRIGGER_TYPE_1` - Generic trigger type 1
- `TRIGGER_TYPE_2` through `TRIGGER_TYPE_14` - Types 2-14
- `REWARD_TRIGGER_TYPE` - Trigger for wired rewards
- `CLOCK_REACH_TIME(15)` - Custom UI triggered reward after time interval (e.g., 10 mins before login)
- Other specialized triggers like COLLISION, GAME_ENDS, USER_PERFORMS_ACTION, etc.

**Implementation Example:**
```kotlin
class ClickFurniTrigger : IRoomTask {
    override fun executeTask(room: Room): IRoomTask? {
        val result = checkFurniInteraction(room.roomUser)
        
        if (result.success && result.triggeredByWired) {
            return TriggeredWithReward(triggerId, wiredId).then() // Schedule wired reward trigger
        }
        return null
    }
}
```

## Sistema de Achievements (Atualizado)

### AchievementManager
```kotlin
object AchievementManager {
    fun progress(userId: Int, name: String, amount: Int, accumulate: Boolean = true): Result {
        return try {
            val result = progress(userId, groupIdByName(name.trimPrefix("ACH_")), amount, accumulate)
            
            runCatching { achievementGroup } // Verifica se level up ocorreu
            
            if (result.level > currentLevel) {
                unlock(achievementGroup, levelDifference)  // Sends notification
            }
            
            result // Returns progress update status
        } catch (e: Exception) {
            throw e
        }
    }
    
    private suspend fun saveQueuedAchievements(): Result {
        return try {
            val queued = concurrentQueue.toList()
            for ((userId, groupId, amount) in queued) {
                progress(userId, groupId, amount)
            }
            queue.clear()  // Remove saved progress from queue
            
            Result.success(true)
        } catch (e: Exception) {
            logError(e)
            Result.fail("Queue save failed", e)
        }
    }
}

// Concurrency control via concurrent collections prevents race conditions
class ConcurrentQueue : ConcurrentHashMap<Long, Pair<Int, Int>>()  // userId -> groupId/amount
```

### Achievement Categories (Types):
- `ACHIEVEMENT_CATEGORY_ROOM_DECORATION` - Mudança de pisos/decorações
- `ACHIEVEMENT_CATEGORY_BATTLE_BANZAI` - Batalhas Banzai
- `ACHIEVEMENT_CATEGORY_CUSTOMIZER` - Mudanças visuais, state (motto/avatar)
- `ACHIEVEMENT_CATEGORY_SHOPPING` - Compras LTDs/fotos/moedas câmbio
- `ACHIEVEMENT_CATEGORY_GAME_CENTER_SCORE` - Score achievements via game center

## Sistema de Database (DAO System)

### Padrão DAO para Achievements:
```kotlin
object AchievementDao {
    fun getAchievement(groupId: Int): List<Achievement> = select(
        "/sql/achievement/select_achievements.sql", 
        mapOf("groupId" to groupId)
    ) { rows ->
        Type(Achievement(
            id = it.int("id"),
            groupId = it.int("groupId"),
            level = it.int("level"), // target level (ex: 1-3)
            rewardActivityPoints = calculateRewardPoints(targetLevel), // 2x * remaining levels
            rewardAchievementPoints = calculateBonusPoints(targetLevel), // Bonus points
            progressRequirement = getProgressRequirement(level),
            enabled = false // Disabled in database to mark inactive/unused achievements
        ))
    }

    fun updateBatchUserProgress(userId: Int, groupIdsAndAmounts: List<Pair<Int, Int>>) = update {
        query(
            "/sql/achievement/update_user_achievement.sql",
            mapOf(
                "userId" to userId,
                "groupIds" to groupIdsAndAmounts.joinToString(","), // Batch format
                "amounts" to groupIdsAndAmounts.map { it.second }  // Corresponding amounts
            )
        )
    }
}
```

### SQL Queries (formato R63B):
**Insert Batch Update:**
```sql
INSERT INTO user_achievements (userId, groupId, progress) 
VALUES (:userId, :groupIds, :amounts)
ON DUPLICATE KEY UPDATE progress = VALUES(progress) + PROGRESS_GROUP(groupIds, groupIds)
```

**Update with Batch Format:**
```sql
UPDATE user_achievements u SET progress = p WHERE userId = :userId AND groupId = :groupIds
VALUES (:userId, SELECT SUM((progress - oldProgress) * GROUP_ID) FROM (
  -- List of pairs in format: userId,groupId,amount
)) groupIds
```

**Batch Query Pattern:** Uses `:parameter_name` for named parameters
```sql
SELECT id, groupId, name FROM achievement WHERE groupId IN (:group1, :group2, :group3)
SELECT * FROM user_achievements WHERE userId = :userId AND groupId IN (:groups)
```

## Sistema de Quartos (Rooms) - R63B Updates

### Dimmer System (Novidade R63B):
- Controle de desvio de luz (dimming) em quarto
- `RoomDimmer` class controla state por área/room section
- Integrado com Wired system para triggers no dimmer

### Task System:
```kotlin
class RoomTask : IRoomTask {
    private set val hostingCounter = AtomicInteger(0)  // ACH_RoomDecoHosting & Builders Club
    
    // ACH_BuildersClub detection (1 minute cycle):
    if (hostingCounter.incrementAndGet() >= 120) {
        hostingCounter.set(0)
        
        val isBCDecorated = checkRoomDecorationForBuildersClub(furnishings)
        if (isBCDecorated && hasNotWonACH_BuildersClub(session)) {
            progress("ACH_BuildersClub", amount, accumulate = true)
        }
    }
    
    override fun executeTask(room: Room): IRoomTask? {
        // Wired trigger execution
        return triggerTask.executeTask(room)  // Scheduled for next cycle
    }
}
```

### ACH_RoomDecoHosting (1 minute check):
```kotlin
if (room.hostingCounter.incrementAndGet() >= 120) {
    val guestCount = room.roomUsers.values.count { 
        it.habboSession != null && it.habboSession.userInformation.id != roomId 
    }
    
    progress("ACH_RoomDecoHosting", guestCount, accumulate = true)
}
```

## Sistema de Wired Triggers (19 Types)

### Trigger Base Implementation:
```kotlin
class IRoomTask {
    @get:Inject var triggerType: WiredTriggerType = null
    
    private init {
        val wiredTriggerId = generateWiredId() // Generate unique wire IDs
    }
    
    fun executeTask(room: Room)? {
        when(this.triggerType) {
            is TRIGGER_TYPE_1 -> ...     // Type 1 logic
            is TRIGGER_TYPE_2 -> ...     // Type 2 logic
            // ... each type implemented separately
            
            is CLOCK_REACH_TIME -> {   // Custom UI trigger
                val timer = System.currentTimeMillis() - lastTriggerTime
                if (timer >= requiredMinutes) {
                    // Send wired reward notification via plugin listener
                }
            }
            
            is COLLISION -> {  // Collision-based triggers
                checkCollision(roomUser)
            }
            
            // Each trigger type has its own executeTask logic
        }
    }
}
```

### Wiring to the Game:
- Wired triggers fire when user interacts or condition met during cycle
- Reward system integrated via `WiredRewardNotificationResponse`
- Plugin-based architecture for badge and item rewards (RoomBadgeCommandsListener, RoomItemCommandsListener)

## Plugin System (R63B)

**Plugin Events & Listeners:**
```kotlin
// Event listeners trigger wired notifications on room items
class RoomItemCommandsListener : IRoomCommandsListener {
    override fun sendReward(room: Room, item: Item?): RewardNotification? {
        return when(item?.wiredTriggerId) {
            null -> RewardNotification.ERROR_GENERIC
            // Plugin logic for wired triggers (badge/item rewards)
            // Checks if user has already received reward (prevents duplicate awards)
            is Badge -> sendBadgeReward(room, item, rewarded)
            else -> sendItemReward(room, item, rewarded)
        }
    }
    
    fun sendBadgeReward(room: Room, item: Item?, reward: Reward): String? {
        // Sends WiredRewardNotificationResponse via plugin system
        return "BADGE_REWARDED"  // Reason code for wired notification
    }
}
```

## Padrões Anti-Flood

**Wired Handler Flood Protection:**
```kotlin
// Per request throttle per wired trigger type per session
val lastWiredRequests: MutableMap<Int, Long> = ConcurrentHashMap()

@Handler(Incoming.WIRED_TRIGGER)
fun handleTrigger(wireId: Int): Result {
    val now = System.currentTimeMillis()
    val lastRequest = lastWiredRequests[wireId] ?: 0
    
    // Allow one request per session per type every X ms (e.g., 500ms)
    if (now - lastRequest < 500) return Result.error("Too frequent", null)
    
    lastWiredRequests.putIfAbsent(wireId, now)  // Set/update timestamp
}
```

**Benefício:** Each wired trigger has per-session limits to prevent client spammers from abusing the system.

## Convenções (Atualizadas com Wired Rewards System)

### Achievements: `ACH_[Description]`
- `ACH_Login`, `ACH_BuildersClub`, `ACH_BattleBallTilesLocked`, etc.

### Handlers: `[Module][Action]Handler`
- `CatalogOfferHandler`, `RoomApplyDecorationHandler`, `LandingRequestBadgeHandler`, `WiredTriggerXHandler`

### Wired Rewards Responses: `[Reward|System]NotificationResponse`
- `WiredRewardNotificationResponse` for all wired reward notifications (badges, items, etc.)

### DAOs: `[Module]Dao`
- `AchievementDao`, `ItemDao`, `RoomDataDao`

## Checklist R63B Development

- [x] Wired system 19 trigger types implemented (game/item/wired/trigger/triggers/)
- [x] Wired reward triggers with notification responses (WiredRewardNotificationResponse)
- [x] Room dimmer management (game/room/dimmer)
- [x] Wired item interactors (RollerItemInteractor, WiredItemInteractor)
- [x] Wired trigger listeners for rewards (RoomBadgeCommandsListener, RoomItemCommandsListener)
- [x] Wired trigger flood control (per-session limits)
- [x] Achievements system (AchievementManager, queue-based batch save)
- [x] Achievement inheritance support (login processing, herding from R63B data)
- [x] Room tasks for achievements (ACH_RoomDecoHosting, ACH_BuildersClub)
- [x] Catalog purchase integration (LTDs + Camera photos)
- [x] Wired reward reason codes documented

## Dicas de Debugging (R63B)

1. **Wired Triggers:** Check trigger state in `game/room/wired` for active/wired triggers
2. **Achievements:** Check `queue` in AchievementManager for pending saves; batch updates in SQL queries use GROUPING_ID format
3. **Wired Rewards:** Use plugin listeners (RoomItemCommandsListener) to debug which wired triggers are sending rewards
4. **SQL Batch Queries:** Use `GROUPING_ID` pattern: `(userId, groupId, progress)` as composite where key is `groupId`
5. **Plugin System:** Check event listeners for wired reward triggers (badge/item notification via WiredRewardNotificationResponse)

Recursos Úteis R63B:
- `HabboServer.reflections` - Scan of annotated handlers (@Handler) and tasks (@Task)
- `HabboConfig` - Server configuration including save intervals, time thresholds
- `WiredRewardNotificationResponse` - Wired reward system (badges, items, reason codes explained)
- `plugin/event/events/room/annotation` - Room plugin events and listeners for wired rewards

## Novidades Adicionadas na Versão Atualizada:

**R63B Features:**
1. **Wired Reward System:** Complete integration with Wired triggers (19 types), providing reward notifications via WiredRewardNotificationResponse, including reason codes for badges/items.
2. **Wired Triggers:** Full support for all 19 wired trigger types, integrated into room tasks for performance optimization and event-driven rewards.
3. **Dimmer System:** Room dimming with area-based control logic (game/room/dimmer/) managing brightness states for wired interactions.
4. **Wired Item Interactors:** Extended interactors supporting roller items and wired triggers with notification responses.
5. **RoomBadgeCommandsListener & RoomItemCommandsListener:** Plugin system for triggering wired rewards via badge/item plugins, using reason codes in WiredRewardNotificationResponse.
6. **Flood Control per Session:** Wired trigger handlers implement per-session limits to prevent client spam of wired reward notifications and actions.

**Wired Reward System (Novo em R63B):**
- `WiredRewardNotificationResponse` for all wired reward events (badge/item triggers)
- Reason codes explained: BADGE_REWARDED (0), ITEM_REWARDED (-7), etc.
- Plugin listeners handle wired rewards based on WiredTriggerType
- Per-session limits prevent abuse of wired reward features

**Achievements Herded:**
- `ACH_BuildersClub` detected via R63B decoration feature check (game/room/decorations) for BC furni ID detection at room level, similar to ACH_RoomDecoHosting but with specific BC decor IDs.

---
Documento criado e mantido continuamente como parte do guia de desenvolvimento Habbo R63B v2