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

import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.chest.ChestItemsUpdatedData
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.logic.ChestFurnitureLogic
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.stuff.StuffData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

object ChestVisuals {

    fun setState(room: Room, chest: ChestData, open: Boolean) {
        val stateMode = chest.stateMode

        // stateMode: 0 = abre quando observado, 1 = sempre aberto, 2 = sempre fechado, 3 = controle Wired
        val shouldOpen = when (stateMode) {
            1 -> true
            2 -> false
            else -> open
        }

        val roomItem = room.itemManager.items[chest.itemId] ?: return
        val values = ChestFurnitureLogic.parseChestExtraData(roomItem.extraData)

        // Estado aberto é efêmero (client-only): o banco tem SEMPRE o baú fechado
        values[StuffData.KEY_STATE] = if (shouldOpen) "1" else "0"
        values[ChestFurnitureLogic.KEY_VISUALS] = if (shouldOpen) getPreviewVisuals(chest) else ""
        applyChestValues(values, chest)

        roomItem.extraData = ChestFurnitureLogic.formatChestExtraData(values)
        // Quando fecha, salva no banco (state=0); quando abre, é efêmero no client
        roomItem.update(updateDb = !shouldOpen, updateClient = true)
    }

    fun updateChestExtraData(room: Room, chest: ChestData, commitClient: Boolean = true) {
        val roomItem = room.itemManager.items[chest.itemId] ?: return
        val values = ChestFurnitureLogic.parseChestExtraData(roomItem.extraData)
        val isOpen = values[StuffData.KEY_STATE] == "1"

        applyChestValues(values, chest)

        // Se estiver aberto e puder permanecer aberto, mantém os previews visuais no client
        if (isOpen && chest.stateMode != 2 && !chest.locked) {
            values[StuffData.KEY_STATE] = "1"
            values[ChestFurnitureLogic.KEY_VISUALS] = getPreviewVisuals(chest)
        } else {
            values[StuffData.KEY_STATE] = if (chest.stateMode == 1) "1" else "0"
            values[ChestFurnitureLogic.KEY_VISUALS] = if (chest.stateMode == 1) getPreviewVisuals(chest) else ""
        }

        roomItem.extraData = ChestFurnitureLogic.formatChestExtraData(values)
        // RoomDao.saveItems sempre sanitiza chests para state=0 no banco (a menos que stateMode=1)
        if (commitClient) roomItem.update(updateDb = true, updateClient = true)
    }

    fun applyChestValues(values: LinkedHashMap<String, String>, chest: ChestData) {
        values[ChestFurnitureLogic.KEY_IS_WIRED_ENABLED] = if (chest.isWired) "1" else "0"
        values[ChestFurnitureLogic.KEY_CHEST_NAME] = chest.name
        values[ChestFurnitureLogic.KEY_CHEST_DESC] = chest.description
        values[ChestFurnitureLogic.KEY_EVERYONE_CAN_OPEN] = if (chest.anyoneCanOpen) "1" else "0"
        values[ChestFurnitureLogic.KEY_EVERYONE_CAN_DONATE] = if (chest.anyoneCanDonate) "1" else "0"
        values[ChestFurnitureLogic.KEY_STATE_CONTROL_MODE] = chest.stateMode.toString()
        values[ChestFurnitureLogic.KEY_PREVIEW_MODE] = chest.previewMode.toString()
        values[ChestFurnitureLogic.KEY_PREVIEW_AMOUNT] = chest.previewAmount.toString()
        values[ChestFurnitureLogic.KEY_NOTIFY_MODE] = chest.notificationMode.toString()
        values[ChestFurnitureLogic.KEY_NOTIFICATION_CHEST_FULL] = if (chest.notifyFull) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_DONATION] = if (chest.notifyDonation) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_SOMEONE_WITHDRAWS] = if (chest.notifyWithdraw) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_CHEST_EMPTY] = if (chest.notifyEmpty) "1" else "0"
        values[ChestFurnitureLogic.KEY_NOTIFICATION_WIRED_TRANSACTION] = if (chest.notifyTransaction) "1" else "0"
        values[ChestFurnitureLogic.KEY_CONTENTS_COUNT] = chest.entries.size.toString()
        values[ChestFurnitureLogic.KEY_CONTENTS_COINS] = chest.coins.toString()
    }

    /**
     * Pré-visualização dos itens quando o baú está aberto.
     * previewMode: 0 nenhuma, 1-2 aleatórios, 3-4 mais recentes, 5-6 mais antigos, 7 Wired.
     * Modos pares (2/4/6) preferem tipos diferentes; 7 (fila Wired) ainda não existe -> aleatório.
     */
    fun getPreviewVisuals(chest: ChestData): String {
        if (chest.previewMode == 0) return ""

        val distinctTypes = chest.previewMode in 2..6 && chest.previewMode % 2 == 0
        val amount = chest.previewAmount.coerceAtLeast(1)

        val candidates = when (chest.previewMode) {
            3, 4 -> chest.entries.sortedByDescending { it.chestItemId }
            5, 6 -> chest.entries.sortedBy { it.chestItemId }
            else -> chest.entries.shuffled()
        }

        val selected = mutableListOf<ChestEntry>()
        val seenTypes = mutableSetOf<Pair<Boolean, Int>>()

        for (entry in candidates) {
            if (selected.size >= amount) break

            val furnishing = entry.item.furnishing
            val typeKey = (furnishing.type == ItemType.WALL) to furnishing.spriteId

            if (distinctTypes) {
                if (!seenTypes.add(typeKey)) continue
            }

            selected += entry
        }

        return selected.joinToString(ChestFurnitureLogic.VISUALS_ITEM_SEPARATOR) { entry ->
            val furnishing = entry.item.furnishing
            val isWallItem = furnishing.type == ItemType.WALL
            val legacyPosterId = if (entry.item.itemName.contains("poster")) entry.item.extraData else ""

            itemTypeToString(isWallItem, furnishing.spriteId, legacyPosterId)
        }
    }

    fun sendContentsUpdated(
        room: Room,
        chest: ChestData,
        removedIds: List<Int>,
        addedEntries: List<ChestEntry>
    ) {
        room.userManager.entities.values
            .filterIsInstance<RoomUser>()
            .map { it.habboSession }
            .forEach { session ->
                session.sendHabboResponse(
                    Outgoing.CHEST_ITEMS_UPDATED,
                    ChestItemsUpdatedData(
                        chestItemId = chest.itemId,
                        removedIds = removedIds,
                        addedEntries = addedEntries,
                    )
                )
            }
    }

    fun formatForUnload(roomItem: RoomItem, chest: ChestData, autoLockTriggered: Boolean): Boolean {
        var needsSave = autoLockTriggered
        val values = ChestFurnitureLogic.parseChestExtraData(roomItem.extraData)

        if (chest.stateMode != 1) {
            if (values[StuffData.KEY_STATE] != "0" || !values[ChestFurnitureLogic.KEY_VISUALS].isNullOrEmpty()) {
                values[StuffData.KEY_STATE] = "0"
                values[ChestFurnitureLogic.KEY_VISUALS] = ""
                needsSave = true
            }
            applyChestValues(values, chest)
            roomItem.extraData = ChestFurnitureLogic.formatChestExtraData(values)
        } else if (needsSave) {
            applyChestValues(values, chest)
            roomItem.extraData = ChestFurnitureLogic.formatChestExtraData(values)
        }

        return needsSave
    }
}
