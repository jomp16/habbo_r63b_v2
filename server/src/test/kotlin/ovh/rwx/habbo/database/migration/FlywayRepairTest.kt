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

package ovh.rwx.habbo.database.migration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.Test
import ovh.rwx.habbo.config.HabboConfig
import java.io.File

class FlywayRepairTest {

    @Test
    fun testRepairAndMigrate() {
        val configFile = File("config.yaml").takeIf { it.exists() } ?: File("../config.yaml").takeIf { it.exists() }
        if (configFile == null || !configFile.exists()) {
            println("config.yaml not found, skipping DB test")
            return
        }

        val habboConfig: HabboConfig = ObjectMapper(YAMLFactory()).registerKotlinModule().readValue(configFile)
        val dataSource = HikariDataSource(habboConfig.databaseConfig.hikariConfig)

        try {
            // Clean up migration ranks >= 5 to cleanly re-apply V5..V10
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("DELETE FROM `flyway_schema_history` WHERE CAST(`version` AS UNSIGNED) >= 10")
                }
            }

            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()

            println("Running Flyway repair...")
            val repairResult = flyway.repair()
            println("Flyway repair finished. Warnings: ${repairResult.warnings.size}")

            println("Running Flyway migrate...")
            val migrateResult = flyway.migrate()
            println("Flyway migrate finished. Executed: ${migrateResult.migrationsExecuted}, target version: ${migrateResult.targetSchemaVersion}")

            val info = flyway.info()
            for (item in info.all()) {
                println("Migration V${item.version}: ${item.description} | state: ${item.state} | checksum: ${item.checksum}")
                org.junit.Assert.assertTrue(
                    "Migration V${item.version} must be successful or baseline",
                    item.state.isApplied || item.state.name == "BASELINE_IGNORED"
                )
            }
        } finally {
            dataSource.close()
        }
    }
}
