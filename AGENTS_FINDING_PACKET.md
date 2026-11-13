# Engenharia Reversa e Mapeamento de Pacotes (2011–2013)

Técnicas e armadilhas para mapear pacotes entre **R63A (pré-shuffle, 2011)** e **R63B (pós-shuffle, 2011–2013)**. As
regras gerais (nunca regex pesado no dump, In/Out invertido, hex no AS3, stacking height, Flyway) estão em `AGENTS.md`;
aqui ficam o pipeline, as âncoras e as armadilhas por pacote.

## 1. Fontes e ferramentas

1. **AS3 pré-shuffle não-ofuscado (referência dourada)**:
   `/home/jomp16/Downloads/Habbo/RELEASE63-35255-34886-201108111108/src/`. Nomes reais de classes/compositores:
    - `com/sulake/habbo/communication/HabboMessages.as`: `var_1414[header] = ComposerClass` (todos os headers estáticos
      da R63A).
    - `catalog/` (compra, recicladouro, presentes), `session/RoomSession.as` (dimmer, presentes, ações de quarto),
      `inventory/` (abas, unseen, floor vs wall), `roomevents/` (Wired).
2. **Dumps pós-shuffle**: `/mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/`
    - `Messages.txt`: `Outgoing[header, md5] = ClassName(param1:type, ...)` e
      `Incoming[header, md5] = ClassName(param1:type) [Parser: ParserClass]`.
    - `Habbo_codigo_completo.txt`: AS3 unificado (30MB+, só `rg`).
3. **`habbo-timeline <hash_ou_nome> <incoming|outgoing>`**: mostra em qual build um campo foi
   adicionado/alterado/removido (2009–2026).

**Lembrete In/Out**: `Outgoing[...]` do Flash = `releases_incoming_headers` do emulador; `Incoming[...]` =
`releases_outgoing_headers`.

## 2. Pipeline em 4 passos

1. **Nome e assinatura no pré-shuffle**:
   ```bash
   rg "^Outgoing\[<HEADER_ESTATICO>," /mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE_PRE_SHUFFLE>/Messages.txt
   rg "\[<HEADER_ESTATICO>\]\s*=" /home/jomp16/Downloads/Habbo/RELEASE63-35255-34886-201108111108/src/com/sulake/habbo/communication/HabboMessages.as
   habbo-timeline <HEADER_ESTATICO> incoming|outgoing
   ```
   (`Messages.txt` de `RELEASE34-18054-...` até `RELEASE63-20111004...` devolve classe, MD5 e argumentos; o timeline
   cruza as 275+ builds pré-shuffle.)
2. **Buscar na release alvo** por assinatura (2+ params ou tipos incomuns):
   ```bash
   rg "= _-.*\(param1:int, param2:int, param3:String\)" /mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/Messages.txt
   ```
3. **Propagar por hash MD5** para as releases irmãs (o hash costuma se manter se a assinatura não mudou):
   ```bash
   for rel in RELEASE63-201110051250-692472661 RELEASE63-201112130905-713192101 RELEASE63-201204021456-654375178 RELEASE63-201207040943-767851649; do
       echo "=== $rel ==="; rg "<HASH_MD5>" /mnt/DADOS/Downloads/Habbo/dumps_repo/releases/$rel/Messages.txt
   done
   ```
4. **Construtor genérico (`param1:int` ou vazio)**: não varra classes; use a tabela de âncoras.

Busca por classe no código completo (hex + decimal):

```bash
rg "\[(0x[0-9a-fA-F]+|\d+)\]\s*=\s*_SafeStr_XXXX;" /mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/Habbo_codigo_completo.txt
```

## 3. Âncoras diretas (`rg` no `Habbo_codigo_completo.txt`)

| Sistema                     | Âncora                                                                                                              | Entrega                                                                                                                                                                                                                   |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Trading (8 pacotes)         | `inventory.trading.warning.others_account_disabled`                                                                 | `TradingModel`; os 8 compositores ficam no mesmo package ofuscado (`_-BK` 2011-10, `_-2sQ` 2011-12, `_-008` 2012-04, `_-499` 2012-07).                                                                                    |
| Dados, Roda, Porta, Post-it | `ROFCAE_DICE_ACTIVATE` / `ROFCAE_DICE_OFF` / `ROFCAE_USE_HABBOWHEEL` / `ROFCAE_ENTER_ONEWAYDOOR` / `ROFCAE_STICKIE` | `useObject` no `RoomEngine`, um único `switch (_arg_5)`: `ROOM_TRIGGER_ROLL_DICE`, `ROOM_TRIGGER_CLOSE_DICE`, `ROOM_TRIGGER_HABBO_WHEEL`, `ROOM_TRIGGER_ONE_WAY_GATE`, `ROOM_POST_IT` (Incoming).                         |
| Moderação e quarto          | `function assignRights` ou `banUser`                                                                                | `RoomSession` (`_SafeStr_1107` em 2011-10, `_SafeStr_1731` em 2013-05). `kickUser`, `banUser`, `assignRights`, `muteUser`, `pickUpPet`, `sendCreditFurniRedeemMessage` ficam em métodos contíguos, sempre na mesma ordem. |
| Resgate de câmbios          | `RWFCRM_REDEEM`                                                                                                     | `CreditFurniRedeemMessageComposer` (`ROOM_REEDEM_EXCHANGE_ITEM`).                                                                                                                                                         |
| Deletar quarto              | `deleteroom.confirm.message`                                                                                        | `_SafeStr_9231` em `RoomSettingsCtrl` → `DeleteRoomMessageComposer` (`ROOM_DELETE`).                                                                                                                                      |
| Colocar pet                 | `param1:int, param2:int, param3:int, param4:int = 0, param5:int = 0` (em `Messages.txt`)                            | Construtor único do `PlacePetMessageComposer` (`ROOM_PLACE_PET`).                                                                                                                                                         |
| Dimmer (presets)            | `"000000"` e `toString(16).toUpperCase()` em `RoomSession`                                                          | Salvar presets (`ROOM_DIMMER_UPDATE`).                                                                                                                                                                                    |
| Post-it                     | `stickieui_container` ou `ROFCAE_STICKIE`                                                                           | `ROOM_POST_IT`, `ROOM_SAVE_POST_IT`, `ROOM_REMOVE_POST_IT`.                                                                                                                                                               |
| Abrir presente              | `RWVFM_OPEN_PRESENT`                                                                                                | `PresentOpenMessageComposer` (`ROOM_ITEM_OPEN_GIFT`).                                                                                                                                                                     |

## 4. Armadilhas de protocolo (2011–2013)

### 4.1. IDs no inventário: positivo vs negativo

- **Chave de inventário** (`stripId`, 1º int de `INVENTORY_ITEMS` e `FURNI_ADD_OR_UPDATE`): sempre **negativa (`-id`)**
  desde o R62 (fim de 2010). `INVENTORY_REMOVE_OBJECT` também manda `-id`.
- **`INVENTORY_UNSEEN_ITEMS`**:
    - **< 2013-05-22**: `FurniModel.as` compara com `param1.ref` (`objId`, o 3º int do item = id real no banco). Envie
      **IDs POSITIVOS** (negativo quebra o `indexOf(param1.ref)`).
    - **≥ 2013-05-22 / AIR**: `UnseenItemTracker` consulta `_arg_1.id` (o `stripId` negativo). Envie **`-it.id`**, senão
      o slot nunca acende verde (`10275685`).

### 4.2. Abas do inventário (Floor vs Wall)

- **2011-02-25 a 2013-05-22**: abas separadas. Em `INVENTORY_UNSEEN_ITEMS`, categoria `1` = FLOOR
  (`unseen_symbol_floor`), `2` = WALL (`unseen_symbol_wall`). Mandar `1` para papel de parede acende "NEW" na aba
  errada.
- **≥ 2013-05-22 (AIR)**: aba única `"furni"`; categoria `1` = `OWNED_FURNI` (chão e parede), `2` = `RENTED_FURNI`.

### 4.3. `CATALOG_PAGE` (header 2443 / 127)

- `isRent` (Boolean): só desde **26/02/2013**; antes disso desalinha os bytes
  (`Failed to parse incoming message using _$_43B::_$_3RU`). Use
  `if (isVersionAtLeast(2013, 2, 26)) writeBoolean(false)`.
- `expiration` (Int) em `ProductData`, logo após `productCount`: existiu só entre **20/03/2012 e 30/01/2013**. Use
  `if (isVersionBetween(2012, 3, 20, 2013, 1, 30)) writeInt(-1)`.

### 4.4. `ROOM_USER_ACTION` vs `WaveMessageComposer`

- Em **2011-10 (`20111005`)** o cliente usava o `WaveMessageComposer` sem payload; `readInt()` quebra o alinhamento.
  Unifique em `RoomUserActionHandler.handle` com `if (habboSession.isVersionBefore(2011, 12, 13)) UserAction.WAVE` e
  `override_method = NULL` (`handleR63A` é exclusivo do R63A).
- Desde **2011-12 (`20111213`)**: `ActionMessageComposer(action: Int)` unifica aceno e gestos (handler padrão).

### 4.5. Ações inexistentes em 2011-10

`ROOM_USER_DROP_HANDITEM`, `ROOM_USER_GIVE_HANDITEM` e `ROOM_USER_SIT` (`ChangePosture`) só surgiram em dez/2011
(`20111213`). Em `20111005`: `header = NULL, override_method = 'DISABLED'`.

### 4.6. `TRADING_OPEN`: cuidado com In/Out

- 2012-07: header `5` é a resposta do servidor; o envio do cliente é **1783** (`_SafeStr_2812`).
- 2013-05: header `2606` é a resposta do servidor (`TradingOpenParser`); o envio do cliente é **2177**
  (`_SafeStr_3112`).

### 4.7. `ROOM_UPDATE_FURNI_STACK` (detalhes; a regra está no `AGENTS.md`)

Estrutura binária: `byte` contagem de ladrilhos, depois `byte x`, `byte y`, `short height`. Hash HabBit da release de
introdução: `ab04cd0c4ed3cc4d4f74ff130ce92660`. Colisões de header: em `20111005`, `3887` =
`ObjectsDataUpdateMessageEvent` (parser `_-0ha` / `_SafeStr_3059`, erro
`Failed to parse incoming message using _$_22::_$_0ha`); em `20120704`, `2358` = `HeightMapUpdateMessageParser`.

### 4.8. Quarto privado vs público (`ROOM_INITIAL_INFO`, prefixo `model_`)

Até **24/04/2013** (`RELEASE63-201304241145-22909261`), o `RoomReadyMessageParser` faz
`if (this.roomType.indexOf("model_") < 0) this._roomCategory = 1; // público`. Sem o prefixo `"model_"` (ex: `"a"` em
vez de `"model_a"`), o cliente trata o quarto como público e, ao sair, o Recycler/Furni-matic mostra
`${recycler.alert.privateroom}`. Em `RoomInitialInfoResponse`: `if (isVersionBefore(2013, 4, 24))` prefixe com
`"model_"`. Depois disso a distinção foi extinta.

## 5. Mapeamento rápido de headers

| Pacote                              | 20111005 | 20111213 | 20120402 | 20120704 | 20130522 |
|-------------------------------------|----------|----------|----------|----------|----------|
| `ROOM_PLACE_ITEM`                   | 2774     | 2161     | 2484     | 1871     | 1082     |
| `ROOM_TAKE_ITEM`                    | 3747     | 1129     | 1580     | 720      | 2944     |
| `ROOM_MOVE_WALL_ITEM`               | 3500     | 2526     | 1592     | 30       | 1734     |
| `ROOM_MOVE_FLOOR_ITEM`              | 2901     | 3879     | 2486     | 3897     | 1849     |
| `NAVIGATOR_CAN_CREATE_ROOM`         | 686      | 3058     | 2801     | 2030     | 2830     |
| `ROOM_POST_IT` (Incoming)           | 1079     | 1445     | 2650     | 1874     | 1179     |
| `ROOM_SAVE_POST_IT`                 | 1923     | 1045     | 226      | 315      | 1938     |
| `ROOM_REMOVE_POST_IT`               | 651      | 3848     | 3697     | 2223     | 1962     |
| `ROOM_POST_IT` (Outgoing)           | 1226     | 881      | 1731     | 3292     | 294      |
| `ROOM_DIMMER_INFO`                  | 2587     | 1079     | 1483     | 1264     | 3876     |
| `ROOM_DIMMER_SWITCH`                | 442      | 2024     | 636      | 2599     | 2293     |
| `ROOM_DIMMER_UPDATE`                | 2689     | 3061     | 729      | 3575     | 703      |
| `ROOM_ITEM_OPEN_GIFT`               | 1433     | 1257     | 3860     | 2468     | 3835     |
| `ACHIEVEMENT_LIST`                  | 681      | 2047     | 483      | 3436     | 1966     |
| `AVATAR_EFFECT`                     | 2130     | 2662     | 618      | 2144     | 1731     |
| `CATALOG_PURCHASE`                  | 1274     | 350      | 3189     | 2591     | 748      |
| `CATALOG_PURCHASE_GIFT`             | 1249     | 588      | 1277     | 700      | 1583     |
| `CATALOG_GIFT_WRAPPING`             | 1973     | 1540     | 984      | 2578     | 2947     |
| `CATALOG_RECYCLER_STATUS`           | 1676     | 1177     | 154      | 2504     | 39       |
| `CATALOG_RECYCLER_REWARDS`          | 648      | 785      | 1735     | 997      | 2813     |
| `MESSENGER_FOLLOW_FRIEND`           | 1113     | 162      | 2767     | 305      | 820      |
| `INVENTORY_UNSEEN_ITEMS_TRACKER`    | 3050     | 1495     | 1166     | 682      | 3819     |
| `WIRED_SAVE_TRIGGER`                | 2601     | 2969     | 525      | 632      | 1052     |
| `WIRED_SAVE_CONDITION`              | 1021     | 3357     | 17       | 3854     | 2859     |
| `ROOM_BAN_USER`                     | 274      | 895      | 741      | 1530     | 1607     |
| `ROOM_DELETE`                       | 1393     | 2495     | 1465     | 2370     | 2957     |
| `ROOM_GIVE_RIGHTS`                  | 2468     | 3699     | 2637     | 1305     | 2160     |
| `ROOM_MUTE_USER`                    | 3052     | 1311     | 3672     | 1682     | 1214     |
| `ROOM_PLACE_PET`                    | 74       | 3836     | 3848     | 1221     | 3269     |
| `ROOM_REEDEM_EXCHANGE_ITEM`         | 2324     | 2736     | 2243     | 3689     | 753      |
| `ROOM_TAKE_PET`                     | 2723     | 487      | 1396     | 2703     | 1480     |
| `ROOM_TRIGGER_CLOSE_DICE`           | 1385     | 145      | 1536     | 867      | 2349     |
| `ROOM_TRIGGER_HABBO_WHEEL`          | 3633     | 2360     | 3188     | 338      | 1880     |
| `ROOM_TRIGGER_ONE_WAY_GATE`         | 2942     | 3522     | 327      | 208      | 598      |
| `ROOM_TRIGGER_ROLL_DICE`            | 3003     | 3656     | 2023     | 763      | 3148     |
| `ROOM_USER_ACTION`                  | 2301     | 1908     | 2534     | 3631     | 3496     |
| `ROOM_USER_DROP_HANDITEM`           | DISABLED | 2055     | 1613     | 858      | 3890     |
| `ROOM_USER_GIVE_HANDITEM`           | DISABLED | 2702     | 1367     | 2264     | 3891     |
| `ROOM_USER_SIT`                     | DISABLED | 3253     | 1383     | 2899     | 3182     |
| `TRADING_CONFIRM_DECLINE`           | 2435     | 2420     | 3685     | 3271     | 2667     |
| `TRADING_OPEN`                      | 2947     | 2610     | 3111     | 1783     | 2177     |
| `TRADING_REMOVE_ITEM`               | 756      | 976      | 1488     | 610      | 2795     |
| `TRADING_UNACCEPT`                  | 1144     | 882      | 3837     | 62       | 3139     |
| `USER_CHANGE_MOTTO`                 | 58       | 371      | 2010     | 2134     | 1791     |
| `USER_BADGES` (Incoming)            | 3339     | 1660     | 1504     | 2252     | 3307     |
| `USER_BADGES` (Outgoing)            | 3760     | 2159     | 1770     | 1292     | 500      |
| `USER_PROFILE` (Incoming)           | 2595     | 3223     | 274      | 846      | 1219     |
| `USER_PROFILE` (Outgoing)           | 1781     | 2587     | 2153     | 105      | 3786     |
| `CATALOG_MARKETPLACE_CONFIGURATION` | 3708     | 2347     | 1246     | 1163     | 3225     |
| `HABBO_CLUB_GIFTS`                  | 2747     | 3305     | 1908     | 525      | 474      |
| `MODERATION_CALL_FOR_HELP`          | 1204     | 362      | 1576     | 2165     | 3265     |
| `MODERATION_BAN_USER`               | 1257     | 1346     | 3035     | DISABLED | 856      |
| `MODERATION_KICK_USER`              | 2687     | 2404     | 870      | DISABLED | 2691     |
| `MODERATION_ROOM_CHATLOG`           | 825      | 471      | 3180     | 3144     | 3928     |
| `MODERATION_TICKET_CHATLOG`         | 3776     | 2348     | 2539     | DISABLED | 2899     |
| `NAVIGATOR_ADD_FAVORITE_ROOM`       | 2528     | 1963     | 2571     | 3205     | 543      |
| `NAVIGATOR_HOME_ROOM`               | 3771     | 502      | 100      | 3202     | 2699     |
| `NAVIGATOR_REMOVE_FAVORITE_ROOM`    | 3800     | 2508     | 1710     | 1872     | 1753     |
| `PET_INFO`                          | 2928     | 1712     | 3950     | 2770     | 241      |
| `PET_RESPECT`                       | 151      | 1090     | 1007     | 2103     | 2506     |
| `REFRESH_CAMPAIGN`                  | 3393     | 2504     | 314      | DISABLED | 573      |
| `ROOM_BANNED_USERS`                 | 2244     | 2955     | 3107     | 1128     | 3361     |
| `ROOM_IGNORE_USER`                  | 3362     | 1610     | 1129     | 405      | 3934     |
| `ROOM_LOAD_BY_DOORBELL`             | 3070     | 1624     | 2425     | 50       | 1344     |
| `ROOM_MANNEQUIN_CHANGE_NAME`        | 2250     | 2031     | 2863     | 2202     | 2498     |
| `SETTINGS_SAVE_VOLUME`              | 1528     | 547      | 1765     | 1704     | 247      |
| `SETTINGS_SAVE_FOCUS`               | DISABLED | DISABLED | DISABLED | DISABLED | DISABLED |
| `CATALOG_HABBO_CLUB_PAGE`           | 2304     | 1811     | 442      | 424      | 1070     |
| `USER_SETTINGS`                     | 1600     | 3845     | 478      | 3979     | 3235     |

## 6. Migrations de headers

As regras gerais de Flyway (UPDATE in-place, sem `DELETE`+`INSERT`, teste com rollback) estão no `AGENTS.md`. Específico
de headers: `releases_incoming_headers` e `releases_outgoing_headers` têm chave única em `(release_name, header)`; antes
do `UPDATE`, confira se o número já está ocupado:

```sql
SELECT name, header
FROM releases_incoming_headers
WHERE release_name = '...'
  AND header = < NUMERO >;
```

Isso evita `SQLIntegrityConstraintViolationException` nos testes de integração (`ItemPersistenceContainerTest`) que
rodam o Flyway sobre MariaDB.
