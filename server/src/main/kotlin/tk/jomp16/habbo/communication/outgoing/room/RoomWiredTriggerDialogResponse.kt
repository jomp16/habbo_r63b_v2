package tk.jomp16.habbo.communication.outgoing.room

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.WiredData
import tk.jomp16.habbo.game.item.room.RoomItem

@Suppress("unused", "UNUSED_PARAMETER")
class RoomWiredTriggerDialogResponse {
    @Response(Outgoing.ROOM_WIRED_TRIGGER_DIALOG)
    fun handle(habboResponse: HabboResponse, roomItem: RoomItem, wiredData: WiredData) {
        habboResponse.apply {
            // todo: add support to all teh triggers
            writeBoolean(false)
            writeInt(0) // todo: selectable items?
            writeInt(0) // todo: selected items?
            writeInt(roomItem.furnishing.spriteId)
            writeInt(roomItem.id)
            writeUTF(wiredData.extra2) // todo: what is this stuff?
            writeInt(0) // todo: another array with int structure
            writeInt(0)
            // end Triggerable

            writeInt(7) // wired type
            writeInt(0) // todo: another array with int structure
        }
    }
}