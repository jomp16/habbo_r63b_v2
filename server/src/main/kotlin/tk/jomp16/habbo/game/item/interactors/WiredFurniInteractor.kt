package tk.jomp16.habbo.game.item.interactors

import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class WiredFurniInteractor : ItemInteractor() {
    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (!hasRights || roomItem.wiredData == null) return

        val headerId =
                if (roomItem.furnishing.interactionType.name.startsWith("WIRED_TRIGGER")) Outgoing.ROOM_WIRED_TRIGGER_DIALOG
                else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_ACTION")) Outgoing.ROOM_WIRED_EFFECT_DIALOG
                else if (roomItem.furnishing.interactionType.name.startsWith("WIRED_CONDITION")) Outgoing.ROOM_WIRED_EFFECT_DIALOG
                else -1

        if (headerId != -1) roomUser?.habboSession?.sendHabboResponse(headerId, roomItem, roomItem.wiredData)
    }
}