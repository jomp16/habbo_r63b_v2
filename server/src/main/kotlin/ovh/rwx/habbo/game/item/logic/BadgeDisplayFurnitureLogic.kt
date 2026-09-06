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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.LimitedItemData
import ovh.rwx.habbo.game.item.stuff.BadgeDisplayData
import ovh.rwx.habbo.game.item.stuff.StuffData
import ovh.rwx.habbo.game.user.HabboSession
import java.time.LocalDateTime

class BadgeDisplayFurnitureLogic : FurnitureLogic() {
    override val interactionTypes: List<InteractionType> = listOf(InteractionType.BADGE_DISPLAY)

    override fun parseStuffData(
        extraData: String,
        furnishing: Furnishing,
        limitedItemData: LimitedItemData?,
        magicRemove: Boolean
    ): StuffData =
        BadgeDisplayData.parse(extraData).toStuffData()

    override fun correctCatalogExtraData(
        habboSession: HabboSession,
        extraData: String,
        furnishing: Furnishing
    ): String? {
        if (!habboSession.habboBadge.badges.containsKey(extraData.trim())) return null

        return BadgeDisplayData(
            badgeName = extraData.trim(),
            ownerName = habboSession.userInformation.username,
            displayedAt = HabboServer.DATE_TIME_FORMATTER_ONLY_DAYS.format(LocalDateTime.now()),
        ).toExtraData()
    }

    override fun sanitizeForDatabase(extraData: String): String {
        if (extraData.isBlank()) return extraData
        return BadgeDisplayData.parse(extraData).toExtraData()
    }
}
