---
name: timeline-as3
description: Rastreia o histórico evolutivo de um pacote Habbo AS3 (2009-2026) e gera o roteamento Kotlin com nomes reais de variáveis usando isVersionAtLeast.
register_cmd: true
cmd_info: "Timeline AS3 -> Kotlin (ex: /timeline-as3 . 140)"
---

# Analisador Temporal AS3 para Kotlin (`habbo-timeline`)

Você é um engenheiro reverso focado no protocolo Habbo. Sua missão é analisar o histórico evolutivo de um pacote (2009 a 2026) e gerar o código de serialização/desserialização em Kotlin para o emulador multi-release.

---

### Passo 1: Executar o Analisador Temporal

Use a ferramenta nativa em Go **`habbo-timeline`** (instalada em `~/.local/bin/habbo-timeline` e `/home/jomp16/IdeaProjects/Habbo/habbo-timeline/bin/habbo-timeline`):

```bash
habbo-timeline "<QUERY>" [incoming|outgoing]
```

**Opções de busca suportadas:**
- **Por Nome do Emulador (ex: R63B)**:
  `habbo-timeline ROOM_WALL_ITEM_UPDATE`
  `habbo-timeline ROOM_FLOOR_ITEM_UPDATE`
- **Por Nome Oficial da Mensagem (Sulek / AS3)**:
  `habbo-timeline ItemUpdateMessageEvent`
  `habbo-timeline ObjectUpdateMessageEvent`
  `habbo-timeline ItemRemoveMessageEvent`
- **Por Header Numérico Pré-Shuffle (2009 a 2011)**:
  `habbo-timeline 85 incoming`
  `habbo-timeline 94 incoming`
  `habbo-timeline 96 outgoing`
- **Por Hash MD5 do HabBit**:
  `habbo-timeline 7edf08f08b98370ea2a185c0583f5123`

> [!NOTE]
> O binário Go substitui os scripts Python legados (`trace_timeline_full.py`), reduzindo o tempo de varredura de 1.330+ releases de ~5 minutos para **~8 segundos**.

---

### Passo 2: Interpretar o Histórico Evolutivo

O analisador detecta as transições estruturais e exibe a cronologia em formato de data `YYYYMMDDHHMM`:
- `BUILD 201110031118` -> Ano: 2011, Mês: 10, Dia: 03.
- `BUILD 201201130937` -> Ano: 2012, Mês: 01, Dia: 13.
- `BUILD 201302130247` -> Ano: 2013, Mês: 02, Dia: 13.

Tipos lidos/escritos no Flash:
- `Int`: `readInteger()`, `readInt()`, `readShort()`, `readByte()`
- `String`: `readUTF()`, `readString()`
- `Boolean`: `readBoolean()`
- `CLASS[Nome: ...]`: sub-estruturas ou delegações a parsers e DTOs
- `LOOP[...]`: iterações sobre coleções

---

### Passo 3: Descoberta de Nomes de Variáveis

O analisador correlaciona as compilações com as releases modernas desofuscadas (`WIN63 / Flash 32`), descobrindo os nomes reais através de:
1. **Getters Públicos**: campos privados mapeados para getters (ex: `itemId`, `pickerId`).
2. **Propriedades de DTOs**: propriedades de objetos de dados (ex: `x`, `y`, `z`, `sizeZ`, `dir`, `extra`, `expiryTime`, `usagePolicy`, `ownerId`, `staticClass`).
3. **Strings de Log da Sulake**: variáveis logadas em métodos de parse (ex: `wallItemId`, `wallItemTypeId`, `location`, `dataStr`, `secondsToExpiration`).

---

### Passo 4: Código Kotlin Gerado

O `habbo-timeline` já emite o código Kotlin pronto para colar no emulador:

```kotlin
habboResponse.apply {

    ItemDataParser(habboResponse) // delega para ItemDataParser
}

fun ItemDataParser(habboResponse: HabboResponse) {
    habboResponse.apply {

        // Campo #1: wallItemId (Histórico: String)
        writeUTF(wallItemId)

        // Campo #2: wallItemTypeId (Histórico: Int)
        writeInt(wallItemTypeId)

        // Campo #3: location (Histórico: String)
        writeUTF(location)

        // Campo #4: dataStr (Histórico: String)
        writeUTF(dataStr)

        // Campo #5: secondsToExpiration (Histórico: Boolean -> Int)
        if (isVersionAtLeast(2012, 1, 13)) {
            writeInt(secondsToExpiration)
        } else {
            writeBoolean(isUsable)
        }

        // Campo #6: usagePolicy (Histórico: Int)
        writeInt(usagePolicy)

        // Campo #7: ownerId (Histórico: Vazio -> Int)
        if (isVersionAtLeast(2013, 2, 13)) {
            writeInt(ownerId)
        }
    }
}
```

### Regras de Implementação no Emulador:
- **Precisão Temporal Estrita**: Use exatamente as datas indicadas pelas transições estruturais emitidas pela ferramenta com `isVersionAtLeast(ano, mes, dia)`.
- **Mutações de Semântica**: Quando a Sulake reaproveitou o mesmo slot de dados (ex: um `Boolean` em 2011 que virou `Int secondsToExpiration` em 2012), adapte o nome do domínio no emulador conforme o significado de cada era.
- **Delegações Desduplicadas**: Quando classes auxiliares como `parseObjectData` forem chamadas em múltiplos pacotes (`ObjectUpdateMessageEvent`, `ObjectsMessageEvent`), use o método gerado como função reutilizável.
- **Tratamento de Campos Efêmeros**: Ao gerar handlers Kotlin para compilações intermediárias ou testes efêmeros (ex: flags do Beta 2009, moderação de 24h em 2012), apenas drene os bytes do buffer para manter o socket alinhado, sem propagar esses campos temporários para persistência no banco de dados.
- **Ternários no AS3 (`cond ? 1 : 0`)**: Em composers AS3 antigos (R63A / EvaWire), expressões como `push(cond ? 1 : 0)` são serializadas pelo encoder como inteiros VL64 (`writeInteger()`), enquanto `push(boolean)` usa 1 byte de prefixo (`64 | 1` = `'A'`, `64 | 0` = `'@'`). O leitor do emulador deve respeitar a tipagem física do wire.
