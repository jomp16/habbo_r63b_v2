package tk.jomp16.habbo.game.item.interactors

import tk.jomp16.habbo.game.item.ItemInteractor
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.util.Utils

class VendorFurniInteractor : ItemInteractor() {
    override fun onTrigger(room: Room, roomUser: RoomUser?, roomItem: RoomItem, hasRights: Boolean, request: Int) {
        super.onTrigger(room, roomUser, roomItem, hasRights, request)

        if (roomUser == null) return

        if (!roomItem.isTouching(roomUser.currentVector3, roomUser.bodyRotation)) {
            roomUser.moveTo(roomItem.getFrontPosition(), roomItem.getFrontRotation(), actingItem = roomItem)

            return
        }

        roomItem.extraData = "1"
        roomItem.update(false, true)

        roomUser.vendingMachine(roomItem.furnishing.vendingIds[Utils.randInt(0..roomItem.furnishing.vendingIds.size - 1)])

        roomItem.requestCycles(2)

        // todo: wired
        // room.getWiredHandler().triggerWired(WiredTriggerStateChanged::class.java, roomUser, roomItem)
    }

    override fun onCycle(room: Room, roomItem: RoomItem) {
        super.onCycle(room, roomItem)

        roomItem.extraData = "0"
        roomItem.update(false, true)
    }
}