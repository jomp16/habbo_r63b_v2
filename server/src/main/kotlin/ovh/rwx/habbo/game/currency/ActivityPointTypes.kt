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

package ovh.rwx.habbo.game.currency

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.database.currency.ActivityPointTypeDao
import ovh.rwx.habbo.util.ActivityPointType
import java.time.LocalDateTime

/**
 * Tipo de moeda alternativa (activity point) e a janela de data de build do client em que ele pode ser enviado.
 * A janela vem da tabela `activity_point_types` (ver V25 para a evidência no AS3).
 */
data class ActivityPointTypeInfo(
    val code: Int,
    val name: String,
    val availableFrom: LocalDateTime?,
    val availableUntil: LocalDateTime?,
    val enabled: Boolean
) {
    fun isAvailableAt(buildDate: LocalDateTime): Boolean =
        enabled
                && (availableFrom == null || !buildDate.isBefore(availableFrom))
                && (availableUntil == null || buildDate.isBefore(availableUntil))
}

/**
 * Quais tipos de moeda o client de cada release entende. Pixels (0) sempre é aceito: é a única moeda alternativa que
 * existe em todas as eras.
 */
object ActivityPointTypes {
    private val log = LoggerFactory.getLogger(ActivityPointTypes::class.java)

    @Volatile
    private var types: Map<Int, ActivityPointTypeInfo>? = null

    private fun loaded(): Map<Int, ActivityPointTypeInfo> = types ?: synchronized(this) {
        types ?: runCatching { ActivityPointTypeDao.getAll().associateBy { it.code } }
            .onFailure { log.error("Falha ao carregar activity_point_types; só os pixels serão aceitos.", it) }
            .getOrDefault(emptyMap())
            .also { types = it }
    }

    /** Recarrega a tabela (ex.: depois de habilitar um tipo no banco). */
    fun reload() {
        types = null
        loaded()
    }

    fun info(code: Int): ActivityPointTypeInfo? = loaded()[code]

    fun isSupported(version: HabboVersion, code: Int): Boolean {
        if (code == ActivityPointType.PIXELS.code) return true
        return loaded()[code]?.isAvailableAt(version.buildDate) == true
    }

    /** Só os saldos de tipos que o client dessa release conhece (um tipo desconhecido pode quebrar a UI dele). */
    fun supportedBalances(version: HabboVersion, balances: Map<Int, Int>): Map<Int, Int> =
        balances.filterKeys { isSupported(version, it) }
}
