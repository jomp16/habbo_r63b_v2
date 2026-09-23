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

package ovh.rwx.habbo.game.item.wired.selector

enum class WiredSelectorType(val code: Int) {
    FURNI_BY_TYPE(0), // has custom UI
    FURNI_BY_FURNI(1),
    USERS_BY_TYPE(2), // has custom UI
    USERS_IN_TEAM(3), // has custom UI
    FURNI_ON_FURNI(4), // has custom UI
    FURNI_FROM_SIGNAL(5),
    FURNI_IN_NEIGHBORHOOD(6), // has custom UI
    FURNI_IN_AREA(7), // has custom UI
    USERS_ON_FURNI(8),
    USERS_PERFORMING_ACTION(9), // has custom UI
    USERS_FROM_SIGNAL(10),
    USERS_BY_NAME(11), // has custom UI
    USERS_IN_NEIGHBORHOOD(12), // has custom UI
    USERS_IN_AREA(13), // has custom UI
    USERS_WITH_HANDITEM(14), // has custom UI
    USERS_IN_GROUP(15), // has custom UI
    FURNI_WITH_ALTITUDE(16), // has custom UI
    FURNI_WITH_VARIABLE(17), // has custom UI
    USERS_WITH_VARIABLE(18), // has custom UI
    REMOTE_SELECTOR(19) // has custom UI
}