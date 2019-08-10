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

package ovh.rwx.fastfood.communication

import org.reflections8.Reflections
import org.reflections8.scanners.MethodAnnotationsScanner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.communication.incoming.FFIncoming
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.WrongMethodTypeException

class FastFoodHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val fastFoodMessageHandlers: MutableMap<FFIncoming, Pair<Any, MethodHandle>> = mutableMapOf()
    private val fastFoodMessageResponses: MutableMap<FFOutgoing, Pair<Any, MethodHandle>> = mutableMapOf()
    private val instances: MutableMap<Class<*>, Any> = mutableMapOf()
    var largestNameSize: Int = 0

    init {
        val lookup = MethodHandles.lookup()
        val reflections = Reflections(javaClass.classLoader, javaClass.`package`.name, MethodAnnotationsScanner())

        reflections.getMethodsAnnotatedWith(FFHandler::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val handler = it.getAnnotation(FFHandler::class.java)
            val methodHandle = lookup.unreflect(it)

            handler.headers.forEach { incoming ->
                if (!fastFoodMessageHandlers.containsKey(incoming)) fastFoodMessageHandlers[incoming] = Pair(clazz, methodHandle)
            }
        }

        reflections.getMethodsAnnotatedWith(FFResponse::class.java).forEach {
            val clazz = getInstance(it.declaringClass)
            val response = it.getAnnotation(FFResponse::class.java)
            val methodHandle = lookup.unreflect(it)

            response.headers.forEach { outgoing ->
                if (!fastFoodMessageResponses.containsKey(outgoing)) fastFoodMessageResponses[outgoing] = Pair(clazz, methodHandle)
            }
        }

        largestNameSize = FFIncoming.values().map { it.name }.plus(FFOutgoing.values().map { it.name }).sortedByDescending { it.length }.first().length

        log.info("Loaded {} FastFood request handlers", fastFoodMessageHandlers.size)
        log.info("Loaded {} FastFood response handlers", fastFoodMessageResponses.size)
    }

    fun handle(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        fastFoodSession.incomingExecutor.execute {
            habboRequest.use {
                val ffIncoming: FFIncoming? = FFIncoming.values().firstOrNull { ffIncoming -> ffIncoming.headerId == habboRequest.headerId }

                if (ffIncoming != null && fastFoodMessageHandlers.containsKey(ffIncoming)) {
                    val (clazz, methodHandle) = fastFoodMessageHandlers[ffIncoming] ?: return@use

                    try {
                        methodHandle.invokeWithArguments(clazz, fastFoodSession, habboRequest)
                    } catch (e: Exception) {
                        log.error("Error when invoking HabboRequest for headerID: ${habboRequest.headerId} - $ffIncoming!", e)

                        if (e is ClassCastException || e is WrongMethodTypeException) {
                            log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { clazz1 -> clazz1.simpleName })
                            log.error("Received parameters: {}", listOf(FastFoodSession::class.java.simpleName, HabboRequest::class.java))
                        }
                    }
                } else {
                    log.warn("Non existent request header ID: {} - {}", habboRequest.headerId, ffIncoming)
                }
            }
        }
    }

    fun invokeResponse(ffOutgoing: FFOutgoing, vararg args: Any?): HabboResponse? {
        if (fastFoodMessageResponses.containsKey(ffOutgoing)) {
            val (clazz, methodHandle) = fastFoodMessageResponses[ffOutgoing] ?: return null
            val habboResponse = HabboResponse(ffOutgoing.headerId, null)

            try {
                methodHandle.invokeWithArguments(clazz, habboResponse, *args)

                return habboResponse
            } catch (e: Exception) {
                log.error("Error when invoking HabboResponse for ${ffOutgoing.headerId} - $ffOutgoing!", e)

                if (e is ClassCastException || e is WrongMethodTypeException) {
                    log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { it.simpleName })
                    log.error("Received parameters: {}", listOf(HabboResponse::class.java.simpleName).plus(args.map { it?.javaClass?.simpleName }))
                }
                // Close the Habbo Response
                habboResponse.close()
            }
        } else {
            log.error("Non existent response header ID: {} - {}", ffOutgoing.headerId, ffOutgoing)
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
}