/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.navigator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.navigator.NavigatorDao

class NavigatorManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val navTabs: Array<String> = arrayOf("official_view", "hotel_view", "roomads_view", "myworld_view")
    val navigatorRoomCategories: MutableMap<Int, NavigatorRoomCategory> = mutableMapOf()
    val navigatorEventCategories: MutableMap<Int, NavigatorEventCategory> = mutableMapOf()

    fun load() {
        log.info("Loading navigator...")

        navigatorRoomCategories.clear()
        navigatorEventCategories.clear()

        navigatorRoomCategories += NavigatorDao.getNavigatorRoomCategories().associateBy { it.id }
        navigatorEventCategories += NavigatorDao.getNavigatorEventCategories().associateBy { it.id }

        log.info("Loaded {} navigator categories!", navigatorRoomCategories.size)
        log.info("Loaded {} navigator promo categories!", navigatorEventCategories.size)
    }
}