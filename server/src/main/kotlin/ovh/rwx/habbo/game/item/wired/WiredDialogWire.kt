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

package ovh.rwx.habbo.game.item.wired

import ovh.rwx.habbo.communication.HabboVersion

/**
 * Topologia temporal do wire do diálogo Wired — a ÚNICA fonte destas datas.
 * O parse do save (incoming) e a escrita do writeDialog (outgoing) são espelhos do
 * mesmo wire e consultam estes fatos em vez de declarar datas próprias; datas
 * verificadas contra o AS3/habbo-timeline.
 */
class WiredDialogWire(private val version: HabboVersion) {
    /** Diálogo AIR (com seções de fontes e extras): AIR && ≥ 2023-04-14 */
    val isAirDialog: Boolean
        get() = version.isAir && version.isVersionAtLeast(2023, 4, 14)

    /** Seção variableIds (dialog E save, nascem juntos): ≥ 2024-05-06, a mesma
     *  atualização do WiredContext. Verificado por bissecção AS3 (2024-04-23: inexistente;
     *  2024-05-06: presente) — o gate 2023-06-30 que existia aqui era mito: entre
     *  2023-04-14 e 2024-05-05 o parser base lê apenas furniSourceTypes + userSourceTypes. */
    val hasVariableIds: Boolean
        get() = isAirDialog && version.isVersionAtLeast(2024, 5, 6)

    /** Forma binária dos variableIds: LONG (8 bytes) de 2024-05-06 até < 2024-12-12
     *  (readLong/new Long verificado em 2024-07-09, 2024-10-18 e 2024-11-19); String/UTF
     *  a partir de 2024-12-12. A forma Int NUNCA existiu no wire. */
    val variableIdsAsLong: Boolean
        get() = version.isVersionBefore(2024, 12, 12)

    /** stuffIds2 (mobis secundários, lado save) e defaultIntParams (lado dialog):
     *  mesma atualização de 2025-05-06 */
    val hasStuffIds2: Boolean
        get() = isAirDialog && version.isVersionAtLeast(2025, 5, 6)

    /** isInvert boolean após writeConditionDefinitionSpecifics até < 2024-05-06 */
    val hasConditionInvert: Boolean
        get() = version.isVersionBefore(2024, 5, 6)

    /** Seção WiredContext e conditionTypeSpecifics (AIR): nascidas em 2024-05-06 */
    val hasWiredContext: Boolean
        get() = isAirDialog && version.isVersionAtLeast(2024, 5, 6)
}
