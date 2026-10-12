/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DynamicDnsConfig(
    /**
     * User and host used to SSH into the machine that proxies the WebSocket
     * (e.g. the nginx box). The SSH key must already be authorized; nothing
     * is passed interactively.
     */
    @param:JsonProperty("ssh_target", required = true)
    val sshTarget: String,
    /**
     * Hostname entry to keep updated in the target's /etc/hosts.
     */
    @param:JsonProperty("hostname", required = true)
    val hostname: String,
    /**
     * Reload command run on the target after /etc/hosts changes
     * (e.g. "nginx -s reload").
     */
    @param:JsonProperty("reload_command", required = true)
    val reloadCommand: String,
    /**
     * LAN subnets whose first matching local address is published. Defaults to 192.168.0.0/16.
     */
    @param:JsonProperty("subnets")
    val subnets: List<String> = listOf("192.168.0.0/16"),
)
