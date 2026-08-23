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

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class HabboConfig(
        @param:JsonProperty("port", required = true)
        val port: Int,
        @param:JsonProperty("ws_port", required = true)
        val wsPort: Int,
        @param:JsonProperty("web_port", required = true)
        val webPort: Int,
        @param:JsonProperty("database", required = true)
        val databaseConfig: DatabaseConfig,
        @param:JsonProperty("encryption", required = true)
        val encryptionConfig: EncryptionConfig,
        @param:JsonProperty("furnidata_xml", required = true)
        val furnidataXml: String,
        @param:JsonProperty("figuredata_xml", required = true)
        val figuredataXml: String,
        @param:JsonProperty("reward", required = true)
        val rewardConfig: RewardConfig,
        @param:JsonProperty("auto_join_room", required = true)
        val autoJoinRoom: Boolean,
        @param:JsonProperty("timer", required = true)
        val timerConfig: TimerConfig,
        @param:JsonProperty("room_task", required = true)
        val roomTaskConfig: RoomTaskConfig,
        @param:JsonProperty("camera", required = true)
        val cameraConfig: CameraConfig,
        @param:JsonProperty("catalog", required = true)
        val catalogConfig: CatalogConfig,
        @param:JsonProperty("recycler", required = true)
        val recyclerConfig: RecyclerConfig,
        @param:JsonProperty("motd_enabled", required = true)
        val motdEnabled: Boolean,
        @param:JsonProperty("motd_file_path", required = true)
        private val motdFilePath: String,
        @param:JsonProperty("server_console_figure", required = true)
        val serverConsoleFigure: String,
        @param:JsonProperty("analytics", required = true)
        val analyticsConfig: AnalyticsConfig,
        @param:JsonProperty("game", required = true)
        val gameConfig: GameConfig,
        @param:JsonProperty("pets")
        val petConfig: PetConfig = PetConfig(),
        @param:JsonProperty("stress_test")
        val stressTest: Boolean = false,
        @param:JsonProperty("bot_ticket_prefix")
        val botTicketPrefix: String = "bot-",
        @param:JsonProperty("debug")
        val debug: Boolean = false,
) {
    val motdContents: String by lazy {
        val f = File(motdFilePath)

        when {
            f.exists() -> f.readLines().filter { !it.startsWith('#') }.joinToString("\n").trim()
            else -> ""
        }
    }
}

