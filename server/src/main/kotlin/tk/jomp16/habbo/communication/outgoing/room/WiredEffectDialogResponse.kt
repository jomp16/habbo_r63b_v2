package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.WiredData
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeBlockedTriggers
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeDelay
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeEmptyItems
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeEmptySettings
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeItemInfo
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeItems
import tk.jomp16.habbo.game.item.wired.WiredItem.Companion.writeSettings

@Suppress("unused", "UNUSED_PARAMETER")
class WiredEffectDialogResponse {
    @Response(Outgoing.ROOM_WIRED_EFFECT_DIALOG)
    fun handle(habboResponse: HabboResponse, roomItem: RoomItem, wiredData: WiredData) {
        habboResponse.apply {
            // todo: add support to all teh effects
            writeBoolean(false)

            parseWired(roomItem, wiredData)
        }
    }

    private fun HabboResponse.parseWired(roomItem: RoomItem, wiredData: WiredData) {
        when (roomItem.furnishing.interactionType) {
            InteractionType.WIRED_ACTION_TOGGLE_STATE,
            InteractionType.WIRED_ACTION_TELEPORT_TO -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(8)
                writeDelay(wiredData)
                writeBlockedTriggers()
            }
            InteractionType.WIRED_ACTION_KICK_USER -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(7)
                writeDelay(wiredData)
                writeBlockedTriggers()
            }
            InteractionType.WIRED_ACTION_MOVE_ROTATE -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeSettings("", wiredData.options, 2)
                writeInt(4)
                writeDelay(wiredData)
                writeBlockedTriggers()
            }
            InteractionType.WIRED_ACTION_MATCH_TO_SCREENSHOT -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeSettings("", wiredData.options, 3)
                writeInt(3)
                writeDelay(wiredData)
                writeBlockedTriggers()
            }
            InteractionType.WIRED_ACTION_CALL_STACK -> {
                writeItems(wiredData)
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(18)
                writeDelay(wiredData)
                writeBlockedTriggers()
            }
            InteractionType.WIRED_ACTION_SHOW_MESSAGE -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeSettings(wiredData.message, emptyList(), 0)
                writeInt(7)
                writeDelay(wiredData)
                writeBlockedTriggers()
            }
            else -> {
                writeEmptyItems()
                writeItemInfo(roomItem)
                writeEmptySettings()
                writeInt(0)
                writeDelay(wiredData)
                writeBlockedTriggers()
            }
        }
    }
}