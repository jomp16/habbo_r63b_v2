# Wired 2.0 - Arquitetura e Implementação Completa

## 📌 Visão Geral

O sistema Wired 2.0 é uma reformulação completa do mecanismo de automação de quartos do Habbo. O servidor implementa o pipeline completo de execução, gerenciamento de contexto, resolução de alvos, interpolação de placeholders, sistema de variáveis multinível, monitoramento de performance e 153 componentes Wired.

---

## ⚙️ 1. Pipeline de Execução (`WiredHandler`)

A execução de qualquer pilha Wired segue uma ordem visual e lógica estrita baseada na coordenada Z (da base para o topo do stack):

```
┌────────────────────────────────────────────────────────────────────────┐
│                        1. SELETORES (Z-Order)                          │
│  (Coleta e pré-popula targetFurnis e targetUsers no WiredContext)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          2. ADDONS (Z-Order)                           │
│  (Aplica limites de execução, filtros, placeholders e modificadores)   │
│  * Se context.cancelled == true -> Aborta a pilha                      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         3. TRIGGER (Ativação)                          │
│  (Valida se o evento corresponde aos parâmetros do gatilho)            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       4. CONDIÇÕES (Validação)                         │
│  - Padrão: AND (todas as condições precisam ser verdadeiras)           │
│  - Com WiredAddonOrEval: OR (qualquer condição verdadeira avança)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         5. EFEITOS (Execução)                          │
│  - Padrão: Executa todos os efeitos da pilha                           │
│  - Com WiredAddonRandom: Escolhe 1 efeito aleatório                   │
│  - Com WiredAddonUnseen: Escolhe 1 efeito inédito por ciclo            │
│  - Com WiredAddonExecuteInOrder: Escolhe 1 efeito sequencial por tick  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      6. MOVIMENTAÇÃO EM LOTE                           │
│  (Executa flush de todas as animações/slides em lote no tick da sala) │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 2. Resolução Dinâmica de Alvos (`WiredContext`)

O `WiredContext` unifica a comunicação entre os blocos da pilha:

- **Alvos de Usuário (`WiredUserSource`)**:
  - `TRIGGERING_USER (0)`: Usuário que ativou o gatilho.
  - `CLICKED_USER (10)` / `REACHED_USER (11)`: Usuário clicado ou colidido.
  - `BOT_BY_NAME (100)`: Bot configurado por nome.
  - `USER_BY_NAME (101)`: Habbo específico pelo nome.
  - `SELECTOR_USERS (200)`: Usuários capturados por Seletores na mesma tick.
  - `ALL_ROOM_USERS (900)`: Todos os usuários presentes no quarto.

- **Alvos de Mobis (`WiredFurniSource`)**:
  - `TRIGGERING_ITEM (0)`: Mobi que acionou o gatilho.
  - `SELECTED_ITEMS (100)`: Mobis selecionados manualmente na janela do Wired.
  - `SELECTOR_ITEMS (200)`: Mobis capturados por Seletores.
  - `ALL_ROOM_ITEMS (900)`: Todos os mobis presentes no quarto.

- **Sistema de Placeholders (`formatPlaceholders`)**:
  - Interpolação de `%username%`, `%user%`, `%userid%`, `%target_user%`.
  - Interpolação de `%furniname%`, `%furni%`, `%target_furni%`.
  - Placeholders dinâmicos: `%chave%`, `$chave`, `$(chave)`, `#chave`, `#(chave)`.
  - Variáveis do quarto e contexto: `%var.nome%`, `%variable.nome%`, `$var.nome`.

---

## 🧩 3. Componentes Implementados (153 no Total)

### 3.1 Seletores (15)
| Classe | InteractionType | Função |
| :--- | :--- | :--- |
| `WiredSelectorFurniAltitude` | `wf_cnd_furni_altitude` | Seleciona mobis por altitude Z (mínimo, máximo, absoluto ou relativo). |
| `WiredSelectorFurniNeighborhood` | `wf_cnd_furni_neighbor` | Seleciona mobis na vizinhança em espiral do mobi de origem. |
| `WiredSelectorFurniOnFurni` | `wf_cnd_furni_on_furni` | Seleciona mobis que estão sobre outros mobis. |
| `WiredSelectorFurniSignal` | `wf_cnd_furni_signal` | Seleciona mobis associados a um canal de sinal. |
| `WiredSelectorFurniWithVariable` | `wf_cnd_furni_with_var` | Seleciona mobis cujo valor de variável satisfaz a condição (`==`, `!=`, `<`, `<=`, `>`, `>=`). |
| `WiredSelectorRemote` | `wf_cnd_remote` | Importa alvos de pilhas ou seletores remotos no quarto. |
| `WiredSelectorUsersByAction` | `wf_cnd_users_act` | Seleciona usuários executando ações específicas (sentar, deitar, acenar, etc.). |
| `WiredSelectorUsersByName` | `wf_cnd_users_name` | Seleciona usuários por lista de nomes. |
| `WiredSelectorUsersGroup` | `wf_cnd_users_group` | Seleciona usuários membros de um grupo específico. |
| `WiredSelectorUsersHanditem` | `wf_cnd_users_hand` | Seleciona usuários segurando um item de mão específico. |
| `WiredSelectorUsersNeighborhood` | `wf_cnd_users_neighbor` | Seleciona usuários na vizinhança em espiral do emissor/mobi. |
| `WiredSelectorUsersOnFurni` | `wf_cnd_users_on_furni` | Seleciona usuários situados sobre os mobis configurados. |
| `WiredSelectorUsersSignal` | `wf_cnd_users_signal` | Seleciona usuários associados a um sinal de rádio/wired. |
| `WiredSelectorUsersTeam` | `wf_cnd_users_team` | Seleciona usuários pertencentes a times (Banzai/Freeze). |
| `WiredSelectorUsersWithVariable` | `wf_cnd_users_with_var` | Seleciona usuários cujo valor de variável satisfaz a condição. |

### 3.2 Addons (33)
| Addon | InteractionType | Função |
| :--- | :--- | :--- |
| `WiredAddonOrEval` | `wf_xtra_or_eval` | Avalia condições da pilha usando lógica `OR` (`any`). |
| `WiredAddonUnseen` | `wf_xtra_unseen` | Roteia para efeitos ainda não executados antes de reiniciar o ciclo. |
| `WiredAddonRandom` | `wf_xtra_random` | Roteia a execução para 1 efeito aleatório da pilha. |
| `WiredAddonExecuteInOrder` | `wf_xtra_exec_in_order` | Executa 1 efeito sequencial por ativação. |
| `WiredAddonExecutionLimit` | `wf_xtra_execution_limit` | Limita o número de execuções por janela de tempo (pulses de 500ms). |
| `WiredAddonFilterFurni` | `wf_xtra_filter_furni` | Filtra mobis selecionados por quantidade ou intervalo de índices. |
| `WiredAddonFilterUsers` | `wf_xtra_filter_users` | Filtra usuários selecionados por quantidade ou intervalo de índices. |
| `WiredAddonFilterFurniByVariable` | `wf_xtra_filter_furni_by_var` | Filtra mobis selecionados pelo valor de uma variável. |
| `WiredAddonFilterUsersByVariable` | `wf_xtra_filter_users_by_var` | Filtra usuários selecionados pelo valor de uma variável. |
| `WiredAddonNoMoveAnimation` | `wf_xtra_mov_no_animation` | Remove animação de interpolação no movimento de mobis. |
| `WiredAddonMovementPhysics` | `wf_xtra_mov_physics` | Ajusta física e suavização de trajetórias. |
| `WiredAddonCarryUsers` | `wf_xtra_mov_carry_users` | Transporta avatares sobre o mobi durante seu deslocamento. |
| `WiredAddonAnimationTime` | `wf_xtra_anim_time` | Ajusta velocidade e duração de animações de movimento. |
| `WiredAddonRotateToDirection` | `wf_xtra_rotate_to_dir` | Rotaciona mobi para a direção do seu deslocamento (Projétil). |
| `WiredAddonMovementCurve` | `wf_xtra_mov_curve` | Aplica curvatura/pulo e força de salto à trajetória. |
| `WiredAddonTextOutputUsername` | `wf_xtra_text_output_username` | Exporta nome de usuário para placeholder customizado. |
| `WiredAddonTextOutputFurniName` | `wf_xtra_text_output_furni_name` | Exporta nome de mobi para placeholder customizado. |
| `WiredAddonTextOutputVariable` | `wf_xtra_text_output_variable` | Exporta valor de variável para placeholder customizado. |
| `WiredAddonTextInputVariable` | `wf_xtra_text_input_variable` | Captura entrada de texto/número e salva na variável de destino. |
| `WiredAddonGlobalPlaceholder` | `wf_xtra_global_placeholder` | Registra placeholder global (estático ou de outro quarto). |
| `WiredAddonVariableTextConnector` | `wf_xtra_var_text_connector` | Conecta template de texto e exporta string formatada. |
| `WiredAddonVariableLevelUpSystem` | `wf_xtra_var_lvlup_system` | Sistema completo de XP/Nível (Linear, Exponencial ou Tabela Manual). |
| `WiredAddonVariableTimeUtil` | `wf_xtra_var_time_util` | Decompõe timestamps em componentes de tempo e unidades. |
| `WiredAddonVariableWebApi` | `wf_xtra_var_web_api` | Gerencia chaves de leitura/escrita e permissões para Web API. |
| `WiredAddonAchievementEnabler` | `wf_xtra_achievement_enabler` | Permite o desbloqueio/progresso de conquistas via Wired. |
| `WiredAddonCustomContract` | `wf_xtra_custom_contract` | Realiza cobrança e recompensa condicionais entre jogadores e quarto. |
| `WiredAddonScanChestFurniByType` | `wf_xtra_scan_chest_furni_by_type` | Faz escaneamento e contagem de itens em baús/quarto. |
| `WiredAddonVariableFxHealthPoints` | `wf_xtra_varfx_hp` | Barra de pontos de vida (HP) sobre o avatar/mobi. |
| `WiredAddonVariableFxProgressBar` | `wf_xtra_varfx_prog` | Barra de progresso visual sobre avatar/mobi. |
| `WiredAddonVariableFxLevellingProgress` | `wf_xtra_varfx_levelling` | Visualizador visual de progresso de level-up. |
| `WiredAddonVariableFxStatusBar` | `wf_xtra_varfx_status` | Barra de status customizável sobre avatar/mobi. |
| `WiredAddonVariableFxBossBar` | `wf_xtra_varfx_boss` | Barra de chefe (Boss Bar) no topo da tela. |
| `WiredAddonVariableFxNumberDisplay` | `wf_xtra_varfx_number` | Indicador numérico flutuante com ícone customizado. |

### 3.3 Triggers (27)
Gatilhos clássicos e 2.0 incluindo:
- Tempo e Periódicos (`WiredTriggerPeriodically`, `WiredTriggerPeriodicallyLong`, `WiredTriggerPeriodicallyShort`, `WiredTriggerAtGivenTime`, `WiredTriggerAtTimeLong`, `WiredTriggerClockCounter`).
- Interação (`WiredTriggerClickFurni`, `WiredTriggerClickTile`, `WiredTriggerClickUser`, `WiredTriggerWalksOnFurni`, `WiredTriggerWalksOffFurni`, `WiredTriggerCollision`, `WiredTriggerStateChanged`, `WiredTriggerStuffState`).
- Entidades & Chat (`WiredTriggerEnterRoom`, `WiredTriggerLeaveRoom`, `WiredTriggerSaysSomething`, `WiredTriggerUserPerformsAction`).
- Sinais, Variáveis & Transações (`WiredTriggerReceiveSignal`, `WiredTriggerVariableChanged`, `WiredTriggerTransactionComplete`, `WiredTriggerTransactionFail`, `WiredTriggerScoreAchieved`, `WiredTriggerGameStarts`, `WiredTriggerGameEnds`).
- Bots (`WiredTriggerBotReachesAvatar`, `WiredTriggerBotReachesFurni`).

### 3.4 Condições (29)
- Estado e Posição (`WiredConditionStuffIs`, `WiredConditionHasFurniOn`, `WiredConditionTriggerOnFurni`, `WiredConditionHasAltitude`, `WiredConditionMatchSnapshot`, `WiredConditionValidMoves`).
- Usuários & Equipes (`WiredConditionHasAvatars`, `WiredConditionUserCount`, `WiredConditionTriggererMatch`, `WiredConditionInGroup`, `WiredConditionInTeam`, `WiredConditionTeamHasScore`, `WiredConditionTeamHasRank`, `WiredConditionWearingBadge`, `WiredConditionWearingEffect`, `WiredConditionHasHanditem`, `WiredConditionActorDirection`, `WiredConditionUserPerformsAction`).
- Tempo & Contadores (`WiredConditionTime`, `WiredConditionMatchTime`, `WiredConditionMatchDate`, `WiredConditionDateRangeActive`, `WiredConditionCounterTimeMatches`).
- Variáveis & Seletores (`WiredConditionHasVariable`, `WiredConditionVariableValueMatch`, `WiredConditionVariableAgeMatch`, `WiredConditionSelectorQuantity`, `WiredConditionChestHasItems`, `WiredConditionChestHasItemType`).

### 3.5 Efeitos (49)
- Movimentação & Teleporte (`WiredEffectMoveRotate`, `WiredEffectMoveToDirection`, `WiredEffectRelativeMove`, `WiredEffectMoveFurniTo`, `WiredEffectMoveFurniAsGroup`, `WiredEffectMoveUserToFurni`, `WiredEffectMoveRotateUser`, `WiredEffectSetAltitude`, `WiredEffectTeleportToFurni`, `WiredEffectTeleportToRoom`, `WiredEffectChase`, `WiredEffectFlee`, `WiredEffectFurniToFurni`, `WiredEffectFurniToUser`).
- Manipulação de Mobis (`WiredEffectToggleState`, `WiredEffectToggleToRandom`, `WiredEffectMatchToScreenshot`, `WiredEffectPlaceFurni`, `WiredEffectRemoveFurni`, `WiredEffectGiveFurni`).
- Controle de Usuários (`WiredEffectShowMessage`, `WiredEffectFreezeUser`, `WiredEffectMuteTriggerer`, `WiredEffectKickUser`, `WiredEffectJoinTeam`, `WiredEffectLeaveTeam`, `WiredEffectGiveScore`, `WiredEffectGiveScoreTeam`, `WiredEffectGiveCurrency`, `WiredEffectGiveReward`).
- Fluxo, Sinais & Transações (`WiredEffectCallStack`, `WiredEffectResetTimers`, `WiredEffectSendSignal`, `WiredEffectInitTransaction`, `WiredEffectCancelTransaction`, `WiredEffectClickConf`, `WiredEffectAdjustClock`, `WiredEffectControlClock`, `WiredEffectLog`).
- Variáveis (`WiredEffectChangeVariableValue`, `WiredEffectGiveVariable`, `WiredEffectRemoveVariable`).
- Bots (`WiredEffectBotMove`, `WiredEffectBotTalk`, `WiredEffectBotTalkToAvatar`, `WiredEffectBotTeleport`, `WiredEffectBotGiveHanditem`, `WiredEffectBotFollowAvatar`, `WiredEffectChangeBotFigure`).

---

## 💾 4. Sistema de Variáveis (`WiredVariableManager`)

- **Tabela no Banco de Dados**: `wired_variables` (`015_create_wired_variables.sql`).
- **Escopos Suportados**:
  - `ROOM`: Variáveis do quarto (persistentes ou temporárias).
  - `USER`: Variáveis por usuário (persistentes ou de sessão).
  - `FURNI`: Variáveis vinculadas a itens.
  - `CONTEXT`: Variáveis transitórias da execução da pilha.
- **Sincronização com o Cliente**:
  - `WiredAllVariablesHashHandler`: Envia hash determinístico do estado de variáveis.
  - `WiredAllVariablesDiffsHandler`: Envia deltas e atualizações incrementais ao cliente AIR/Modern.

---

## 📈 5. Sistema de Performance & Logs de Erro

- **`WiredPerformanceMonitor`**: Calcula custo por componente (Gatilho: 1.0, Seletor: 2.0, Addon: 1.5, Condição: 2.0, Efeito: 3.0). Detecta quartos com circuitos pesados (`isHeavy()`).
- **`WiredErrorLogger`**: Rastreia exceções por categoria e frequência para diagnóstico.
- **`WiredRoomStatsResponse`**: Envia ao cliente as estatísticas reais de custos e contagens de variáveis.

---

## 🔮 6. Pontos de Integração Futura (TODOs)

- **IA e Entidades de Bots (`RoomUserManager`)**:
  - Os wireds de bot estão scaffoldados (`WiredEffectBot...`, `WiredTriggerBot...`) com marcações `// TODO: Requer implementação completa do subsistema de IA e entidades de Bots no RoomUserManager`.
- **Servidor HTTP REST para Web API (`WiredAddonVariableWebApi`)**:
  - O addon salva as chaves `api_read_key` e `api_write_key` e permissões de deleção em lote. Para uso externo, basta expor rotas HTTP que consultem o `WiredVariableManager` do quarto utilizando essas chaves.

