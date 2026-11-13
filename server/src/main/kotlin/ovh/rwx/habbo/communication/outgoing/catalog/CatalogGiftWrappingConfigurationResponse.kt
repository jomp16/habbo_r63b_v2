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

package ovh.rwx.habbo.communication.outgoing.catalog

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

/**
 * GiftWrappingConfiguration. [stuffTypes] = sprites `present_wrap*N` (pagos), [defaultStuffTypes] = sprites
 * `present_gen1..6` (caixa padrão grátis), [boxTypes]/[ribbonTypes] = ids disponíveis para o client da sessão.
 */
data class CatalogGiftWrappingConfigurationData(
    val enabled: Boolean,
    val price: Int,
    val stuffTypes: List<Int>,
    val boxTypes: List<Int>,
    val ribbonTypes: List<Int>,
    val defaultStuffTypes: List<Int>,
)

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogGiftWrappingConfigurationResponse {
    @Response(Outgoing.CATALOG_GIFT_WRAPPING)
    @ResponseR63A(OutgoingR63A.CATALOG_GIFT_WRAPPING)
    fun response(habboResponse: HabboResponse, data: CatalogGiftWrappingConfigurationData) {
        habboResponse.apply {
            writeBoolean(data.enabled) // isWrappingEnabled
            writeInt(data.price) // wrappingPrice

            writeIntList(data.stuffTypes)
            writeIntList(data.boxTypes)
            writeIntList(data.ribbonTypes)

            // defaultStuffTypes só existe a partir de 2011-12-13 (R63A e 20111005 terminam nos ribbonTypes)
            if (isVersionAtLeast(2011, 12, 13)) {
                writeIntList(data.defaultStuffTypes)
            }
        }
    }

    private fun HabboResponse.writeIntList(values: List<Int>) {
        writeInt(values.size)
        values.forEach { writeInt(it) }
    }
}
