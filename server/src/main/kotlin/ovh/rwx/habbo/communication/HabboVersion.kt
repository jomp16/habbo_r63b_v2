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
import ovh.rwx.habbo.game.user.HabboSession

enum class HabboPlatform {
    AIR,
    FLASH,
    NITRO,
    UNKNOWN;

    val isAir: Boolean get() = this == AIR
    val isFlash: Boolean get() = this == FLASH
    val isNitro: Boolean get() = this == NITRO
}

data class HabboVersion(
    val buildDate: LocalDateTime,
    val majorVersion: Int,
    val platform: HabboPlatform = HabboPlatform.UNKNOWN,
    val releaseString: String = ""
) : Comparable<HabboVersion> {

    val isAir: Boolean get() = platform.isAir
    val isFlash: Boolean get() = platform.isFlash
    val isNitro: Boolean get() = platform.isNitro

    override fun compareTo(other: HabboVersion): Int {
        return this.buildDate.compareTo(other.buildDate)
    }

    companion object {
        // Busca os 12 dígitos da data (ex: 201108301108)
        private val BUILD_DATE_REGEX = Regex("(20\\d{10})")

        // Busca os dígitos da major version após RELEASE, WIN, MAC ou R (ex: RELEASE63, WIN63, MAC63, R63A, RELEASE38)
        private val MAJOR_VERSION_REGEX = Regex("(?:RELEASE|WIN|MAC|R)(\\d+)", RegexOption.IGNORE_CASE)

        // Formato exato do timestamp da Sulake: AnoMesDiaHoraMinuto
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

        fun parse(releaseString: String): HabboVersion {
            // 1. Identifica a plataforma do cliente (WIN63 e MAC63 representam o cliente Habbo AIR desktop)
            val platform = when {
                releaseString.contains("WIN63") || releaseString.contains("MAC63") -> HabboPlatform.AIR
                releaseString.contains("PRODUCTION") || releaseString.contains("RELEASE") || releaseString.contains("R63") -> HabboPlatform.FLASH
                releaseString.contains("NITRO") -> HabboPlatform.NITRO
                else -> HabboPlatform.UNKNOWN
            }

            // 2. Extrai a data de build (12 dígitos numéricos no formato yyyyMMddHHmm)
            val dateMatch = BUILD_DATE_REGEX.find(releaseString)
                ?: throw IllegalArgumentException("Não foi possível encontrar a data de build: $releaseString")
            val buildDate = LocalDateTime.parse(dateMatch.groupValues[1], FORMATTER)

            // 3. Extrai o Major Version
            val majorMatch = MAJOR_VERSION_REGEX.find(releaseString)
            val majorVersion = majorMatch?.groupValues?.get(1)?.toIntOrNull() ?: 63

            return HabboVersion(buildDate, majorVersion, platform, releaseString)
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

    /**
     * Retorna true se a versão do cliente atual for EXATAMENTE a data informada.
     * Útil para pacotes "rebeldes" que mudaram e reverteram em builds específicas.
     */
    fun isExactVersion(year: Int, month: Int, day: Int): Boolean {
        val targetDate = LocalDate.of(year, month, day)
        return buildDate.toLocalDate().isEqual(targetDate)
    }

}

val HabboResponse.isAir: Boolean
    get() = habboVersion.isAir

val HabboResponse.platform: HabboPlatform
    get() = habboVersion.platform

fun HabboResponse.isVersionAtLeast(year: Int, month: Int, day: Int): Boolean {
    // Se a data do cliente NÃO for antes da data alvo, significa que é igual ou maior
    return habboVersion.isVersionAtLeast(year, month, day)
}

fun HabboResponse.isVersionBefore(year: Int, month: Int, day: Int): Boolean {
    return habboVersion.isVersionBefore(year, month, day)
}

fun HabboResponse.isExactVersion(year: Int, month: Int, day: Int): Boolean {
    return habboVersion.isExactVersion(year, month, day)
}

fun HabboResponse.isVersionBetween(
    startYear: Int, startMonth: Int, startDay: Int,
    endYear: Int, endMonth: Int, endDay: Int
): Boolean {
    return isVersionAtLeast(startYear, startMonth, startDay) && isVersionBefore(endYear, endMonth, endDay)
}

val HabboSession.isAir: Boolean
    get() = habboVersion.isAir

fun HabboSession.isVersionAtLeast(year: Int, month: Int, day: Int): Boolean {
    return habboVersion.isVersionAtLeast(year, month, day)
}

fun HabboSession.isVersionBefore(year: Int, month: Int, day: Int): Boolean {
    return habboVersion.isVersionBefore(year, month, day)
}