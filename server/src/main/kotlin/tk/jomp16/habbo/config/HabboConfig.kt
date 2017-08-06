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

package tk.jomp16.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.File

data class HabboConfig(
        @JsonProperty("ip")
        val ip: String,
        @JsonProperty("port")
        val port: Int,
        @JsonProperty("web_port")
        val webPort: Int,
        @JsonProperty("database")
        val databaseConfig: DatabaseConfig,
        @JsonProperty("diffie_hellman_key_size")
        val diffieHellmanKeySize: Int,
        @JsonProperty("rsa")
        val rsaConfig: RSAConfig,
        @JsonProperty("rc4")
        val rc4: Boolean,
        @JsonProperty("c_images_url")
        val cImagesUrl: String,
        @JsonProperty("games_url")
        val gamesUrl: String,
        @JsonProperty("furnidata_xml")
        val furnidataXml: String,
        @JsonProperty("reward")
        val rewardConfig: RewardConfig,
        @JsonProperty("auto_join_room")
        val autoJoinRoom: Boolean,
        @JsonProperty("timer")
        val timerConfig: TimerConfig,
        @JsonProperty("room_task")
        val roomTaskConfig: RoomTaskConfig,
        @JsonProperty("camera")
        val cameraConfig: CameraConfig,
        @JsonProperty("catalog")
        val catalogConfig: CatalogConfig,
        @JsonProperty("recycler")
        val recyclerConfig: RecyclerConfig,
        @JsonProperty("motd_enabled")
        val motdEnabled: Boolean,
        @JsonProperty("motd_file_path")
        val motdFilePath: String,
        @JsonProperty("server_console_figure")
        val serverConsoleFigure: String
) {
    val motdContents: String by lazy {
        val f = File(motdFilePath)

        when {
            f.exists() -> f.readLines().filter { !it.startsWith('#') }.joinToString("\n").trim()
            else -> ""
        }
    }
}