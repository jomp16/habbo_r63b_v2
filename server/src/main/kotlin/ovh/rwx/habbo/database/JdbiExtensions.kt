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

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.Query
import org.jdbi.v3.core.statement.Update

fun resolveSql(sqlOrPath: String): String {
    val trimmed = sqlOrPath.trim()
    return if (trimmed.startsWith("sql/") || (trimmed.contains('/') && trimmed.endsWith(".sql"))) {
        DatabaseManager.loadSql(trimmed)
    } else {
        trimmed
    }
}

/**
 * Extension for binding a map of named parameters to a Query, handling nulls and collections.
 */
fun Query.bindMapSafe(parameters: Map<String, Any?>): Query {
    for ((key, value) in parameters) {
        if (value is Collection<*>) {
            this.bindList(key, value.toList())
        } else {
            this.bind(key, value)
        }
    }
    return this
}

/**
 * Extension for binding a map of named parameters to an Update, handling nulls and collections.
 */
fun Update.bindMapSafe(parameters: Map<String, Any?>): Update {
    for ((key, value) in parameters) {
        if (value is Collection<*>) {
            this.bindList(key, value.toList())
        } else {
            this.bind(key, value)
        }
    }
    return this
}

/**
 * Zero-overhead value class wrapping a Jdbi Handle to provide idiomatic database operations.
 */
@JvmInline
value class DbSession(val handle: Handle) {
    inline fun <reified T : Any> query(
        sqlOrPath: String,
        parameters: Map<String, Any?> = emptyMap()
    ): List<T> = selectClass(sqlOrPath, parameters)

    inline fun <reified T : Any> queryOne(
        sqlOrPath: String,
        parameters: Map<String, Any?> = emptyMap()
    ): T? = selectOneClass(sqlOrPath, parameters)

    inline fun <reified T : Any> selectClass(
        sqlOrPath: String,
        parameters: Map<String, Any?> = emptyMap()
    ): List<T> {
        val sql = resolveSql(sqlOrPath)
        return handle.createQuery(sql)
            .bindMapSafe(parameters)
            .mapTo<T>()
            .list()
    }

    inline fun <reified T : Any> selectOneClass(
        sqlOrPath: String,
        parameters: Map<String, Any?> = emptyMap()
    ): T? {
        val sql = resolveSql(sqlOrPath)
        return handle.createQuery(sql)
            .bindMapSafe(parameters)
            .mapTo<T>()
            .findOne()
            .orElse(null)
    }

    fun update(
        sqlOrPath: String,
        parameters: Map<String, Any?> = emptyMap()
    ): Int {
        val sql = resolveSql(sqlOrPath)
        return handle.createUpdate(sql)
            .bindMapSafe(parameters)
            .execute()
    }

    fun insertAndGetGeneratedKey(
        sqlOrPath: String,
        parameters: Map<String, Any?> = emptyMap()
    ): Int {
        val sql = resolveSql(sqlOrPath)
        return handle.createUpdate(sql)
            .bindMapSafe(parameters)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()
    }

    fun insertWithIntGeneratedKey(
        sqlOrPath: String,
        parameters: Map<String, Any?> = emptyMap()
    ): Pair<Int, Int> {
        val id = insertAndGetGeneratedKey(sqlOrPath, parameters)
        return Pair(1, id)
    }

    /** Batch-insert a collection of Kotlin data-class objects using [bindKotlin] — no manual Map needed. */
    fun <T : Any> batchInsert(
        sqlOrPath: String,
        items: Collection<T>
    ): IntArray {
        if (items.isEmpty()) return intArrayOf()
        val sql = resolveSql(sqlOrPath)
        val batch = handle.prepareBatch(sql)
        for (item in items) {
            batch.bindKotlin(item)
            batch.add()
        }
        return batch.execute()
    }

    fun batchUpdate(
        sqlOrPath: String,
        parametersList: List<Map<String, Any?>>
    ): IntArray {
        if (parametersList.isEmpty()) return intArrayOf()
        val sql = resolveSql(sqlOrPath)
        val batch = handle.prepareBatch(sql)
        for (params in parametersList) {
            for ((k, v) in params) {
                batch.bind(k, v)
            }
            batch.add()
        }
        return batch.execute()
    }

    fun batchInsertAndGetGeneratedKeys(
        sqlOrPath: String,
        parametersList: List<Map<String, Any?>>
    ): List<Int> {
        if (parametersList.isEmpty()) return emptyList()
        val sql = resolveSql(sqlOrPath)
        val batch = handle.prepareBatch(sql)
        for (params in parametersList) {
            for ((k, v) in params) {
                batch.bind(k, v)
            }
            batch.add()
        }
        return batch.executePreparedBatch().mapTo(Int::class.java).list()
    }
}
