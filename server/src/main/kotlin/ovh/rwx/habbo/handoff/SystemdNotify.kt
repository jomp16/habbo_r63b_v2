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

package ovh.rwx.habbo.handoff

import org.slf4j.LoggerFactory

object SystemdNotify {
    private val log = LoggerFactory.getLogger(javaClass)

    fun notifyReady(pid: Long = ProcessHandle.current().pid()) {
        val notifySocket = System.getenv("NOTIFY_SOCKET")
        if (notifySocket.isNullOrBlank()) return

        runCatching {
            val process = ProcessBuilder("systemd-notify", "--ready", "--pid=$pid")
                .redirectErrorStream(true)
                .start()
            val exit = process.waitFor()
            if (exit != 0) {
                log.warn("systemd-notify --ready --pid={} exited with {}", pid, exit)
            }
        }.onFailure { e ->
            log.warn("Failed to invoke systemd-notify: {}", e.message)
        }
    }
}
