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

package tk.jomp16.habbo.game.group

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.database.group.GroupDao
import java.util.*

class GroupManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val badgesBases: MutableList<Triple<Int, String, String>> = ArrayList()
    val badgeBaseColors: MutableList<Pair<Int, String>> = ArrayList()
    val badgesSymbols: MutableList<Triple<Int, String, String>> = ArrayList()
    val badgeSymbolColors: MutableList<Pair<Int, String>> = ArrayList()

    init {
        load()
    }

    fun load() {
        log.info("Loading group badges...")

        badgesBases += GroupDao.getBadgesBases()
        badgeBaseColors += GroupDao.getBadgesBaseColors()
        badgesSymbols += GroupDao.getBadgesSymbols()
        badgeSymbolColors += GroupDao.getBadgesSymbolColors()

        log.info("Loaded ${badgesBases.size} group badges base!")
        log.info("Loaded ${badgeBaseColors.size} group badges base colors!")
        log.info("Loaded ${badgesSymbols.size} group badges symbol!")
        log.info("Loaded ${badgeSymbolColors.size} group badges symbol colors!")
    }
}