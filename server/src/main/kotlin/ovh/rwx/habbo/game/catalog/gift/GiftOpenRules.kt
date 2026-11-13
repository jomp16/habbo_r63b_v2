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

import ovh.rwx.habbo.game.item.ItemType
import java.util.concurrent.ConcurrentHashMap

/** Resultado da revalidação de uma abertura de presente, depois do atraso da animação. */
enum class OpenDecision {
    PROCEED,

    /** A sessão do dono caiu enquanto a caixa abria. */
    ABORT_SESSION_INACTIVE,

    /** A caixa mudou de dono (ou a sessão é de outro usuário). */
    ABORT_OWNER_CHANGED,

    /** A caixa não está mais neste quarto (recolhida/removida). */
    ABORT_BOX_GONE,

    /** Nada a entregar: modelo novo sem conteúdo ou legado com `amount <= 0`. */
    ABORT_EMPTY,
}

/** Para onde vai o conteúdo de uma caixa aberta. */
enum class GiftPlacement {
    /** Um único mobi de chão: tenta no lugar da caixa (se falhar, vai para o inventário). */
    TRY_PLACE_IN_ROOM,

    INVENTORY,
}

/** Reivindicação atômica de caixas em abertura: o clique duplo só reivindica uma vez. */
class OpeningClaims {
    private val claimed: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    /** True só para quem reivindicou primeiro. */
    fun tryClaim(boxId: Int): Boolean = claimed.add(boxId)

    fun release(boxId: Int) {
        claimed.remove(boxId)
    }

    fun isClaimed(boxId: Int): Boolean = boxId in claimed
}

/** Decisões puras da abertura de presente (sem sessão/sala/banco), testáveis. */
object GiftOpenRules {
    /** Antes de reivindicar: só o dono abre, e a caixa precisa estar no quarto. */
    fun preflight(boxOwnerId: Int, sessionUserId: Int, boxStillInRoom: Boolean): Boolean =
        boxOwnerId == sessionUserId && boxStillInRoom

    /**
     * Revalida depois do atraso, na ordem: sessão viva, mesmo dono, caixa ainda no quarto, há o que entregar.
     * [legacyAmount] != null indica registro legado de `items_gift` (vazio se `<= 0`); null = modelo novo, onde
     * [contentsCount] == 0 é vazio.
     */
    fun validate(
        channelActive: Boolean,
        boxOwnerId: Int,
        sessionUserId: Int,
        boxStillInRoom: Boolean,
        contentsCount: Int,
        legacyAmount: Int?,
    ): OpenDecision = when {
        !channelActive -> OpenDecision.ABORT_SESSION_INACTIVE
        boxOwnerId != sessionUserId -> OpenDecision.ABORT_OWNER_CHANGED
        !boxStillInRoom -> OpenDecision.ABORT_BOX_GONE
        (legacyAmount?.let { it <= 0 } ?: (contentsCount == 0)) -> OpenDecision.ABORT_EMPTY
        else -> OpenDecision.PROCEED
    }

    /** Um único mobi de chão tenta o lugar da caixa; qualquer outra coisa (vários, parede, vazio) vai ao inventário. */
    fun placement(contentTypes: List<ItemType>): GiftPlacement =
        if (contentTypes.singleOrNull() == ItemType.FLOOR) GiftPlacement.TRY_PLACE_IN_ROOM else GiftPlacement.INVENTORY
}
