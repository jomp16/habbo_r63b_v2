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

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier

// Fábrica compartilhada de invokers (Habbo + FastFood): shapeia MethodHandles em
// call sites estáticos no load (bindTo + asType + asSpreader), com suporte a
// default args Kotlin via o sintético `name$default`.
internal object HabboInvokers {
    fun buildRequestInvoker(lookup: MethodHandles.Lookup, instance: Any, method: Method, sessionType: Class<*>): RequestInvoker {
        val shaped = unreflectWithDefaults(lookup, method, providedArgs = 2)
            .bindTo(instance)
            .asType(MethodType.methodType(Void.TYPE, sessionType, HabboRequest::class.java))

        return RequestInvoker { session, request -> shaped.invoke(session, request) }
    }

    // ponytail: responses não suportam default args (aridade é decidida pelo caller em cada
    // call site); se precisar, replicar unreflectWithDefaults com providedArgs dinâmico.
    fun buildResponseInvoker(instance: Any, method: Method): ResponseInvoker {
        val extraParams = method.parameterCount - 1
        val genericType = MethodType.methodType(
            Void.TYPE,
            HabboResponse::class.java,
            *Array(extraParams) { Any::class.java }
        )
        val shaped = MethodHandles.lookup().unreflect(method)
            .bindTo(instance)
            .asType(genericType)
            .asSpreader(Array<Any?>::class.java, extraParams)

        return ResponseInvoker { response, args -> shaped.invoke(response, args) }
    }

    // Métodos Kotlin com default args geram um sintético estático `name$default`
    // (receiver, params..., int mask, Object marker). Aqui deixamos os primeiros
    // `providedArgs` como argumentos reais e ativamos o default dos demais (mask + zeros).
    private fun unreflectWithDefaults(lookup: MethodHandles.Lookup, method: Method, providedArgs: Int): MethodHandle {
        val raw = lookup.unreflect(method)
        val synthetic = method.declaringClass.declaredMethods.firstOrNull {
            it.name == method.name + "\$default" && Modifier.isStatic(it.modifiers)
        } ?: return raw

        var handle = lookup.unreflect(synthetic)
        val params = handle.type().parameterList()
        val argCount = params.size - 2

        var mask = 0
        for (i in providedArgs + 1 until argCount) mask = mask or (1 shl (i - 1))

        handle = MethodHandles.insertArguments(handle, params.size - 1, null as Any?)
        handle = MethodHandles.insertArguments(handle, params.size - 2, mask)
        for (i in argCount - 1 downTo providedArgs + 1) {
            handle = MethodHandles.insertArguments(handle, i, zeroValue(params[i]))
        }

        return handle
    }

    private fun zeroValue(type: Class<*>): Any? = when (type) {
        Boolean -> false
        Char -> ' '
        Byte -> 0.toByte()
        Short -> 0.toShort()
        Int -> 0
        Long -> 0L
        Float -> 0f
        Double -> 0.0
        else -> null
    }
}
