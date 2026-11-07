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

/**
 * Identity of one row (or one aggregate) a deferred write touches. Writes that share a [WriteKey] are persisted in the
 * order they were queued; writes with disjoint keys are persisted in parallel.
 *
 * Declare every row the write creates, changes or deletes, PLUS the parent row it points to when that parent may have
 * been created in the same window (e.g. an `items_wired` insert declares its item; an item moved into a room that was
 * just created declares the room). Declaring too much only costs parallelism; declaring too little can reorder a row.
 */
data class WriteKey(val kind: String, val id: Long) {
    companion object {
        fun user(id: Int) = WriteKey("user", id.toLong())
        fun room(id: Int) = WriteKey("room", id.toLong())
        fun item(id: Int) = WriteKey("item", id.toLong())
        fun group(id: Int) = WriteKey("group", id.toLong())
        fun chest(id: Int) = WriteKey("chest", id.toLong())
        fun pet(id: Int) = WriteKey("pet", id.toLong())
        fun items(ids: Collection<Int>) = ids.map { item(it) }

        /** The default lane used by `queue { }` without keys: strict global FIFO among those writes. */
        internal val DEFAULT_LANE = WriteKey("default-lane", 0)
    }
}
