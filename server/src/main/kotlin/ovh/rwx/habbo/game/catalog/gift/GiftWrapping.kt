/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

package ovh.rwx.habbo.game.catalog.gift

import java.time.LocalDateTime

enum class GiftWrappingKind { BOX, RIBBON }

/** Uma caixa/fita de embrulho e a janela de build do client em que ela existe (null = sem limite). */
data class GiftWrappingType(
    val kind: GiftWrappingKind,
    val typeId: Int,
    val availableFrom: LocalDateTime?,
    val availableUntil: LocalDateTime?,
) {
    fun isAvailableAt(buildDate: LocalDateTime): Boolean =
        (availableFrom == null || !buildDate.isBefore(availableFrom))
                && (availableUntil == null || buildDate.isBefore(availableUntil))
}

/** O embrulho escolhido e validado de um pedido de presente: [price] em créditos (0 = caixa padrão grátis). */
data class GiftWrappingChoice(val spriteId: Int, val boxId: Int, val ribbonId: Int, val price: Int) {
    /** O "extra" do objeto no quarto (`openState` do GiftData): caixa * 1000 + fita. */
    val extra: Int get() = boxId * 1000 + ribbonId
}

/** Regras puras do embrulho (sem banco, testáveis). */
object GiftWrapping {
    const val MAX_MESSAGE_LENGTH = 140

    /**
     * Valida o embrulho pedido contra o que o servidor listou ao client ([stuffTypes], [defaultStuffTypes], [boxTypes],
     * [ribbonTypes], já filtrados pela release do comprador). Embrulhado (sprite em stuffTypes) exige embrulho ligado,
     * caixa e fita listadas e cobra [price]; caixa padrão é grátis, vira box/ribbon 0 e, com sprite 0 (clients antigos
     * mandam tudo 0), sorteia um dos [defaultStuffTypes] via [randomDefault]. Null = pedido inválido.
     */
    fun resolve(
        spriteId: Int,
        boxId: Int,
        ribbonId: Int,
        enabled: Boolean,
        price: Int,
        stuffTypes: List<Int>,
        defaultStuffTypes: List<Int>,
        boxTypes: List<Int>,
        ribbonTypes: List<Int>,
        randomDefault: (List<Int>) -> Int = { it.random() },
    ): GiftWrappingChoice? {
        if (spriteId in stuffTypes) {
            if (!enabled || boxId !in boxTypes || ribbonId !in ribbonTypes) return null
            return GiftWrappingChoice(spriteId, boxId, ribbonId, price.coerceAtLeast(0))
        }

        if (defaultStuffTypes.isEmpty()) return null
        if (spriteId == 0) return GiftWrappingChoice(randomDefault(defaultStuffTypes), 0, 0, 0)
        if (spriteId in defaultStuffTypes) return GiftWrappingChoice(spriteId, 0, 0, 0)

        return null
    }

    /** Mensagem do cartão: aparada, sem caracteres de controle (o 7 é o separador do extra_data) e com limite de tamanho. */
    fun cleanMessage(raw: String): String =
        raw.filter { !it.isISOControl() }.trim().take(MAX_MESSAGE_LENGTH)

    private val WRAP_REGEX = Regex("^present_wrap\\*(\\d+)$")
    private val DEFAULT_REGEX = Regex("^present_gen(\\d+)$")

    /** Ids de caixa/fita disponíveis para o client da build, em ordem crescente. */
    fun typesFor(all: Collection<GiftWrappingType>, kind: GiftWrappingKind, buildDate: LocalDateTime): List<Int> =
        all.filter { it.kind == kind && it.isAvailableAt(buildDate) }.map { it.typeId }.distinct().sorted()

    /** `present_wrap*N` ordenado por N (numérico: *10 vem depois de *9). */
    fun <T> sortedWrapItems(items: Collection<T>, nameOf: (T) -> String): List<T> =
        items.mapNotNull { item -> WRAP_REGEX.matchEntire(nameOf(item))?.let { it.groupValues[1].toInt() to item } }
            .sortedBy { it.first }
            .map { it.second }

    /** `present_gen1..N` ordenado por N; o `present_gen` sem número não entra. */
    fun <T> sortedDefaultItems(items: Collection<T>, nameOf: (T) -> String): List<T> =
        items.mapNotNull { item -> DEFAULT_REGEX.matchEntire(nameOf(item))?.let { it.groupValues[1].toInt() to item } }
            .sortedBy { it.first }
            .map { it.second }
}
