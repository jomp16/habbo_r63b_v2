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

package ovh.rwx.habbo.game.group

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.group.GroupDao

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class GroupManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val groups: ConcurrentHashMap<Int, Group> = ConcurrentHashMap()
    val groupBadgesBases: MutableList<Triple<Int, String, String>> = CopyOnWriteArrayList()
    val groupBaseColors: MutableList<Pair<Int, String>> = CopyOnWriteArrayList()
    val groupBadgesSymbols: MutableList<Triple<Int, String, String>> = CopyOnWriteArrayList()
    val groupBadgeSymbolColors: MutableList<Pair<Int, String>> = CopyOnWriteArrayList()
    val groupBadgeBackgroundColors: MutableList<Pair<Int, String>> = CopyOnWriteArrayList()

    fun load() {
        log.info("Loading group badges...")

        groupBadgesBases.clear()
        groupBaseColors.clear()
        groupBadgesSymbols.clear()
        groupBadgeSymbolColors.clear()
        groupBadgeBackgroundColors.clear()
        groups.clear()

        groups += GroupDao.getGroupsData().associateBy({ it.id }, { Group(it) })
        groupBadgesBases += GroupDao.getGroupsBadgesBases()
        groupBaseColors += GroupDao.getGroupsBadgesBaseColors()
        groupBadgesSymbols += GroupDao.getGroupsBadgesSymbols()
        groupBadgeSymbolColors += GroupDao.getGroupsBadgesSymbolColors()
        groupBadgeBackgroundColors += GroupDao.getGroupsBadgesBackgroundColors()

        log.info("Loaded {} groups!", groups.size)
        log.info("Loaded {} group badges base!", groupBadgesBases.size)
        log.info("Loaded {} group badges base colors!", groupBaseColors.size)
        log.info("Loaded {} group badges symbol!", groupBadgesSymbols.size)
        log.info("Loaded {} group badges symbol colors!", groupBadgeSymbolColors.size)
        log.info("Loaded {} group badges background colors!", groupBadgeBackgroundColors.size)
    }

    fun generateBadge(badgeParts: List<Int>): String {
        if (badgeParts.size < 2) return ""
        val base = String.format("b%02d%02d", badgeParts[0], badgeParts[1])
        val symbols = badgeParts.drop(3).chunked(3).joinToString("") { part ->
            if (part.size == 3) String.format("s%02d%02d%d", part[0], part[1], part[2]) else ""
        }
        return base + symbols
    }

    fun createGroup(
        name: String,
        description: String,
        groupBadge: String,
        ownerId: Int,
        roomId: Int,
        backgroundColorPrimary: Int,
        backgroundColorSecondary: Int
    ): Group {
        val groupId = GroupDao.createGroup(
            name,
            description,
            groupBadge,
            ownerId,
            roomId,
            GroupMembershipState.OPEN,
            backgroundColorPrimary,
            backgroundColorSecondary,
            false
        )
        GroupDao.addMember(groupId, ownerId, 2)

        // todo: delete room rights

        log.info("Created new group n° {} - name {}", groupId, name)
        val groupData = GroupDao.getGroupData(groupId)
        val group = Group(groupData)

        groups[groupId] = group

        return group
    }

    fun getParts(code: String, isSymbol: Boolean): List<String> {
        val tmp = if (isSymbol) groupBadgesSymbols else groupBadgesBases
        val partKey = if (code.startsWith('0')) {
            val matched = tmp.firstOrNull { it.first < 10 && code.startsWith("0${it.first}") }?.first
            if (matched != null) "0$matched" else "00"
        } else {
            val matched = tmp.firstOrNull { it.first >= 10 && code.startsWith("${it.first}") }?.first
            if (matched != null) "$matched" else "0"
        }

        val endColor = if (isSymbol) (code.length - 1).coerceAtLeast(partKey.length) else code.length
        val partColor = if (partKey.length <= code.length) code.substring(partKey.length, endColor).ifEmpty { "0" } else "0"
        val partPos = if (isSymbol && code.isNotEmpty()) code.takeLast(1) else "0"

        return listOf(partKey, partColor, partPos)
    }
}