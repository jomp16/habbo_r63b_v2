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

package ovh.rwx.habbo.game.chest

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.user.UserItem

enum class ChestType {
    FURNI,
    COINS,
}

data class ChestData(
    val itemId: Int,
    var userId: Int,
    var name: String,
    var description: String,
    var capacity: Int,
    var coins: Int,
    var isWired: Boolean,
    var locked: Boolean,
    var autoLock: Boolean,
    var stateMode: Int,
    var previewMode: Int,
    var previewAmount: Int,
    var anyoneCanOpen: Boolean,
    var anyoneCanDonate: Boolean,
    var notificationMode: Int,
    var notifyFull: Boolean,
    var notifyDonation: Boolean,
    var notifyWithdraw: Boolean,
    var notifyEmpty: Boolean,
    var notifyTransaction: Boolean,
) {
    var entries: MutableList<ChestEntry> = mutableListOf()
    var typeName: String = ""

    val type: ChestType
        get() = if (typeName.contains("coins")) ChestType.COINS else ChestType.FURNI

    val isStarter: Boolean
        get() = typeName.contains("starter")

    val usedCount: Int
        get() = if (type == ChestType.COINS) coins else entries.size

    fun isFull(): Boolean = usedCount >= capacity
}

data class ChestEntry(
    val chestItemId: Int,
    val item: UserItem,
    val lockState: Int,
    val transactionId: Long,
)

/**
 * Espelho de _SafeStr_1752.itemTypeToString (client): "isWallItem,typeId[,legacyPosterId]".
 */
fun itemTypeToString(isWallItem: Boolean, typeId: Int, legacyPosterId: String): String =
    buildString {
        append(if (isWallItem) "true" else "false")
        append(",")
        append(typeId)

        if (legacyPosterId.isNotEmpty()) {
            append(",")
            append(legacyPosterId)
        }
    }

/**
 * Serializa uma entrada do baú no formato ChestStorage do client (_-Y1J.ChestStorage):
 * inventoryId:I, lockState:I, transactionId:Long, ChestItemType{B,I,S},
 * groupable:B, specialType:I, stuffData, extra:I (somente floor).
 */
fun ChestEntry.serializeChestStorage(habboResponse: HabboResponse) {
    val furnishing = item.furnishing
    val isWallItem = furnishing.type == ItemType.WALL

    habboResponse.writeInt(item.id) // inventoryId
    habboResponse.writeInt(lockState)
    habboResponse.writeLong(transactionId)

    // ChestItemType
    habboResponse.writeBoolean(isWallItem)
    habboResponse.writeInt(furnishing.spriteId)
    habboResponse.writeUTF(if (item.itemName.contains("poster")) item.extraData else "")

    habboResponse.writeBoolean(furnishing.allowInventoryStack) // groupable
    habboResponse.writeInt(0) // specialType

    // stuffData (formato estruturado, sem o roomExtra)
    HabboServer.habboGame.itemManager
        .getFurnitureLogic(furnishing)
        .parseStuffData(item.extraData, furnishing, item.limitedItemData)
        .writeFull(habboResponse)

    if (!isWallItem) habboResponse.writeInt(0) // extra
}
