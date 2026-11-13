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

package ovh.rwx.habbo.game.catalog.gift

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogGiftWrappingConfigurationData
import java.time.LocalDateTime

/** Monta a configuração de embrulho (config.yaml + tabela + mobis carregados) para a build de um client. */
object GiftWrappingService {
    fun configuration(buildDate: LocalDateTime): CatalogGiftWrappingConfigurationData {
        val config = HabboServer.habboConfig.catalogConfig.giftWrapping
        val itemManager = HabboServer.habboGame.itemManager
        val types = HabboServer.habboGame.catalogManager.giftWrappingTypes

        return CatalogGiftWrappingConfigurationData(
            enabled = config.enabled,
            price = config.price,
            stuffTypes = itemManager.giftWrapWrapper.map { it.spriteId },
            boxTypes = GiftWrapping.typesFor(types, GiftWrappingKind.BOX, buildDate),
            ribbonTypes = GiftWrapping.typesFor(types, GiftWrappingKind.RIBBON, buildDate),
            defaultStuffTypes = itemManager.giftDefaultWrapper.map { it.spriteId },
        )
    }

    /** Valida o embrulho do pedido contra o que esta build recebeu na configuração. */
    fun resolve(buildDate: LocalDateTime, spriteId: Int, boxId: Int, ribbonId: Int): GiftWrappingChoice? {
        val c = configuration(buildDate)
        return GiftWrapping.resolve(
            spriteId, boxId, ribbonId, c.enabled, c.price, c.stuffTypes, c.defaultStuffTypes, c.boxTypes, c.ribbonTypes
        )
    }
}
