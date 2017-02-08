package tk.jomp16.habbo.game.item.interactors

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class WiredFurniInteractor : ItemInteractor() {
    override fun onPlace(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onPlace(room, roomUser, roomItem)

        roomItem.extraData = "0"
    }

    override fun onRemove(room: Room, roomUser: RoomUser?, roomItem: RoomItem) {
        super.onRemove(room, roomUser, roomItem)

        roomItem.extraData = "0"
    }

    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (!hasRights || roomItem.wiredData == null) return

        roomItem.extraData = "1"
        roomItem.update(false, true)
        roomItem.requestCycles(1)

        val headerId =
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_TRIGGER")) Outgoing.ROOM_WIRED_TRIGGER_DIALOG
                else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_ACTION")) Outgoing.ROOM_WIRED_EFFECT_DIALOG
                else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) Outgoing.ROOM_WIRED_EFFECT_DIALOG
                else -1

        if (headerId != -1) roomUser?.habboSession?.sendHabboResponse(headerId, roomItem, roomItem.wiredData)
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)

        if (roomItem.extraData == "1") {
            roomItem.extraData = "0"

            roomItem.update(false, true)
        }
    }
}