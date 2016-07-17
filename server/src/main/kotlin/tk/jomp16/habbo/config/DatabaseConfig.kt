/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.zaxxer.hikari.HikariConfig

data class DatabaseConfig(
        @JsonProperty("host")
        val host: String,
        @JsonProperty("port")
        val port: Int,
        @JsonProperty("user")
        val user: String,
        @JsonProperty("password")
        val password: String,
        @JsonProperty("name")
        val name: String
) {
    val hikariConfig: HikariConfig
        get() {
            val config = HikariConfig()

            config.jdbcUrl = "jdbc:mysql://$host:$port/$name"
            config.username = user
            config.password = password
            config.addDataSourceProperty("useSSL", false)
            config.addDataSourceProperty("serverTimezone", "UTC")

            return config
        }
}