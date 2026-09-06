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

package ovh.rwx.habbo.game.item.logic

import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.LimitedItemData
import ovh.rwx.habbo.game.item.stuff.LegacyStuffData
import ovh.rwx.habbo.game.item.stuff.StuffData
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Espelho server-side de com.sulake.habbo.room.object.logic.furniture.FurnitureLogic (client).
 *
 * Cada família de mobília se responsabiliza por converter o extraData interno
 * (string armazenada no banco, campos separados por [SEPARATOR] quando múltiplos)
 * para o StuffData estruturado do protocolo, e por corrigir o extraData no
 * momento da compra no catálogo.
 *
 * O registro acontece por [interactionTypes] (igual aos ItemInteractor) e,
 * opcionalmente, por [itemNames] para casos especiais por nome (wallpaper etc.).
 */
abstract class FurnitureLogic {
    abstract val interactionTypes: List<InteractionType>
    open val itemNames: List<String> = emptyList()

    open fun parseStuffData(
        extraData: String,
        furnishing: Furnishing,
        limitedItemData: LimitedItemData?,
        magicRemove: Boolean = false,
    ): StuffData = LegacyStuffData(extraData)

    /**
     * ExtraData inicial quando o item é comprado pelo catálogo.
     * Retorna null para cancelar a compra.
     */
    open fun correctCatalogExtraData(habboSession: HabboSession, extraData: String, furnishing: Furnishing): String? =
        ""

    companion object {
        // Formato interno do servidor para extraData multi-campo no banco.
        // O client nunca o vê: ele recebe o StuffData estruturado do protocolo.
        const val SEPARATOR: Char = 7.toChar()
    }
}
