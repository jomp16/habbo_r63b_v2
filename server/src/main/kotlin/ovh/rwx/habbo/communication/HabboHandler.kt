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
import ovh.rwx.habbo.metrics.ServerMetrics
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

/** Falha de validação ao recarregar os handlers: o estado atual é mantido. */
class HandlerReloadException(message: String) : RuntimeException(message)

class HabboHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val slowLog: Logger = LoggerFactory.getLogger("ovh.rwx.habbo.room.slowtick")

    /**
     * Estado completo e imutável dos handlers/headers. Construído por inteiro, validado e só então
     * publicado trocando a referência [snapshot]: as threads do netty enxergam sempre o estado antigo
     * completo ou o novo completo, nunca um mapa limpo/parcial.
     */
    class Snapshot(
        val messageHandlers: Map<Incoming, Map<String, HabboMethodInfo>>,
        val messageResponses: Map<Outgoing, Map<String, RegisteredResponse>>,
        val incomingHeaders: List<ReleaseHeaderInfo>,
        val outgoingHeaders: List<ReleaseHeaderInfo>,
        val messageHandlersR63A: Map<IncomingR63A, HabboMethodInfo>,
        val messageResponsesR63A: Map<OutgoingR63A, RegisteredResponse>,
        val incomingHeadersR63A: List<ReleaseHeaderInfo>,
        val outgoingHeadersR63A: List<ReleaseHeaderInfo>,
        val releases: Set<String>,
        val incomingNames: Map<String, List<Pair<Int, Incoming>>>,
        val outgoingNames: Map<String, List<Pair<Int, Outgoing>>>,
        val incomingNamesR63A: Map<String, List<Pair<Int, IncomingR63A>>>,
        val outgoingNamesR63A: Map<String, List<Pair<Int, OutgoingR63A>>>,
        val largestNameSize: Int,
        val largestNameSizeR63A: Int,
    ) {
        val outgoingHeadersByReleaseAndName: Map<Pair<String, String>, ReleaseHeaderInfo> =
            outgoingHeaders.associateBy { it.release to it.name }
        val outgoingHeadersR63AByReleaseAndName: Map<Pair<String, String>, ReleaseHeaderInfo> =
            outgoingHeadersR63A.associateBy { it.release to it.name }
        val incomingHeadersByReleaseAndHeader: Map<Pair<String, Int>, ReleaseHeaderInfo> =
            incomingHeaders.associateBy { it.release to it.header }
        val incomingHeadersR63AByReleaseAndHeader: Map<Pair<String, Int>, ReleaseHeaderInfo> =
            incomingHeadersR63A.associateBy { it.release to it.header }
        val incomingEnumByReleaseAndHeader: Map<Pair<String, Int>, Incoming> =
            incomingNames.flatMap { (rel, list) -> list.map { (headerId, enumVal) -> (rel to headerId) to enumVal } }.toMap()
        val incomingEnumR63AByReleaseAndHeader: Map<Pair<String, Int>, IncomingR63A> =
            incomingNamesR63A.flatMap { (rel, list) -> list.map { (headerId, enumVal) -> (rel to headerId) to enumVal } }.toMap()
    }

    @Volatile
    private lateinit var snapshot: Snapshot

    // Leitores externos (netty encoder/decoder, handlers) continuam lendo por estas propriedades
    val releases: Set<String> get() = snapshot.releases
    val incomingNames: Map<String, List<Pair<Int, Incoming>>> get() = snapshot.incomingNames
    val outgoingNames: Map<String, List<Pair<Int, Outgoing>>> get() = snapshot.outgoingNames
    val incomingNamesR63A: Map<String, List<Pair<Int, IncomingR63A>>> get() = snapshot.incomingNamesR63A
    val outgoingNamesR63A: Map<String, List<Pair<Int, OutgoingR63A>>> get() = snapshot.outgoingNamesR63A
    val largestNameSize: Int get() = snapshot.largestNameSize
    val largestNameSizeR63A: Int get() = snapshot.largestNameSizeR63A

    init {
        // Boot: headers faltando/overrides inválidos continuam encerrando o processo
        try {
            load()
        } catch (e: HandlerReloadException) {
            log.error("{} Exiting!", e.message)
            exitProcess(1)
        }
    }

    /**
     * (Re)carrega handlers e headers. Constrói tudo localmente, valida e só então publica de forma
     * atômica. Se a validação falhar lança [HandlerReloadException] e o estado atual é mantido.
     */
    @Synchronized
    fun load() {
        log.info("Loading handlers...")

        val candidate = buildSnapshot()
        val errors = HandlerValidation.validate(candidate)

        if (errors.isNotEmpty()) {
            errors.forEach { log.error(it) }

            throw HandlerReloadException("Validação dos handlers falhou, mantendo o estado atual: ${errors.joinToString("; ")}")
        }

        snapshot = candidate

        log.info("Loaded {} Habbo releases. Available releases: {}", candidate.releases.size, candidate.releases.sorted().joinToString())
        log.info("All configured override_method entries validated successfully!")
    }

    // ---------------------------------------------------------------- load

    private fun buildSnapshot(): Snapshot {
        val headers = ReleaseDao.getHeaders().groupBy { it.type }
        val incoming = headers[ReleaseType.INCOMING] ?: error("Couldn't find the incoming headers!")
        val outgoing = headers[ReleaseType.OUTGOING] ?: error("Couldn't find the outgoing headers!")

        val incomingHeaders = incoming.filterNot { it.release == R63A_RELEASE }
        val outgoingHeaders = outgoing.filterNot { it.release == R63A_RELEASE }
        val incomingHeadersR63A = incoming.filter { it.release == R63A_RELEASE }
        val outgoingHeadersR63A = outgoing.filter { it.release == R63A_RELEASE }

        val releases: Set<String> = HashSet(ReleaseDao.getReleases())

        val incomingNames = HashMap<String, List<Pair<Int, Incoming>>>()
        val outgoingNames = HashMap<String, List<Pair<Int, Outgoing>>>()
        releases.forEach { release ->
            incomingNames[release] = mapHeaders<Incoming>(incomingHeaders, release)
            outgoingNames[release] = mapHeaders<Outgoing>(outgoingHeaders, release)
        }

        val incomingNamesR63A = HashMap<String, List<Pair<Int, IncomingR63A>>>()
        val outgoingNamesR63A = HashMap<String, List<Pair<Int, OutgoingR63A>>>()
        incomingNamesR63A[R63A_RELEASE] = mapHeaders<IncomingR63A>(incomingHeadersR63A, null)
        outgoingNamesR63A[R63A_RELEASE] = mapHeaders<OutgoingR63A>(outgoingHeadersR63A, null)

        val handlers = HashMap<Incoming, MutableMap<String, HabboMethodInfo>>()
        val responses = HashMap<Outgoing, MutableMap<String, RegisteredResponse>>()
        val handlersR63A = HashMap<IncomingR63A, HabboMethodInfo>()
        val responsesR63A = HashMap<OutgoingR63A, RegisteredResponse>()

        loadAnnotatedMethods(handlers, responses, handlersR63A, responsesR63A)

        return Snapshot(
            handlers, responses, incomingHeaders, outgoingHeaders, handlersR63A, responsesR63A,
            incomingHeadersR63A, outgoingHeadersR63A, releases,
            incomingNames, outgoingNames, incomingNamesR63A, outgoingNamesR63A,
            largestNameSizeOf(incomingNames.values.flatten().map { it.second }, outgoingNames.values.flatten().map { it.second }),
            largestNameSizeOf(incomingNamesR63A.values.flatten().map { it.second }, outgoingNamesR63A.values.flatten().map { it.second }),
        )
    }

    private fun loadAnnotatedMethods(
        messageHandlers: MutableMap<Incoming, MutableMap<String, HabboMethodInfo>>,
        messageResponses: MutableMap<Outgoing, MutableMap<String, RegisteredResponse>>,
        messageHandlersR63A: MutableMap<IncomingR63A, HabboMethodInfo>,
        messageResponsesR63A: MutableMap<OutgoingR63A, RegisteredResponse>,
    ) {
        val lookup = MethodHandles.lookup()
        // instâncias dos handlers desta carga (local; cada carga cria as suas)
        val instances = ConcurrentHashMap<Class<*>, Any>()

        ClassGraph()
            .enableAllInfo()
            .acceptPackages(javaClass.`package`.name)
            .scan()
            .use { scanResult ->
                runBlocking {
                    val jobHandlers = HabboServer.applicationScope.launch {
                        loadAnnotated(lookup, scanResult, instances, Handler::class.java, { it.headers }, { it.requiredAuth }) { incoming, method, instance, handle, requiredAuth ->
                            messageHandlers.getOrPut(incoming) { mutableMapOf() }[method.name] =
                                HabboMethodInfo(requiredAuth, HabboInvokers.buildRequestInvoker(lookup, instance, method, HabboSession::class.java), handle)
                        }
                        loadAnnotated(lookup, scanResult, instances, HandlerR63A::class.java, { it.headers }, { it.requiredAuth }) { incoming, method, instance, handle, requiredAuth ->
                            messageHandlersR63A[incoming] =
                                HabboMethodInfo(requiredAuth, HabboInvokers.buildRequestInvoker(lookup, instance, method, HabboSession::class.java), handle)
                        }
                        log.info("Loaded {} Habbo request handlers", messageHandlers.size)
                        log.info("Loaded {} Habbo R63A request handlers", messageHandlersR63A.size)
                    }

                    val jobResponses = HabboServer.applicationScope.launch {
                        loadAnnotated(lookup, scanResult, instances, Response::class.java, { it.headers }, { false }) { outgoing, method, instance, handle, _ ->
                            messageResponses.getOrPut(outgoing) { mutableMapOf() }[method.name] =
                                RegisteredResponse(HabboInvokers.buildResponseInvoker(instance, method), handle)
                        }
                        loadAnnotated(lookup, scanResult, instances, ResponseR63A::class.java, { it.headers }, { false }) { outgoing, method, instance, handle, _ ->
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
        instances: ConcurrentHashMap<Class<*>, Any>,
        annotationClass: Class<A>,
        headersOf: (A) -> Array<out E>,
        requiredAuthOf: (A) -> Boolean,
        register: (enumValue: E, method: Method, instance: Any, handle: MethodHandle, requiredAuth: Boolean) -> Unit
    ) {
        scanResult.getClassesWithMethodAnnotation(annotationClass.name).forEach { classInfo ->
            classInfo.methodInfo.filter { it.hasAnnotation(annotationClass) }.forEach { methodInfo ->
                val method = methodInfo.loadClassAndGetMethod()
                val annotation = method.getAnnotation(annotationClass) ?: return@forEach
                val instance = getInstance(instances, method.declaringClass)
                val handle = lookup.unreflect(method)
                val requiredAuth = requiredAuthOf(annotation)

                headersOf(annotation).forEach { enumValue -> register(enumValue, method, instance, handle, requiredAuth) }
            }
        }
    }

    // ---------------------------------------------------------------- dispatch (request)

    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val s = snapshot
        habboRequest.use {
            val incomingEnum: Incoming? =
                if (habboRequest.headerId == RELEASE_CHECK_ID) Incoming.RELEASE_CHECK
                else s.incomingEnumByReleaseAndHeader[habboSession.release to habboRequest.headerId]

            if (incomingEnum == null || !s.messageHandlers.containsKey(incomingEnum)) {
                warnUnknownRequest(habboRequest.headerId, incomingEnum)
                return@use
            }

            val methodName = overrideMethodFor(s, habboSession.release, habboRequest.headerId) ?: "handle"
            val methodInfo = s.messageHandlers[incomingEnum]?.get(methodName)

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
        val s = snapshot
        habboRequest.use {
            val incomingEnum: IncomingR63A? =
                if (habboRequest.headerId == RELEASE_CHECK_ID) IncomingR63A.RELEASE_CHECK
                else s.incomingEnumR63AByReleaseAndHeader[habboSession.release to habboRequest.headerId]

            val methodInfo = incomingEnum?.let { s.messageHandlersR63A[it] }
            if (methodInfo == null) {
                warnUnknownRequest(habboRequest.headerId, incomingEnum)
                return@use
            }

            dispatchRequest(habboSession, habboRequest, methodInfo, incomingEnum) {
                habboRequest.incomingR63A = incomingEnum
                habboRequest.protocolEra = habboSession.protocolEra
            }
        }
    }

    private fun overrideMethodFor(s: Snapshot, release: String, headerId: Int): String? =
        s.incomingHeadersByReleaseAndHeader[release to headerId]?.overrideMethod

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

        val start = System.nanoTime()
        // espera entre o pedido nascer no decoder e o handler começar: fila do dispatcher, GC, CPU saturada
        ServerMetrics.recordHandlerQueueWait(start - habboRequest.receivedNano)
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
        } finally {
            val elapsed = System.nanoTime() - start
            // a chave é o nome semântico do enum (SSO_TICKET), nunca o id do cabeçalho: o id muda a cada release
            ServerMetrics.recordHandler(logLabel?.toString() ?: "UNKNOWN", elapsed)
            if (elapsed > 5_000_000L && slowLog.isWarnEnabled) {
                slowLog.warn("[slow-handler] {} (header {}) took {}ms", logLabel, habboRequest.headerId, "%.2f".format(elapsed / 1_000_000.0))
            }
        }
    }

    // ---------------------------------------------------------------- dispatch (response)

    fun invokeResponse(habboSession: HabboSession, outgoing: Outgoing, vararg args: Any?): HabboResponse? {
        val s = snapshot
        val headerInfo = s.outgoingHeadersByReleaseAndName[habboSession.release to outgoing.name]
        val headerId = headerInfo?.header

        return dispatchResponse(
            habboSession,
            headerId,
            headerInfo?.overrideMethod,
            s.messageResponses[outgoing]?.get(headerInfo?.overrideMethod ?: DEFAULT_METHOD),
            outgoing,
            { id -> HabboResponse(headerId = id, outgoing = outgoing, habboVersion = habboSession.habboVersion) },
            args
        )
    }

    fun invokeResponse(habboSession: HabboSession, outgoing: OutgoingR63A, vararg args: Any?): HabboResponse? {
        val s = snapshot
        val headerInfo = s.outgoingHeadersR63AByReleaseAndName[habboSession.release to outgoing.name]
        val headerId = headerInfo?.header
        val overrideMethod = headerInfo?.overrideMethod

        return dispatchResponse(
            habboSession,
            headerId,
            overrideMethod,
            s.messageResponsesR63A[outgoing],
            outgoing,
            { id ->
                HabboResponse(
                    headerId = id,
                    outgoing = null,
                    outgoingR63A = outgoing,
                    protocolEra = habboSession.protocolEra,
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

    private fun largestNameSizeOf(vararg enumLists: Collection<Enum<*>>): Int =
        enumLists.asSequence()
            .flatten()
            .maxOfOrNull { it.name.length } ?: 0

    // computeIfAbsent é atômico: as duas corrotinas (handlers e responses) nunca criam duas instâncias da mesma classe
    private fun getInstance(instances: ConcurrentHashMap<Class<*>, Any>, clazz: Class<*>): Any =
        instances.computeIfAbsent(clazz) { it.getDeclaredConstructor().newInstance() }

    private companion object {
        const val DISABLED = "DISABLED"
        const val DEFAULT_METHOD = "response"
        const val R63A_RELEASE = "R63A"
        const val RELEASE_CHECK_ID = 4000
    }
}
