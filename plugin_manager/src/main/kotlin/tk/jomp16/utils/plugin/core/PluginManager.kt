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

package tk.jomp16.utils.plugin.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.bus.error.IPublicationErrorHandler
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.utils.plugin.api.PluginListener
import tk.jomp16.utils.plugin.event.events.PluginListenerAddedEvent
import tk.jomp16.utils.plugin.event.events.PluginListenerRemovedEvent
import tk.jomp16.utils.plugin.json.PluginInfo
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*

class PluginManager : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val pluginsListener: MutableList<Pair<ClassLoader, PluginListener>> = ArrayList()
    val pluginsJar: MutableMap<String, Triple<PluginInfo, ClassLoader, List<PluginListener>>> = HashMap()
    private val eventBus = MBassador<Any>(IPublicationErrorHandler { error -> log.error("An error happened when handling listener!", error) })
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    fun loadPluginsFromDir(fileDir: File) {
        if (!fileDir.exists() || !fileDir.isDirectory) return

        log.info("Loading plugins from dir ${fileDir.name}...")

        fileDir.walk().filter { it.isFile && it.extension == "jar" }.forEach { pluginFile ->
            val pluginPair = loadPluginListenersFromJar(pluginFile)

            val pluginPropertiesStream = pluginPair.first.getResourceAsStream("plugin.json")

            if (pluginPropertiesStream == null) {
                log.error("Plugin ${pluginFile.name} didn't have a plugin.json!")

                return@forEach
            }

            val pluginInfo: PluginInfo = objectMapper.readValue(pluginPropertiesStream)

            if (pluginsJar.containsKey(pluginInfo.name)) return@forEach

            pluginPair.second.forEach { addPlugin(it, pluginPair.first) }
            pluginsJar.put(pluginInfo.name, Triple(pluginInfo, pluginPair.first, pluginPair.second))

            log.info("Loaded plugin: $pluginInfo")
        }
    }

    fun addPlugin(pluginListener: PluginListener, classLoader: ClassLoader = javaClass.classLoader) {
        if (pluginsListener.map { it.second }.any { it == pluginListener }) return

        pluginListener.onCreate()

        pluginsListener.add(classLoader to pluginListener)
        eventBus.subscribe(pluginListener)

        executeEvent(PluginListenerAddedEvent(pluginListener))

        log.trace("${pluginListener.javaClass.simpleName} subscribed!")
    }

    fun removePlugin(pluginListener: PluginListener) {
        if (!pluginsListener.map { it.second }.any { it == pluginListener }) return

        pluginListener.onDestroy()

        pluginsListener.removeAll { it.second == pluginListener }
        eventBus.unsubscribe(pluginListener)

        executeEvent(PluginListenerRemovedEvent(pluginListener))

        log.trace("${pluginListener.javaClass.simpleName} unsubscribed!")
    }

    @Suppress("unused")
    fun removePluginJarByName(pluginName: String) {
        if (!pluginsJar.containsKey(pluginName)) return

        pluginsJar.remove(pluginName)?.third?.forEach { removePlugin(it) }
    }

    fun executeEventAsync(eventClass: Any) {
        eventBus.publishAsync(eventClass)
    }

    fun executeEvent(eventClass: Any) {
        eventBus.publish(eventClass)
    }

    private fun loadPluginListenersFromJar(jarFile: File): Pair<ClassLoader, List<PluginListener>> {
        val pluginListeners: MutableList<PluginListener> = ArrayList()

        val urls = arrayOf<URL>(jarFile.toURI().toURL())
        val urlClassLoader = URLClassLoader(urls, ClassLoader.getSystemClassLoader())

        val reflections = Reflections(ConfigurationBuilder().addUrls(URL("file:" + jarFile.path)).addClassLoader(urlClassLoader))
        val eventsClasses = reflections.getSubTypesOf(PluginListener::class.java)

        eventsClasses.map { it.getConstructor() }.mapTo(pluginListeners) { it.newInstance() }

        return urlClassLoader to pluginListeners
    }

    override fun close() {
        log.info("Closing plugin manager...")

        pluginsJar.values.flatMap { it.third }.forEach { removePlugin(it) }
        pluginsJar.clear()

        pluginsListener.map { it.second }.forEach { removePlugin(it) }

        log.info("Closed plugin manager!")
    }
}