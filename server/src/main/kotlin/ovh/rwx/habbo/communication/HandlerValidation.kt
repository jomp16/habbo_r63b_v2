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

import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.release.ReleaseHeaderInfo

/**
 * Validação pura (sem banco/servidor) de um estado candidato dos handlers: devolve a lista de erros
 * em vez de encerrar o processo, para o boot e o reload decidirem o que fazer.
 */
object HandlerValidation {
    private const val DISABLED = "DISABLED"

    fun validate(s: HabboHandler.Snapshot): List<String> =
        missingHeaders("incoming", s.incomingNames, Incoming.entries - Incoming.RELEASE_CHECK) +
            missingHeaders("outgoing", s.outgoingNames, Outgoing.entries.toList()) +
            missingHeaders("incoming", s.incomingNamesR63A, IncomingR63A.entries - IncomingR63A.RELEASE_CHECK) +
            missingHeaders("outgoing", s.outgoingNamesR63A, OutgoingR63A.entries.toList()) +
            invalidOverrides("incoming", s.incomingHeaders, { runCatching { Incoming.valueOf(it) }.getOrNull() }) { s.messageHandlers[it as Incoming] } +
            invalidOverrides("outgoing", s.outgoingHeaders, { runCatching { Outgoing.valueOf(it) }.getOrNull() }) { s.messageResponses[it as Outgoing] }

    fun <E : Enum<E>> missingHeaders(kind: String, names: Map<String, List<Pair<Int, E>>>, expected: Collection<E>): List<String> =
        names.mapNotNull { (release, pairs) ->
            val missing = expected - pairs.map { it.second }.toSet()

            if (missing.isEmpty()) null else "Missing $kind headers for release=$release: ${missing.joinToString()}"
        }

    fun invalidOverrides(
        kind: String,
        headers: List<ReleaseHeaderInfo>,
        valueOf: (String) -> Enum<*>?,
        methodsOf: (Enum<*>) -> Map<String, *>?,
    ): List<String> = headers.mapNotNull { h ->
        val override = h.overrideMethod
        if (override.isNullOrEmpty() || override == DISABLED) return@mapNotNull null

        val enum = valueOf(h.name) ?: return@mapNotNull null
        val methods = methodsOf(enum)

        if (methods == null || !methods.containsKey(override)) {
            "Invalid override_method '$override' for $kind '${h.name}' in release ${h.release} (header ${h.header}): method does not exist"
        } else null
    }
}
