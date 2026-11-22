# Passe de Trocas (Trading Pass) — Tutorial + Trocas Ativadas

Status: **gatilhos implementados** (exceto `ACH_TradingPass6`); o **gate de troca** (`canTrade()`) ainda não existe.

## Contexto

No Habbo, o usuário só pode trocar itens após desbloquear o **Passe de Trocas**.
Referência moderna (news 2021 / AIR 2026): 2 categorias — **Tutorial** e
**Trocas Ativadas**. No legado o "estrelas de cristal" não existe (o sistema
clássico é o respeito diário, `ACH_RespectGiven`/`ACH_RespectEarned`), e o
schema do banco já reflete isso (TradingPass4 ausente, intencional).

Todos os grupos são **permissões**: grupos separados de 1 nível, requisito
conforme a tabela abaixo (textos conferidos em `external_flash_texts`).

## Grupos e gatilhos

### Tutorial (5 conquistas, sem `ACH_AvatarLooks`)

| Código | Requisito | Gatilho |
|---|---|---|
| `ACH_Tutorial1` | Dar uma volta pelo Hotel | `RoomUser.onMovementStepCommitted()` |
| `ACH_Tutorial2` | Abrir o próprio perfil | `UserProfileHandler` |
| `ACH_Tutorial3` | Escrever no chat do quarto | `UserChatTask` |
| `ACH_Tutorial4` | Conferir configurações do quarto | `RoomSettingsHandler` |
| `ACH_Tutorial5` | Mover/girar/colocar mobi | `RoomTakeItemHandler`, `RoomPlaceItemHandler` |

`ACH_AvatarLooks` (mudar o visual, `UserFigureChanger`) tem gatilho próprio mas
**não faz parte do tutorial** e não conta para o `ACH_TradingPass1`.

### Trocas Ativadas

| Grupo | Requisito | Gatilho ao vivo | Backfill no login |
|---|---|---|---|
| `ACH_TradingPass1` | Completar o tutorial (5 grupos no nível máximo) | `AchievementManager.progress` → `completeTutorialIfDone` | `completeTutorialIfDone` |
| `ACH_TradingPass2` | 120 minutos online | tick periódico (`processPeriodicAchievementsImpl`) | `totalOnlineSeconds / 60` |
| `ACH_TradingPass3` | 3 dias de conta | — | dias desde `accountCreated` |
| `ACH_TradingPass5` | 20 visitas a salas de outros | `UserJoinRoomTask` (dono ≠ visitante) | progresso já gravado de `ACH_RoomEntry` |
| `ACH_TradingPass6` | Nível 6 do avatar | **sem gatilho** | — |
| `ACH_TradingPass7` | 2 amizades | `MessengerAcceptRequestHandler` (quem aceita e quem convidou, se online) | `habboMessenger.friends.size` |
| `ACH_TradingPass4` | — | **Inexistente** (estrelas de cristal = só AIR moderno). Manter ausente. | — |

`ACH_TradingPass6`: o servidor não tem XP/nível de avatar. Pendente definir o
critério (ideia: limite de `achievementScore`, valor a decidir).

## Mecanismo de backfill (`SessionAchievements.kt`)

`processLoginAchievements()` roda a cada login. `backfillDerivedAchievements()`
recalcula grupos derivados de estado já persistido com `accumulate = false`
(só sobe, idempotente), creditando contas antigas e eventos de antes do gatilho
ao vivo existir. Além dos TradingPass cobre `ACH_Name` (vale quando
`userInformation.realname` está preenchido) e `ACH_PetLover` (contagem de
`users_pets`).

Regra para grupos novos: gatilho ao vivo (`accumulate = true` para contadores,
`false` para estados absolutos) **e**, se o dado existir no banco, a linha
correspondente no backfill. Contadores sem histórico (carinhos, recados,
níveis de mascote) não têm backfill.

## Outras conquistas ligadas junto

| Grupo | Gatilho |
|---|---|
| `ACH_Login` | 1º login do dia; **dias seguidos**: `UserStats.loginStreakContinues` (último login = ontem), senão `AchievementManager.resetProgress` zera a sequência (o nível conquistado fica) |
| `ACH_GiftGiver` / `ACH_GiftReceiver` | `CatalogGiftService`; backfill por `giftsGiven`/`giftsReceived` |
| `ACH_NotesLeft` / `ACH_NotesReceived` | `RoomSavePostItHandler`: 1º texto de um post-it em sala de outro (editar não pontua); dono pode estar offline |
| `ACH_PetLover` | `+1` na compra (`CatalogPetService`); backfill por contagem de pets |
| `ACH_PetRespectGiver` / `ACH_PetRespectReceiver` | `PetRespectHandler`, só em mascote de outro usuário |
| `ACH_PetLevelUp` | níveis ganhos pelo mascote, creditados ao dono (`PetRespectHandler`, `RoomPet`) |

Os gatilhos de mascote ficam em `game/pet/PetAchievements.kt` (rodam em IO: o
dono pode estar offline e `progress(session, userId, ...)` grava no banco).
`ACH_PetFeeding` **não tem gatilho**: não há alimentação de mascote no servidor.

## Gate de troca — não implementado ❌

`TradeManager.canTrade()` (`server/.../game/room/trading/TradeManager.kt`) hoje
só exige `authenticated && inventário não vazio`. **Ninguém é bloqueado.**

Decisões pendentes:

1. **Gate**: pode trocar somente com `ACH_TradingPass1,2,3,5,6,7` no nível 1
   (o 4 não existe). Consulta por userId (funciona offline, ver
   `AchievementDao.loadUserAchievements`). **Só ativar quando o `TradingPass6`
   tiver gatilho**, senão ninguém consegue trocar.
2. **Feedback de bloqueio**: ver no AS3 a resposta de erro de troca
   (provavelmente alerta via `GenericError`/`TradeFailed`) e definir a UX.

## Pendências

- Critério e gatilho do `ACH_TradingPass6` (nível do avatar).
- Gate do `canTrade()` + mensagem de bloqueio.
- `ACH_PetFeeding` (depende do sistema de comida de mascote).
- `update_achievements.py` do mirror (`swf.habbo.rwx.ovh/`) mexe na tabela
  `achievements`: confirmar que não conflita com os grupos remodelados.
