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

package ovh.rwx.habbo.game

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.camera.AssetMapping
import ovh.rwx.habbo.game.item.ItemManager
import ovh.rwx.habbo.game.item.xml.FurniXMLInfo
import ovh.rwx.habbo.util.ClassIndex
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Cold start: o que não depende do banco começa logo depois do `external_variables` e roda enquanto o pool, o Flyway e
 * os handlers esperam o banco remoto (~7 s parados antes). Hoje: o `asset_mapping.json` (disco) e a furnidata (HTTP).
 *
 * Só serve o boot: cada resultado é entregue uma vez; no reload (ou se o prefetch falhou) o dono volta a ler sozinho.
 * Threads daemon: se o boot falhar, não seguram a JVM.
 */
object BootPrefetch {
    private val log = LoggerFactory.getLogger(javaClass)

    private val assetMapping = AtomicReference<CompletableFuture<AssetMapping>?>()
    private val furnidata = AtomicReference<CompletableFuture<Map<String, FurniXMLInfo>>?>()

    fun start(assetsPath: String) {
        assetMapping.set(async("asset-mapping") { AssetMapping.load(assetsPath) })
        furnidata.set(async("furnidata") { ItemManager.fetchFurniXml() })
        // Só aquecimento (ninguém espera): carrega do JAR, sem inicializar, as ~1.000 classes do índice (handlers,
        // responses, interactors, wireds) e a reflexão delas, que o HabboHandler e o ItemManager usam logo depois
        // do banco. Num HDD lento isso eram segundos de leitura no caminho serial do boot.
        async("classes") {
            ClassIndex.Kind.entries.sumOf { kind -> ClassIndex.classesOf(kind).onEach { it.declaredMethods }.size }
        }
    }

    /** O mapping carregado em paralelo (espera se ainda não terminou), ou `null` se não houve prefetch ou ele falhou. */
    fun takeAssetMapping(): AssetMapping? = take(assetMapping, "asset mapping")

    /** A furnidata baixada em paralelo (espera se ainda não terminou), ou `null` se não houve prefetch ou ele falhou. */
    fun takeFurnidata(): Map<String, FurniXMLInfo>? = take(furnidata, "furnidata")

    private fun <T> take(ref: AtomicReference<CompletableFuture<T>?>, name: String): T? {
        val future = ref.getAndSet(null) ?: return null

        return runCatching { future.join() }
            .onFailure { log.warn("Boot prefetch of {} unavailable; loading it again", name) }
            .getOrNull()
    }

    private fun <T> async(name: String, block: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        thread(isDaemon = true, name = "boot-prefetch-$name") {
            val start = System.nanoTime()
            runCatching(block)
                .onSuccess {
                    log.info("Boot prefetch of {} finished in {}ms", name, (System.nanoTime() - start) / 1_000_000)
                    future.complete(it)
                }
                .onFailure {
                    log.warn("Boot prefetch of {} failed", name, it)
                    future.completeExceptionally(it)
                }
        }

        return future
    }
}
