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

package ovh.rwx.habbo.game.permission

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.DbSession
import ovh.rwx.habbo.database.db
import java.util.concurrent.ConcurrentHashMap

class PermissionManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val permissionsUser: ConcurrentHashMap<Int, Set<String>> = ConcurrentHashMap()
    private val permissionsRank: ConcurrentHashMap<Int, Set<String>> = ConcurrentHashMap()
    val availablePermissions: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun load() {
        log.info("Loading permissions...")

        permissionsUser.clear()
        permissionsRank.clear()
        availablePermissions.clear()

        db {
            loadPermissionsTable("SELECT * FROM `permissions_users`", "user_id", permissionsUser)
            loadPermissionsTable("SELECT * FROM `permissions_ranks`", "rank", permissionsRank)
        }

        log.info("Loaded {} permissions for user!", permissionsUser.size)
        log.info("Loaded {} permissions for rank!", permissionsRank.size)
    }

    private fun DbSession.loadPermissionsTable(
        sql: String,
        idColumn: String,
        targetMap: ConcurrentHashMap<Int, Set<String>>
    ) {
        val rows = handle.createQuery(sql).mapToMap().list()

        for (row in rows) {
            val id = (row[idColumn] as? Number)?.toInt() ?: continue
            val permissions = mutableSetOf<String>()

            for ((column, value) in row) {
                if (column.equals("id", ignoreCase = true) || column.equals(idColumn, ignoreCase = true)) {
                    continue
                }

                availablePermissions.add(column)

                val isGranted = when (value) {
                    is Boolean -> value
                    is Number -> value.toInt() != 0
                    is String -> value == "1" || value.equals("true", ignoreCase = true)
                    else -> false
                }

                if (isGranted) {
                    permissions.add(column)
                }
            }

            targetMap[id] = permissions
        }
    }

    fun userHasCustomPermission(userId: Int): Boolean = permissionsUser.containsKey(userId)

    fun userHasPermission(userId: Int, permission: String): Boolean =
        permissionsUser[userId]?.contains(permission) == true

    fun rankHasPermission(rankId: Int, permission: String): Boolean =
        permissionsRank[rankId]?.contains(permission) == true

    fun getUserPermissions(userId: Int, rankId: Int): List<String> =
        (permissionsUser[userId] ?: permissionsRank[rankId] ?: emptySet()).toList()
}
