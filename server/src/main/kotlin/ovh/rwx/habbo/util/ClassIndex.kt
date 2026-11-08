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

package ovh.rwx.habbo.util

/**
 * Índice estático de classes gerado em build-time pela task `generateClassIndex`
 * (`build-logic/GenerateClassIndexTask`), que substitui o scan de classpath do boot.
 *
 * O arquivo é relido a cada [classesOf]: em dev, `compilar` regenera o índice e o próximo
 * `reload_handlers` já enxerga o handler novo, sem reiniciar.
 */
internal object ClassIndex {
    enum class Kind {
        HANDLER, HANDLER_R63A, RESPONSE, RESPONSE_R63A,
        FF_HANDLER, FF_RESPONSE,
        WIRED_ITEM, ITEM_INTERACTOR, FURNITURE_LOGIC,
        PLUGIN_LISTENER,
    }

    private const val RESOURCE = "META-INF/habbo/class-index.txt"

    fun classesOf(kind: Kind): List<Class<*>> {
        val classLoader = ClassIndex::class.java.classLoader
        val urls = classLoader.getResources(RESOURCE).toList()
        check(urls.isNotEmpty()) {
            "Índice de classes '$RESOURCE' não encontrado no classpath — rode ./gradlew :server:generateClassIndex (ou compile via Gradle)"
        }

        return urls.asSequence()
            .flatMap { url -> url.openConnection().apply { useCaches = false }.getInputStream().bufferedReader().use { it.readLines() } }
            .filter { it.isNotBlank() }
            .map { it.split('\t', limit = 2) }
            .filter { it[0] == kind.name }
            .map { it[1] }
            .distinct()
            .map { Class.forName(it, false, classLoader) }
            .toList()
    }
}
