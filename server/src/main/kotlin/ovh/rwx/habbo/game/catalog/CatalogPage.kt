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

import org.apache.commons.lang3.StringUtils
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize

data class CatalogPage(
    val id: Int,
    private val parentId: Int,
    val name: String,
    private val codename: String,
    private val iconImage: Int,
    val visible: Boolean,
    val enabled: Boolean,
    val minRank: Int,
    val clubOnly: Boolean,
    private val orderNum: Int,
    val pageLayout: String,
    val pageHeadline: String,
    val pageTeaser: String,
    val pageSpecial: String,
    val pageText1: String,
    val pageText2: String,
    val pageTextDetails: String,
    val pageTextTeaser: String,
    val pageLinkDescription: String,
    val pageLinkPagename: String,
    val customData: Map<String, Any>
) : IHabboResponseSerialize {
    val catalogItems: List<CatalogItem>
        get() = HabboServer.habboGame.catalogManager.catalogItems.filter { it.pageId == id }.sortedBy { it.id }
            .sortedBy { it.orderNum }
    private val offerItems: List<CatalogItem>
        get() = catalogItems.filter { it.offerId != -1 }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val rank = params[0] as Int
            val club = params[1] as Boolean

            writeBoolean(visible)
            writeInt(iconImage)
            writeInt(if (pageLayout == "category") -1 else id)
            writeUTF(codename)

            if (HabboServer.habboConfig.catalogConfig.showHowManyItemsInTitle && pageLayout != "category" && parentId != -1) writeUTF(
                "$name (${catalogItems.size})"
            )
            else writeUTF(name)

            writeInt(offerItems.size)
            offerItems.forEach {
                writeInt(it.offerId)
            }
            val childCatalogPages =
                HabboServer.habboGame.catalogManager.catalogPages.filter { it.parentId == id && it.enabled && it.visible && it.minRank <= rank && if (it.clubOnly) club else true }
                    .sortedBy { it.orderNum }
            writeInt(childCatalogPages.size)

            childCatalogPages.forEach {
                it.serializeHabboResponse(habboResponse, rank, club)
            }

            if (id == -1 || id == -2) {
                writeBoolean(false)
                writeUTF(if (id == -1) "NORMAL" else "BUILDERS_CLUB")
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val rank = params[0] as Int
            val club = params[1] as Boolean

            writeBoolean(visible)
            writeInt(0) // todo: icon color
            writeInt(iconImage) // icon
            writeInt(id)

            if (HabboServer.habboConfig.catalogConfig.showHowManyItemsInTitle && pageLayout != "category" && parentId != -1) writeUTF(
                "${StringUtils.stripAccents(name)} (${catalogItems.size})"
            )
            else writeUTF(StringUtils.stripAccents(name))

            // CASO ESPECIAL: Se eu sou a página ROOT (-1), eu busco os NETOS
            if (id == -1) {
                val level1Nodes = HabboServer.habboGame.catalogManager.catalogPages.filter {
                    it.parentId == -1 &&
                            it.enabled && it.visible && it.minRank <= rank &&
                            if (it.clubOnly) club else true
                }

                val level1Ids = level1Nodes.map { it.id }

                // 2. Busca os "Netos" (Nível 2) que são filhos desses IDs (Ex: ID 14 filho de 2, ID 55 filho de 6)
                val level2Nodes = HabboServer.habboGame.catalogManager.catalogPages
                    .filter { page ->
                        level1Ids.contains(page.parentId) &&
                                page.enabled && page.visible && page.minRank <= rank &&
                                if (page.clubOnly) club else true
                    }

                // 3. Identifica quais IDs do nível 1 serviram de "Pasta"
                // (Ex: ID 2 e 6 estão aqui porque seus filhos entraram na lista level2Nodes)
                val parentIdsThatAreFolders = level2Nodes.map { it.parentId }.distinct()

                // 4. Filtra os "Orfãos" do Nível 1 (Páginas Reais)
                // Mantemos o ID 1 aqui porque ele NÃO está na lista de 'parentIdsThatAreFolders'
                val orphansLevel1 = level1Nodes.filter { !parentIdsThatAreFolders.contains(it.id) }

                // 5. Une as duas listas: (Página Principal) + (Conteúdo das Pastas)
                val finalPages = (orphansLevel1 + level2Nodes)
                    .sortedWith(compareBy({
                        // Lógica de Ordenação:
                        // Se for orfão (ID 1), usa o próprio ID como agrupador (ou -1 para garantir topo)
                        // Se for neto (ID 14), usa o ID do pai (2) para manter agrupado com sua categoria original
                        if (it.parentId == -1) it.id else it.parentId
                    }, { it.orderNum }))

                writeInt(finalPages.size)

                finalPages.forEach { page ->
                    // Truque: Se já for filho de -1 (Principal), manda normal.
                    // Se for neto, manda uma cópia com parentId -1.
                    val pageToSend = if (page.parentId == -1) page else page.copy(parentId = -1)
                    serialize(pageToSend, rank, club)
                }
            } else {
                // CASO PADRÃO: Se eu sou uma página normal (ex: 14), busco meus filhos normais (ex: 35)
                val directChildren = HabboServer.habboGame.catalogManager.catalogPages
                    .filter { page ->
                        page.parentId == id && // Busca filhos deste ID atual
                                page.enabled && page.visible && page.minRank <= rank &&
                                if (page.clubOnly) club else true
                    }
                    .sortedBy { it.orderNum }

                writeInt(directChildren.size)

                directChildren.forEach { page ->
                    // Serialização normal, sem fake
                    serialize(page, rank, club)
                }
            }

            if (id == -1 || id == -2) {
                writeBoolean(false) // newAdditionsAvailable -> link to catalog.page.latest_added
            }
        }
    }
}