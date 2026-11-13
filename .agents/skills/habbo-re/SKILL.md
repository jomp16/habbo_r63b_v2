---
name: habbo-re
description: "Engenharia reversa de clientes Habbo (SWF/HabboAir/WIN63). Use quando precisar identificar a qual pacote pertence um header/classe ofuscada (ex: erro do cliente \"Failed to parse incoming message using ...\"), achar um pacote na Sulek/banco, traçar o ciclo de vida e mutações de um pacote entre releases (habbo-timeline, structure-diff, search-class, inspect)."
---

# Habbo RE — encontrar pacotes e diferenças

Playbook para localizar pacotes de rede do Habbo e comparar sua estrutura entre builds.

## Projetos

| Projeto | Papel |
|---|---|
| `~/IdeaProjects/Habbo/habbo-timeline` | Análise histórica global (`habbo-timeline <PACOTE>`, 1.330+ releases 2009-2026, desofuscação de variáveis, gerador de Kotlin) |
| `~/IdeaProjects/Habbo/habboair-patcher` | Ferramentas (`release.sh`, extractor `SwfMessageDumper`, `MessageSearcher`, `StructureDiffer`) |
| `~/IdeaProjects/Habbo/habbo_r63b_v2` | Emulador (handlers `incoming/`, responses `outgoing/`) |
| `/mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/` | **Fonte Primária AS3**: `Habbo_codigo_completo.txt` (código decompilado completo de toda a release) |
| `~/Downloads/HabboWin/Habbo_SWF_Collection/<RELEASE>/` | SWFs: `HabboAir.swf`, `HabboAir.cracked.swf`, `HabboAir_scripts.txt` (decompilado AS3 Sorcerer) |

## Contexto essencial

- Nome de release: `WIN63-YYYYMMDDHHMM-XXXXXXX`. **Ordenação lexicográfica = cronológica** ("última" = maior string).
- Cada build troca os headers (ofuscação): o mesmo pacote tem id diferente em cada release.
- **Direções (crítico):**
  - Banco `releases_incoming_headers` = o que o servidor RECEBE do cliente = Sulek **outgoing** (Composers, client→server).
  - Banco `releases_outgoing_headers` = o que o servidor ENVIA = Sulek **incoming** (Events, server→client).
- Sulek: `GET https://api.sulek.dev/releases/flash-windows/<RELEASE>/messages` → `{messages:{incoming:[{id,name,asClass,asNamespace}],outgoing:[...]}}`; `name` é não-ofuscado. Cache em `cache/flash-windows-<RELEASE>.json`.
- Banco: PK `(release_name, name)`, UNIQUE `(release_name, header)`; `header NULL` + `override_method NULL` = não implementado; `header NULL` + `override_method='DISABLED'` = removido. **NULL é intencional para releases sem a feature** (o emulador exige header para todas as releases, mas NULL nunca despacha — ReleaseDao converte para -1).
- **`override_method` também pode NOMEAR o método Kotlin que roda** (não só `DISABLED`): `HabboHandler.invokeResponse` faz `outgoingHeaders.find{...}?.overrideMethod ?: "response"`. Para saber QUAL método do `*Response.kt` é invocado para uma release, consultar o banco — senão você corrige o método errado:
  ```bash
  # credenciais vêm do config.yaml do emulador (database.host/user/password):
  DB_HOST=$(yq '.database.host' config.yaml); DB_USER=$(yq '.database.user' config.yaml); DB_PASS=$(yq '.database.password' config.yaml); DB_NAME=$(yq '.database.name' config.yaml)
  mariadb -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -B \
    -e "SELECT release_name, header, override_method FROM releases_outgoing_headers WHERE name='INVENTORY_BADGES' ORDER BY release_name;"
  # ex: todas as releases usam override_method='simplifiedInventoryBadgeResponse' (e não 'response')
  ```
- Nomes de negócio no banco não usam prefixo `GET_` (ex: `USER_BADGES`, `BADGE_LEADERBOARD`).

## Encontrar um pacote rapidamente (Zero Desvios e Alta Performance)

### ⚠️ Regras Anti-Desvio e Performance

1. **NUNCA use scripts com `re.DOTALL` ou regex `.*` sobre `Habbo_codigo_completo.txt`**: O arquivo tem 30 MB+. Regex
   multiline trava o processo e gera timeouts. Use sempre **`rg` (ripgrep)**.
2. **NUNCA confunda In/Out do dump do Flash**:
    - `Outgoing[header, md5]`: Envio cliente→servidor = **`releases_incoming_headers`** no emulador.
    - `Incoming[header, md5]`: Envio servidor→cliente = **`releases_outgoing_headers`** no emulador.
3. **Cuidado com Hexadecimal**: Compiladores Flash exportam índices de array em hex (ex: `[0x0600] = 1536`,
   `[0x0101] = 257`). Ao buscar classes no ActionScript, use:
   `rg "\[(0x[0-9a-fA-F]+|\d+)\]\s*=\s*<Classe>;" <arquivo>`

### O Pipeline em 4 Passos

1. **Nome real e assinatura (Era Pré-Shuffle: 2009 R34 a 2011 R63A)**:
    - **No `Messages.txt` de qualquer release pré-shuffle (em `dumps_repo/releases/`)**:
      ```bash
      rg "^Outgoing\[<HEADER_ESTATICO>," /mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE_PRE_SHUFFLE>/Messages.txt
      ```
      *(Ex: em `RELEASE34-18054-...` de 2009 até `RELEASE63-20111004...` de 2011; devolve imediatamente a classe, hash
      MD5 e a lista de tipos do construtor).*
    - **No AS3 limpo de 2011 (para obter o nome oficial Sulake)**:
      ```bash
      rg "\[<HEADER_ESTATICO>\]\s*=" /home/jomp16/Downloads/Habbo/RELEASE63-35255-34886-201108111108/src/com/sulake/habbo/communication/HabboMessages.as
      ```
    - **Via `habbo-timeline`**:
      ```bash
      habbo-timeline <HEADER_ESTATICO> incoming|outgoing
      ```
      *(Rastreia automaticamente as 275+ builds pré-shuffle e obtém o nome canônico).*
2. **Busca direta por assinatura em `Messages.txt` da release alvo**:
   Se o construtor tiver 2+ argumentos ou tipos específicos (`String`, `Boolean`):
   ```bash
   rg "= _-.*\(param1:int, param2:int, param3:String\)" /mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE>/Messages.txt
   ```
3. **Propagação instantânea por MD5**:
   O hash MD5 da classe costuma ser idêntico entre releases da mesma era:
   ```bash
   rg "<HASH_MD5>" /mnt/DADOS/Downloads/Habbo/dumps_repo/releases/<RELEASE_IRMA>/Messages.txt
   ```
4. **Assinatura genérica (`param1:int` ou vazia)? Use Âncoras Diretas**:
    - **Trading**: `rg "inventory.trading.warning.others_account_disabled"` no `Habbo_codigo_completo.txt` → entrega
      todos os 8 pacotes de trading agrupados no mesmo package (`_-BK`, `_-2sQ`, `_-008`, `_-499`).
    - **Dados, Roda, Porta, Stickie**: `rg "ROFCAE_DICE_OFF"` → entrega o `switch` dentro de `useObject` no `RoomEngine`
      com todos os 5 pacotes.
    - **Resgate de Câmbios**: `rg "RWFCRM_REDEEM"` → entrega `CreditFurniRedeemMessageComposer`
      (`ROOM_REEDEM_EXCHANGE_ITEM`).
    - **Moderação e Pets**: `rg "function assignRights"` ou `banUser` → entrega `RoomSession`, onde ban, mute, rights,
      pet pickup e câmbios estão sequenciais.
    - **Deletar Quarto**: `rg "deleteroom.confirm.message"` → entrega `DeleteRoomMessageComposer` em `RoomSettingsCtrl`.
    - **Colocar Pet**: `rg "param1:int, param2:int, param3:int, param4:int = 0, param5:int = 0"` no `Messages.txt` →
      construtor exclusivo de `PlacePetMessageComposer`.

---

## Encontrar um pacote (Ferramentas de Suporte)

1. **Do erro do cliente** — o erro mostra a classe do PARSER (não o evento):
   ```
   Failed to parse incoming message using _-Q12::_-r14
   ```
   ```bash
   cd ~/IdeaProjects/Habbo/habboair-patcher
   ./release.sh search-class r14 WIN63-202608061514-667123007
   # → id=2911 event=_-yW parser=_-r14 → banco=USER_PROFILE sulek=ExtendedProfileMessageEvent
   ```
   `search-class` extrai id+evento+parser do SWF e cruza com banco (nome de negócio) e cache Sulek (nome real).

2. **Pelo nome ou histórico completo (Método Recomendado: `habbo-timeline`)**:
   Uma vez identificado o nome do evento ou composer:
   ```bash
   habbo-timeline ExtendedProfileMessageEvent
   # ou pelo nome de negócio do banco:
   habbo-timeline USER_PROFILE
   ```
   Isso traça a evolução de 2009 a 2026, descobre quando o pacote surgiu, mudanças de formato e campos desofuscados (nomes reais de getters e DTOs).

3. **Pelo nome rápido no patcher** (banco/cache):
   ```bash
   cd ~/IdeaProjects/Habbo/habboair-patcher
   ./release.sh search NOME_PACOTE
   ```

4. **No AS3 decompilado** (`HabboAir_scripts.txt` ou `habbo-timeline inspect`):
   - Inspecionar diretamente bytecode ABC sem decompilar:
     ```bash
     habbo-timeline inspect ~/Downloads/HabboWin/Habbo_SWF_Collection/<RELEASE>/HabboAir.cracked.swf _-r14
     ```
   - Achar classe ofuscada `_-B1L` no decompilado: `grep '"_-B1L"' scripts.txt` → dá o `_SafeStr_XXXX` → `grep "class _SafeStr_XXXX"`.
   - Parsers implementam `IMessageParser`; a ordem dos `read*` no `parse()` = ordem no wire. Getters do Event dão os nomes dos campos.
   - Composers implementam `IMessageComposer`; `getMessageArray()` = campos que o cliente envia (handler do emulador lê nessa ordem).
   - `new _SafeStr_XXXX(args)` mostra o significado dos argumentos.

## Analisar uma feature completa no AS3 (estrutura de pacotes)

Quando o cache Sulek não nomeia as mensagens, os IDs saem do próprio decompilado ou do `habbo-timeline`:

1. **Direção por tipo de classe** (o decompilado não tem "incoming/outgoing"):
   - `extends MessageEvent` = server→client (emulador `Outgoing.kt` / banco `releases_outgoing_headers` / Sulek `incoming`)
   - `implements IMessageComposer` = client→server (emulador `Incoming.kt` / banco `releases_incoming_headers` / Sulek `outgoing`)
2. **Registro de IDs**: o arquivo de registro do client tem os mapas
   `_composers[NNN] = _SafeStr_XXXX` (composers) e um mapa irmão para events
   (o nome varia por build, ex `_SafeStr_5990[NNN] = _SafeStr_XXXX`).
   ```bash
   grep -n "_composers\[.*\] = _SafeStr_2437\|_SafeStr_5990\[.*\] = _SafeStr_2937" scripts.txt
   ```
3. **Fluxo da feature**: ache o controller/domínio (ex: `WiredChestController`) e liste
   `addMessageEvent(new _SafeStr_XXXX(callback))` (events que ele escuta) e
   `send(new _SafeStr_YYYY(...))` (composers que ele dispara). Isso define a ordem do
   protocolo (quem responde o quê), não só os pacotes isolados.
4. **Tipos compostos**: campos com `Xxx.readFromMessage(wrapper)` (static factory) ou
   classe própria com construtor `(IMessageDataWrapper)` (ex: `ChestStorage`, `ChestItemType`)
   são payloads reutilizados em vários pacotes — extraia uma vez e re-use na serialização
   do emulador. `readLong` = 8 bytes big-endian (add `writeLong/readLong` no emulador se
   for a primeira vez).
5. **Constantes de negócio**: `public static const` no parser/companion (ex: `SUCCESS = 0`,
   mapas de estado) viram os result codes / enums do manager no emulador.
6. **Model keys**: `object.getModel().get/setNumber|String("furniture_...")` revela as
   chaves que o servidor precisa popular via stuff data (ex: MapStuffData keys
   `state`, `chest_name`, `contents_count`) e de onde o client lê cada uma
   (`getLegacyString()` = key `"state"` no MapStuffData).

## Comparar estrutura de pacotes e detectar mutações

### Método 1 (Recomendado): Linha do tempo global com `habbo-timeline`
Varre 1.330+ releases automaticamente de 2009 até a última release WIN63, detectando onde cada campo novo apareceu:
```bash
habbo-timeline ItemUpdateMessageEvent
# ou com desofuscação avançada e código Kotlin pronto:
habbo-timeline -v ItemUpdateMessageEvent
```
A ferramenta aponta os pontos exatos de mutação estrutural (ex: quando um `Boolean` ou `Int` foi adicionado), com data e build exatas para usar no `isVersionAtLeast(...)`.

### Método 2: Diff pontual entre duas releases específicas no patcher
```bash
cd ~/IdeaProjects/Habbo/habboair-patcher
./release.sh structure-diff <OLD> <NEW> <PACOTE>
# PACOTE: header numérico | classe ofuscada (ex: B1L) | nome de negócio (ex: ROOM_USERS)
# Ex:
./release.sh structure-diff WIN63-202604081729-823620372 WIN63-202608061514-667123007 ROOM_USERS
```
- Aceita nome de negócio e resolve o header **separadamente em cada release** (o header muda entre builds).
- Saída: estrutura de cada (string de tipos `I S B ...`), readable, e diff campo a campo (`pos N: OLD X -> NEW Y`).

## Corrigir um pacote que dessincronizou

Quando o `habbo-timeline` ou `structure-diff` mostra campos novos/alterados:

1. Executar `habbo-timeline <PACOTE>` para ver a evolução e o template Kotlin sugerido.
2. Ler o parser decompilado da release NOVA para confirmar a semântica de cada campo (getters e construtores).
3. No `*Response.kt` correspondente do emulador, manter os campos comuns fora e os **campos novos mockados dentro de** `isVersionAtLeast(<ano>, <mes>, <dia>)` (data da release nova indicada pelo `habbo-timeline` — clientes antigos dessincronizam se receberem campos novos):
   ```kotlin
   writeInt(itemId)
   writeUTF(itemType)
   if (habboResponse.isVersionAtLeast(2026, 8, 6)) {
       writeInt(newField) // Campo introduzido nesta build
   }
   ```
4. Campos que o parser lê mas não tem getter/atribuição são descartados pelo cliente — mock `""` ou `0` é suficiente.
5. Compilar: `cd ~/IdeaProjects/Habbo/habbo_r63b_v2 && bash gradlew :server:compileKotlin`.
6. `writeBoolean` (WIN63) escreve 1 byte → compatível com `readByte` do cliente novo.
7. **Antes de corrigir, cheque `override_method` no banco** (ver "Contexto essencial") para saber qual método do response realmente roda.

## Propagação de pacote novo

1. Descobrir o header na Sulek da release nova (cache/`search-class`/`habbo-timeline`).
2. Inserir na release fonte (oldRelease do config) — incoming e/ou outgoing.
3. `./release.sh propagate NOME` → propaga para todas as newReleases (header real se existir naquela release, `NULL` se não — o emulador não reclama). Usar `propagate` em vez do `map` completo quando for pacote isolado.
4. Se o emulador não tiver enum/handler: adicionar em `Incoming.kt`/`Outgoing.kt` + criar handler/response stub lendo os campos do AS3/timeline.
