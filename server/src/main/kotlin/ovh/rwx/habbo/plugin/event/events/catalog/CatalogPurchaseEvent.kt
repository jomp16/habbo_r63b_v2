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

package ovh.rwx.habbo.plugin.event.events.catalog

import ovh.rwx.habbo.game.catalog.CatalogItem
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.user.HabboSession

class CatalogPurchaseEvent(
    val habboSession: HabboSession,
    val catalogItem: CatalogItem,
    val extraData: String,
    val amount: Int,
    val purchasedItems: List<UserItem>
)
