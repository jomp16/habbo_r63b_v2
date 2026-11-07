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

package ovh.rwx.habbo.database

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import ovh.rwx.habbo.metrics.ServerMetrics
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

object DatabaseManager {
    lateinit var jdbi: Jdbi
        private set

    private val slowDbLog = org.slf4j.LoggerFactory.getLogger("ovh.rwx.habbo.room.slowtick")
    const val SLOW_DB_NANOS = 20_000_000L

    /**
     * Chamado ao fim de toda db { } / dbTransaction { }: alimenta a métrica de latência do banco (inclui a espera por
     * uma conexão do pool) e, acima do limite, registra no log de lentidão com os dois frames de quem a originou.
     */
    fun onQueryFinished(startNano: Long) {
        val elapsed = System.nanoTime() - startNano
        ServerMetrics.recordDbQuery(elapsed)
        if (elapsed < SLOW_DB_NANOS || !slowDbLog.isWarnEnabled) return
        val callers = Throwable().stackTrace.asSequence()
            .filter { it.className.startsWith("ovh.rwx") && !it.className.contains("DatabaseManager") && !it.className.contains("JdbiExtensions") }
            .take(2)
            .joinToString(" <- ") { "${it.className.substringAfterLast('.')}.${it.methodName}" }
        slowDbLog.warn("[slow-db] {}ms {} (thread {})", "%.1f".format(elapsed / 1_000_000.0), callers, Thread.currentThread().name)
    }

    private val sqlCache = ConcurrentHashMap<String, String>()

    fun init(dataSource: DataSource): Jdbi {
        val instance = Jdbi.create(dataSource)
            .installPlugin(SqlObjectPlugin())
            .installPlugin(KotlinPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
        jdbi = instance
        return instance
    }

    fun loadSql(path: String): String {
        return sqlCache.computeIfAbsent(path) {
            val resourcePath = if (it.startsWith("/")) it.substring(1) else it
            javaClass.classLoader.getResource(resourcePath)?.readText()
                ?: error("SQL resource not found: $path")
        }
    }
}

/**
 * Executes a block of database operations within a Jdbi handle.
 */
inline fun <R> db(crossinline task: DbSession.() -> R): R {
    val start = System.nanoTime()
    try {
        return DatabaseManager.jdbi.withHandle<R, Exception> { handle ->
            DbSession(handle).task()
        }
    } finally {
        DatabaseManager.onQueryFinished(start)
    }
}

/**
 * Executes a block of database operations within a transactional Jdbi handle.
 */
inline fun <R> dbTransaction(crossinline task: DbSession.() -> R): R {
    val start = System.nanoTime()
    try {
        return DatabaseManager.jdbi.inTransaction<R, Exception> { handle ->
            DbSession(handle).task()
        }
    } finally {
        DatabaseManager.onQueryFinished(start)
    }
}

