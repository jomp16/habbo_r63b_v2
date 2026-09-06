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

class PressurePadFurnitureLogic : FurnitureLogic() {
    override val interactionTypes: List<InteractionType> = listOf(InteractionType.PRESSURE_PAD)

    override fun correctCatalogExtraData(
        habboSession: HabboSession,
        extraData: String,
        furnishing: Furnishing
    ): String = "0"

    // Placa de pressão só fica ativa ("1") enquanto um avatar estiver sobre ela. No banco, sempre "0".
    override fun sanitizeForDatabase(extraData: String): String = "0"
}
