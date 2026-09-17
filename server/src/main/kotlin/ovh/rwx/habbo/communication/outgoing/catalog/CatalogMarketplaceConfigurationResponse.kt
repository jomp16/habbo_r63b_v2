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

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

data class CatalogMarketplaceConfigurationData(
    val enabled: Boolean = true,
    val commission: Int = 1,
    val tokenBatchPrice: Int = 0,
    val tokensBatchSize: Int = 0,
    val offerMinPrice: Int = 1,
    val offerMaxPrice: Int = 25000,
    val expirationHours: Int = 48,
    val averagePricePeriod: Int = 7,
    val sellingFeePercentage: Int = 0,
    val revenueLimit: Int = 0,
    val halfTaxLimit: Int = 0
)

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogMarketplaceConfigurationResponse {
    @Response(Outgoing.CATALOG_MARKETPLACE_CONFIGURATION)
    @ResponseR63A(OutgoingR63A.CATALOG_MARKETPLACE_CONFIGURATION)
    fun response(habboResponse: HabboResponse, data: CatalogMarketplaceConfigurationData) {
        habboResponse.apply {
            // 1. Dados Fundamentais da Feira (Presentes desde a RELEASE42)
            writeBoolean(data.enabled)
            writeInt(data.commission)
            writeInt(data.tokenBatchPrice)
            writeInt(data.tokensBatchSize)
            writeInt(data.offerMinPrice)
            writeInt(data.offerMaxPrice)
            writeInt(data.expirationHours)

            // 2. Período da média móvel de preço (Introduzido na RELEASE44 - 2010-01-22)
            if (isVersionAtLeast(2010, 1, 22)) {
                writeInt(data.averagePricePeriod)
            }

            // 3. Sistema de Taxação Progressiva (Introduzido exclusivamente no cliente AIR WIN63/MAC63 em 2021-03-17)
            if (isAir && isVersionAtLeast(2021, 3, 17)) {
                writeInt(data.sellingFeePercentage)
                writeInt(data.revenueLimit)
                writeInt(data.halfTaxLimit)
            }
        }
    }
}