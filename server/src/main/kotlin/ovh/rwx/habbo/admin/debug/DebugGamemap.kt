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

package ovh.rwx.habbo.admin.debug

import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.model.SquareState
import ovh.rwx.habbo.game.room.user.RoomBot
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Vector2

/**
 * Estado do gamemap de uma sala para a ferramenta `room_gamemap`: o mesmo JSON serve à CMS (que desenha a grade) e a uma IA
 * pelo MCP (as camadas são texto, uma fileira por linha). Só lê: não passa pela thread da sala, então funciona com o loop
 * morto (os mapas concorrentes são seguros de ler; os arrays podem estar um tick atrás, o que não importa para diagnóstico).
 */
internal object DebugGamemap {
    private const val LEVELS = "0123456789abcdefghijklmnopqrstuvwxyz"

    const val LEGEND = "layers: uma string por fileira (y), um caractere por coluna (x). " +
        "floor = altura do piso (0-9, a-z = 10-35; x = buraco/casa fechada). " +
        "height = altura absoluta com os itens (mesma escala, arredondada). " +
        "state: # buraco/fechada, D porta, B bloqueada por item, E entidade, i item que não bloqueia, . livre, A anomalia (veja tiles)."

    /** Altura → um caractere (0-9, a-z; arredonda; abaixo de zero '-', acima de 35 '+'). */
    fun levelChar(height: Double): Char {
        val n = Math.round(height).toInt()
        return when {
            n < 0 -> '-'
            n >= LEVELS.length -> '+'
            else -> LEVELS[n]
        }
    }

    /** Prioridade: anomalia, porta, buraco, bloqueada por item, entidade, item, livre. */
    fun stateChar(closed: Boolean, door: Boolean, anomaly: Boolean, blockedByItem: Boolean, hasEntity: Boolean, hasItem: Boolean): Char = when {
        anomaly -> 'A'
        door -> 'D'
        closed -> '#'
        blockedByItem -> 'B'
        hasEntity -> 'E'
        hasItem -> 'i'
        else -> '.'
    }

    fun describe(room: Room, x: Int?, y: Int?, limit: Int): Map<String, Any?> {
        if (!room.initialized) throw DebugToolException("sala ${room.roomData.id} sem gamemap (não inicializada)")
        if ((x == null) != (y == null)) throw DebugToolException("informe x e y juntos")
        val model = room.roomModel
        val w = model.mapSizeX
        val h = model.mapSizeY
        val entityTiles = room.roomGamemap.entityTiles()

        if (x != null && y != null) {
            if (x !in 0 until w || y !in 0 until h) throw DebugToolException("casa fora do mapa (${w}x$h)")
            return linkedMapOf("room_id" to room.roomData.id, "size" to mapOf("x" to w, "y" to h), "tile" to tile(room, x, y, entityTiles))
        }

        val floor = ArrayList<String>(h)
        val height = ArrayList<String>(h)
        val state = ArrayList<String>(h)
        val tiles = ArrayList<Map<String, Any?>>()
        var truncated = false
        var holes = 0
        var withItems = 0
        var withEntities = 0
        var blockedItems = 0
        var cannotStack = 0
        var anomalyTiles = 0
        var minFloor = Int.MAX_VALUE
        var maxFloor = Int.MIN_VALUE
        val itemIds = HashSet<Int>()
        val door = model.doorVector3

        for (yy in 0 until h) {
            val f = StringBuilder(w)
            val hh = StringBuilder(w)
            val s = StringBuilder(w)
            for (xx in 0 until w) {
                val v = Vector2(xx, yy)
                val items = room.roomGamemap.getItemsFromVector2(v)
                val ents = entityTiles[v].orEmpty()
                val closed = model.squareStates[xx][yy] == SquareState.CLOSED
                val isDoor = door.x == xx && door.y == yy
                val anomalies = anomalies(room, xx, yy, closed && !isDoor, items, ents)
                val blockedByItem = room.roomGamemap.blockedItem[xx][yy]

                f.append(if (closed && !isDoor) 'x' else levelChar(model.floorHeight[xx][yy].toDouble()))
                hh.append(if (closed && !isDoor) 'x' else levelChar(room.roomGamemap.getAbsoluteHeight(xx, yy)))
                s.append(stateChar(closed, isDoor, anomalies.isNotEmpty(), blockedByItem, ents.isNotEmpty(), items.isNotEmpty()))

                if (closed && !isDoor) holes++ else {
                    minFloor = minOf(minFloor, model.floorHeight[xx][yy].toInt())
                    maxFloor = maxOf(maxFloor, model.floorHeight[xx][yy].toInt())
                }
                if (items.isNotEmpty()) withItems++
                if (ents.isNotEmpty()) withEntities++
                if (blockedByItem) blockedItems++
                if (room.roomGamemap.cannotStackItem[xx][yy]) cannotStack++
                if (anomalies.isNotEmpty()) anomalyTiles++
                items.forEach { itemIds.add(it.id) }

                if (items.isNotEmpty() || ents.isNotEmpty() || blockedByItem || anomalies.isNotEmpty()) {
                    if (tiles.size < limit) tiles.add(tile(room, xx, yy, entityTiles)) else truncated = true
                }
            }
            floor.add(f.toString())
            height.add(hh.toString())
            state.add(s.toString())
        }

        return linkedMapOf(
            "room_id" to room.roomData.id,
            "size" to mapOf("x" to w, "y" to h),
            "door" to mapOf("x" to door.x, "y" to door.y, "z" to door.z, "dir" to model.doorDir),
            "summary" to linkedMapOf(
                "tiles" to w * h,
                "holes" to holes,
                "floor_height_min" to if (minFloor == Int.MAX_VALUE) null else minFloor,
                "floor_height_max" to if (maxFloor == Int.MIN_VALUE) null else maxFloor,
                "items" to itemIds.size,
                "tiles_with_items" to withItems,
                "tiles_blocked_by_items" to blockedItems,
                "tiles_cannot_stack" to cannotStack,
                "entities" to entityTiles.values.sumOf { it.size },
                "tiles_with_entities" to withEntities,
                "tiles_with_anomalies" to anomalyTiles,
            ),
            "legend" to LEGEND,
            "layers" to linkedMapOf("floor" to floor, "height" to height, "state" to state),
            "tiles" to tiles,
            "tiles_truncated" to truncated,
        )
    }

    private fun tile(room: Room, x: Int, y: Int, entityTiles: Map<Vector2, List<RoomEntity>>): Map<String, Any?> {
        val model = room.roomModel
        val gm = room.roomGamemap
        val v = Vector2(x, y)
        val items = gm.getItemsFromVector2(v).sortedWith(compareBy<RoomItem> { it.position.z }.thenBy { it.id })
        val ents = entityTiles[v].orEmpty()
        val closed = model.squareStates[x][y] == SquareState.CLOSED
        val isDoor = model.doorVector3.x == x && model.doorVector3.y == y
        val anomalies = anomalies(room, x, y, closed && !isDoor, items, ents)
        val blockedByItem = gm.blockedItem[x][y]
        return linkedMapOf(
            "x" to x,
            "y" to y,
            "state" to stateChar(closed, isDoor, anomalies.isNotEmpty(), blockedByItem, ents.isNotEmpty(), items.isNotEmpty()).toString(),
            "floor_z" to model.floorHeight[x][y],
            "height" to gm.getAbsoluteHeight(x, y),
            "closed" to closed,
            "door" to isDoor,
            "blocked" to gm.isBlocked(x, y),
            "blocked_by_item" to blockedByItem,
            "cannot_stack" to gm.cannotStackItem[x][y],
            "items" to items.map { describeItem(it) },
            "entities" to ents.map { describeTileEntity(it) },
            "anomalies" to anomalies,
        )
    }

    private fun describeItem(i: RoomItem): Map<String, Any?> = linkedMapOf(
        "id" to i.id,
        "name" to i.furnishing.itemName,
        "origin" to "${i.position.x},${i.position.y}",
        "z" to i.position.z,
        "height" to i.height,
        "total_height" to i.totalHeight,
        "rotation" to i.rotation,
        "interaction" to i.furnishing.interactionType.name,
        "walkable" to i.furnishing.walkable,
        "can_sit" to i.furnishing.canSit,
        "can_lay" to i.furnishing.canLay,
        "can_stack" to i.furnishing.canStack,
        "extra_data" to i.extraData.take(40),
    )

    private fun describeTileEntity(e: RoomEntity): Map<String, Any?> = linkedMapOf(
        "virtual_id" to e.virtualID,
        "type" to e.javaClass.simpleName,
        "name" to when (e) {
            is RoomUser -> e.habboSession.userInformation.username
            is RoomBot -> e.botName
            is RoomPet -> e.petData.name
            else -> null
        },
        "user_id" to (e as? RoomUser)?.habboSession?.userInformation?.id,
        "z" to e.currentVector3.z,
    )

    /** Inconsistências de uma casa (o que o gamemap diz contra o que os itens e entidades dizem). */
    private fun anomalies(room: Room, x: Int, y: Int, hole: Boolean, items: Collection<RoomItem>, ents: Collection<RoomEntity>): List<String> {
        val gm = room.roomGamemap
        val floor = room.roomModel.floorHeight[x][y]
        val out = ArrayList<String>(0)
        if (hole && items.isNotEmpty()) out.add("item em casa fechada (buraco)")
        if (hole && ents.isNotEmpty()) out.add("entidade em casa fechada (buraco)")
        if (gm.blockedItem[x][y] && items.isEmpty()) out.add("marcada como bloqueada por item, mas sem itens")
        if (gm.cannotStackItem[x][y] && items.isEmpty()) out.add("marcada como sem empilhar, mas sem itens")
        if (!hole && items.any { it.position.z < floor - 0.01 }) out.add("item abaixo do piso")
        return out
    }
}
