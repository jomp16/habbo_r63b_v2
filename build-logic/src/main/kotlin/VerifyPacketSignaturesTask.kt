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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

abstract class VerifyPacketSignaturesTask : DefaultTask() {

    @get:InputFiles
    abstract val classesDirs: ConfigurableFileCollection

    @get:Input
    abstract val failOnError: Property<Boolean>

    init {
        description = "Verifica estaticamente assinaturas de pacotes Habbo (@Response / @ResponseR63A vs callsites)."
        group = "verification"
        failOnError.convention(true)
    }

    data class ResponseDef(
        val headerName: String,
        val isR63A: Boolean,
        val className: String,
        val methodName: String,
        val paramTypes: List<Type>,
        val sourceFile: String?
    ) {
        val paramCount: Int get() = paramTypes.size
    }

    data class CallSite(
        val callerClass: String,
        val callerMethod: String,
        val sourceFile: String?,
        val lineNumber: Int,
        val outgoingName: String?,
        val outgoingR63AName: String?,
        val actualArgCount: Int?, // null se dinâmico (*args)
        val methodNameCalled: String
    )

    data class SignatureMismatch(
        val callSite: CallSite,
        val targetHeader: String,
        val isR63A: Boolean,
        val expectedDefs: List<ResponseDef>,
        val reason: String
    )

    @TaskAction
    fun verify() {
        val classFiles = classesDirs.asFileTree.filter { it.extension == "class" }.files
        logger.lifecycle("🔍 Verificando assinaturas de pacotes em ${classFiles.size} classes compiladas...")

        val classNodes = mutableListOf<ClassNode>()
        for (file in classFiles) {
            try {
                val cr = ClassReader(file.readBytes())
                val cn = ClassNode()
                cr.accept(cn, ClassReader.SKIP_FRAMES)
                classNodes.add(cn)
            } catch (e: Exception) {
                logger.warn("Não foi possível ler classe: ${file.path}", e)
            }
        }

        // 1. Mapear todas as definições de Response (@Response e @ResponseR63A)
        val outgoingDefs = mutableMapOf<String, MutableList<ResponseDef>>()
        val outgoingR63ADefs = mutableMapOf<String, MutableList<ResponseDef>>()

        for (cn in classNodes) {
            for (mn in cn.methods) {
                val annotations = mutableListOf<AnnotationNode>()
                mn.visibleAnnotations?.let { annotations.addAll(it) }
                mn.invisibleAnnotations?.let { annotations.addAll(it) }

                for (ann in annotations) {
                    val isModern = ann.desc == "Lovh/rwx/habbo/communication/Response;"
                    val isR63A = ann.desc == "Lovh/rwx/habbo/communication/ResponseR63A;"
                    if (!isModern && !isR63A) continue

                    val headers = extractHeaderNames(ann)
                    val allArgs = Type.getArgumentTypes(mn.desc)
                    // O 1º argumento é sempre HabboResponse
                    val payloadArgs = if (allArgs.isNotEmpty()) allArgs.drop(1) else emptyList()

                    for (header in headers) {
                        val def = ResponseDef(
                            headerName = header,
                            isR63A = isR63A,
                            className = cn.name,
                            methodName = mn.name,
                            paramTypes = payloadArgs,
                            sourceFile = cn.sourceFile
                        )
                        if (isModern) {
                            outgoingDefs.computeIfAbsent(header) { mutableListOf() }.add(def)
                        } else {
                            outgoingR63ADefs.computeIfAbsent(header) { mutableListOf() }.add(def)
                        }
                    }
                }
            }
        }

        logger.lifecycle("📦 Encontrados ${outgoingDefs.size} headers Modern e ${outgoingR63ADefs.size} headers R63A com handlers mapeados.")

        // 2. Localizar e verificar call-sites
        val mismatches = mutableListOf<SignatureMismatch>()
        var totalCheckedCallSites = 0
        var dynamicCallSites = 0

        for (cn in classNodes) {
            // Ignorar forwarders de rede centrais
            if (cn.name == "ovh/rwx/habbo/game/user/HabboSession" ||
                cn.name == "ovh/rwx/habbo/communication/HabboHandler" ||
                cn.name == "ovh/rwx/habbo/game/room/managers/RoomNetworkDispatcher"
            ) {
                continue
            }

            for (mn in cn.methods) {
                analyzeMethodCalls(
                    cn = cn,
                    mn = mn,
                    outgoingDefs = outgoingDefs,
                    outgoingR63ADefs = outgoingR63ADefs,
                    onCallSite = { cs ->
                        totalCheckedCallSites++
                        if (cs.actualArgCount == null) {
                            dynamicCallSites++
                        }
                    },
                    onMismatch = { mismatch ->
                        mismatches.add(mismatch)
                    }
                )
            }
        }

        logger.lifecycle("✅ Analisados $totalCheckedCallSites call-sites de envio de pacotes ($dynamicCallSites com *args dinâmicos).")

        if (mismatches.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine("\n❌ FORAM ENCONTRADAS ${mismatches.size} DIVERGÊNCIAS DE ASSINATURA DE PACOTES HABBO:\n")
            for ((idx, m) in mismatches.withIndex()) {
                val cs = m.callSite
                val sourceLoc = "${cs.sourceFile ?: cs.callerClass}:${cs.lineNumber}"
                sb.appendLine("--------------------------------------------------------------------------------")
                sb.appendLine("[${idx + 1}] Em $sourceLoc (no método ${cs.callerClass.substringAfterLast('/')}.${cs.callerMethod}):")
                sb.appendLine("    Chamada: ${cs.methodNameCalled}")
                sb.appendLine("    Header: ${if (m.isR63A) "OutgoingR63A." else "Outgoing."}${m.targetHeader}")
                sb.appendLine("    Argumentos passados: ${cs.actualArgCount ?: "dinâmico (*args)"}")
                sb.appendLine("    Motivo: ${m.reason}")
                sb.appendLine("    Handlers esperados:")
                for (def in m.expectedDefs) {
                    val typesFormatted = def.paramTypes.joinToString(", ") { it.className.substringAfterLast('.') }
                    sb.appendLine("      - ${def.className.substringAfterLast('/')}.${def.methodName}(${if (typesFormatted.isEmpty()) "nenhum parâmetro" else typesFormatted}) [total: ${def.paramCount}]")
                }
            }
            sb.appendLine("--------------------------------------------------------------------------------\n")

            if (failOnError.get()) {
                throw GradleException(sb.toString())
            } else {
                logger.error(sb.toString())
            }
        } else {
            logger.lifecycle("✨ Todas as assinaturas estáticas de pacotes verificadas com SUCESSO! Nenhuma divergência encontrada.")
        }
    }

    private fun extractHeaderNames(ann: AnnotationNode): List<String> {
        val result = mutableListOf<String>()
        val values = ann.values ?: return result
        for (i in 0 until values.size step 2) {
            val key = values[i] as? String ?: continue
            val value = values[i + 1]
            if (key == "headers") {
                if (value is List<*>) {
                    for (item in value) {
                        if (item is Array<*> && item.size == 2) {
                            val enumVal = item[1] as? String
                            if (enumVal != null) result.add(enumVal)
                        }
                    }
                } else if (value is Array<*> && value.size == 2) {
                    val enumVal = value[1] as? String
                    if (enumVal != null) result.add(enumVal)
                }
            }
        }
        return result
    }

    private fun analyzeMethodCalls(
        cn: ClassNode,
        mn: MethodNode,
        outgoingDefs: Map<String, List<ResponseDef>>,
        outgoingR63ADefs: Map<String, List<ResponseDef>>,
        onCallSite: (CallSite) -> Unit,
        onMismatch: (SignatureMismatch) -> Unit
    ) {
        var currentLine = -1

        for (insn in mn.instructions) {
            if (insn is LineNumberNode) {
                currentLine = insn.line
                continue
            }
            if (insn !is MethodInsnNode) continue

            val isSendHabboResponseModern = insn.name == "sendHabboResponse" &&
                    insn.desc == "(Lovh/rwx/habbo/communication/outgoing/Outgoing;[Ljava/lang/Object;)V"

            val isSendHabboResponseR63A = insn.name == "sendHabboResponse" &&
                    insn.desc == "(Lovh/rwx/habbo/communication/outgoing/OutgoingR63A;[Ljava/lang/Object;)V"

            val isSendResponseBoth = insn.name == "sendResponse" &&
                    insn.desc == "(Lovh/rwx/habbo/communication/outgoing/Outgoing;Lovh/rwx/habbo/communication/outgoing/OutgoingR63A;[Ljava/lang/Object;)V"

            val isSendResponseModern = insn.name == "sendResponseModern" &&
                    insn.desc == "(Lovh/rwx/habbo/communication/outgoing/Outgoing;[Ljava/lang/Object;)V"

            val isSendResponseR63A = insn.name == "sendResponseR63A" &&
                    insn.desc == "(Lovh/rwx/habbo/communication/outgoing/OutgoingR63A;[Ljava/lang/Object;)V"

            val isInvokeResponseModern = insn.name == "invokeResponse" &&
                    insn.desc.contains("Lovh/rwx/habbo/communication/outgoing/Outgoing;")

            val isInvokeResponseR63A = insn.name == "invokeResponse" &&
                    insn.desc.contains("Lovh/rwx/habbo/communication/outgoing/OutgoingR63A;")

            if (!isSendHabboResponseModern && !isSendHabboResponseR63A && !isSendResponseBoth &&
                !isSendResponseModern && !isSendResponseR63A && !isInvokeResponseModern && !isInvokeResponseR63A
            ) {
                continue
            }

            val expectsModern =
                isSendHabboResponseModern || isSendResponseModern || isInvokeResponseModern || isSendResponseBoth
            val expectsR63A =
                isSendHabboResponseR63A || isSendResponseR63A || isInvokeResponseR63A || isSendResponseBoth

            // Descobrir a contagem do array de varargs e os enums passados
            val (outgoingName, outgoingR63AName, argCount) = resolveInvocationDetails(
                methodInsn = insn,
                expectsModern = expectsModern,
                expectsR63A = expectsR63A
            )

            val callSite = CallSite(
                callerClass = cn.name,
                callerMethod = mn.name,
                sourceFile = cn.sourceFile,
                lineNumber = currentLine,
                outgoingName = outgoingName,
                outgoingR63AName = outgoingR63AName,
                actualArgCount = argCount,
                methodNameCalled = "${insn.owner.substringAfterLast('/')}.${insn.name}"
            )

            onCallSite(callSite)

            // Se for chamada com argumentos dinâmicos (*args), não temos como checar o tamanho estaticamente
            if (argCount == null) {
                continue
            }

            // Validar Modern se presente
            if (outgoingName != null) {
                val defs = outgoingDefs[outgoingName]
                if (defs != null && defs.isNotEmpty()) {
                    val matchingDef = defs.firstOrNull { it.paramCount == argCount }
                    if (matchingDef == null) {
                        onMismatch(
                            SignatureMismatch(
                                callSite = callSite,
                                targetHeader = outgoingName,
                                isR63A = false,
                                expectedDefs = defs,
                                reason = "Esperado ${
                                    defs.map { it.paramCount }.distinct().joinToString(" ou ")
                                } argumento(s), mas foram passados $argCount."
                            )
                        )
                    }
                }
            }

            // Validar R63A se presente
            if (outgoingR63AName != null) {
                val defs = outgoingR63ADefs[outgoingR63AName]
                if (defs != null && defs.isNotEmpty()) {
                    val matchingDef = defs.firstOrNull { it.paramCount == argCount }
                    if (matchingDef == null) {
                        onMismatch(
                            SignatureMismatch(
                                callSite = callSite,
                                targetHeader = outgoingR63AName,
                                isR63A = true,
                                expectedDefs = defs,
                                reason = "Esperado ${
                                    defs.map { it.paramCount }.distinct().joinToString(" ou ")
                                } argumento(s), mas foram passados $argCount."
                            )
                        )
                    }
                }
            }
        }
    }

    private data class InvocationDetails(
        val outgoingName: String?,
        val outgoingR63AName: String?,
        val argCount: Int?
    )

    private fun resolveInvocationDetails(
        methodInsn: MethodInsnNode,
        expectsModern: Boolean,
        expectsR63A: Boolean
    ): InvocationDetails {
        var outgoingName: String? = null
        var outgoingR63AName: String? = null
        var argCount: Int? = null

        // 1. Procurar o ANEWARRAY java/lang/Object antes do methodInsn
        val p = methodInsn.previousReal()

        // Pode ser um ALOAD se o array foi armazenado em variável local
        var arrayLocalVar: Int? = null
        if (p is VarInsnNode && p.opcode == Opcodes.ALOAD) {
            arrayLocalVar = p.`var`
        }

        var anewArrayNode: TypeInsnNode? = null
        var search: AbstractInsnNode? = p
        var steps = 0
        while (search != null && steps < 300) {
            if (search is TypeInsnNode && search.opcode == Opcodes.ANEWARRAY && search.desc == "java/lang/Object") {
                if (arrayLocalVar != null) {
                    val nextReal = search.nextReal()
                    if (nextReal is VarInsnNode && nextReal.opcode == Opcodes.ASTORE && nextReal.`var` == arrayLocalVar) {
                        anewArrayNode = search
                        break
                    }
                } else {
                    anewArrayNode = search
                    break
                }
            }
            search = search.previous
            steps++
        }

        if (anewArrayNode != null) {
            val sizeInsn = anewArrayNode.previousReal()
            if (sizeInsn != null) {
                argCount = resolveArraySize(sizeInsn)
            }
        }

        // 2. Procurar os enums Outgoing e OutgoingR63A antes da criação do array
        val enumSearchStart = anewArrayNode?.previousReal()?.previousReal() ?: methodInsn.previousReal()
        var enumSearch = enumSearchStart
        steps = 0
        while (enumSearch != null && steps < 15) {
            if (enumSearch is FieldInsnNode && enumSearch.opcode == Opcodes.GETSTATIC) {
                if (expectsModern && enumSearch.owner == "ovh/rwx/habbo/communication/outgoing/Outgoing") {
                    if (outgoingName == null) outgoingName = enumSearch.name
                } else if (expectsR63A && enumSearch.owner == "ovh/rwx/habbo/communication/outgoing/OutgoingR63A") {
                    if (outgoingR63AName == null) outgoingR63AName = enumSearch.name
                }
            }
            enumSearch = enumSearch.previous
            steps++
        }

        return InvocationDetails(outgoingName, outgoingR63AName, argCount)
    }

    private fun resolveArraySize(insn: AbstractInsnNode): Int? {
        return when (insn.opcode) {
            Opcodes.ICONST_0 -> 0
            Opcodes.ICONST_1 -> 1
            Opcodes.ICONST_2 -> 2
            Opcodes.ICONST_3 -> 3
            Opcodes.ICONST_4 -> 4
            Opcodes.ICONST_5 -> 5
            Opcodes.BIPUSH, Opcodes.SIPUSH -> (insn as? IntInsnNode)?.operand
            Opcodes.LDC -> ((insn as? LdcInsnNode)?.cst as? Number)?.toInt()
            else -> null
        }
    }

    private fun AbstractInsnNode.previousReal(): AbstractInsnNode? {
        var p = this.previous
        while (p != null && (p is LabelNode || p is LineNumberNode || p is FrameNode)) {
            p = p.previous
        }
        return p
    }

    private fun AbstractInsnNode.nextReal(): AbstractInsnNode? {
        var n = this.next
        while (n != null && (n is LabelNode || n is LineNumberNode || n is FrameNode)) {
            n = n.next
        }
        return n
    }
}
