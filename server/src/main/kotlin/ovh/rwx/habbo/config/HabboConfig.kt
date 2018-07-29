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

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class HabboConfig(
        @JsonProperty("port", required = true)
        val port: Int,
        @JsonProperty("web_port", required = true)
        val webPort: Int,
        @JsonProperty("database", required = true)
        val databaseConfig: DatabaseConfig,
        @JsonProperty("encryption", required = true)
        val encryptionConfig: EncryptionConfig,
        @JsonProperty("furnidata_xml", required = true)
        val furnidataXml: String,
        @JsonProperty("figuredata_xml", required = true)
        val figuredataXml: String,
        @JsonProperty("reward", required = true)
        val rewardConfig: RewardConfig,
        @JsonProperty("auto_join_room", required = true)
        val autoJoinRoom: Boolean,
        @JsonProperty("timer", required = true)
        val timerConfig: TimerConfig,
        @JsonProperty("room_task", required = true)
        val roomTaskConfig: RoomTaskConfig,
        @JsonProperty("camera", required = true)
        val cameraConfig: CameraConfig,
        @JsonProperty("catalog", required = true)
        val catalogConfig: CatalogConfig,
        @JsonProperty("recycler", required = true)
        val recyclerConfig: RecyclerConfig,
        @JsonProperty("motd_enabled", required = true)
        val motdEnabled: Boolean,
        @JsonProperty("motd_file_path", required = true)
        private val motdFilePath: String,
        @JsonProperty("server_console_figure", required = true)
        val serverConsoleFigure: String,
        @JsonProperty("analytics", required = true)
        val analyticsConfig: AnalyticsConfig
) {
    val motdContents: String by lazy {
        val f = File(motdFilePath)

        when {
            f.exists() -> f.readLines().filter { !it.startsWith('#') }.joinToString("\n").trim()
            else -> ""
        }
    }
}

