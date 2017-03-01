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

package tk.jomp16.habbo.game.permission

import kotlinx.support.jdk7.use
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import java.sql.ResultSet
import java.util.*

class PermissionManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val permissionsUser: MutableMap<Int, MutableList<String>> = HashMap()
    val permissionsRank: MutableMap<Int, MutableList<String>> = HashMap()

    init {
        log.info("Loading permissions...")

        HabboServer.database {
            connection.prepareStatement("SELECT * FROM permissions_users").use { preparedStatement ->
                preparedStatement.executeQuery().use { resultSet ->
                    readColumnsAndAddToMap(permissionsRank, resultSet, "user_id")
                }
            }

            connection.prepareStatement("SELECT * FROM permissions_ranks").use { preparedStatement ->
                preparedStatement.executeQuery().use { resultSet ->
                    readColumnsAndAddToMap(permissionsRank, resultSet, "rank")
                }
            }
        }

        log.info("Loaded " + permissionsUser.size + " permissions for user!")
        log.info("Loaded " + permissionsRank.size + " permissions for rank!")
    }

    private fun readColumnsAndAddToMap(map: MutableMap<Int, MutableList<String>>, resultSet: ResultSet, columnName: String) {
        val metadata = resultSet.metaData

        while (resultSet.next()) {
            val permissions: MutableList<String> = ArrayList()

            for (i in 3..metadata.columnCount) {
                if (resultSet.getBoolean(i)) {
                    permissions.add(metadata.getColumnName(i))
                }

                map.putIfAbsent(resultSet.getInt(columnName), permissions)
            }
        }
    }

    fun userHasCustomPermission(userId: Int) = this.permissionsUser.containsKey(userId)

    fun userHasPermission(userId: Int, permission: String) = this.userHasCustomPermission(userId) && permissionsUser[userId]!!.filter { it == permission }.isNotEmpty()

    fun rankHasPermission(rankId: Int, permission: String) = this.permissionsRank.containsKey(rankId) && permissionsRank[rankId]!!.filter { it == permission }.isNotEmpty()
}