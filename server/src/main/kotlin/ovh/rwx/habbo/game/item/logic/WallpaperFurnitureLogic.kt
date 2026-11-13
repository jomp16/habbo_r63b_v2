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

import ovh.rwx.habbo.game.item.FurniCategory
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.LimitedItemData
import ovh.rwx.habbo.game.item.stuff.LegacyStuffData
import ovh.rwx.habbo.game.item.stuff.StuffData
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Espelho de FurnitureInternalLinkLogic + papéis de parede/piso/fundo:
 * o roomExtra carrega o paperId/categoria (2 = wallpaper, 3 = floor, 4 = landscape).
 */
class WallpaperFurnitureLogic : FurnitureLogic() {
    override val interactionTypes: List<InteractionType> = listOf(InteractionType.ROOM_EFFECT)
    override val itemNames: List<String> = listOf("wallpaper", "floor", "landscape")

    override fun parseStuffData(
        extraData: String,
        furnishing: Furnishing,
        limitedItemData: LimitedItemData?,
        magicRemove: Boolean
    ): StuffData {
        val roomExtra = getFurniCategory(furnishing).id
        return LegacyStuffData(extraData, roomExtra)
    }

    override fun correctCatalogExtraData(
        habboSession: HabboSession,
        extraData: String,
        furnishing: Furnishing
    ): String =
        extraData.toDoubleOrNull()?.let { extraData.trim() } ?: "0"

    override fun sanitizeForDatabase(extraData: String): String =
        extraData.toDoubleOrNull()?.let { extraData.trim() } ?: "0"

    override fun getFurniCategory(furnishing: Furnishing): FurniCategory = when (furnishing.itemName) {
        "wallpaper" -> FurniCategory.WALL_PAPER
        "floor" -> FurniCategory.FLOOR
        "landscape" -> FurniCategory.LANDSCAPE
        else -> when {
            furnishing.itemName.contains("a1") -> FurniCategory.WALL_PAPER
            furnishing.itemName.contains("a2") -> FurniCategory.FLOOR
            furnishing.itemName.contains("landscape") -> FurniCategory.LANDSCAPE
            else -> FurniCategory.DEFAULT
        }
    }
}
