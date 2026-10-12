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

package ovh.rwx.habbo.communication

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.release.ReleaseDao
import ovh.rwx.habbo.database.release.ReleaseHeaderInfo
import ovh.rwx.habbo.database.release.ReleaseType
import ovh.rwx.habbo.game.user.HabboSession
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.WrongMethodTypeException
import java.lang.reflect.Method
import kotlin.system.exitProcess

class HabboHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val messageHandlers: MutableMap<Incoming, MutableMap<String, HabboMethodInfo>> = mutableMapOf()
    private val messageResponses: MutableMap<Outgoing, MutableMap<String, RegisteredResponse>> = mutableMapOf()
    private val instances: MutableMap<Class<*>, Any> = mutableMapOf()
    private var incomingHeaders: List<ReleaseHeaderInfo> = emptyList()
    private var outgoingHeaders: List<ReleaseHeaderInfo> = emptyList()

    // R63A
    private val messageHandlersR63A: MutableMap<IncomingR63A, HabboMethodInfo> = mutableMapOf()
    private val messageResponsesR63A: MutableMap<OutgoingR63A, RegisteredResponse> = mutableMapOf()
    private var incomingHeadersR63A: List<ReleaseHeaderInfo> = emptyList()
    private var outgoingHeadersR63A: List<ReleaseHeaderInfo> = emptyList()

    val releases: MutableSet<String> by lazy { HashSet(ReleaseDao.getReleases()) }
    val incomingNames: MutableMap<String, List<Pair<Int, Incoming>>> = mutableMapOf()
    val outgoingNames: MutableMap<String, List<Pair<Int, Outgoing>>> = mutableMapOf()
    val incomingNamesR63A: MutableMap<String, List<Pair<Int, IncomingR63A>>> = mutableMapOf()
    val outgoingNamesR63A: MutableMap<String, List<Pair<Int, OutgoingR63A>>> = mutableMapOf()
    var largestNameSize: Int = 0
    var largestNameSizeR63A: Int = 0

    init {
        load()
    }

    fun load() {
        log.info("Loading handlers...")

        instances.clear()
        messageHandlers.clear()
        messageResponses.clear()
        incomingNames.clear()
        outgoingNames.clear()
        messageHandlersR63A.clear()
        messageResponsesR63A.clear()
        incomingNamesR63A.clear()
        outgoingNamesR63A.clear()

        loadReleaseHeaders()
        loadHeaderNames()
        loadAnnotatedMethods()
        validateOverrideMethods()
    }

    // ---------------------------------------------------------------- load

    private fun loadReleaseHeaders() {
        val headers = ReleaseDao.getHeaders().groupBy { it.type }
        val incoming = headers[ReleaseType.INCOMING] ?: error("Couldn't find the incoming headers!")
        val outgoing = headers[ReleaseType.OUTGOING] ?: error("Couldn't find the outgoing headers!")

        incomingHeaders = incoming.filterNot { it.release == R63A_RELEASE }
        outgoingHeaders = outgoing.filterNot { it.release == R63A_RELEASE }
        incomingHeadersR63A = incoming.filter { it.release == R63A_RELEASE }
        outgoingHeadersR63A = outgoing.filter { it.release == R63A_RELEASE }
    }

    private fun loadHeaderNames() {
        if (incomingNames.isEmpty() && outgoingNames.isEmpty()) {
            releases.forEach { release ->
                incomingNames[release] = mapHeaders<Incoming>(incomingHeaders, release)
                outgoingNames[release] = mapHeaders<Outgoing>(outgoingHeaders, release)
            }

            verifyNoMissingHeaders("incoming", incomingNames, Incoming.entries - Incoming.RELEASE_CHECK)
            verifyNoMissingHeaders("outgoing", outgoingNames, Outgoing.entries.toList())

            largestNameSize = largestNameSizeOf(incomingNames.values.flatten().map { it.second }, outgoingNames.values.flatten().map { it.second })

            log.info("Loaded {} Habbo releases. Available releases: {}", releases.size, releases.sorted().joinToString())
        }

        if (incomingNamesR63A.isEmpty() && outgoingNamesR63A.isEmpty()) {
            incomingNamesR63A[R63A_RELEASE] = mapHeaders<IncomingR63A>(incomingHeadersR63A, null)
            outgoingNamesR63A[R63A_RELEASE] = mapHeaders<OutgoingR63A>(outgoingHeadersR63A, null)

            verifyNoMissingHeaders("incoming", incomingNamesR63A, IncomingR63A.entries - IncomingR63A.RELEASE_CHECK)
            verifyNoMissingHeaders("outgoing", outgoingNamesR63A, OutgoingR63A.entries.toList())

            largestNameSizeR63A = largestNameSizeOf(incomingNamesR63A.values.flatten().map { it.second }, outgoingNamesR63A.values.flatten().map { it.second })
        }
    }

    private fun loadAnnotatedMethods() {
        val lookup = MethodHandles.lookup()

        ClassGraph()
            .enableAllInfo()
            .acceptPackages(javaClass.`package`.name)
            .scan()
            .use { scanResult ->
                runBlocking {
                    val jobHandlers = HabboServer.applicationScope.launch {
                        loadAnnotated(lookup, scanResult, Handler::class.java, { it.headers }, { it.requiredAuth }) { incoming, method, instance, handle, requiredAuth ->
                            messageHandlers.getOrPut(incoming) { mutableMapOf() }[method.name] =
                                HabboMethodInfo(requiredAuth, HabboInvokers.buildRequestInvoker(lookup, instance, method, HabboSession::class.java), handle)
                        }
                        loadAnnotated(lookup, scanResult, HandlerR63A::class.java, { it.headers }, { it.requiredAuth }) { incoming, method, instance, handle, requiredAuth ->
                            messageHandlersR63A[incoming] =
                                HabboMethodInfo(requiredAuth, HabboInvokers.buildRequestInvoker(lookup, instance, method, HabboSession::class.java), handle)
                        }
                        log.info("Loaded {} Habbo request handlers", messageHandlers.size)
                        log.info("Loaded {} Habbo R63A request handlers", messageHandlersR63A.size)
                    }

                    val jobResponses = HabboServer.applicationScope.launch {
                        loadAnnotated(lookup, scanResult, Response::class.java, { it.headers }, { false }) { outgoing, method, instance, handle, _ ->
                            messageResponses.getOrPut(outgoing) { mutableMapOf() }[method.name] =
                                RegisteredResponse(HabboInvokers.buildResponseInvoker(instance, method), handle)
                        }
                        loadAnnotated(lookup, scanResult, ResponseR63A::class.java, { it.headers }, { false }) { outgoing, method, instance, handle, _ ->
                            messageResponsesR63A[outgoing] =
                                RegisteredResponse(HabboInvokers.buildResponseInvoker(instance, method), handle)
                        }
                        log.info("Loaded {} Habbo response handlers", messageResponses.size)
                        log.info("Loaded {} Habbo R63A response handlers", messageResponsesR63A.size)
                    }

                    joinAll(jobHandlers, jobResponses)
                }
            }
    }

    // Carga genérica de métodos anotados (Handler/HandlerR63A/Response/ResponseR63A):
    // anotações não têm ancestral com membros, então os accessors (headers/requiredAuth)
    // entram como lambdas tipados — E infere do retorno de headersOf, sem casts.
    private fun <A : Annotation, E> loadAnnotated(
        lookup: MethodHandles.Lookup,
        scanResult: ScanResult,
        annotationClass: Class<A>,
        headersOf: (A) -> Array<out E>,
        requiredAuthOf: (A) -> Boolean,
        register: (enumValue: E, method: Method, instance: Any, handle: MethodHandle, requiredAuth: Boolean) -> Unit
    ) {
        scanResult.getClassesWithMethodAnnotation(annotationClass.name).forEach { classInfo ->
            classInfo.methodInfo.filter { it.hasAnnotation(annotationClass) }.forEach { methodInfo ->
                val method = methodInfo.loadClassAndGetMethod()
                val annotation = method.getAnnotation(annotationClass) ?: return@forEach
                val instance = getInstance(method.declaringClass)
                val handle = lookup.unreflect(method)
                val requiredAuth = requiredAuthOf(annotation)

                headersOf(annotation).forEach { enumValue -> register(enumValue, method, instance, handle, requiredAuth) }
            }
        }
    }

    // ---------------------------------------------------------------- validação

    private fun validateOverrideMethods() {
        val missingOverrides = validateOverrides("incoming", incomingHeaders, { runCatching { Incoming.valueOf(it) }.getOrNull() }) { messageHandlers[it as Incoming] }
            .plus(validateOverrides("outgoing", outgoingHeaders, { runCatching { Outgoing.valueOf(it) }.getOrNull() }) { messageResponses[it as Outgoing] })

        if (missingOverrides > 0) {
            log.error(
                "Found {} invalid override_method entries configured in releases headers. Fix database or add missing handler/response methods! Exiting!",
                missingOverrides
            )
            exitProcess(1)
        }

        log.info("All configured override_method entries validated successfully!")
    }

    private fun validateOverrides(
        kind: String,
        headers: List<ReleaseHeaderInfo>,
        valueOf: (String) -> Enum<*>?,
        methodsOf: (Enum<*>) -> Map<String, *>?
    ): Int {
        var missing = 0

        headers.forEach { headerInfo ->
            val override = headerInfo.overrideMethod
            if (override.isNullOrEmpty() || override == DISABLED) return@forEach

            val enum = valueOf(headerInfo.name) ?: return@forEach

            val methods = methodsOf(enum)
            if (methods == null || !methods.containsKey(override)) {
                log.error(
                    "Override validation error: Release '{}' specifies override_method='{}' for {} '{}' (header {}), but method does not exist! Registered methods: {}",
                    headerInfo.release,
                    override,
                    kind,
                    headerInfo.name,
                    headerInfo.header,
                    methods?.keys?.joinToString() ?: "none"
                )
                missing++
            }
        }

        return missing
    }

    // ---------------------------------------------------------------- dispatch (request)

    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboRequest.use {
            val incomingEnum: Incoming? =
                if (habboRequest.headerId == RELEASE_CHECK_ID) Incoming.RELEASE_CHECK
                else incomingNames[habboSession.release]?.firstOrNull { it.first == habboRequest.headerId }?.second

            if (incomingEnum == null || !messageHandlers.containsKey(incomingEnum)) {
                warnUnknownRequest(habboRequest.headerId, incomingEnum)
                return@use
            }

            val methodName = overrideMethodFor(habboSession.release, habboRequest.headerId) ?: "handle"
            val methodInfo = messageHandlers[incomingEnum]?.get(methodName)

            if (methodInfo == null) {
                log.warn("No method with name '{}' found for {}!", methodName, incomingEnum)
                return@use
            }

            dispatchRequest(habboSession, habboRequest, methodInfo, incomingEnum) {
                habboRequest.incoming = incomingEnum
                habboRequest.methodName = methodName
            }
        }
    }

    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        habboRequest.use {
            val incomingEnum: IncomingR63A? =
                if (habboRequest.headerId == RELEASE_CHECK_ID) IncomingR63A.RELEASE_CHECK
                else incomingNamesR63A[habboSession.release]?.firstOrNull { it.first == habboRequest.headerId }?.second

            val methodInfo = incomingEnum?.let { messageHandlersR63A[it] }
            if (methodInfo == null) {
                warnUnknownRequest(habboRequest.headerId, incomingEnum)
                return@use
            }

            dispatchRequest(habboSession, habboRequest, methodInfo, incomingEnum) {
                habboRequest.incomingR63A = incomingEnum
                habboRequest.r63ANewEncoding = habboSession.r63ANewEncoding
            }
        }
    }

    private fun overrideMethodFor(release: String, headerId: Int): String? =
        incomingHeaders.firstOrNull { it.header == headerId && it.release == release }?.overrideMethod

    private fun warnUnknownRequest(headerId: Int, incomingEnum: Any?) {
        log.warn("Non existent request header ID: {} - {}", headerId, incomingEnum)
    }

    // Núcleo compartilhado de handle/handleR63A: guard de auth + invoke/catch.
    private fun dispatchRequest(
        habboSession: HabboSession,
        habboRequest: HabboRequest,
        methodInfo: HabboMethodInfo,
        logLabel: Any?,
        prepare: () -> Unit
    ) {
        if (methodInfo.requiredAuth && !habboSession.authenticated) {
            log.error("${habboRequest.headerId} - $logLabel - method requires authenticated user, but user wasn't authenticated!")
            return
        }

        try {
            prepare()
            methodInfo.invoker.invoke(habboSession, habboRequest)
        } catch (e: Exception) {
            log.error("Error when invoking HabboRequest for headerID: ${habboRequest.headerId} - $logLabel!", e)

            if (e is ClassCastException || e is WrongMethodTypeException || e is ArrayIndexOutOfBoundsException) {
                logParamMismatch(
                    methodInfo.handle,
                    listOf(HabboSession::class.java.simpleName, HabboRequest::class.java.simpleName)
                )
            }
        }
    }

    // ---------------------------------------------------------------- dispatch (response)

    fun invokeResponse(habboSession: HabboSession, outgoing: Outgoing, vararg args: Any?): HabboResponse? {
        val headerId = outgoingNames[habboSession.release]?.firstOrNull { it.second == outgoing }?.first
        val headerInfo = outgoingHeaders.firstOrNull { it.release == habboSession.release && it.name == outgoing.name }

        return dispatchResponse(
            habboSession,
            headerId,
            headerInfo?.overrideMethod,
            messageResponses[outgoing]?.get(headerInfo?.overrideMethod ?: DEFAULT_METHOD),
            outgoing,
            { id -> HabboResponse(headerId = id, outgoing = outgoing, habboVersion = habboSession.habboVersion) },
            args
        )
    }

    fun invokeResponse(habboSession: HabboSession, outgoing: OutgoingR63A, vararg args: Any?): HabboResponse? {
        val headerId = outgoingNamesR63A[habboSession.release]?.firstOrNull { it.second == outgoing }?.first
        val overrideMethod = outgoingHeadersR63A.firstOrNull { it.release == habboSession.release && it.name == outgoing.name }?.overrideMethod

        return dispatchResponse(
            habboSession,
            headerId,
            overrideMethod,
            messageResponsesR63A[outgoing],
            outgoing,
            { id ->
                HabboResponse(
                    headerId = id,
                    outgoing = null,
                    outgoingR63A = outgoing,
                    r63ANewEncoding = habboSession.r63ANewEncoding,
                    habboVersion = habboSession.habboVersion
                )
            },
            args
        )
    }

    // Núcleo compartilhado dos dois invokeResponse: guard NULL/DISABLED + invoke/catch.
    // headerId < 0 = linha NULL na tabela da release (o pacote não existe no cliente
    // dessa era). Enviar isso no wire corrompe o stream (writeShort(-1) = 0xFFFF e o
    // cliente derruba a conexão) — dropa em vez de serializar.
    private fun dispatchResponse(
        habboSession: HabboSession,
        headerId: Int?,
        overrideMethod: String?,
        registered: RegisteredResponse?,
        logLabel: Any,
        buildResponse: (Int) -> HabboResponse,
        args: Array<out Any?>
    ): HabboResponse? {
        if (headerId == null || headerId < 0) {
            // DISABLED = ausência intencional (feature não existe na era) — dropa sem erro
            if (overrideMethod != DISABLED) {
                log.error("Response header for {} is NULL in release {} — dropping", logLabel, habboSession.release)
            }
            return null
        }

        if (registered == null) {
            if (overrideMethod != DISABLED) {
                log.warn("No response method found for {} (override '{}')!", logLabel, overrideMethod ?: DEFAULT_METHOD)
            }
            return null
        }

        val habboResponse = buildResponse(headerId)

        return try {
            registered.invoker.invoke(habboResponse, args)
            habboResponse
        } catch (e: Exception) {
            log.error("Error when invoking HabboResponse for $headerId - $logLabel!", e)

            if (e is ClassCastException || e is WrongMethodTypeException || e is ArrayIndexOutOfBoundsException) {
                logParamMismatch(
                    registered.handle,
                    listOf(HabboResponse::class.java.simpleName) + args.map { it?.javaClass?.simpleName }
                )
            }
            habboResponse.close()
            null
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun logParamMismatch(methodHandle: MethodHandle, received: List<String?>) {
        log.error("Excepted parameters: {}", methodHandle.type().parameterList().drop(1).map { it.simpleName })
        log.error("Received parameters: {}", received)
    }

    private inline fun <reified E : Enum<E>> mapHeaders(
        headers: List<ReleaseHeaderInfo>,
        release: String?
    ): List<Pair<Int, E>> =
        headers.filter { (release == null || it.release == release) && enumValues<E>().any { e -> e.name == it.name } }
            .map { it.header to enumValueOf<E>(it.name) }

    private fun <E : Enum<E>> verifyNoMissingHeaders(
        kind: String,
        names: Map<String, List<Pair<Int, E>>>,
        expected: Collection<E>
    ) {
        var missing = false

        names.forEach { (release, pairs) ->
            val missingHeaders = expected - pairs.map { it.second }.toSet()

            if (missingHeaders.isNotEmpty()) {
                log.error("Missing {} headers for release={}, headers={}", kind, release, missingHeaders.joinToString())
                missing = true
            }
        }

        if (missing) {
            log.error("Missing headers... Fix it! Exiting!")
            exitProcess(1)
        }
    }

    private fun largestNameSizeOf(vararg enumLists: Collection<Enum<*>>): Int =
        enumLists.asSequence()
            .flatten()
            .maxOfOrNull { it.name.length } ?: 0

    private fun getInstance(clazz: Class<*>): Any =
        instances.getOrPut(clazz) { clazz.getDeclaredConstructor().newInstance() }

    fun getOverrideMethodForHeader(outgoing: Outgoing, release: String): String =
        outgoingHeaders.firstOrNull { it.release == release && it.name == outgoing.name }?.overrideMethod
            ?: DEFAULT_METHOD

    private companion object {
        const val DISABLED = "DISABLED"
        const val DEFAULT_METHOD = "response"
        const val R63A_RELEASE = "R63A"
        const val RELEASE_CHECK_ID = 4000
    }
}
