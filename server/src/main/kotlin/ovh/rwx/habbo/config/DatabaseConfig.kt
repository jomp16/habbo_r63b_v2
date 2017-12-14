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

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.zaxxer.hikari.HikariConfig

data class DatabaseConfig(
        @JsonProperty("host", required = true)
        private val host: String,
        @JsonProperty("port", required = true)
        private val port: Int,
        @JsonProperty("user", required = true)
        private val user: String,
        @JsonProperty("password", required = true)
        private val password: String,
        @JsonProperty("name", required = true)
        private val name: String,
        @JsonProperty("timeout", required = true)
        private val timeout: Long
) {
    val hikariConfig: HikariConfig
        get() {
            val config = HikariConfig()

            config.jdbcUrl = "jdbc:mysql://$host:$port/$name"
            config.username = user
            config.password = password
            config.connectionTimeout = timeout
            config.maximumPoolSize = (Runtime.getRuntime().availableProcessors() * 2) + 1

            config.addDataSourceProperty("serverTimezone", "UTC")
            config.addDataSourceProperty("prepStmtCacheSize", "250")
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            config.addDataSourceProperty("cachePrepStmts", true)
            config.addDataSourceProperty("useServerPrepStmts", true)
            config.addDataSourceProperty("characterEncoding", "UTF-8")
            config.addDataSourceProperty("useUnicode", "true")

            return config
        }
}