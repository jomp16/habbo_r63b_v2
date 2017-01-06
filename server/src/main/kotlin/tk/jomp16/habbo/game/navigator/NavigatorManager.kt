/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.game.navigator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.database.navigator.NavigatorDao
import java.util.*

class NavigatorManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val navTabs: Array<String> = arrayOf(
            "official_view",
            "hotel_view",
            "roomads_view",
            "myworld_view"
    )

    val navigatorFlatCats: MutableMap<Int, NavigatorFlatcat> = HashMap()
    val navigatorPromoCats: MutableMap<Int, NavigatorPromocat> = HashMap()

    init {
        log.info("Loading navigator...")

        navigatorFlatCats += NavigatorDao.getNavigatorFlatCats().associateBy { it.id }
        navigatorPromoCats += NavigatorDao.getNavigatorPromoCats().associateBy { it.id }

        log.info("Loaded {} navigator categories!", navigatorFlatCats.size)
        log.info("Loaded {} navigator promo categories!", navigatorPromoCats.size)
    }
}