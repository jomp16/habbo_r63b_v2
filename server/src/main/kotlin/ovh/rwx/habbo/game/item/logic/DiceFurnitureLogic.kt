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
import ovh.rwx.habbo.game.user.HabboSession

class DiceFurnitureLogic : FurnitureLogic() {
    override val interactionTypes: List<InteractionType> = listOf(InteractionType.DICE)

    override fun correctCatalogExtraData(
        habboSession: HabboSession,
        extraData: String,
        furnishing: Furnishing
    ): String = "0"

    override fun sanitizeForDatabase(extraData: String): String {
        val value = extraData.toIntOrNull()
        // -1 indica rolagem ativa (efêmero). Valores fora de 0..6 resetam para 0 (fechado)
        return if (value == null || value !in 0..6) "0" else extraData
    }
}
