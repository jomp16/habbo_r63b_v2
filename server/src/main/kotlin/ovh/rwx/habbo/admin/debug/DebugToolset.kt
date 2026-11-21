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

import ovh.rwx.habbo.BuildConfig
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.debug.DebugDao
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomBot
import ovh.rwx.habbo.game.room.user.RoomEntity
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession
import java.lang.management.ManagementFactory
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

/**
 * Catálogo das ferramentas de debug. Leituras não passam pela thread da sala (funcionam com o loop morto, que é
 * justamente o caso a diagnosticar); escritas na sala vão pela thread dela ([DebugTools.onRoomThread]) e só rodam
 * daqui com `force=true`.
 */
internal object DebugToolset {
    private val roomId = DebugParam("room_id", DebugParamType.INT, "ID da sala", required = true)
    private val force = DebugParam(
        "force", DebugParamType.BOOL,
        "Roda fora da thread da sala quando o loop está parado ou travado (padrão false)"
    )

    val tools: List<DebugTool> = listOf(
        DebugTool(
            "server_status",
            "Visão geral: sessões, salas carregadas, salas zumbis (na lista de loops mas sem loop ativo, ou com entidades sem loop), fila do write-behind, memória e build.",
            DebugAccess.READ
        ) { serverStatus() },

        DebugTool(
            "room_list",
            "Salas em memória com loop ativo, entidades ou problemas. Campos: running, loop_state, users, entities, errors, anomalies.",
            DebugAccess.READ,
            listOf(
                DebugParam("only_problems", DebugParamType.BOOL, "Só salas com anomalias (padrão false)"),
                DebugParam("limit", DebugParamType.INT, "Máximo de salas (padrão 50, até 500)"),
            )
        ) { a -> roomList(a.bool("only_problems"), (a.int("limit") ?: 50).coerceIn(1, 500)) },

        DebugTool(
            "room_inspect",
            "Estado completo de uma sala: loop (running, loop_state, motivo da morte do loop, filas, erros), cada entidade (sessão, canal, sala atual da sessão, pendingJoin, posição), o gamemap de entidades e a lista de anomalias (fantasmas).",
            DebugAccess.READ,
            listOf(roomId)
        ) { a -> roomInspect(room(a.requireInt("room_id"))) },

        DebugTool(
            "user_inspect",
            "Sessões de um usuário (canal, release, sala atual, entidade) e todas as salas em memória onde há uma entidade dele (acha fantasmas). Informe user_id ou username.",
            DebugAccess.READ,
            listOf(
                DebugParam("user_id", DebugParamType.INT, "ID do usuário"),
                DebugParam("username", DebugParamType.STRING, "Nome do usuário"),
            )
        ) { a -> userInspect(a.int("user_id"), a.string("username")) },

        DebugTool(
            "writebehind_status",
            "Fila do write-behind: escritas pendentes, a mais antiga, as chaves com mais escritas na fila, entidades sujas e as últimas 50 falhas (com as chaves da escrita).",
            DebugAccess.READ
        ) { WriteBehindManager.status() },

        DebugTool(
            "db_query",
            "Consulta SQL somente leitura (um SELECT/SHOW/EXPLAIN/DESCRIBE/WITH, transação READ ONLY + rollback, timeout 10 s). Use para comparar o banco com o estado em memória.",
            DebugAccess.WRITE,
            listOf(
                DebugParam("sql", DebugParamType.STRING, "Consulta", required = true),
                DebugParam("max_rows", DebugParamType.INT, "Máximo de linhas (padrão 100, até 1000)"),
            )
        ) { a ->
            val sql = a.requireString("sql")
            DebugDao.rejectReason(sql)?.let { throw DebugToolException(it) }
            try {
                DebugDao.readOnlyQuery(sql, (a.int("max_rows") ?: 100).coerceIn(1, 1000), 10)
            } catch (e: java.sql.SQLException) {
                // erro da consulta (sintaxe, READ ONLY, timeout) é resposta, não falha do servidor
                throw DebugToolException("SQL: ${e.message}")
            }
        },

        DebugTool(
            "room_remove_entity",
            "Remove uma entidade da sala (sem mandar ROOM_EXIT/kick à sessão dela, que pode estar em outra sala) e avisa a sala. Também tira do gamemap.",
            DebugAccess.WRITE,
            listOf(roomId, DebugParam("virtual_id", DebugParamType.INT, "virtual_id da entidade (veja room_inspect)", required = true), force)
        ) { a ->
            val room = room(a.requireInt("room_id"))
            val vid = a.requireInt("virtual_id")
            DebugTools.onRoomThread(room, a.bool("force")) { r ->
                val entity = r.userManager.entities[vid] ?: throw DebugToolException("virtual_id $vid não está na sala ${r.roomData.id}")
                removeEntitySilently(r, entity)
                mapOf("removed" to describeEntity(r, entity), "gamemap_orphans_removed" to purgeGamemap(r))
            }
        },

        DebugTool(
            "room_purge_ghosts",
            "Remove todas as entidades fantasmas da sala (critérios de room_inspect: canal fechado, sessão fora do gerenciador, sessão em outra sala, duplicadas) e as sobras do gamemap. dry_run=true só lista.",
            DebugAccess.WRITE,
            listOf(roomId, DebugParam("dry_run", DebugParamType.BOOL, "Só listar (padrão false)"), force)
        ) { a ->
            val room = room(a.requireInt("room_id"))
            if (a.bool("dry_run")) {
                mapOf("would_remove" to ghosts(room).map { (e, why) -> describeEntity(room, e) + ("reasons" to why) }, "gamemap_orphans" to gamemapOrphans(room).size)
            } else DebugTools.onRoomThread(room, a.bool("force")) { r ->
                val removed = ghosts(r).map { (e, why) ->
                    removeEntitySilently(r, e)
                    describeEntity(r, e) + ("reasons" to why)
                }
                mapOf("removed" to removed, "gamemap_orphans_removed" to purgeGamemap(r))
            }
        },

        DebugTool(
            "room_force_unload",
            "Descarrega a sala mesmo com o loop morto (sala zumbi): tira fantasmas sem avisar as sessões deles, expulsa quem está de fato na sala, salva e para o loop. A próxima entrada recarrega a sala do zero.",
            DebugAccess.WRITE,
            listOf(roomId)
        ) { a ->
            val room = room(a.requireInt("room_id"))
            if (!room.initialized) throw DebugToolException("sala ${room.roomData.id} nunca foi inicializada; nada a descarregar")
            val before = mapOf("running" to room.running, "loop_state" to room.loopState, "entities" to room.userManager.entities.size)
            val ghostsRemoved = ghosts(room).map { (e, _) -> removeEntitySilently(room, e); e.virtualID }
            HabboServer.habboGame.roomManager.roomTaskManager.forceRemoveRoom(room)
            mapOf(
                "before" to before, "ghosts_removed" to ghostsRemoved,
                "after" to mapOf("running" to room.running, "loop_state" to room.loopState, "entities" to room.userManager.entities.size)
            )
        },

        DebugTool(
            "session_disconnect",
            "Fecha a(s) conexão(ões) de um usuário (o cliente cai e o close() limpa a sala). channel limita a uma sessão (id curto do canal, veja user_inspect).",
            DebugAccess.WRITE,
            listOf(
                DebugParam("user_id", DebugParamType.INT, "ID do usuário", required = true),
                DebugParam("channel", DebugParamType.STRING, "Id curto do canal (opcional)"),
            )
        ) { a ->
            val uid = a.requireInt("user_id")
            val channel = a.string("channel")
            val targets = sessionsOf(uid).filter { channel == null || it.channel.id().asShortText() == channel }
            if (targets.isEmpty()) throw DebugToolException("nenhuma sessão do usuário $uid" + (channel?.let { " no canal $it" } ?: ""))
            targets.map { s -> s.channel.close(); s.channel.id().asShortText() }.let { mapOf("closed" to it) }
        },

        DebugTool(
            "eval",
            "Avalia JavaScript (Nashorn) com acesso total à JVM. Variáveis: habboServer, habboGame, roomManager, sessions (HabboSessionManager), debug (este objeto: debug.room(id), debug.sessionsOf(userId)). Devolve o toString() do resultado. Só existe com debug_tools.eval_enabled.",
            DebugAccess.EVAL,
            listOf(DebugParam("script", DebugParamType.STRING, "Código JavaScript", required = true))
        ) { a -> eval(a.requireString("script")) },
    )

    // region Helpers (públicos para o eval: debug.room(id) etc.)

    // sem @JvmStatic: num object ele deixa o método só estático, e o Nashorn não chama estático pela instância (debug.room)
    fun room(id: Int): Room = HabboServer.habboGame.roomManager.rooms[id] ?: throw DebugToolException("sala $id não está em memória")

    fun sessionsOf(userId: Int): List<HabboSession> =
        HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated && it.userInformation.id == userId }

    private fun inTaskManager(room: Room) = room in HabboServer.habboGame.roomManager.roomTaskManager.rooms

    private fun channelAlive(s: HabboSession) = s.channel.isActive && s.channel.isOpen

    private fun describeSession(s: HabboSession): Map<String, Any?> = mapOf(
        "channel" to s.channel.id().asShortText(),
        "channel_active" to s.channel.isActive,
        "channel_open" to s.channel.isOpen,
        "registered" to (HabboServer.habboSessionManager.habboSessions[s.channel.id()] === s),
        "websocket" to s.isWebSocket,
        "version" to s.habboVersion.toString(),
        "protocol_era" to s.protocolEra.name,
        "bot" to s.isBot,
        "current_room" to s.currentRoom?.roomData?.id,
        "room_user_vid" to s.roomUser?.virtualID,
        "room_user_room" to s.roomUser?.room?.roomData?.id,
    )

    private fun describeEntity(room: Room, e: RoomEntity): Map<String, Any?> {
        val base = linkedMapOf<String, Any?>(
            "virtual_id" to e.virtualID,
            "type" to e.javaClass.simpleName,
            "pending_join" to e.pendingJoin,
            "position" to "${e.currentVector3.x},${e.currentVector3.y},${e.currentVector3.z}",
            "next_step" to e.nextStepVector?.let { "${it.x},${it.y}" },
            "in_gamemap" to room.initializedGamemap()?.let { gm -> gm.getEntitiesFromVector2(e.currentVector3.vector2).contains(e) },
        )
        when (e) {
            is RoomUser -> {
                val s = e.habboSession
                base["user_id"] = s.userInformation.id
                base["username"] = s.userInformation.username
                base["session"] = describeSession(s)
                base["session_points_here"] = s.roomUser === e
            }
            is RoomBot -> base["bot"] = "${e.botId}/${e.botName}"
            is RoomPet -> base["pet"] = "${e.petData.id}/${e.petData.name}"
        }
        return base
    }

    private fun Room.initializedGamemap() = if (initialized) roomGamemap else null

    /** Entidades fantasmas da sala com os motivos. Pets e bots não entram (não têm sessão). */
    fun ghosts(room: Room): List<Pair<RoomEntity, List<String>>> {
        val users = room.userManager.entities.values.filterIsInstance<RoomUser>()
        val result = ArrayList<Pair<RoomEntity, List<String>>>()

        for (u in users) {
            val s = u.habboSession
            val why = ArrayList<String>()
            if (!channelAlive(s)) why += "canal fechado"
            if (HabboServer.habboSessionManager.habboSessions[s.channel.id()] !== s) why += "sessão fora do gerenciador"
            if (!u.pendingJoin && s.currentRoom !== room) why += "sessão está na sala ${s.currentRoom?.roomData?.id}"
            if (!u.pendingJoin && s.roomUser !== u) why += "sessão aponta para outra entidade (${s.roomUser?.virtualID})"
            if (u.pendingJoin && !room.running) why += "join pendente com o loop parado"
            if (why.isNotEmpty()) result += u to why
        }

        // mesmo usuário duas vezes: fica a entidade para a qual a sessão aponta
        users.filter { u -> result.none { it.first === u } }
            .groupBy { it.habboSession.userInformation.id }
            .filter { it.value.size > 1 }
            .forEach { (_, dupes) ->
                dupes.filter { it.habboSession.roomUser !== it }.forEach { result += it to listOf("duplicada do mesmo usuário") }
            }

        return result
    }

    private fun gamemapOrphans(room: Room): List<Map<String, Any?>> {
        val gm = room.initializedGamemap() ?: return emptyList()
        val alive = room.userManager.entities.values.toSet()
        return gm.entityTiles().flatMap { (tile, list) ->
            list.filter { it !in alive }.map { mapOf("tile" to "${tile.x},${tile.y}", "virtual_id" to it.virtualID, "type" to it.javaClass.simpleName) }
        }
    }

    private fun purgeGamemap(room: Room): Int =
        room.initializedGamemap()?.purgeEntitiesNotIn(room.userManager.entities.values)?.size ?: 0

    /** Sem ROOM_EXIT/kick: a sessão do fantasma pode estar em outra sala. O removeEntity já enfileira o ROOM_USER_REMOVE. */
    private fun removeEntitySilently(room: Room, e: RoomEntity) {
        room.userManager.removeEntity(e, notifyClient = false, kickNotification = false)
        // removeEntity só tira do gamemap a posição atual e o próximo passo; garante o resto
        purgeGamemap(room)
        // com o loop parado o UserPartRoomTask nunca roda: avisa quem ainda está conectado na sala
        if (!room.running) {
            room.sendResponse(
                ovh.rwx.habbo.communication.outgoing.Outgoing.ROOM_USER_REMOVE,
                ovh.rwx.habbo.communication.outgoing.OutgoingR63A.ROOM_USER_REMOVE,
                e.virtualID
            )
        }
    }

    private fun roomAnomalies(room: Room): List<String> {
        val out = ArrayList<String>()
        val inTm = inTaskManager(room)
        val entities = room.userManager.entities.size
        if (inTm && !room.running) out += "na lista de loops do RoomTaskManager mas sem loop ativo (${room.loopState}): entradas não reiniciam a sala"
        if (!inTm && room.running) out += "loop ativo fora da lista do RoomTaskManager"
        if (!room.running && room.userManager.entities.values.any { it is RoomUser }) out += "usuários na sala sem loop ativo"
        room.loopEndedReason?.let { out += "loop morreu: $it" }
        if (room.errorsCounter.get() > 0) out += "errorsCounter=${room.errorsCounter.get()}"
        ghosts(room).forEach { (e, why) -> out += "fantasma vid=${e.virtualID}: ${why.joinToString("; ")}" }
        gamemapOrphans(room).takeIf { it.isNotEmpty() }?.let { out += "${it.size} entidade(s) no gamemap fora da sala" }
        if (entities > 0 && !room.initialized) out += "entidades numa sala não inicializada"
        return out
    }

    private fun roomSummary(room: Room): Map<String, Any?> = linkedMapOf(
        "id" to room.roomData.id,
        "name" to room.roomData.name,
        "owner_id" to room.roomData.ownerId,
        "running" to room.running,
        "loop_state" to room.loopState,
        "in_task_manager" to inTaskManager(room),
        "users" to room.userManager.entities.values.count { it is RoomUser },
        "entities" to room.userManager.entities.size,
        "errors" to room.errorsCounter.get(),
        "anomalies" to roomAnomalies(room),
    )

    // endregion

    // region Tools

    private fun serverStatus(): Map<String, Any?> {
        val sm = HabboServer.habboSessionManager.habboSessions.values
        val rm = HabboServer.habboGame.roomManager
        val tm = rm.roomTaskManager.rooms
        val rt = Runtime.getRuntime()
        val wb = WriteBehindManager.status()
        val zombies = (tm + rm.rooms.values.filter { r -> r.userManager.entities.isNotEmpty() })
            .distinct().filter { !it.running }

        return linkedMapOf(
            "build" to "${BuildConfig.VERSION} ${BuildConfig.GIT_COMMIT_SHORT} (${BuildConfig.BUILD_INSTANT})",
            "started" to HabboServer.started,
            "uptime_s" to ManagementFactory.getRuntimeMXBean().uptime / 1000,
            "heap_used_mb" to (rt.totalMemory() - rt.freeMemory()) / 1_048_576,
            "heap_max_mb" to rt.maxMemory() / 1_048_576,
            "sessions" to sm.size,
            "sessions_authenticated" to sm.count { it.authenticated },
            "sessions_dead_channel" to sm.count { !channelAlive(it) },
            "rooms_in_memory" to rm.rooms.size,
            "rooms_in_task_manager" to tm.size,
            "rooms_running" to tm.count { it.running },
            "zombie_rooms" to zombies.map { mapOf("id" to it.roomData.id, "name" to it.roomData.name, "loop_state" to it.loopState, "entities" to it.userManager.entities.size, "loop_ended" to it.loopEndedReason) },
            "writebehind" to mapOf("running" to wb.running, "pending" to wb.queue.pending, "oldest_pending_ms" to wb.queue.oldestPendingMs, "dirty" to wb.dirtyEntities, "total_failures" to wb.totalFailures),
        )
    }

    private fun roomList(onlyProblems: Boolean, limit: Int): Map<String, Any?> {
        val rm = HabboServer.habboGame.roomManager
        val candidates = (rm.roomTaskManager.rooms + rm.rooms.values.filter { it.userManager.entities.isNotEmpty() || it.running }).distinct()
        val rows = candidates.map { roomSummary(it) }
            .filter { !onlyProblems || (it["anomalies"] as List<*>).isNotEmpty() }
            .sortedWith(compareByDescending<Map<String, Any?>> { (it["anomalies"] as List<*>).size }.thenByDescending { it["users"] as Int })
        return mapOf("total" to rows.size, "rooms" to rows.take(limit))
    }

    private fun roomInspect(room: Room): Map<String, Any?> {
        val (hf, major) = room.pendingTaskCounts
        return roomSummary(room) + linkedMapOf(
            "initialized" to room.initialized,
            "virtual" to room.roomData.virtual,
            "users_max" to room.roomData.usersMax,
            "loop_ended_reason" to room.loopEndedReason,
            "pending_tasks" to mapOf("high_frequency" to hf, "major_tick" to major),
            "empty_counter" to room.emptyCounter.get(),
            "entity_list" to room.userManager.entities.values.map { describeEntity(room, it) },
            "gamemap_entities" to (room.initializedGamemap()?.entityTiles()?.map { (tile, list) ->
                mapOf("tile" to "${tile.x},${tile.y}", "virtual_ids" to list.map { it.virtualID })
            } ?: emptyList<Any>()),
            "gamemap_orphans" to gamemapOrphans(room),
        )
    }

    private fun userInspect(userId: Int?, username: String?): Map<String, Any?> {
        if (userId == null && username.isNullOrBlank()) throw DebugToolException("informe user_id ou username")
        val name = username.orEmpty()
        val uid = userId ?: HabboServer.habboSessionManager.getHabboSessionByUsername(name)?.userInformation?.id
            ?: ovh.rwx.habbo.database.user.UserInformationDao.getUserInformationByUsername(name)?.id
            ?: throw DebugToolException("usuário '$username' não existe")

        val appearances = HabboServer.habboGame.roomManager.rooms.values.flatMap { room ->
            room.userManager.entities.values.filterIsInstance<RoomUser>()
                .filter { it.habboSession.authenticated && it.habboSession.userInformation.id == uid }
                .map { e ->
                    mapOf("room_id" to room.roomData.id, "room_name" to room.roomData.name, "room_running" to room.running) +
                        describeEntity(room, e) + ("ghost_reasons" to ghosts(room).firstOrNull { it.first === e }?.second)
                }
        }

        return linkedMapOf(
            "user_id" to uid,
            "sessions" to sessionsOf(uid).map { describeSession(it) },
            "room_entities" to appearances,
        )
    }

    private fun eval(script: String): Map<String, Any?> {
        val engine = ScriptEngineManager().getEngineByName("JavaScript")
            ?: throw DebugToolException("engine JavaScript (Nashorn) indisponível")
        val bindings = SimpleBindings(
            mutableMapOf(
                "habboServer" to HabboServer,
                "habboGame" to HabboServer.habboGame,
                "roomManager" to HabboServer.habboGame.roomManager,
                "sessions" to HabboServer.habboSessionManager,
                "debug" to DebugToolset,
            )
        )
        val result = engine.eval(script, bindings)
        return mapOf("type" to result?.javaClass?.name, "value" to result?.toString()?.take(100_000))
    }

    // endregion
}
