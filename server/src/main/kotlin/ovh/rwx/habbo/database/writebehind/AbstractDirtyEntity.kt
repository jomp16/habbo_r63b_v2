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

package ovh.rwx.habbo.database.writebehind

abstract class AbstractDirtyEntity {
    @Transient
    @Volatile
    var isDirty: Boolean = false
        protected set

    /**
     * Entidade só em memória (ex.: sessão de bot de stress test): nunca fica suja nem entra no conjunto
     * de escrita do write-behind, então nada dela chega ao banco (o INSERT falharia por FK, aliás).
     */
    @Transient
    @Volatile
    var ephemeral: Boolean = false

    fun markDirty() {
        if (ephemeral) return
        isDirty = true
        WriteBehindManager.markDirty(this)
    }

    fun markClean() {
        isDirty = false
    }

    /**
     * Persists this dirty entity to the database and marks it clean.
     */
    abstract fun flush()
}
