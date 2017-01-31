package tk.jomp16.habbo.game.item.interactors

import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser

class GateFurniInteractor : ItemInteractor() {
    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (!hasRights) return

        if (roomItem.canClose()) {
            roomItem.affectedTiles.forEach { room.roomGamemap.blockedItem[it.x][it.y] = roomItem.extraData != "0" }

            roomItem.extraData = if (roomItem.extraData == "0") "1" else "0"

            roomItem.update(true, true)
        }

        // todo: wired

        /*if (roomUser != null) {
            room.getWiredHandler().triggerWired(WiredTriggerStateChanged::class.java, roomUser, roomItem)
        }*/
    }
}