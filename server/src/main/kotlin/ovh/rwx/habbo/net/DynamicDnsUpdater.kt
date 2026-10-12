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

package ovh.rwx.habbo.net

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.config.DynamicDnsConfig
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

/**
 * Keeps a hostname entry in the /etc/hosts of the reverse-proxy machine (reached
 * via SSH, key-based) pointing at this machine's LAN IP, so the WebSocket proxy
 * always targets wherever the emulator is currently running.
 */
object DynamicDnsUpdater {
    private val log = LoggerFactory.getLogger(javaClass)

    fun start(config: DynamicDnsConfig) {
        val subnets = config.subnets.map { Subnet(it) }

        // periodic refresh: DHCP may renew/change our address while running
        val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "dynamic-dns").apply { isDaemon = true }
        }
        scheduler.scheduleWithFixedDelay(
            { runOnce(config, subnets) },
            0, 10, TimeUnit.MINUTES
        )
    }

    private fun runOnce(config: DynamicDnsConfig, subnets: List<Subnet>) {
        try {
            val ip = findLocalIp(subnets) ?: run {
                log.warn("[dynamic-dns] no local address in {}, skipping update", config.subnets)
                return
            }

            val current = ssh(config, "awk '/[[:space:]]${config.hostname}([[:space:]]|$)/ {print $1}' /etc/hosts")
            if (current == ip) return // nothing to do

            val changed = ssh(
                config,
                "sed -i -E 's/^[^#[:space:]].*[[:space:]]${config.hostname}([[:space:]]|$)/${ip} ${config.hostname}/' /etc/hosts && grep -q '${ip}[[:space:]]${config.hostname}' /etc/hosts" +
                    " || echo '${ip} ${config.hostname}' >> /etc/hosts"
            ) != null
            if (changed) {
                ssh(config, config.reloadCommand)
                log.info("[dynamic-dns] ${config.hostname} -> {} (updated on ${config.sshTarget})", ip)
            }
        } catch (e: Exception) {
            log.warn("[dynamic-dns] update failed: {}", e.message)
        }
    }

    /**
     * First non-loopback address of this machine that falls inside [subnets].
     * Excludes virtual bridge/docker interfaces so we don't publish a docker0 IP.
     */
    private fun findLocalIp(subnets: List<Subnet>): String? {
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .filter { !it.name.startsWith("docker") && !it.name.startsWith("br-") && !it.name.startsWith("veth") && !it.name.startsWith("vmnet") }
            .flatMap { ni: NetworkInterface -> ni.interfaceAddresses.asSequence() }
            .mapNotNull { ia: InterfaceAddress? -> ia?.address }
            .filter { it is java.net.Inet4Address }
            .map { it as java.net.Inet4Address }
            .firstOrNull { addr -> subnets.any { it.contains(addr) } }
            ?.hostAddress
    }

    private fun ssh(config: DynamicDnsConfig, remoteCommand: String): String? {
        val p = ProcessBuilder(
            "ssh",
            "-o", "BatchMode=yes",
            "-o", "StrictHostKeyChecking=accept-new",
            "-o", "ConnectTimeout=10",
            config.sshTarget,
            remoteCommand
        ).redirectErrorStream(true).start()

        p.inputStream.bufferedReader().use { r ->
            val out = r.readText()
            p.waitFor()
            if (p.exitValue() != 0) {
                log.warn("[dynamic-dns] ssh exited {}: {}", p.exitValue(), out.trim())
                return null
            }
            return out.trim().takeIf { it.isNotEmpty() }
        }
    }

    private class Subnet(cidr: String) {
        private val addr: Int
        private val mask: Int

        init {
            val (base, bits) = cidr.split("/")
            val b = InetAddress.getByName(base).address
            addr = ((b[0].toInt() and 0xff) shl 24) or ((b[1].toInt() and 0xff) shl 16) or
                ((b[2].toInt() and 0xff) shl 8) or (b[3].toInt() and 0xff)
            mask = if (bits.toInt() == 0) 0 else (-1 shl (32 - bits.toInt()))
        }

        fun contains(a: java.net.Inet4Address): Boolean {
            val v = a.address.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xff) }
            return (v and mask) == (addr and mask)
        }
    }
}
