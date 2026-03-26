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

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class HabboVersion(
    val buildDate: LocalDateTime,
    val majorVersion: Int
) : Comparable<HabboVersion> {

    override fun compareTo(other: HabboVersion): Int {
        return this.buildDate.compareTo(other.buildDate)
    }

    companion object {
        // Busca os 12 dígitos da data (ex: 201108301108)
        private val BUILD_DATE_REGEX = Regex("(20\\d{10})")

        // Busca os dígitos logo após a palavra RELEASE (ex: 63)
        private val MAJOR_VERSION_REGEX = Regex("RELEASE(\\d+)")

        // Formato exato do timestamp da Sulake: AnoMesDiaHoraMinuto
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

        fun parse(releaseString: String): HabboVersion {
            // 1. Extrai a data de build
            val dateMatch = BUILD_DATE_REGEX.find(releaseString)
            val dateString = dateMatch?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Não foi possível encontrar a data de build: $releaseString")

            // Transforma a string de 12 dígitos em LocalDateTime
            val buildDate = LocalDateTime.parse(dateString, FORMATTER)

            // 2. Extrai o Major Version
            val majorMatch = MAJOR_VERSION_REGEX.find(releaseString)
            val majorVersion = majorMatch?.groupValues?.get(1)?.toIntOrNull() ?: 63

            return HabboVersion(buildDate, majorVersion)
        }
    }

    /**
     * Retorna true se a versão do cliente atual for igual ou superior à data informada.
     * Ignora horas e minutos na comparação.
     */
    fun isVersionAtLeast(year: Int, month: Int, day: Int): Boolean {
        val targetDate = LocalDate.of(year, month, day)
        val clientDate = buildDate.toLocalDate()

        // Se a data do cliente NÃO for antes da data alvo, significa que é igual ou maior
        return !clientDate.isBefore(targetDate)
    }

    /**
     * Retorna true se a versão do cliente atual for estritamente anterior à data informada.
     * Útil para pacotes antigos que foram removidos ou substituídos.
     */
    fun isVersionBefore(year: Int, month: Int, day: Int): Boolean {
        val targetDate = LocalDate.of(year, month, day)
        val clientDate = buildDate.toLocalDate()

        return clientDate.isBefore(targetDate)
    }
}

fun HabboResponse.isVersionAtLeast(year: Int, month: Int, day: Int): Boolean {
    // Se a data do cliente NÃO for antes da data alvo, significa que é igual ou maior
    return habboVersion.isVersionAtLeast(year, month, day)
}

fun HabboResponse.isVersionBefore(year: Int, month: Int, day: Int): Boolean {
    return habboVersion.isVersionBefore(year, month, day)
}