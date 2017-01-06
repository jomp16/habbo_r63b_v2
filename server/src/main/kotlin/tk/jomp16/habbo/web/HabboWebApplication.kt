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

package tk.jomp16.habbo.web

import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element
import ro.pippo.core.Application
import tk.jomp16.habbo.BuildConfig
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.kotlin.addAndGetEhCache
import tk.jomp16.habbo.util.Utils
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class HabboWebApplication : Application() {
    val badgeCache: Ehcache = HabboServer.cacheManager.addAndGetEhCache("badgeCache")

    override fun onInit() {
        super.onInit()

        GET("/", {
            it.response.text().send("Hello world!")
        })

        GET("/api/v1/status", {
            it.response.json().send(mapOf(
                    "version" to BuildConfig.VERSION,
                    "used_ram" to Utils.ramUsageString,
                    "users_online" to HabboServer.habboSessionManager.habboSessions.size,
                    "rooms_loaded" to HabboServer.habboGame.roomManager.roomTaskManager.rooms.size,
                    "uptime_seconds" to TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().uptime)
            ))
        })

        GET("/habbo-imaging/badge/{badge_code}", {
            val badgeCode = File(it.getParameter("badge_code").toString("")).nameWithoutExtension

            if (!badgeCache.isKeyInCache(badgeCode)) {
                val badgeImage = HabboServer.habboGame.groupManager.groupBadge.getGroupBadge(badgeCode)

                val byteArrayOutputStream = ByteArrayOutputStream()

                ImageIO.write(badgeImage, "png", byteArrayOutputStream)

                badgeCache.put(Element(badgeCode, byteArrayOutputStream.toByteArray()))
            }

            val badgeImageByteArray = badgeCache[badgeCode]!!.objectValue as ByteArray

            it.response.contentType("image/png")
            it.response.contentLength(badgeImageByteArray.size.toLong())
            it.response.outputStream.write(badgeImageByteArray)
        })
    }
}