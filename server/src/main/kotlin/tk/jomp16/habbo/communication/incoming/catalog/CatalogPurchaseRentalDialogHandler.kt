package tk.jomp16.habbo.communication.incoming.catalog

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseRentalDialogHandler {
    @Handler(Incoming.CATALOG_PURCHASE_RENTAL_DIALOG)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        // stub

        habboRequest.readBoolean() // useless?

        val itemName = habboRequest.readUTF()

        habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_RENTAL_DIALOG, itemName, 16, 0, 0)
    }
}