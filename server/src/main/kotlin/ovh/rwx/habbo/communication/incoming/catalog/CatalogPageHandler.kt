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

package ovh.rwx.habbo.communication.incoming.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogPageHandler {
    private fun validateAndGetCatalogPage(
        habboSession: HabboSession,
        pageId: Int,
        chosenOfferId: Int,
        category: String
    ) =
        HabboServer.habboGame.catalogManager.catalogPages.find { it.id == pageId }?.takeIf {
            it.enabled && it.visible &&
                    habboSession.userInformation.rank >= it.minRank &&
                    it.pageLayout != "category" &&
                    (!it.clubOnly || (habboSession.userInformation.vip && habboSession.habboSubscription.validUserSubscription))
        }

    @Handler(Incoming.CATALOG_PAGE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val pageId = habboRequest.readInt()
        val chosenOfferId = habboRequest.readInt()
        val category = habboRequest.readUTF()

        validateAndGetCatalogPage(habboSession, pageId, chosenOfferId, category)?.let {
            val habboAir = false
            habboSession.sendHabboResponse(Outgoing.CATALOG_PAGE, it, chosenOfferId, habboAir)
        }
    }

    @Handler(Incoming.CATALOG_PAGE)
    fun handleHabboAir(habboSession: HabboSession, habboRequest: HabboRequest) {
        val pageId = habboRequest.readInt()
        val chosenOfferId = habboRequest.readInt()
        val category = habboRequest.readUTF()

        validateAndGetCatalogPage(habboSession, pageId, chosenOfferId, category)?.let {
            val habboAir = true
            habboSession.sendHabboResponse(Outgoing.CATALOG_PAGE, it, chosenOfferId, habboAir)
        }
    }

    @HandlerR63A(IncomingR63A.CATALOG_PAGE)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        val pageId = habboRequest.readInt()
        val chosenOfferId = habboRequest.readInt()
        val category = "NORMAL"

        validateAndGetCatalogPage(habboSession, pageId, chosenOfferId, category)?.let {
            habboSession.sendHabboResponse(OutgoingR63A.CATALOG_PAGE, it, chosenOfferId)
        }
    }
}