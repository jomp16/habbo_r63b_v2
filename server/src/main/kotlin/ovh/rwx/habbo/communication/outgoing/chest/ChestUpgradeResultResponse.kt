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

package ovh.rwx.habbo.communication.outgoing.chest

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

/**
 * Códigos de resultado de upgrade de capacidade do baú (CHEST_UPGRADE_RESULT).
 * Mapeamento das chaves de erro no AS3 (wiredchests.upgrade.result.error.<id>):
 *  - SUCCESS (0): Sucesso na expansão de capacidade.
 *  - NOT_ENOUGH_CREDITS (2): Créditos insuficientes.
 *  - CANNOT_UPGRADE (4): Tipo de baú não suporta upgrade.
 *  - INSUFFICIENT_VIP (5): Diamantes insuficientes.
 *  - MAX_CAPACITY_REACHED (6): Limite máximo de upgrades atingido.
 *  - CHEST_LOCKED (7): O baú está trancado.
 *  - NOT_OWNER (10): Usuário não é o dono do baú.
 */
enum class ChestUpgradeResult(val id: Int) {
    SUCCESS(0),
    NOT_ENOUGH_CREDITS(2),
    CANNOT_UPGRADE(4),
    INSUFFICIENT_VIP(5),
    MAX_CAPACITY_REACHED(6),
    CHEST_LOCKED(7),
    NOT_OWNER(10);

    companion object {
        fun fromId(id: Int): ChestUpgradeResult? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Payload estruturado para resultado de upgrade de capacidade do baú (CHEST_UPGRADE_RESULT).
 *
 * @param chestItemId ID do item do baú no quarto.
 * @param result Código de resultado ([ChestUpgradeResult]).
 */
data class ChestUpgradeResultData(
    val chestItemId: Int,
    val result: ChestUpgradeResult,
)

@Suppress("unused", "UNUSED_PARAMETER")
class ChestUpgradeResultResponse {
    @Response(Outgoing.CHEST_UPGRADE_RESULT)
    fun response(habboResponse: HabboResponse, data: ChestUpgradeResultData) {
        habboResponse.apply {
            writeInt(data.chestItemId)
            writeInt(data.result.id)
        }
    }
}
