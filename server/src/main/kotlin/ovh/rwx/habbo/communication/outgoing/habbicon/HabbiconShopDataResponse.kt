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

package ovh.rwx.habbo.communication.outgoing.habbicon

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.habbicon.HabbiconCollection
import ovh.rwx.habbo.game.habbicon.UserHabbicon

/**
 * Payload estruturado para exibição da loja/catálogo de coleções de Habbicons (HABBICON_SHOP_DATA).
 *
 * @param collections Coleções de Habbicons disponíveis na loja.
 * @param userHabbicons Mapa dos Habbicons que o usuário já possui (habbiconId -> UserHabbicon).
 */
data class HabbiconShopData(
    val collections: List<HabbiconCollection>,
    val userHabbicons: Map<Int, UserHabbicon>,
)

@Suppress("unused", "UNUSED_PARAMETER")
class HabbiconShopDataResponse {
    @Response(Outgoing.HABBICON_SHOP_DATA)
    fun response(habboResponse: HabboResponse, data: HabbiconShopData) {
        habboResponse.writeInt(data.collections.size)
        data.collections.forEach { collection ->
            habboResponse.writeInt(collection.id)
            habboResponse.writeUTF(collection.name)
            val items = collection.habbicons.filter { it.id != collection.rewardHabbiconId && it.enabled }
            val completed = items.isNotEmpty() && items.all { data.userHabbicons.containsKey(it.id) }
            habboResponse.writeBoolean(completed)
            habboResponse.writeInt(collection.rewardHabbiconId ?: 0)
            habboResponse.writeInt(collection.rewardHabbiconId?.let { data.userHabbicons[it]?.state } ?: 0)
            habboResponse.writeInt(collection.priceCredits)
            habboResponse.writeInt(collection.priceActivityPoints)
            habboResponse.writeInt(collection.activityPointType)
            habboResponse.writeInt(items.size)
            items.forEach { habbicon ->
                habboResponse.serialize(habbicon, data.userHabbicons[habbicon.id]?.state ?: 0)
            }
        }
    }
}
