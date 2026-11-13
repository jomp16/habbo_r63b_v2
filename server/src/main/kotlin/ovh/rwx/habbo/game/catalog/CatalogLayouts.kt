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

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.HabboVersion
import ovh.rwx.habbo.database.catalog.CatalogDao
import java.time.LocalDateTime

/** Um layout de página do catálogo Flash e o que o client dessa faixa de build tem nele (ver V27). */
data class CatalogLayoutWindow(
    val layoutCode: String,
    val availableFrom: LocalDateTime,
    val availableUntil: LocalDateTime,
    /** Elementos de imagem que o layout possui (nomes da tabela padrão [CatalogLayouts.DEFAULT_IMAGE_ELEMENTS]). */
    val imageElements: Set<String>,
    /** Quantos textos o client lê para esse layout (tamanho da tabela de nomes, no máximo 4). */
    val textCount: Int
) {
    fun isAvailableAt(buildDate: LocalDateTime): Boolean =
        !buildDate.isBefore(availableFrom) && buildDate.isBefore(availableUntil)
}

/** O layout que o servidor deve mandar para a release e o que cabe nele. */
data class ResolvedCatalogLayout(val layoutCode: String, val imageElements: Set<String>, val textCount: Int) {
    /**
     * Mantém só as imagens cujo elemento existe no layout; as outras viram `""` (o client ignora imagem vazia).
     * A imagem N é o elemento N da tabela padrão do client.
     */
    fun pruneImages(images: List<String>): List<String> = images.mapIndexed { index, image ->
        if (CatalogLayouts.DEFAULT_IMAGE_ELEMENTS.getOrNull(index) in imageElements) image else ""
    }

    /**
     * Os campos da página que o client lê, na ordem das posições dele: 1 = text1; 2 = text1, details;
     * 3 = text1, details, teaser; 4 (ou mais) = text1, text2, details, teaser.
     */
    fun <T> pickTexts(text1: T, text2: T, details: T, teaser: T): List<T> = when (textCount) {
        0 -> emptyList()
        1 -> listOf(text1)
        2 -> listOf(text1, details)
        3 -> listOf(text1, details, teaser)
        else -> listOf(text1, text2, details, teaser)
    }
}

/**
 * A regra de resolução, sem banco: escolhe o layout que existe na release (seguindo os fallbacks) e devolve o que ele
 * tem. `null` = a tabela não cobre a build (os clients de 2014 em diante carregam os layouts de fora do SWF e já
 * checam null): o chamador manda a página como está.
 */
class CatalogLayoutTable(windows: List<CatalogLayoutWindow>, private val fallbacks: Map<String, String>) {
    private val byLayout = windows.groupBy { it.layoutCode }
    private val coverageStart = windows.minOfOrNull { it.availableFrom }
    private val coverageEnd = windows.maxOfOrNull { it.availableUntil }

    fun covers(buildDate: LocalDateTime): Boolean =
        coverageStart != null && coverageEnd != null && !buildDate.isBefore(coverageStart) && buildDate.isBefore(coverageEnd)

    fun resolve(buildDate: LocalDateTime, requested: String): ResolvedCatalogLayout? {
        if (!covers(buildDate)) return null
        var candidate = requested
        val seen = HashSet<String>()
        while (seen.add(candidate)) {
            find(buildDate, candidate)?.let { return it }
            candidate = fallbacks[candidate] ?: break
        }
        return find(buildDate, CatalogLayouts.DEFAULT_LAYOUT)
    }

    private fun find(buildDate: LocalDateTime, layout: String): ResolvedCatalogLayout? =
        byLayout[layout]?.firstOrNull { it.isAvailableAt(buildDate) }
            ?.let { ResolvedCatalogLayout(it.layoutCode, it.imageElements, it.textCount) }
}

/**
 * Layouts de página do catálogo Flash por release (tabelas `catalog_layouts` e `catalog_layout_fallbacks`, V27).
 *
 * Substitui o que antes eram regras fixas em código: quais layouts a release tem (senão a página abre em branco),
 * quantos textos cada um lê e quais imagens ele comporta. O `LocalizationCatalogWidget` faz
 * `findChildByName(nome).tags` sem checar null nas releases de 2009-06 a 2013-06: imagem não vazia numa posição que o
 * layout não tem derrubava o update receiver do client (TypeError ao abrir a página, ex.: o HC).
 */
object CatalogLayouts {
    private val log = LoggerFactory.getLogger(CatalogLayouts::class.java)

    const val DEFAULT_LAYOUT = "default_3x3"

    /** Ordem fixa da tabela do client (`_SafeStr_8470`): a posição N da lista de imagens do pacote é o elemento N. */
    val DEFAULT_IMAGE_ELEMENTS = listOf(
        "ctlg_header_img", "ctlg_teaserimg_1", "ctlg_special_img", "ctlg_teaserimg_2", "ctlg_teaserimg_3"
    )

    @Volatile
    private var table: CatalogLayoutTable? = null

    private fun loaded(): CatalogLayoutTable = table ?: synchronized(this) {
        table ?: runCatching { CatalogLayoutTable(CatalogDao.getCatalogLayouts(), CatalogDao.getCatalogLayoutFallbacks()) }
            .onFailure { log.error("Falha ao carregar catalog_layouts; as páginas vão como estão.", it) }
            .getOrDefault(CatalogLayoutTable(emptyList(), emptyMap()))
            .also { table = it }
    }

    /** Recarrega as tabelas (ex.: depois de ajustar uma linha no banco). */
    fun reload() {
        table = null
        loaded()
    }

    fun resolve(version: HabboVersion, requested: String): ResolvedCatalogLayout? =
        loaded().resolve(version.buildDate, requested)
}
