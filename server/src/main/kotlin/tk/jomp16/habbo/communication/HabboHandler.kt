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

import kotlinx.support.jdk7.use
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.WrongMethodTypeException
import java.util.*

class HabboHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val messageHandlers: MutableMap<Int, Pair<Any, MethodHandle>> = HashMap()
    private val messageResponses: MutableMap<Int, Pair<Any, MethodHandle>> = HashMap()
    private val instances: MutableMap<Class<*>, Any> = HashMap()

    val incomingNames: MutableMap<Int, String> = HashMap()
    val outgoingNames: MutableMap<Int, String> = HashMap()

    val largestNameSize: Int

    init {
        val lookup = MethodHandles.lookup()

        val reflections = Reflections(javaClass.classLoader, javaClass.`package`.name, MethodAnnotationsScanner())

        // Load incoming
        Incoming::class.java.declaredFields.forEach {
            if (it.type == Int::class.java) incomingNames.put(it.getInt(null), it.name)
        }

        reflections.getMethodsAnnotatedWith(Handler::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val handler = it.getAnnotation(Handler::class.java)

            val methodHandle = lookup.unreflect(it)

            handler.headerIds.forEach { headerId ->
                if (!messageHandlers.containsKey(headerId)) messageHandlers.put(headerId, Pair(clazz, methodHandle))
            }
        }

        // Load outgoing
        Outgoing::class.java.declaredFields.forEach {
            if (it.type == Int::class.java) outgoingNames.put(it.getInt(null), it.name)
        }

        reflections.getMethodsAnnotatedWith(Response::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val response = it.getAnnotation(Response::class.java)

            val methodHandle = lookup.unreflect(it)

            response.headerIds.forEach { headerId ->
                if (!messageResponses.containsKey(headerId)) messageResponses.put(headerId, Pair(clazz, methodHandle))
            }
        }

        largestNameSize = incomingNames.plus(outgoingNames).values.sortedByDescending { it.length }.first().length

        log.info("Loaded {} Habbo request handlers", messageHandlers.size)
        log.info("Loaded {} Habbo response handlers", messageResponses.size)
    }

    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        HabboServer.serverExecutor.execute {
            habboRequest.use {
                if (messageHandlers.containsKey(it.headerId)) {
                    val (clazz, methodHandle) = messageHandlers[it.headerId] ?: return@use

                    try {
                        methodHandle.invokeWithArguments(clazz, habboSession, it)
                    } catch (e: Exception) {
                        log.error("Error when invoking HabboRequest for headerID: ${habboRequest.headerId} - ${incomingNames[habboRequest.headerId]}!", e)

                        if (e is ClassCastException || e is WrongMethodTypeException) {
                            log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { it.simpleName })
                            log.error("Received parameters: {}", listOf(HabboSession::class.java.simpleName, HabboRequest::class.java))
                        }
                    }
                } else {
                    log.warn("Non existent request header ID: {} - {}", habboRequest.headerId, incomingNames[habboRequest.headerId])
                }
            }
        }
    }

    fun invokeResponse(headerId: Int, vararg args: Any?): HabboResponse? {
        if (messageResponses.containsKey(headerId)) {
            val (clazz, methodHandle) = messageResponses[headerId] ?: return null

            val habboResponse = HabboResponse(headerId)

            try {
                methodHandle.invokeWithArguments(clazz, habboResponse, *args)

                return habboResponse
            } catch (e: Exception) {
                log.error("Error when invoking HabboResponse for $headerId - ${outgoingNames[headerId]}!", e)

                if (e is ClassCastException || e is WrongMethodTypeException) {
                    log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { it.simpleName })
                    log.error("Received parameters: {}", listOf(HabboResponse::class.java.simpleName).plus(args.map { it?.javaClass?.simpleName }))
                }

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

            instances.put(clazz, clazz1)
        } else {
            clazz1 = instances[clazz]!!
        }

        return clazz1
    }
}