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

package ovh.rwx.habbo.game.room.wired

import ovh.rwx.habbo.game.room.Room
import java.util.concurrent.ConcurrentHashMap

class WiredErrorLogger(private val room: Room) {
    private val errors = ConcurrentHashMap<Int, WiredError>()

    companion object {
        const val MAX_ERRORS = 500
    }

    fun logError(errorName: String, category: String, exception: Exception? = null) {
        val errorId = (errorName + category).hashCode()
        val now = System.currentTimeMillis()

        errors.compute(errorId) { _, existing ->
            existing?.copy(
                throwCount = existing.throwCount + 1,
                lastOccurrence = now
            ) ?: if (errors.size >= MAX_ERRORS) {
                null
            } else {
                WiredError(errorId, errorName, category, 1, now)
            }
        }
    }

    fun getErrors(): List<WiredError> = errors.values.toList()

    fun clearErrors() {
        errors.clear()
    }

    fun dispose() {
        errors.clear()
    }
}
