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

package tk.jomp16.habbo.communication

import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.release.ReleaseDao
import tk.jomp16.habbo.game.user.HabboSession
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.WrongMethodTypeException
import java.util.*
import kotlin.collections.MutableMap.MutableEntry

class HabboHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val messageHandlers: MutableMap<Incoming, Pair<Any, MethodHandle>> = mutableMapOf()
    private val messageResponses: MutableMap<Outgoing, Pair<Any, MethodHandle>> = mutableMapOf()
    private val instances: MutableMap<Class<*>, Any> = mutableMapOf()
    val releases: MutableSet<String> = HashSet()
    val incomingNames: MutableMap<String, List<Pair<Int, Incoming>>> = mutableMapOf()
    val outgoingNames: MutableMap<String, List<Pair<Int, Outgoing>>> = mutableMapOf()
    var largestNameSize: Int = 0

    init {
        releases.addAll(ReleaseDao.getReleases())
        val incomingHeaders = ReleaseDao.getIncomingHeaders()
        val outgoingHeaders = ReleaseDao.getOutgoingHeaders()

        releases.forEach { release ->
            val inHeaders = incomingHeaders.filter { it.release == release }
            val outHeaders = outgoingHeaders.filter { it.release == release }

            incomingNames.put(release, inHeaders.map { it.header to Incoming.valueOf(it.name) })
            outgoingNames.put(release, outHeaders.map { it.header to Outgoing.valueOf(it.name) })
        }
        val exceptedIncomingHeaders: List<Incoming> = Incoming.values().toMutableList().minus(Incoming.RELEASE_CHECK)
        val exceptedOutgoingHeaders: List<Outgoing> = Outgoing.values().toMutableList()

        if (isMissingIncomingHeaders(incomingNames.entries, exceptedIncomingHeaders) || isMissingOutgoingHeaders(outgoingNames.entries, exceptedOutgoingHeaders)) {
            log.error("Missing headers... Fix it! Exiting!")

            System.exit(1)
        }
        val lookup = MethodHandles.lookup()
        val reflections = Reflections(javaClass.classLoader, javaClass.`package`.name, MethodAnnotationsScanner())

        reflections.getMethodsAnnotatedWith(Handler::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val handler = it.getAnnotation(Handler::class.java)
            val methodHandle = lookup.unreflect(it)

            handler.headers.forEach { incoming ->
                if (!messageHandlers.containsKey(incoming)) messageHandlers.put(incoming, Pair(clazz, methodHandle))
            }
        }

        reflections.getMethodsAnnotatedWith(Response::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val response = it.getAnnotation(Response::class.java)
            val methodHandle = lookup.unreflect(it)

            response.headers.forEach { outgoing ->
                if (!messageResponses.containsKey(outgoing)) messageResponses.put(outgoing, Pair(clazz, methodHandle))
            }
        }

        largestNameSize = incomingNames.plus(outgoingNames).values.flatMap { it.map { it.second } }.map { it.name }.sortedByDescending { it.length }.first().length

        log.info("Loaded {} Habbo releases. Available releases: {}", releases.size, releases.sorted().joinToString())
        log.info("Loaded {} Habbo request handlers", messageHandlers.size)
        log.info("Loaded {} Habbo response handlers", messageResponses.size)
    }

    private fun isMissingIncomingHeaders(availableHeadersEntries: MutableSet<MutableEntry<String, List<Pair<Int, Incoming>>>>, exceptedHeaders: List<Incoming>): Boolean {
        var missing = false

        availableHeadersEntries.forEach {
            val missingHeaders = exceptedHeaders.minus(it.value.map { it.second })

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
            val missingHeaders = exceptedHeaders.minus(it.value.map { it.second })

            if (missingHeaders.isNotEmpty()) {
                log.error("Missing outgoing headers for release={}, headers={}", it.key, missingHeaders.joinToString())

                if (!missing) missing = true
            }
        }

        return missing
    }

    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboSession.incomingExecutor.execute {
            habboRequest.use {
                val incomingEnum: Incoming? =
                        if (habboRequest.headerId == 4000) Incoming.RELEASE_CHECK
                        else incomingNames[habboSession.release]?.find { it.first == habboRequest.headerId }?.second

                if (incomingEnum != null && messageHandlers.containsKey(incomingEnum)) {
                    val (clazz, methodHandle) = messageHandlers[incomingEnum] ?: return@use

                    try {
                        habboRequest.incoming = incomingEnum

                        methodHandle.invokeExact(clazz, habboSession, habboRequest)
                    } catch (e: Exception) {
                        log.error("Error when invoking HabboRequest for headerID: ${habboRequest.headerId} - $incomingEnum!", e)

                        if (e is ClassCastException || e is WrongMethodTypeException) {
                            log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { it.simpleName })
                            log.error("Received parameters: {}", listOf(HabboSession::class.java.simpleName, HabboRequest::class.java))
                        }
                    }
                } else {
                    log.warn("Non existent request header ID: {} - {}", habboRequest.headerId, incomingEnum)
                }
            }
        }
    }

    fun invokeResponse(habboSession: HabboSession, outgoing: Outgoing, vararg args: Any?): HabboResponse? {
        val headerId = outgoingNames[habboSession.release]?.find { it.second == outgoing }?.first ?: return null

        if (messageResponses.containsKey(outgoing)) {
            val (clazz, methodHandle) = messageResponses[outgoing] ?: return null
            val habboResponse = HabboResponse(headerId)

            try {
                methodHandle.invokeExact(clazz, habboResponse, *args)

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

            instances.put(clazz, clazz1)
        } else {
            clazz1 = instances[clazz]!!
        }

        return clazz1
    }
}