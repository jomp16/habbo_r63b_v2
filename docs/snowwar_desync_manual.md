# Manual de Diagnóstico e Resolução de Desync / Checksum Mismatch (SnowWar / Game2)

Este manual é o guia definitivo para identificar, isolar matematicamente e corrigir divergências de simulação física (checksum mismatches e desyncs) entre o servidor Kotlin e o cliente oficial Habbo AS3 (Flash / AIR / WIN63).

---

## 1. Princípio Fundamental de Diagnóstico

> **NUNCA deduza ou altere física na base de "tentativa e erro".**  
> A simulação do SnowWar no AS3 é 100% determinística a cada subturn de 50ms (3 subturns por turno de 150ms). Se o checksum divergiu em 1 unidade que seja, há uma fórmula matemática exata que revela **qual objeto**, **qual variável** e **qual subturn** divergiu.

### Arquivos e Fontes da Verdade
- **Log do Cliente Flash com dump de turnos:** `/home/jomp16/Downloads/HabboWin/logger.txt`
- **Código decompilado completo do AS3:** `/mnt/DADOS/Downloads/Habbo/dumps_repo/releases/WIN63-202609161723-93809945/Habbo_codigo_completo.txt`
- **Código do servidor:** `server/src/main/kotlin/ovh/rwx/habbo/game/snowwar/`

---

## 2. A Matemática do Checksum no AS3

No cliente AS3 (`_SafeStr_2526.calculateChecksum` em `Habbo_codigo_completo.txt` linha 233420):
Para cada objeto ativo na arena (iterados na mesma ordem determinística), o checksum soma cada uma de suas variáveis multiplicada pelo seu **índice base 1**:

$$\text{ChecksumContribution}(obj) = \sum_{i=0}^{N-1} \text{variable}[i] \times (i + 1)$$

Onde:
- Variável 0 tem multiplicador **1**
- Variável 1 tem multiplicador **2**
- Variável 2 tem multiplicador **3**
- Variável $i$ tem multiplicador **$i + 1$**

### Diagnóstico por Delta
Calcule sempre:
$$\Delta = \text{serverChecksum} - \text{clientChecksum} \quad (\text{ou vice-versa})$$

Se apenas uma entidade divergiu em uma coordenada ou estado:
$$\Delta = \Delta_{\text{variável}} \times (i + 1)$$

Portanto, **o delta DEVE ser estritamente divisível por $(i + 1)$**!

---

## 3. Matriz de Variáveis e Multiplicadores por Objeto

### 3.1 HumanGameObject (`type = 5`, 19 variáveis, índices 0..18)
| Índice | Variável | Multiplicador | Significado / Unidade |
|---|---|---|---|
| **0** | `type` | **1** | Sempre 5 |
| **1** | `objectId` | **2** | ID do game object do avatar |
| **2** | `currentLocation.x` | **3** | Posição X no mundo (3200 por tile). **Deltas de X são múltiplos de 3!** |
| **3** | `currentLocation.y` | **4** | Posição Y no mundo (3200 por tile). **Deltas de Y são múltiplos de 4!** |
| **4** | `currentTile.x` | **5** | Coordenada X da tile atual |
| **5** | `currentTile.y` | **6** | Coordenada Y da tile atual |
| **6** | `bodyDirection` | **7** | Direção do corpo (0..7) |
| **7** | `hitPoints` | **8** | Vida (inicia em 5). **Deltas de dano são múltiplos de 8!** |
| **8** | `snowBallCount` | **9** | Quantidade de bolas (0..5). **Deltas de contagem são múltiplos de 9!** |
| **9** | `activityState` | **10** | 0=Normal, 1=Making, 2=Stunned, 3=Invincible |
| **10** | `activityTimer` | **11** | Timer da atividade em subturns. **Deltas de timer são múltiplos de 11!** |
| **11** | `unused` | **12** | Sempre 0 |
| **12** | `nextTile.x` | **13** | X da próxima tile da rota |
| **13** | `nextTile.y` | **14** | Y da próxima tile da rota |
| **14** | `moveTarget.x` | **15** | Destino final X |
| **15** | `moveTarget.y` | **16** | Destino final Y |
| **16** | `score` | **17** | Pontos do jogador |
| **17** | `team` | **18** | Time (1=Blue, 2=Red) |
| **18** | `userId` | **19** | ID real do usuário / bot |

### 3.2 SnowBallGameObject (`type = 1`, 11 variáveis, índices 0..10)
| Índice | Variável | Multiplicador | Significado |
|---|---|---|---|
| **0** | `type` | **1** | Sempre 1 |
| **1** | `objectId` | **2** | ID da bola |
| **2** | `locH` | **3** | Posição X da bola. **Deltas de voo X são múltiplos de 3!** |
| **3** | `locV` | **4** | Posição Y da bola. **Deltas de voo Y são múltiplos de 4!** |
| **4** | `height` | **5** | Altura Z da bola |
| **5** | `direction` | **6** | Ângulo de lançamento (0..359) |
| **6** | `trajectory` | **7** | 0=Quick, 1=ShortLob, 2=LongLob |
| **7** | `timeToLive` | **8** | TTL restante em subturns |
| **8** | `throwerId` | **9** | ID do atirador |
| **9** | `parabolaOffset` | **10** | Offset da parábola |
| **10** | `planarVelocity` | **11** | Velocidade planar |

### 3.3 SnowballPileGameObject (`type = 3`, 7 variáveis, índices 0..6)
| Índice | Variável | Multiplicador | Significado |
|---|---|---|---|
| **0** | `type` | **1** | Sempre 3 |
| **1** | `objectId` | **2** | ID do game object da pilha |
| **2** | `x` | **3** | Posição mundo X (`x * 3200`) |
| **3** | `y` | **4** | Posição mundo Y (`y * 3200`) |
| **4** | `maxSnowballs` | **5** | Capacidade máxima da pilha |
| **5** | `snowballCount` | **6** | Bolas restantes. **Deltas de coleta são múltiplos de 6!** |
| **6** | `fuseObjectId` | **7** | ID da mobília fuse associada |

### 3.4 SnowWarTree (`type = 2`, 9 variáveis, índices 0..8)
| Índice | Variável | Multiplicador | Significado |
|---|---|---|---|
| **0** | `type` | **1** | Sempre 2 |
| **1** | `objectId` | **2** | ID da árvore |
| **2** | `x` | **3** | Posição mundo X |
| **3** | `y` | **4** | Posição mundo Y |
| **4** | `direction` | **5** | Direção da árvore |
| **5** | `height` | **6** | Altura da árvore |
| **6** | `fuseObjectId` | **7** | ID fuse |
| **7** | `maxHits` | **8** | Hits máximos |
| **8** | `hits` | **9** | Hits sofridos. **Deltas de dano na árvore são múltiplos de 9!** |

### 3.5 SnowballMachineGameObject (`type = 4`, 8 variáveis, índices 0..7)
| Índice | Variável | Multiplicador | Significado |
|---|---|---|---|
| **0** | `type` | **1** | Sempre 4 |
| **1** | `objectId` | **2** | ID da máquina |
| **2** | `x` | **3** | Posição mundo X |
| **3** | `y` | **4** | Posição mundo Y |
| **4** | `direction` | **5** | Direção |
| **5** | `maxSnowballs` | **6** | Máximo de bolas |
| **6** | `snowballCount` | **7** | Bolas disponíveis. **Deltas são múltiplos de 7!** |
| **7** | `fuseObjectId` | **8** | ID fuse |

---

## 4. Script de Extração Rápida de Mismatch

Quando ocorrer `CHECKSUM MISMATCH, requesting full status!`:
Execute este script Python no terminal para isolar imediatamente as entidades que divergiram entre o cliente (`logger.txt`) e o servidor:

```python
import re

LOGGER_PATH = '/home/jomp16/Downloads/HabboWin/logger.txt'

def inspect_turn(target_turn):
    with open(LOGGER_PATH) as f:
        text = f.read()

    # Extrai o dump de objetos do turno no log do cliente
    m = re.search(r'turn ### ' + str(target_turn) + r' ###\s+seed (\d+)\s+([\s\S]*?)Turn:' + str(target_turn + 1), text)
    if not m:
        print(f"Turn {target_turn} não encontrado no logger.txt")
        return

    seed = m.group(1)
    body = m.group(2).strip()
    objects = {}
    for line in body.split('\n'):
        mat = re.match(r'\+\+ \"O(\d+)-CS:(\d+)\s+Parms:(.*?)\"', line)
        if mat:
            obj_num, cs, parms = mat.group(1), int(mat.group(2)), [int(x) for x in mat.group(3).split(',')]
            objects[obj_num] = {'cs': cs, 'parms': parms}

    print(f"Turn {target_turn}: seed={seed}, objetos ativos={len(objects)}")
    
    # Procura a mensagem de mismatch seguinte
    m_err = re.search(r'Turn:\s*' + str(target_turn) + r'[\s\S]*?serverChecksum:(\d+)\s+clientChecksum:(\d+)', text)
    if m_err:
        srv_cs, cli_cs = int(m_err.group(1)), int(m_err.group(2))
        delta = srv_cs - cli_cs
        print(f"SERVER CS: {srv_cs} | CLIENT CS: {cli_cs} | DELTA: {delta:+d}")
        for div in [3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19]:
            if delta % div == 0:
                print(f"  -> Divisível por {div} (Índice {div-1}): quociente = {delta // div}")

    # Procura os dados enviados pelo servidor no FullGameStatus subsequente
    m_full = re.search(r'Turn:\s*' + str(target_turn) + r'[\s\S]*?Parsed human game object data, variables:(.*?)\n', text)
    if m_full:
        print("\nPrimeiro avatar enviado pelo servidor no Full Status:")
        print("  Server:", m_full.group(1))

# Substitua pelo número do turno com mismatch
inspect_turn(260)
```

---

## 5. Catálogo de Armadilhas Frequentes de Desync

### Armadilha 1: Pilhas Vazias (`SnowballPile`) Tornam-se Caminháveis
- **AS3:** Quando `snowballCount <= 0`, o AS3 chama `tile.removeGameObject()`. O `_gameObject` torna-se `null` e a tile vira **caminhável**.
- **Servidor:** NUNCA adicione pilhas ao conjunto estático `blockedTiles`. A checagem de bloqueio de pilhas em `isTileWalkable` DEVE ser dinâmica:
  ```kotlin
  for (pile in piles) {
      if (pile.snowballCount > 0 && pile.x == tileX && pile.y == tileY) return false
  }
  ```

### Armadilha 2: Árvores Destruídas Mantêm a Tile Bloqueada
- **AS3:** Quando uma árvore atinge `hits >= maxHits`, ela deixa de colidir com bolas de neve, mas seu construtor executou `_tile.blocked = true;`, que NUNCA é removido. Avatares continuam sem poder andar onde havia uma árvore.
- **Servidor:** Árvores permanecem permanentemente em `blockedTiles`.

### Armadilha 3: Voando sobre Buracos / Abismos ('x')
- **AS3:** No método `SnowBallGameObject.subturn()`, `getTileAt(tileX, tileY)` retorna `null` para qualquer tile `'x'`.
- Como `testCollisions` é guardado por `if (tile != null)`, **a bola NÃO testa colisão com nenhum avatar nem mobília enquanto voa sobre buracos**. Ela só cai se $Z < 1$.
- **Servidor:** `getCollisionTiles(ball)` DEVE retornar `emptyList()` se `!isValidTile(currentX, currentY)`.

### Armadilha 4: Gates de Input vs. Aplicação de Eventos na Simulação
- **AS3:** No loop de ticks (`tickGame()`), eventos aplicados (`HumanThrowsSnowballAtPosition`, `HumanThrowsSnowballAtHuman`) **NUNCA** devem ser bloqueados por verificações de input (ex: `throwTimer < 1` ou `canMove()`).
- O AS3 decrementa incondicionalmente `snowBallCount` ao receber o evento de atirar se `snowBallCount > 0`. Barrar o evento no servidor causa divergência imediata de `snowBallCount` ($\Delta = 9$).

### Armadilha 5: Ações Param Movimento (`stopWalking`)
- Tanto atirar bola (Events 3 e 4) quanto começar a fazer bola (Event 7) chamam explicitamente `stopMovement()` no AS3.
- Isso alinha imediatamente `currentLocation` e `moveTarget` e para a caminhada. O servidor deve chamar `stopWalking()` em ambos para sincronizar direção e coordenadas.

### Armadilha 6: Concorrência e Eventos no `FullGameStatus`
- O handler `onFullStatusRequest` DEVE ser sincronizado por mutex (`synchronized(this)`) com o loop `tickGame()`.
- O servidor deve responder com o `lastCompletedTurn` e `lastCompletedChecksum`, NUNCA calculando um checksum novo fora de hora enquanto o relógio já avançou.
- O campo `subTurns` do `FullGameStatus` NÃO pode ser `[emptyList(), emptyList(), emptyList()]` se houver `deferredEvents` pendentes: deve serializar os eventos daquele turno para que o cliente os execute e permaneça em sincronia no turno seguinte.
