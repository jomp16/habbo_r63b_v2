/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.group

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.group.GroupDao

class GroupManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val groupBadgesBases: MutableList<Triple<Int, String, String>> = mutableListOf()
    val groupBaseColors: MutableList<Pair<Int, String>> = mutableListOf()
    val groupBadgesSymbols: MutableList<Triple<Int, String, String>> = mutableListOf()
    val groupBadgeSymbolColors: MutableList<Pair<Int, String>> = mutableListOf()

    fun load() {
        log.info("Loading group badges...")

        groupBadgesBases.clear()
        groupBaseColors.clear()
        groupBadgesSymbols.clear()
        groupBadgeSymbolColors.clear()

        groupBadgesBases += GroupDao.getGroupsBadgesBases()
        groupBaseColors += GroupDao.getGroupsBadgesBaseColors()
        groupBadgesSymbols += GroupDao.getGroupsBadgesSymbols()
        groupBadgeSymbolColors += GroupDao.getGroupsBadgesSymbolColors()

        log.info("Loaded ${groupBadgesBases.size} group badges base!")
        log.info("Loaded ${groupBaseColors.size} group badges base colors!")
        log.info("Loaded ${groupBadgesSymbols.size} group badges symbol!")
        log.info("Loaded ${groupBadgeSymbolColors.size} group badges symbol colors!")
    }
}