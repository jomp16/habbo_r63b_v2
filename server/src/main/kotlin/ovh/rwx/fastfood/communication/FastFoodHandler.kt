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

package ovh.rwx.fastfood.communication

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.fastfood.communication.incoming.FFIncoming
import ovh.rwx.fastfood.communication.outgoing.FFOutgoing
import ovh.rwx.fastfood.game.FastFoodSession
import ovh.rwx.habbo.communication.HabboInvokers
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.communication.RequestInvoker
import ovh.rwx.habbo.communication.ResponseInvoker
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.invoke.WrongMethodTypeException
import java.time.LocalDateTime

class FastFoodHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val fastFoodMessageHandlers: MutableMap<FFIncoming, Pair<RequestInvoker, MethodHandle>> = mutableMapOf()
    private val fastFoodMessageResponses: MutableMap<FFOutgoing, Pair<ResponseInvoker, MethodHandle>> = mutableMapOf()
    private val instances: MutableMap<Class<*>, Any> = mutableMapOf()
    var largestNameSize: Int = 0

    init {
        val lookup = MethodHandles.lookup()

        ClassGraph()
            .enableAllInfo()
            .acceptPackages(javaClass.`package`.name)
            .scan()
            .use { scanResult ->
                annotatedMethods(scanResult, FFHandler::class.java).forEach { method ->
                    val handler = method.getAnnotation(FFHandler::class.java) ?: return@forEach
                    val instance = getInstance(method.declaringClass)
                    val invoker = HabboInvokers.buildRequestInvoker(lookup, instance, method, FastFoodSession::class.java)
                    val handle = lookup.unreflect(method)

                    handler.headers.forEach { incoming ->
                        if (!fastFoodMessageHandlers.containsKey(incoming)) fastFoodMessageHandlers[incoming] = invoker to handle
                    }
                }

                annotatedMethods(scanResult, FFResponse::class.java).forEach { method ->
                    val response = method.getAnnotation(FFResponse::class.java) ?: return@forEach
                    val instance = getInstance(method.declaringClass)
                    val invoker = HabboInvokers.buildResponseInvoker(instance, method)
                    val handle = lookup.unreflect(method)

                    response.headers.forEach { outgoing ->
                        if (!fastFoodMessageResponses.containsKey(outgoing)) fastFoodMessageResponses[outgoing] = invoker to handle
                    }
                }
            }

        largestNameSize = FFIncoming.values().map { it.name }.plus(FFOutgoing.values().map { it.name })
            .maxByOrNull { it.length }!!.length

        log.info("Loaded {} FastFood request handlers", fastFoodMessageHandlers.size)
        log.info("Loaded {} FastFood response handlers", fastFoodMessageResponses.size)
    }

    private fun annotatedMethods(scanResult: ScanResult, annotation: Class<out Annotation>): Sequence<Method> =
        scanResult.getClassesWithMethodAnnotation(annotation.name).asSequence()
            .flatMap { classInfo -> classInfo.methodInfo.asSequence().filter { it.hasAnnotation(annotation) } }
            .map { it.loadClassAndGetMethod() }

    fun handle(fastFoodSession: FastFoodSession, habboRequest: HabboRequest) {
        fastFoodSession.incomingExecutor.execute {
            habboRequest.use {
                val ffIncoming: FFIncoming? = FFIncoming.values().firstOrNull { ffIncoming -> ffIncoming.headerId == habboRequest.headerId }

                if (ffIncoming != null && fastFoodMessageHandlers.containsKey(ffIncoming)) {
                    val (invoker, methodHandle) = fastFoodMessageHandlers[ffIncoming] ?: return@use

                    try {
                        invoker.invoke(fastFoodSession, habboRequest)
                    } catch (e: Exception) {
                        log.error("Error when invoking HabboRequest for headerID: ${habboRequest.headerId} - $ffIncoming!", e)

                        if (e is ClassCastException || e is WrongMethodTypeException || e is ArrayIndexOutOfBoundsException) {
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
            val (invoker, methodHandle) = fastFoodMessageResponses[ffOutgoing] ?: return null
            val habboResponse = HabboResponse(
                headerId = ffOutgoing.headerId,
                outgoing = null,
                habboVersion = HabboVersion(LocalDateTime.now(), 63)
            )

            try {
                invoker.invoke(habboResponse, args)

                return habboResponse
            } catch (e: Exception) {
                log.error("Error when invoking HabboResponse for ${ffOutgoing.headerId} - $ffOutgoing!", e)

                if (e is ClassCastException || e is WrongMethodTypeException || e is ArrayIndexOutOfBoundsException) {
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
