package tk.jomp16.habbo.communication.outgoing.catalog

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPurchaseRentalDialogResponse {
    @Response(Outgoing.CATALOG_PURCHASE_RENTAL_DIALOG)
    fun handle(habboResponse: HabboResponse, itemName: String, credits: Int, points: Int, pointsType: Int) {
        habboResponse.apply {
            writeBoolean(false) // useless
            writeUTF(itemName) // item name?
            writeBoolean(true) // buyout - ???
            writeInt(credits) // credits
            writeInt(points) // points
            writeInt(pointsType) // points type
        }
    }
}