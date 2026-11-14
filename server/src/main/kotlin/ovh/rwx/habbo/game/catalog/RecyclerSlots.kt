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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.game.gamedata.ExternalVariables
import java.time.LocalDate

/**
 * Quantos itens o Ecotron exige, conforme o cliente.
 *
 * AS3: até `RELEASE63-201204132305` o `RecyclerLogic` tem a constante fixa `= 5`; a partir de
 * `RELEASE63-201204162302` o número vem do `external_variables` (`catalog.recycler.number.of.slots`, depois
 * `recycler.number_of_slots`, padrão 5). O código do cliente nunca fixa 8: o 8 é o valor que o hotel serve.
 *
 * Por isso, a partir do corte vale o que o servidor lê do mesmo `external_variables` ([fromExternalVariables]), a
 * única fonte (não há valor no `config.yaml`). Se divergirem do client, ele manda N itens e o servidor recusa (o
 * client só tem o alerta "fechado" como falha).
 */
object RecyclerSlots {
    /** Slots fixos no cliente antes de virarem configuráveis; também o padrão do client sem a variável. */
    const val FIXED = 5

    private val CONFIGURABLE_FROM: LocalDate = LocalDate.of(2012, 4, 16)

    fun forBuildDate(buildDate: LocalDate, configuredSlots: Int): Int =
        if (buildDate.isBefore(CONFIGURABLE_FROM)) FIXED else configuredSlots

    /**
     * Slots dos clients configuráveis: `recycler.number_of_slots`, senão `catalog.recycler.number.of.slots`; valores
     * não positivos ou não numéricos contam como ausentes. O boot exige um dos dois válido
     * ([ExternalVariables.REQUIREMENTS]); o [FIXED] final é só o padrão do client, por segurança.
     */
    fun fromExternalVariables(variables: Map<String, String>?): Int =
        sequenceOf(ExternalVariables.RECYCLER_SLOTS, ExternalVariables.RECYCLER_SLOTS_LEGACY)
            .mapNotNull { variables?.get(it)?.trim()?.toIntOrNull()?.takeIf { slots -> slots > 0 } }
            .firstOrNull() ?: FIXED
}
