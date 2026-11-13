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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogPurchaseOkData
import ovh.rwx.habbo.communication.outgoing.inventory.UnseenItemCategory
import ovh.rwx.habbo.communication.outgoing.inventory.UnseenItemsData
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.game.currency.CurrencyService

class CatalogPetService {

    private val petNameRegex = Regex("^[a-zA-Z0-9 ]+$")

    fun purchasePet(habboSession: HabboSession, catalogItem: CatalogItem, extraData: String) {
        val parts = extraData.split("\n")
        if (parts.size != 3) return

        val petName = parts[0]
        val petRace = parts[1].toIntOrNull() ?: return
        val petColor = parts[2]

        val config = HabboServer.habboConfig.petConfig
        if (petName.length !in config.nameMinLength..config.nameMaxLength) return
        if (!petName.matches(petNameRegex)) return

        // Extract pet type from item name: "a0 pet0" → 0
        val petType = HabboServer.habboGame.petManager.extractPetType(catalogItem.catalogName) ?: return

        // Moeda precisa existir para o client e ter saldo; cobra de forma atômica (o saldo é reenviado pelo CurrencyService)
        val price = catalogItem.price
        if (!CurrencyService.isSupported(habboSession, price) || !CurrencyService.tryCharge(habboSession, price).ok) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_PURCHASE_ERROR, 0)
            return
        }

        // Falhou depois da cobrança: devolve o valor
        val petId = try {
            PetDao.insertPet(habboSession.userInformation.id, petName, petType, petRace, petColor)
        } catch (e: Throwable) {
            CurrencyService.refund(habboSession, price)
            throw e
        }
        val petData = PetDao.getPetById(petId) ?: run {
            CurrencyService.refund(habboSession, price)
            return
        }

        habboSession.habboInventory.addPet(petData, openInventory = true)

        // todo: give 1 free pet food

        habboSession.sendResponse(
            Outgoing.INVENTORY_UNSEEN_ITEMS,
            OutgoingR63A.INVENTORY_UNSEEN_ITEMS,
            UnseenItemsData.single(UnseenItemCategory.PET, listOf(petId))
        )
        habboSession.sendResponse(Outgoing.PET_BOUGHT_NOTIFICATION, null, petData, false)
        habboSession.sendResponse(
            Outgoing.CATALOG_PURCHASE_OK,
            OutgoingR63A.CATALOG_PURCHASE_OK,
            CatalogPurchaseOkData(catalogItem, emptyList())
        )
    }
}
