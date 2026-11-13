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

import ovh.rwx.habbo.communication.HabboVersion

/**
 * Quais campos de uma oferta do catálogo (e dos produtos dela) o client de cada build lê. Um byte a mais ou a menos
 * desalinha o pacote inteiro ("Failed to parse incoming message ... CatalogPageMessageParser").
 *
 * Provado no AS3 com `habbo-timeline CatalogPageMessageEvent incoming` (campo → build que o introduz):
 * - `giftable` (boolean depois do tipo da moeda): 201111082302
 * - produto-emblema (`b`): 201201242303. A build 201201241118 do mesmo dia ainda não o lê, por isso o corte é 2012-01-25
 *   (um emblema a menos na 201201242303 é melhor que um pacote corrompido na 201201241118).
 * - `bundlePurchaseAllowed` (boolean depois do `clubLevel`): 201201302302
 * - produto: `isUnique` (e o tamanho/restante da série limitada): 201203202303
 * - produto: `expiration` (Int): existe desde o início e sai em 201301301102
 *
 * Antes de 201111082302 a oferta é `id | nome | créditos | pontos | tipo | produtos | clubLevel`.
 */
class CatalogOfferWire(version: HabboVersion) {
    val giftable = version.isVersionAtLeast(2011, 11, 8)
    val badgeProduct = version.isVersionAtLeast(2012, 1, 25)
    val bundlePurchaseAllowed = version.isVersionAtLeast(2012, 1, 30)
    val productUniqueFlag = version.isVersionAtLeast(2012, 3, 20)
    val productExpiration = version.isVersionBefore(2013, 1, 30)
}
