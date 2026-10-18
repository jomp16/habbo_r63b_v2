# Escopo: Passe de Trocas (Trading Pass) — Tutorial + Trocas Ativadas

Status: **escopo apenas** (nada implementado além dos hooks do Tutorial).

## Contexto

No Habbo, o usuário só pode trocar itens após desbloquear o **Passe de Trocas**.
Referência moderna (news 2021 / AIR 2026): 2 categorias — **Tutorial** e
**Trocas Ativadas**. No legado o "estrelas de cristal" não existe (o sistema
clássico é o respeito diário, `ACH_RespectGiven`/`ACH_RespectEarned`), e o
schema atual do banco já reflete isso (TradingPass4 ausente, intencional).

## Estado atual

### Hooks do Tutorial — 100% implementados ✅

| Código | Requisito | Hook |
|---|---|---|
| `ACH_AvatarLooks` (id 6, identity) | Mudar o visual | `UserChangeFigureHandler.kt:51` |
| `ACH_Tutorial1` (262) | Caminhar em um quarto | `RoomUser.onMovementStepCommitted()` (`RoomUser.kt:43`) |
| `ACH_Tutorial2` (263) | Abrir o próprio perfil | `UserProfileHandler.kt:61` |
| `ACH_Tutorial3` (264) | Escrever no chat do quarto | `UserChatTask.kt:99` |
| `ACH_Tutorial4` (265) | Conferir configurações do quarto | `RoomSettingsHandler.kt:44` |
| `ACH_Tutorial5` (266) | Mover/girar/colocar mobi | `RoomTakeItemHandler.kt:64`, `RoomPlaceItemHandler.kt:195` |

Todos são grupos de 1 nível, requisito 1 (exceto AvatarLooks também 1/1).

### Schema de TradingPass no banco (atual)

| Grupo | id | Níveis | Requisitos | Observação |
|---|---|---|---|---|
| `ACH_TradingPass1` | 268 | 1 | 1 | **Remodelado em 2026-10-09 via mariadb direto** (não há dump/schema Flyway de achievements): era grupo de 7 níveis (1,40,60,80,100,120,140) fora do padrão; nenhum `users_achievements` do grupo existia, delete seguro |
| `ACH_TradingPass2` | 269 | 1 | 120 (minutos online) | ok |
| `ACH_TradingPass3` | 270 | 1 | 3 (dias de conta) | ok |
| `ACH_TradingPass5` | 272 | 1 | 20 (salas de outros) | ok |
| `ACH_TradingPass6` | 273 | 1 | 6 (nível do avatar) | ok |
| `ACH_TradingPass7` | 274 | 1 | 2 (amizades) | ok |
| `ACH_TradingPass4` | — | — | — | **Inexistente** (estrelas de cristal = só AIR moderno). Intencional, manter ausente. |

Decisão do usuário (m00482): TradingPass são **permissões, igual ao tutorial** —
grupos separados de 1 nível. ✅ Migration aplicada (m00512): grupo 268 renomeado
para `ACH_TradingPass1`, níveis >1 removidos, requisito 1/1.

### Gate de troca — não implementado ❌

`TradeManager.canTrade()` (`server/.../game/room/trading/TradeManager.kt:245`)
hoje só exige `authenticated && inventário não vazio`. **Ninguém é bloqueado.**

## Decisões a tomar

1. **Gate do `canTrade()`**: usuário pode trocar somente com TODOS os grupos
   TradingPass1,2,3,5,6,7 no nível 1 (o 4 não existe — pular). Fonte:
   `achievementManager`, consulta por userId (funciona offline também, ver
   `AchievementDao.loadUserAchievements`).
3. **Feedback de bloqueio**: o que o cliente 2013 mostra quando a troca é
   negada? Verificar no AS3 a resposta de erro de troca
   (provavelmente um alerta via `GenericError`/`TradeFailed`); definir a UX.
4. **Fechamento do ciclo**: `ACH_TradingPass1` progride automaticamente quando
   os 6 grupos do Tutorial (incl. `ACH_AvatarLooks`) atingem nível máximo.
   Onde colocar o check? Sugestão: dentro de `AchievementManager` após cada
   level up (barato: 6 lookups em map).

## Hooks a implementar

| Grupo | Evento fonte | Local provável |
|---|---|---|
| `ACH_TradingPass1` | Level up: todos os grupos tutorial completos | `AchievementManager` (pós-level-up) |
| `ACH_TradingPass2` | Acumular minutos online (120) | Task de heartbeat de sessão (verificar `UserStats`/cron existente) |
| `ACH_TradingPass3` | Idade da conta ≥ 3 dias | Login (compute de `account_created`; retroativo, `accumulate=false`) |
| `ACH_TradingPass5` | Visitou 20 salas de outros usuários | Handler de entrada em quarto (`roomData.ownerId != userId`) |
| `ACH_TradingPass6` | Avatar nível 6 | Hook de level up do XP do usuário |
| `ACH_TradingPass7` | 2 amizades aceitas | `MessengerManager`/friend accept |

## Ordem sugerida

1. Migration: remodelar 268 → TradingPass1 (grupo 1 nível).
2. `canTrade()` gate + resposta de erro do cliente.
3. Hooks TradingPass3 (idade — trivial, retroativo), TradingPass5 (salas), TradingPass7 (amigos).
4. TradingPass2 (minutos online) — depende da infra de heartbeat.
5. TradingPass6 (nível 6) — depende do sistema de nível/XP existir; se não existir, deixar stub.
6. TradingPass1 (tutorial completo) — por último, fecha o ciclo e ativa o gate de fato.

## Pendências de investigação

- Como o cliente 2013 sinaliza "troca bloqueada" (composição AS3 do erro).
- Existe task de minutos online / XP de nível de avatar no servidor hoje?
- O `update_achievements.py` do mirror (`swf.habbo.rwx.ovh/`) interage com a
  tabela `achievements` — confirmar que a migration do 268 não conflita.
