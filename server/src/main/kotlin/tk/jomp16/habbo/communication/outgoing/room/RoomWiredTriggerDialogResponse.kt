package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.WiredData
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeBlockedActions
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeEmptyItems
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeEmptySettings
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeItemInfo
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeItems
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeSettings

@Suppress("unused", "UNUSED_PARAMETER")
class RoomWiredTriggerDialogResponse {
    @Response(Outgoing.ROOM_WIRED_TRIGGER_DIALOG)
    fun handle(habboResponse: HabboResponse, roomItem: RoomItem, wiredData: WiredData) {
        habboResponse.apply {
            writeBoolean(false)

            parseWired(roomItem, wiredData)
        }
    }

    private fun HabboResponse.parseWired(roomItem: RoomItem, wiredData: WiredData) {
        when (roomItem.furnishing.interactionType) {
            InteractionType.WIRED_TRIGGER_ENTER_ROOM -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(7)
                writeBlockedActions()
            }
            InteractionType.WIRED_TRIGGER_SAYS_SOMETHING -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, wiredData.options, 1)
                writeInt(0)
                writeBlockedActions()
            }
            InteractionType.WIRED_TRIGGER_STATE_CHANGED -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(4)
                writeBlockedActions()
            }
            InteractionType.WIRED_TRIGGER_WALKS_ON_FURNI,
            InteractionType.WIRED_TRIGGER_WALKS_OFF_FURNI -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptyItems()
                writeInt(1)
                writeBlockedActions()
            }
            else -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(0)
                writeBlockedActions()
            }
        }
    }
}