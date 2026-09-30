# Plano de Implementação: Configurações de AFK, Sono e Moderação de Pets do Quarto (WIN63)

Este documento define a especificação e o plano de implementação completo para suporte, persistência e execução no ciclo de vida do jogo das novas configurações de quarto introduzidas na versão moderna do Habbo AIR Desktop (`WIN63-202605181326-263458590`+).

---

## 1. Contexto e Motivação

Durante a análise temporal da desserialização de `SaveRoomSettingsMessageComposer` ([`RoomSaveSettingsHandler.kt`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/communication/incoming/room/RoomSaveSettingsHandler.kt)) e serialização de `RoomSettingsDataEvent` ([`RoomSettingsResponse.kt`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/communication/outgoing/room/RoomSettingsResponse.kt)), identificou-se que na era AIR moderna (`WIN63 >= 2026-05-18`) a Sulake introduziu 6 novas configurações comportamentais de quarto:

| Nome do Campo | Tipo no AS3 | Tipo Kotlin | Default Sulake | Descrição |
|---|---|---|---|---|
| `leaveOnDoorTileEnabled` | `Boolean` (`readBoolean()`) | `Boolean` | `true` (1) | Expulsa o avatar do quarto caso permaneça parado no tile da porta (`!_doNotLeaveOnDoorTileCheckBox.isSelected`). |
| `idleSleepEnabled` | `Boolean` (`readBoolean()`) | `Boolean` | `true` (1) | Habilita o avatar a "dormir" (animação Zzz) quando inativo no quarto (padrão clássico do Habbo). |
| `idleSleepTimeoutSeconds` | `int` (`readInt()`) | `Int` | `1200` (20 min) | Tempo de inatividade (em segundos) necessário para o avatar adormecer. |
| `idleAutokickEnabled` | `Boolean` (`readBoolean()`) | `Boolean` | `false` (0) | Expulsa automaticamente o jogador por inatividade no quarto. |
| `idleAutokickTimeoutSeconds` | `int` (`readInt()`) | `Int` | `1800` (30 min) | Tempo de inatividade (em segundos) necessário para kickar o jogador. |
| `muteAllPets` | `Boolean` (`readBoolean()`) | `Boolean` | `false` (0) | Muta todas as falas de mascotes/pets dentro do quarto. |

Atualmente, o emulador apenas descarta esses campos ao ler (`RoomSaveSettingsHandler`) e envia valores estáticos mockados (`false`, `1200`, `1800`) na resposta (`RoomSettingsResponse`).

---

## 2. Decisão Arquitetural: Colunas SQL Explícitas vs. JSON

**Decisão**: Criar colunas relacionais explícitas na tabela `rooms` via migration Flyway (`V3`).

**Justificativas**:
1. **Zero Breaking Change**: A migration não altera nem move dados existentes; apenas adiciona 6 colunas com seus valores `DEFAULT`.
2. **Zero Overhead no Netty e Startup**: O emulador carrega todas as salas na subida via `SELECT * FROM rooms`. O mapeamento JDBC/Jdbi para tipos primitivos (`Int`, `Boolean`) é instantâneo e livre do overhead de CPU de parse de JSON (Jackson) em milhares de registros.
3. **Write-Behind sem Serialização**: O salvamento polimórfico (`AbstractDirtyEntity.flush()`) grava as propriedades com parâmetros nomeados sem re-serializar strings JSON.
4. **Consistência do Repositório**: Segue o padrão de `mute_settings`, `ban_settings`, `chat_type` e `allow_pets`.

---

## 3. Banco de Dados: Flyway Migration

Criar o arquivo `server/src/main/resources/db/migration/V3__add_room_afk_and_idle_settings.sql`:

```sql
ALTER TABLE `rooms`
  ADD COLUMN `leave_on_door_tile_enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `allow_walk_through`,
  ADD COLUMN `idle_sleep_enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `leave_on_door_tile_enabled`,
  ADD COLUMN `idle_sleep_timeout_seconds` int(11) NOT NULL DEFAULT 1200 AFTER `idle_sleep_enabled`,
  ADD COLUMN `idle_autokick_enabled` tinyint(1) NOT NULL DEFAULT 0 AFTER `idle_sleep_timeout_seconds`,
  ADD COLUMN `idle_autokick_timeout_seconds` int(11) NOT NULL DEFAULT 1800 AFTER `idle_autokick_enabled`,
  ADD COLUMN `mute_all_pets` tinyint(1) NOT NULL DEFAULT 0 AFTER `idle_autokick_timeout_seconds`;
```

---

## 4. Camada de Domínio e Persistência

### 4.1. [`RoomData.kt`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/game/room/RoomData.kt)
1. Adicionar os 6 campos no construtor primário (com defaults):
   ```kotlin
   leaveOnDoorTileEnabled: Boolean = false,
   idleSleepEnabled: Boolean = false,
   idleSleepTimeoutSeconds: Int = 1200,
   idleAutokickEnabled: Boolean = false,
   idleAutokickTimeoutSeconds: Int = 1800,
   muteAllPets: Boolean = false,
   ```
2. Adicionar as propriedades mutáveis com dirty-tracking:
   ```kotlin
   var leaveOnDoorTileEnabled: Boolean = leaveOnDoorTileEnabled; set(v) { if (field != v) { field = v; markDirty() } }
   var idleSleepEnabled: Boolean = idleSleepEnabled; set(v) { if (field != v) { field = v; markDirty() } }
   var idleSleepTimeoutSeconds: Int = idleSleepTimeoutSeconds; set(v) { if (field != v) { field = v; markDirty() } }
   var idleAutokickEnabled: Boolean = idleAutokickEnabled; set(v) { if (field != v) { field = v; markDirty() } }
   var idleAutokickTimeoutSeconds: Int = idleAutokickTimeoutSeconds; set(v) { if (field != v) { field = v; markDirty() } }
   var muteAllPets: Boolean = muteAllPets; set(v) { if (field != v) { field = v; markDirty() } }
   ```
3. Atualizar o método fábrica `RoomData.createPrivate(...)` para repassar os defaults.

### 4.2. [`RoomDao.kt`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/database/room/RoomDao.kt)
1. Atualizar o DTO `RoomDataDto`:
   - Adicionar os 6 campos com valores default para compatibilidade.
   - Mapear os 6 campos no método `toDomain()`.
2. Atualizar `RoomDao.updateRoomData(...)`:
   - Vincular os 6 novos parâmetros no mapa de parâmetros JDBC.

### 4.3. [`update_room_data.sql`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/resources/sql/rooms/data/update_room_data.sql)
Adicionar os campos no statement de update:
```sql
    `leave_on_door_tile_enabled`    = :leave_on_door_tile_enabled,
    `idle_sleep_enabled`            = :idle_sleep_enabled,
    `idle_sleep_timeout_seconds`    = :idle_sleep_timeout_seconds,
    `idle_autokick_enabled`         = :idle_autokick_enabled,
    `idle_autokick_timeout_seconds` = :idle_autokick_timeout_seconds,
    `mute_all_pets`                 = :mute_all_pets,
```

---

## 5. Camada de Rede (Packets)

### 5.1. Entrada: [`RoomSaveSettingsHandler.kt`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/communication/incoming/room/RoomSaveSettingsHandler.kt)
No bloco `when`:
```kotlin
habboSession.isAir && habboSession.isVersionAtLeast(2026, 5, 18) -> {
    chatFloodProtection = habboRequest.readInt()
    room.roomData.leaveOnDoorTileEnabled = habboRequest.readBoolean()
    room.roomData.idleSleepEnabled = habboRequest.readBoolean()
    room.roomData.idleSleepTimeoutSeconds = habboRequest.readInt()
    room.roomData.idleAutokickEnabled = habboRequest.readBoolean()
    room.roomData.idleAutokickTimeoutSeconds = habboRequest.readInt()
    room.roomData.muteAllPets = habboRequest.readBoolean()
}
```

### 5.2. Saída: [`RoomSettingsResponse.kt`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/communication/outgoing/room/RoomSettingsResponse.kt)
Substituir os mocks estáticos pelas propriedades reais do `room.roomData`:
```kotlin
isAir && isVersionAtLeast(2026, 5, 18) -> {
    writeInt(room.roomData.chatType)
    writeBoolean(room.roomData.leaveOnDoorTileEnabled)
    writeBoolean(room.roomData.idleSleepEnabled)
    writeInt(room.roomData.idleSleepTimeoutSeconds)
    writeBoolean(room.roomData.idleAutokickEnabled)
    writeInt(room.roomData.idleAutokickTimeoutSeconds)
    writeBoolean(room.roomData.muteAllPets)
}
```

---

## 6. Onde e Como Implementar a Lógica do Jogo

O próximo agente ou sessão deve analisar e plugar o comportamento dessas propriedades nas seguintes rotinas do servidor:

### 6.1. `leaveOnDoorTileEnabled` (Porta do Quarto)
- **Onde verificar**: Na task de movimentação de avatares ([`RoomUserWalkTask`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/game/room/tasks/RoomUserWalkTask.kt) ou [`RoomUserManager`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/game/room/managers/RoomUserManager.kt)).
- **Lógica**:
  1. Identificar o tile da porta da sala (`room.model.door`).
  2. Quando um avatar para de andar (`!roomUser.isWalking`) ou se encontra no tile da porta:
     - Se `room.roomData.leaveOnDoorTileEnabled == true`:
     - O servidor deve acionar a saída do usuário da sala (`room.userManager.removeUserFromRoom(...)`) ou desconectá-lo da sala de volta ao hotel view, simulando o efeito de pisar na porta para sair.

### 6.2. `idleSleepEnabled` & `idleSleepTimeoutSeconds` (Animação Zzz / Sono)
- **Onde verificar**: Na rotina de tick / status de inatividade do usuário (`RoomUser.idleSeconds` ou task periódica de quarto [`RoomUserStatusTask`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/game/room/tasks/RoomUserStatusTask.kt)).
- **Lógica**:
  1. Verificar se `room.roomData.idleSleepEnabled == true`.
  2. Se `roomUser.idleSeconds >= room.roomData.idleSleepTimeoutSeconds`:
     - Adicionar o status de sono ao avatar (ex: `user.setStatus("asleep", "true")` ou disparar a animação Zzz).
  3. Se `idleSleepEnabled == false`, avatares nesta sala não exibem a animação de sono por inatividade.

### 6.3. `idleAutokickEnabled` & `idleAutokickTimeoutSeconds` (Expulsão por Inatividade)
- **Onde verificar**: No loop de verificação de inatividade de quarto ([`RoomUserTask`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/game/room/tasks/RoomUserTask.kt)).
- **Lógica**:
  1. Se `room.roomData.idleAutokickEnabled == true`:
  2. Para cada usuário na sala:
     - **Bypass**: Verificar se o usuário é o dono da sala (`isOwner`), moderador/staff ou possui permissões especiais que impedem o kick por inatividade.
     - Se `roomUser.idleSeconds >= room.roomData.idleAutokickTimeoutSeconds`:
       - Enviar aviso ou remover o usuário do quarto (`room.userManager.removeUserFromRoom(roomUser, notify = true)`).

### 6.4. `muteAllPets` (Silenciar Mascotes no Quarto)
- **Onde verificar**: Na lógica de chat / IA de pets e bots ([`PetManager`](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/server/src/main/kotlin/ovh/rwx/habbo/game/pet/PetManager.kt), rotinas de fala de pet ou processamento de comandos de voz em quarto).
- **Lógica**:
  1. Quando um pet tenta disparar uma mensagem de chat ou som no quarto:
  2. Verificar se `room.roomData.muteAllPets == true`.
  3. Se verdadeiro, cancelar o broadcast do pacote de chat do mascote para a sala.

---

## 7. Checklist de Validação

- [ ] Executar `./gradlew check` para garantir 0 erros de compilação.
- [ ] Validar que `verifyPacketSignatures` continua sem divergências em todos os 512 call-sites.
- [ ] Aplicar a migration e testar subida do servidor com HikariCP e Jdbi 3.
- [ ] Conectar com cliente AIR moderno (`WIN63`) e testar salvamento e recarregamento da janela de configurações do quarto.
