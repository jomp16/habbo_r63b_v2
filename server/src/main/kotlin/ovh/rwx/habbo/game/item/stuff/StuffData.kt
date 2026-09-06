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

package ovh.rwx.habbo.game.item.stuff

import ovh.rwx.habbo.communication.HabboResponse

/**
 * Espelho server-side de com.sulake.habbo.room.object.data.IStuffData (client).
 *
 * Formato do protocolo: [roomExtra:Int (apenas no quarto)] [formatKey:Int] [payload]
 * O [roomExtra] carrega o state/paperId no quarto (0 na maioria, 2/3/4 para papéis,
 * state calculado para gifts, 1 para limited).
 *
 * A criação das instâncias é responsabilidade dos FurnitureLogic de cada mobília.
 */
sealed class StuffData(
    val formatKey: Int,
    val roomExtra: Int = 0,
) {
    /**
     * Valor "legacy" (state) usado pelo client para o estado visual do furni.
     */
    open val legacyValue: String = ""

    fun write(habboResponse: HabboResponse, inventory: Boolean = false) {
        if (!inventory) habboResponse.writeInt(roomExtra)

        writeFull(habboResponse)
    }

    /**
     * Payload completo: formatKey + dados.
     * Usado quando o stuff é serializado dentro de outro pacote (ex: ChestStorage).
     */
    fun writeFull(habboResponse: HabboResponse) {
        habboResponse.writeInt(formatKey)
        writePayload(habboResponse)
    }

    protected abstract fun writePayload(habboResponse: HabboResponse)

    companion object {
        const val FORMAT_KEY_LEGACY = 0
        const val FORMAT_KEY_MAP = 1
        const val FORMAT_KEY_STRING_ARRAY = 2
        const val FORMAT_KEY_INT_ARRAY = 3
        const val FORMAT_KEY_LEGACY_WITH_LIMITED = 256

        const val KEY_STATE = "state"
        const val KEY_RARITY = "rarity"
    }
}

