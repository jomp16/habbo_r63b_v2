/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.kotlin

import com.github.andrewoma.kwery.core.Row
import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.StatementOptions
import java.time.LocalDate
import java.time.LocalDateTime


/**
 * Inserts and fetch the rows affected and the generated key
 */
fun Session.insertWithIntGeneratedKey(sql: String, parameters: Map<String, Any?> = mapOf(), options: StatementOptions = defaultOptions, columnName: String = "GENERATED_KEY"): Pair<Int, Int> = insert(sql, parameters, options) { it.int(columnName) }

/**
 * Inserts and fetch the rows affected and the generated key
 */
fun Session.insertAndGetGeneratedKey(sql: String, parameters: Map<String, Any?> = mapOf(), options: StatementOptions = defaultOptions): Int = insertWithIntGeneratedKey(sql, parameters, options).second

fun Session.batchInsertWithIntGeneratedKey(sql: String, parametersList: List<Map<String, Any?>>, options: StatementOptions = defaultOptions, columnName: String = "GENERATED_KEY"): List<Pair<Int, Int>> = batchInsert(sql, parametersList, options) { it.int(columnName) }

fun Session.batchInsertAndGetGeneratedKeys(sql: String, parametersList: List<Map<String, Any?>>, options: StatementOptions = defaultOptions): List<Int> = batchInsertWithIntGeneratedKey(sql, parametersList, options).map { it.second }

fun Row.localDateTime(name: String): LocalDateTime = localDateTimeOrNull(name)!!

@Suppress("unused")
fun Row.localDate(name: String): LocalDate = localDateOrNull(name)!!

fun Row.localDateTimeOrNull(name: String): LocalDateTime? = timestampOrNull(name)?.toLocalDateTime()

fun Row.localDateOrNull(name: String): LocalDate? = dateOrNull(name)?.toLocalDate()