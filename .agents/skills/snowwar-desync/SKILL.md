---
name: snowwar-desync
description: Diagnóstico matemático e resolução de checksum mismatches e desyncs no SnowWar (Game2). Use sempre que houver erro de checksum, loop de FullGameStatus, ou divergências de física entre o servidor Kotlin e o cliente AS3.
---

# SnowWar Desync Troubleshooting Playbook

Este skill guia o diagnóstico determinístico de qualquer divergência de simulação física (checksum mismatch) entre o servidor Kotlin e o cliente oficial Habbo AS3.

Consulte o manual detalhado completo em [docs/snowwar_desync_manual.md](file:///home/jomp16/IdeaProjects/Habbo/habbo_r63b_v2/docs/snowwar_desync_manual.md).

## Protocolo de Diagnóstico em 4 Passos

### Passo 1: Isolar o Turno e o Delta
No log do cliente (`/home/jomp16/Downloads/HabboWin/logger.txt`), localize a linha:
```text
Turn: <turn>,<turn> currentSubTurn:<subturn> serverChecksum:<serverCs> clientChecksum:<clientCs>
CHECKSUM MISMATCH, requesting full status!
```
Calcule:
$$\Delta = \text{serverChecksum} - \text{clientChecksum}$$

### Passo 2: Fatorar o Delta pelo Multiplicador do AS3
Cada variável $i$ (0-indexed) de um objeto contribui com o peso $(i + 1)$ no checksum:
- Se $\Delta$ é múltiplo de **3**: divergência em `X` (`currentLocation.x` do avatar ou `locH` da bola de neve).
- Se $\Delta$ é múltiplo de **4**: divergência em `Y` (`currentLocation.y` do avatar ou `locV` da bola de neve).
- Se $\Delta$ é múltiplo de **6**: divergência em `snowballCount` de uma pilha (`SnowballPile`).
- Se $\Delta$ é múltiplo de **7**: divergência em `bodyDirection` de um avatar ou `snowballCount` de uma máquina.
- Se $\Delta$ é múltiplo de **8**: divergência em `hitPoints` (vida) de um avatar.
- Se $\Delta$ é múltiplo de **9**: divergência em `snowBallCount` de um avatar ou `hits` de uma árvore.
- Se $\Delta$ é múltiplo de **11**: divergência em `activityTimer` (cooldown de stun/snowball).

### Passo 3: Comparar o Dump do Cliente com o Full Status do Servidor
Execute o script em `docs/snowwar_desync_manual.md` apontando para o turno que falhou. Compare as coordenadas e estados do objeto identificado.

### Passo 4: Verificar as Invariantes Físicas
1. **Pilhas Vazias:** `SnowballPile` com `snowballCount <= 0` é **caminhável**! Não deve estar em `blockedTiles`.
2. **Árvores Destruídas:** `Tree` mantém a tile permanentemente bloqueada para avatares (`_tile.blocked = true`).
3. **Buracos ('x'):** Bolas voando com $Z \ge 1$ ignoram buracos e não testam colisão com nada até reencontrar chão válido.
4. **FullGameStatus:** Respostas devem usar `lastCompletedTurn`/`lastCompletedChecksum` protegidos por `synchronized(this)` e incluir `deferredEvents` pendentes.
