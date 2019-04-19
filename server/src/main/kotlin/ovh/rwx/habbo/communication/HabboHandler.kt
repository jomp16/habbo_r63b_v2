/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.release.ReleaseDao
import ovh.rwx.habbo.database.release.ReleaseHeaderInfo
import ovh.rwx.habbo.database.release.ReleaseType
import ovh.rwx.habbo.game.user.HabboSession
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.WrongMethodTypeException
import kotlin.collections.MutableMap.MutableEntry

class HabboHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val messageHandlers: MutableMap<Incoming, MutableMap<String, Pair<Any, MethodHandle>>> = mutableMapOf()
    private val messageResponses: MutableMap<Outgoing, MutableMap<String, Pair<Any, MethodHandle>>> = mutableMapOf()
    private val instances: MutableMap<Class<*>, Any> = mutableMapOf()
    private var incomingHeaders: List<ReleaseHeaderInfo> = emptyList()
    private var outgoingHeaders: List<ReleaseHeaderInfo> = emptyList()

    val releases: MutableSet<String> by lazy { HashSet(ReleaseDao.getReleases()) }
    val incomingNames: MutableMap<String, List<Pair<Int, Incoming>>> = mutableMapOf()
    val outgoingNames: MutableMap<String, List<Pair<Int, Outgoing>>> = mutableMapOf()
    var largestNameSize: Int = 0

    init {
        load()
    }

    fun load() {
        log.info("Loading handlers...")

        instances.clear()
        messageHandlers.clear()
        messageResponses.clear()

        val headers = ReleaseDao.getHeaders().groupBy { it.type }

        incomingHeaders = headers[ReleaseType.INCOMING] ?: error("Couldn't find the incoming headers!")
        outgoingHeaders = headers[ReleaseType.OUTGOING] ?: error("Couldn't find the outgoing headers!")

        if (incomingNames.isEmpty() && outgoingNames.isEmpty()) {
            releases.forEach { release ->
                val inHeaders = incomingHeaders.filter { it.release == release }.filter { Incoming.values().map { incoming -> incoming.name }.contains(it.name) }
                val outHeaders = outgoingHeaders.filter { it.release == release }.filter { Outgoing.values().map { outgoing -> outgoing.name }.contains(it.name) }

                incomingNames[release] = inHeaders.map { it.header to Incoming.valueOf(it.name) }
                outgoingNames[release] = outHeaders.map { it.header to Outgoing.valueOf(it.name) }
            }
            val exceptedIncomingHeaders: List<Incoming> = Incoming.values().toMutableList().minus(Incoming.RELEASE_CHECK)
            val exceptedOutgoingHeaders: List<Outgoing> = Outgoing.values().toMutableList()

            if (isMissingIncomingHeaders(incomingNames.entries, exceptedIncomingHeaders) || isMissingOutgoingHeaders(outgoingNames.entries, exceptedOutgoingHeaders)) {
                log.error("Missing headers... Fix it! Exiting!")

                System.exit(1)
            }

            largestNameSize = incomingNames.plus(outgoingNames).values.flatMap { it.map { pair -> pair.second } }.map { it.name }.sortedByDescending { it.length }.first().length

            log.info("Loaded {} Habbo releases. Available releases: {}", releases.size, releases.sorted().joinToString())
        }

        val lookup = MethodHandles.lookup()
        val reflections = Reflections(javaClass.classLoader, javaClass.`package`.name, MethodAnnotationsScanner())

        GlobalScope.launch(HabboServer.cachedExecutorDispatcher) {
            reflections.getMethodsAnnotatedWith(Handler::class.java).forEach {
                val clazz = getInstance(it.declaringClass)
                val handler = it.getAnnotation(Handler::class.java)
                val methodHandle = lookup.unreflect(it)
                val methodName = it.name

                handler.headers.forEach { incoming ->
                    if (!messageHandlers.containsKey(incoming)) messageHandlers[incoming] = mutableMapOf()

                    messageHandlers[incoming]!![methodName] = Pair(clazz, methodHandle)
                }
            }
            log.info("Loaded {} Habbo request handlers", messageHandlers.size)
        }

        GlobalScope.launch(HabboServer.cachedExecutorDispatcher) {
            reflections.getMethodsAnnotatedWith(Response::class.java).forEach {
                val clazz = getInstance(it.declaringClass)
                val response = it.getAnnotation(Response::class.java)
                val methodHandle = lookup.unreflect(it)
                val methodName = it.name

                response.headers.forEach { outgoing ->
                    if (!messageResponses.containsKey(outgoing)) messageResponses[outgoing] = mutableMapOf()

                    messageResponses[outgoing]!![methodName] = Pair(clazz, methodHandle)
                }
            }
            log.info("Loaded {} Habbo response handlers", messageResponses.size)
        }
    }

    private fun isMissingIncomingHeaders(availableHeadersEntries: MutableSet<MutableEntry<String, List<Pair<Int, Incoming>>>>, exceptedHeaders: List<Incoming>): Boolean {
        var missing = false

        availableHeadersEntries.forEach {
            val missingHeaders = exceptedHeaders.minus(it.value.map { pair -> pair.second })

            if (missingHeaders.isNotEmpty()) {
                log.error("Missing incoming headers for release={}, headers={}", it.key, missingHeaders.joinToString())

                if (!missing) missing = true
            }
        }

        return missing
    }

    private fun isMissingOutgoingHeaders(availableHeadersEntries: MutableSet<MutableEntry<String, List<Pair<Int, Outgoing>>>>, exceptedHeaders: List<Outgoing>): Boolean {
        var missing = false

        availableHeadersEntries.forEach {
            val missingHeaders = exceptedHeaders.minus(it.value.map { pair -> pair.second })

            if (missingHeaders.isNotEmpty()) {
                log.error("Missing outgoing headers for release={}, headers={}", it.key, missingHeaders.joinToString())

                if (!missing) missing = true
            }
        }

        return missing
    }

    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboRequest.use {
            val incomingEnum: Incoming? =
                    if (habboRequest.headerId == 4000) Incoming.RELEASE_CHECK
                    else incomingNames[habboSession.release]?.find { pair -> pair.first == habboRequest.headerId }?.second

            if (incomingEnum != null && messageHandlers.containsKey(incomingEnum)) {
                val methodName = incomingHeaders.find { releaseHeaderInfo -> releaseHeaderInfo.header == habboRequest.headerId && (releaseHeaderInfo.release == habboSession.release) }?.overrideMethod
                        ?: "handle"
                val pair = messageHandlers[incomingEnum]!![methodName]

                if (pair == null) {
                    log.warn("No method with name '{}' found for {}!", methodName, incomingEnum)

                    return@use
                }

                val (clazz, methodHandle) = pair

                try {
                    habboRequest.incoming = incomingEnum
                    habboRequest.methodName = methodName

                    methodHandle.invokeWithArguments(clazz, habboSession, habboRequest)
                } catch (e: Exception) {
                    log.error("Error when invoking HabboRequest for headerID: ${habboRequest.headerId} - $incomingEnum!", e)

                    if (e is ClassCastException || e is WrongMethodTypeException) {
                        log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { clazz1 -> clazz1.simpleName })
                        log.error("Received parameters: {}", listOf(HabboSession::class.java.simpleName, HabboRequest::class.java))
                    }
                }
            } else {
                log.warn("Non existent request header ID: {} - {}", habboRequest.headerId, incomingEnum)
            }
        }
    }

    fun invokeResponse(habboSession: HabboSession, outgoing: Outgoing, vararg args: Any?): HabboResponse? {
        val headerId = outgoingNames[habboSession.release]?.find { it.second == outgoing }?.first

        if (headerId == null) {
            log.error("Non existent response header {} for release {}", outgoing, habboSession.release)

            return null
        }

        if (messageResponses.containsKey(outgoing)) {
            val methodName = outgoingHeaders.find { it.header == headerId && (it.release == habboSession.release) }?.overrideMethod
                    ?: "response"

            val pair = messageResponses[outgoing]!![methodName]

            if (pair == null) {
                if (methodName != "DISABLED") {
                    log.warn("No method with name '{}' found for {}!", methodName, outgoing)
                }

                return null
            }

            val (clazz, methodHandle) = pair

            val habboResponse = HabboResponse(headerId, outgoing)

            try {
                methodHandle.invokeWithArguments(clazz, habboResponse, *args)

                return habboResponse
            } catch (e: Exception) {
                log.error("Error when invoking HabboResponse for $headerId - $outgoing!", e)

                if (e is ClassCastException || e is WrongMethodTypeException) {
                    log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { it.simpleName })
                    log.error("Received parameters: {}", listOf(HabboResponse::class.java.simpleName).plus(args.map { it?.javaClass?.simpleName }))
                }
                // Close the Habbo Response
                habboResponse.close()
            }
        } else {
            log.error("Non existent response header ID: {} - {}", headerId, outgoing)
        }

        return null
    }

    private fun getInstance(clazz: Class<*>): Any {
        val clazz1: Any

        if (!instances.containsKey(clazz)) {
            clazz1 = clazz.getDeclaredConstructor().newInstance()

            instances[clazz] = clazz1
        } else {
            clazz1 = instances[clazz]!!
        }

        return clazz1
    }

    fun getOverrideMethodForHeader(outgoing: Outgoing, release: String): String {
        return HabboServer.habboHandler.outgoingHeaders.find { it.release == release && it.name == outgoing.name }?.overrideMethod
                ?: "response"
    }
}