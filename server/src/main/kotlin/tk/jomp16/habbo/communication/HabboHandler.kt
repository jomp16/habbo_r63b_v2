/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.communication

import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.*

class HabboHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val messageHandlers: MutableMap<Int, Pair<Any, MethodHandle>> = HashMap()
    private val messageResponses: MutableMap<Int, Pair<Any, MethodHandle>> = HashMap()
    private val instances: MutableMap<Class<*>, Any> = HashMap()

    val blacklistIds: IntArray = intArrayOf(Incoming.CLIENT_DEBUG,
                                            Incoming.CLIENT_VARIABLES,
                                            Incoming.EVENT_TRACKER,
                                            Incoming.LATENCY_TEST,
                                            Incoming.SET_USERNAME,
                                            Incoming.MESSENGER_FRIENDS_UPDATE,
                                            1686,
                                            3784,
                                            1307,
                                            1205,
                                            1502,
                                            3065)

    val incomingNames: MutableMap<Int, String> = HashMap()
    val outgoingNames: MutableMap<Int, String> = HashMap()

    val largestNameSize: Int

    init {
        val lookup = MethodHandles.lookup()

        val reflections = Reflections(javaClass.classLoader, javaClass.`package`.name, MethodAnnotationsScanner())

        // Load incoming
        Incoming::class.java.declaredFields.forEach {
            if (it.type == Int::class.java) incomingNames += it.getInt(null) to it.name
        }

        reflections.getMethodsAnnotatedWith(Handler::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val handler = it.getAnnotation(Handler::class.java)

            val methodHandle = lookup.unreflect(it)

            if (!messageHandlers.containsKey(handler.headerId)) messageHandlers += handler.headerId to Pair(clazz,
                                                                                                            methodHandle)
        }

        // Load outgoing
        Outgoing::class.java.declaredFields.forEach {
            if (it.type == Int::class.java) outgoingNames += it.getInt(null) to it.name
        }

        reflections.getMethodsAnnotatedWith(Response::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val response = it.getAnnotation(Response::class.java)

            val methodHandle = lookup.unreflect(it)

            if (!messageResponses.containsKey(response.headerId)) messageResponses += response.headerId to Pair(clazz,
                                                                                                                methodHandle)
        }

        largestNameSize = incomingNames.plus(outgoingNames).values.sortedByDescending { it.length }.first().length

        log.info("Loaded {} Habbo request handlers", messageHandlers.size)
        log.info("Loaded {} Habbo response handlers", messageResponses.size)
    }

    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboRequest.use {
            if (messageHandlers.containsKey(it.headerId)) {
                try {
                    val (clazz, methodHandle) = messageHandlers[it.headerId] ?: return@use

                    methodHandle.invokeWithArguments(clazz, habboSession, it)
                } catch (e: Exception) {
                    log.error("Error when invoking HabboRequest for headerID: {} - {}.", habboRequest.headerId,
                              incomingNames[habboRequest.headerId])
                    log.error("", e)
                }
            } else if (!blacklistIds.contains(habboRequest.headerId)) {
                log.warn("Non existent request header ID: {} - {}", habboRequest.headerId,
                         incomingNames[habboRequest.headerId])
            }
        }
    }

    fun invokeResponse(headerId: Int, vararg args: Any): HabboResponse? {
        if (messageResponses.containsKey(headerId)) {
            val (clazz, methodHandle) = messageResponses[headerId] ?: return null

            val habboResponse = HabboResponse(headerId)

            try {
                methodHandle.invokeWithArguments(clazz, habboResponse, *args)

                return habboResponse
            } catch (e: Exception) {
                log.error("Error when invoking HabboResponse for {} - {}!", headerId, outgoingNames[headerId])
                log.error("", e)

                // Close the Habbo Response
                habboResponse.close()
            }
        } else {
            log.error("Non existent response header ID: {} - {}", headerId, outgoingNames[headerId])
        }

        return null
    }

    private fun getInstance(clazz: Class<*>): Any {
        val clazz1: Any

        if (!instances.containsKey(clazz)) {
            clazz1 = clazz.newInstance()

            instances += clazz to clazz1
        } else {
            clazz1 = instances[clazz]!!
        }

        return clazz1
    }
}