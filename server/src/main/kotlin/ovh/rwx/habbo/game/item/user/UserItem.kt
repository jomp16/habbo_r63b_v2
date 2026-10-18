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

package ovh.rwx.habbo.game.item.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.Furnishing
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.LimitedItemData
import java.io.Serializable
import java.util.*

data class UserItem(
    val id: Int,
    var userId: Int,
    val itemName: String,
    var extraData: String,
    val limited: Boolean,
    val buildersClub: Boolean = false
) : IHabboResponseSerialize, Serializable {
    val limitedItemData: LimitedItemData? by lazy { if (limited) ItemDao.getLimitedData(id) else null }
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[itemName]!!

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            // Era moderna (≥ shuffle): o stripId (chave de inventário no cliente) trafega NEGATIVO por
            // convenção oficial do protocolo — o cliente indexa o inventário por essa chave
            // (AIR: selectItemById faz getItemById(-id)), faz Math.abs só na hora de colocar no
            // quarto, e espera o MESMO sinal negativo no FurniListRemove. O campo ref abaixo vai
            // positivo. Exceção oficial: presentes (gift) trafegam positivos — todo pendente.
            writeInt(-id) // todo: if gift do not send negative
            writeUTF(furnishing.type.type.uppercase(Locale.getDefault()))
            writeInt(id)
            writeInt(furnishing.spriteId)
            HabboServer.habboGame.itemManager.writeExtradata(habboResponse, extraData, furnishing, limitedItemData)

            writeBoolean(furnishing.allowRecycle)
            writeBoolean(furnishing.allowTrade)
            writeBoolean(limitedItemData == null && furnishing.allowInventoryStack)
            writeBoolean(furnishing.allowMarketplaceSell)
            writeInt(-1) // milliseconds to expire rental

            writeBoolean(true)
            writeInt(-1) // room id

            if (furnishing.type == ItemType.FLOOR) {
                writeUTF("")
                writeInt(0)
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            // Mesma convenção de stripId negativo da era moderna: o cliente oficial R62+
            // (2010-11-12 em diante, ainda Wedgie) compara o id da seleção de placement contra a
            // NEGAÇÃO do id do objeto no quarto (HabboUserDefinedRoomEvents: selected.id == -(objId)).
            // Clientes 2009–R54 não têm expectativa de sinal (armazenam cru e o place volta com abs).
            writeInt(-id)
            if (isVersionBefore(2011, 2, 25)) {
                writeInt(0) // useless?
            }
            writeUTF(furnishing.type.type.uppercase(Locale.getDefault()))
            writeInt(id)
            writeInt(furnishing.spriteId)

            val typeId =
                if (furnishing.itemName.contains("a2")) 3
                else if (furnishing.itemName.contains("wallpaper")) 2
                else if (furnishing.itemName.contains("landscape")) 4
                else 1

            writeInt(typeId)

            writeUTF(extraData)

            writeBoolean(furnishing.allowRecycle)
            writeBoolean(furnishing.allowTrade)
            writeBoolean(limitedItemData == null && furnishing.allowInventoryStack)

            if (isVersionAtLeast(2009, 12, 11)) {
                writeBoolean(furnishing.allowMarketplaceSell)
            }

            writeInt(-1) // milliseconds to expire rental

            if (furnishing.type == ItemType.FLOOR) {
                writeUTF("")
                writeInt(0)
            }
        }
    }
}
