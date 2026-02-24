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

package ovh.rwx.habbo.game.room.games

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerGameEnds
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerGameStarts
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerScoreAchieved
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.BattleBanzaiTilesFlickerTask
import ovh.rwx.habbo.game.room.tasks.UserAction
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.room.user.RoomUserEffect
import ovh.rwx.habbo.util.Direction
import ovh.rwx.habbo.util.Vector2
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class BattleBanzaiGame(room: Room) : RoomGame(room) {
    private val userTeams = mutableMapOf<Int, GameTeam>()
    private val gateAssignments = mutableMapOf<Int, Int>() // userId -> gateId
    private var timeRemaining = 0
    var configuredTime = 30
        private set
    private var tickCounter = 0
    private val pendingTileUpdates = mutableListOf<Pair<RoomUser, RoomItem>>()
    private val movingPucks = mutableSetOf<Int>() // IDs dos pucks em movimento
    private val puckLastMoveTime = ConcurrentHashMap<Int, Long>()
    private val puckJobs = ConcurrentHashMap<Int, Job>()

    companion object {
        private val TEAM_EFFECTS = mapOf(
            GameTeam.RED to 33, // Vermelho
            GameTeam.GREEN to 34, // Verde
            GameTeam.BLUE to 35, // Azul
            GameTeam.YELLOW to 36  // Amarelo
        )
    }

    override fun start() {
        if (running) return
        running = true

        // Limpa tiles pendentes de antes do jogo iniciar
        pendingTileUpdates.clear()

        // Reset da arena ANTES de iniciar
        resetTiles()
        resetScores()

        // Carrega o tempo configurado de qualquer counter ao iniciar o jogo
        room.roomItems.values
            .firstOrNull { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_COUNTER }
            ?.let { counter ->
                configuredTime = counter.extraData.toIntOrNull() ?: 30
                if (configuredTime == 0) configuredTime = 30
            }

        timeRemaining = configuredTime
        tickCounter = 0

        // Atualiza TODOS os counters para mostrar o tempo inicial
        updateClock()

        // Acionar Wired
        room.wiredHandler.triggerWired(WiredTriggerGameStarts::class, null, null)
    }

    override fun pause() {
        if (!running) return

        // Reseta tiles que ficaram em "1" (não foram pintados) para "0"
        room.roomItems.values
            .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE && it.extraData == "1" }
            .forEach {
                it.extraData = "0"
                it.update(updateDb = false, updateClient = true)
            }

        running = false
    }

    override fun resume() {
        if (running) return

        // Reseta tiles "0" para "1"
        room.roomItems.values
            .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE && it.extraData == "1" }
            .forEach {
                it.extraData = "1"
                it.update(updateDb = false, updateClient = true)
            }

        running = true
    }

    override fun stop() {
        if (!running) return
        running = false
        tickCounter = 0

        // Limpa tiles pendentes
        pendingTileUpdates.clear()

        // 1. Congela o Timer em 00:00
        room.roomItems.values
            .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_COUNTER }
            .forEach { counter ->
                counter.extraData = "0"
                counter.update(updateDb = false, updateClient = true)
            }

        // 2. Reseta tiles que ficaram em "1" (não foram pintados) para "0"
        room.roomItems.values
            .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE && it.extraData == "1" }
            .forEach {
                it.extraData = "0"
                it.update(updateDb = false, updateClient = true)
            }

        // 3. Determina o vencedor
        val maxScore = teamScores.values.maxOrNull() ?: 0
        val winningTeams = teamScores.filter { it.value == maxScore }.keys
        val isDraw = winningTeams.size > 1

        // 4. Feedback nos jogadores e efeito de piscar (se não for empate)
        if (!isDraw && winningTeams.isNotEmpty()) {
            val winningTeam = winningTeams.first()

            // Jogadores acenam e ganham achievements
            userTeams.forEach { (userId, team) ->
                room.roomUsers.values.find { it.habboSession?.userInformation?.id == userId }?.let { roomUser ->
                    // ACH_BattleBallPlayer: jogar Battle Banzai
                    roomUser.habboSession?.let {
                        HabboServer.habboGame.achievementManager.progress(
                            it,
                            "ACH_BattleBallPlayer",
                            1,
                            accumulate = true
                        )
                    }

                    if (team == winningTeam) {
                        roomUser.action(UserAction.WAVE)

                        // ACH_BattleBallWinner: ganhar pontos vencedores
                        roomUser.habboSession?.let {
                            val teamScore = teamScores[winningTeam] ?: 0
                            HabboServer.habboGame.achievementManager.progress(
                                it,
                                "ACH_BattleBallWinner",
                                teamScore,
                                accumulate = true
                            )
                        }
                    }
                }
            }

            // Tiles vencedores piscam
            val winningTiles = room.roomItems.values
                .filter {
                    it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                            it.extraData.toIntOrNull()
                                ?.let { state -> state / 3 == winningTeam.color && state % 3 == 2 } == true
                }

            if (winningTiles.isNotEmpty()) {
                room.roomTask?.addTask(room, BattleBanzaiTilesFlickerTask(winningTiles, winningTeam.color))
            }
        }

        // NOTA: Tiles pintados (diferentes de "1") ficam para mostrar o resultado

        // Resetar triggers de pontuação para o próximo jogo
        room.wiredHandler.resetTriggerer(WiredTriggerScoreAchieved::class)

        // Acionar Wired
        room.wiredHandler.triggerWired(WiredTriggerGameEnds::class, null, null)
    }

    override fun handleInteraction(roomUser: RoomUser, roomItem: RoomItem, state: Int) {
        when (roomItem.furnishing.interactionType) {
            InteractionType.BATTLE_BANZAI_COUNTER -> handleCounterInteraction(roomItem, state)
            else -> {}
        }
    }

    override fun onUserWalksOn(roomUser: RoomUser, roomItem: RoomItem) {
        when (roomItem.furnishing.interactionType) {
            InteractionType.BATTLE_BANZAI_GATE_RED, InteractionType.BATTLE_BANZAI_GATE_GREEN,
            InteractionType.BATTLE_BANZAI_GATE_BLUE, InteractionType.BATTLE_BANZAI_GATE_YELLOW -> handleGateEntry(
                roomUser,
                roomItem
            )

            InteractionType.BATTLE_BANZAI_TILE -> {
                // EXPERIMENTO: Agenda para o próximo tick
                pendingTileUpdates.add(Pair(roomUser, roomItem))
            }

            InteractionType.BATTLE_BANZAI_PUCK -> {
                // Lógica Inteligente de Chute vs Drag
                val now = System.currentTimeMillis()
                val lastMove = puckLastMoveTime[roomItem.id] ?: 0L

                // Se o puck se moveu nos últimos 1000ms, ele está "Quente".
                // Significa que o usuário está arrastando/driblando ele.
                // Nesse caso, forçamos Drag (1) para evitar que o último passo vire um chute.
                val isPuckHot = (now - lastMove) < 1000

                var velocity = 1 // Padrão: Drag/Drible

                if (!isPuckHot) {
                    // Só consideramos Chute Forte se o puck estava PARADO (Frio).
                    // Verifica se o destino final é exatamente onde o puck está.
                    val isClickedTarget = roomUser.objectiveVector2?.let { dest ->
                        dest.x == roomItem.position.x && dest.y == roomItem.position.y
                    } ?: false

                    if (isClickedTarget) {
                        velocity = 6 // Chute Forte com Física
                    }
                }

                // Se for Drag (1), usa lógica de "continuidade" para não travar
                handlePuckKick(roomUser, roomItem, velocity)
            }

            else -> {}
        }
    }

    override fun tick() {
        if (!running) return

        // EXPERIMENTO: Processa tiles do tick anterior
        if (pendingTileUpdates.isNotEmpty()) {
            pendingTileUpdates.forEach { (roomUser, tile) ->
                handleTileWalk(roomUser, tile)
            }
            pendingTileUpdates.clear()
        }

        // Verifica se todos os tiles foram locked
        val allTilesLocked = room.roomItems.values
            .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE }
            .all {
                val state = it.extraData.toIntOrNull() ?: 0
                state % 3 == 2 // Locked
            }

        if (allTilesLocked) {
            stop()
            return
        }

        // Tick é chamado a cada 500ms, então 2 ticks = 1 segundo
        tickCounter++
        if (tickCounter >= 2) {
            tickCounter = 0
            timeRemaining--

            // Atualiza TODOS os counters visuais
            updateClock()

            // Para o jogo quando o tempo acabar
            if (timeRemaining <= 0) {
                stop()
                // Timer já é setado para "0" no stop()
            }
        }
    }

    override fun joinTeam(gameTeam: GameTeam, roomUser: RoomUser) {
        val userId = roomUser.habboSession?.userInformation?.id ?: return

        val currentTeam = userTeams[userId]

        if (currentTeam == null) {
            // Verifica limite global se strict mode
            if (HabboServer.habboConfig.gameConfig.banzai.strict) {
                val teamCount = userTeams.values.count { it == gameTeam }
                if (teamCount >= 5) {
                    // Limite global de 5 jogadores por time atingido
                    return
                }
            }

            // Adiciona ao time e associa ao gate
            userTeams[userId] = gameTeam
            TEAM_EFFECTS[gameTeam]?.let { roomUser.effect = RoomUserEffect(it, Integer.MAX_VALUE) }
            updateGateCounter(gameTeam)
        } else {
            // Usuário já tem time - remove do time atual ao passar por qualquer portão
            userTeams.remove(userId)
            gateAssignments.remove(userId)
            roomUser.effect = null
            updateGateCounter(currentTeam)
        }
    }

    override fun leaveTeam(gameTeam: GameTeam, roomUser: RoomUser) {
        val userId = roomUser.habboSession?.userInformation?.id ?: return
        val currentTeam = userTeams[userId]

        if (currentTeam != null) {
            userTeams.remove(userId)
            gateAssignments.remove(userId)
            roomUser.effect = null
            updateGateCounter(currentTeam)
        }
    }

    private fun handleGateEntry(roomUser: RoomUser, gate: RoomItem) {
        val gameTeam = getTeamColor(gate.furnishing.interactionType) ?: return
        val userId = roomUser.habboSession?.userInformation?.id ?: return

        val currentTeam = userTeams[userId]

        if (currentTeam != null) {
            // Usuário já tem time - remove do time atual ao passar por qualquer portão
            leaveTeam(currentTeam, roomUser)
        } else {
            // Entrando no gate - verifica se ESTE gate específico tem espaço
            val playersInThisGate = gateAssignments.count { it.value == gate.id }

            if (playersInThisGate >= 5) {
                // Este gate está cheio - não adiciona efeito
                return
            }

            gateAssignments[userId] = gate.id

            joinTeam(gameTeam, roomUser)
        }
    }

    private fun updateGateCounter(gameTeam: GameTeam) {
        val count = userTeams.values.count { it == gameTeam }
        val gateType = when (gameTeam) {
            GameTeam.RED -> InteractionType.BATTLE_BANZAI_GATE_RED
            GameTeam.GREEN -> InteractionType.BATTLE_BANZAI_GATE_GREEN
            GameTeam.BLUE -> InteractionType.BATTLE_BANZAI_GATE_BLUE
            GameTeam.YELLOW -> InteractionType.BATTLE_BANZAI_GATE_YELLOW
            else -> return
        }

        room.roomItems.values
            .filter { it.furnishing.interactionType == gateType }
            .forEach {
                it.extraData = count.toString()
                it.update(updateDb = false, updateClient = true)
            }
    }

    override fun onUserLeaveRoom(roomUser: RoomUser) {
        val userId = roomUser.habboSession?.userInformation?.id ?: return

        // Remove do time
        userTeams.remove(userId)

        // Libera o gate
        gateAssignments.remove(userId)
    }

    override fun getTeamColor(interactionType: InteractionType): GameTeam? {
        return when (interactionType) {
            InteractionType.BATTLE_BANZAI_GATE_RED -> GameTeam.RED
            InteractionType.BATTLE_BANZAI_GATE_GREEN -> GameTeam.GREEN
            InteractionType.BATTLE_BANZAI_GATE_BLUE -> GameTeam.BLUE
            InteractionType.BATTLE_BANZAI_GATE_YELLOW -> GameTeam.YELLOW
            else -> null
        }
    }

    private fun handleTileWalk(roomUser: RoomUser, tile: RoomItem) {
        if (!running) return

        val userId = roomUser.habboSession?.userInformation?.id ?: return
        val userTeam = userTeams[userId] ?: return

        if (tile.extraData.isEmpty()) {
            tile.extraData = "0"
        }

        val state = tile.extraData.toIntOrNull() ?: 0

        // Verifica se está locked (state % 3 == 2)
        if (state % 3 == 2) return

        val currentTeamColor = userTeam.color
        val check = state - (currentTeamColor * 3)

        val newState = if (check == 0 || check == 1) {
            // Mesmo time - incrementa
            state + 1
        } else {
            // Time inimigo - ROUBA (reseta para nível 0 do novo time)
            currentTeamColor * 3
        }

        tile.extraData = newState.toString()
        tile.update(updateDb = false, updateClient = true)

        if (newState % 3 == 2) {
            // LOCK! Adiciona pontuação de lock
            addScore(userTeam, 1)
            tileLocked(userTeam, tile, roomUser)
        }

        // IMPORTANTE: Não validamos a posição do usuário aqui
        // O onUserWalksOn é chamado quando o usuário está "no ar" indo para o tile
        // A coordenada currentVector3 ainda não foi atualizada, mas isso é esperado
    }

    private fun tileLocked(gameTeam: GameTeam, tile: RoomItem, roomUser: RoomUser) {
        val x = tile.position.x
        val y = tile.position.y

        val lockedTilesOfTeam = room.roomItems.values
            .filter {
                it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                        it.extraData.toIntOrNull()
                            ?.let { state -> state / 3 == gameTeam.color && state % 3 == 2 } == true
            }
            .toMutableSet()

        if (!lockedTilesOfTeam.contains(tile)) {
            lockedTilesOfTeam.add(tile)
        }

        val filledAreas = listOfNotNull(
            floodFill(x, y - 1, lockedTilesOfTeam, mutableSetOf(), gameTeam),
            floodFill(x, y + 1, lockedTilesOfTeam, mutableSetOf(), gameTeam),
            floodFill(x - 1, y, lockedTilesOfTeam, mutableSetOf(), gameTeam),
            floodFill(x + 1, y, lockedTilesOfTeam, mutableSetOf(), gameTeam)
        )

        val largestArea = filledAreas.maxByOrNull { it.size }

        var totalLockedCount = 1 // Conta o tile que foi pisado

        if (!largestArea.isNullOrEmpty()) {
            largestArea.forEach { (tileX, tileY) ->
                room.roomItems.values
                    .filter {
                        it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                                it.position.x == tileX && it.position.y == tileY &&
                                it.extraData.toIntOrNull()?.let { state -> state % 3 != 2 } == true
                    }
                    .forEach { tileItem ->
                        tileItem.extraData = ((gameTeam.color * 3) + 2).toString()
                        tileItem.update(updateDb = false, updateClient = true)
                        totalLockedCount++
                    }
            }
        }

        addScore(gameTeam, totalLockedCount - 1) // Score do flood fill apenas

        roomUser.habboSession?.let { session ->
            HabboServer.habboGame.achievementManager.progress(
                session,
                "ACH_BattleBallTilesLocked",
                totalLockedCount,
                accumulate = true
            )
        }
    }

    private fun floodFill(
        x: Int,
        y: Int,
        lockedTiles: Set<RoomItem>,
        visited: MutableSet<Pair<Int, Int>>,
        gameTeam: GameTeam
    ): Set<Pair<Int, Int>>? {
        if (isOutOfBounds(x, y)) return null
        if (isForeignLockedTile(x, y, gameTeam)) return null

        val coord = Pair(x, y)
        if (hasLockedTileAt(x, y, lockedTiles) || visited.contains(coord)) return visited

        // Verifica se o tile atual é do mesmo time e locked - se sim, é uma borda
        val currentTile = room.roomItems.values.find {
            it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                    it.position.x == x && it.position.y == y
        }

        if (currentTile != null) {
            val state = currentTile.extraData.toIntOrNull() ?: 0
            // Se é locked do mesmo time, é uma borda (não adiciona à área)
            if (state % 3 == 2 && state / 3 == gameTeam.color) {
                return visited
            }
        }

        visited.add(coord)

        val results = listOf(
            floodFill(x, y - 1, lockedTiles, visited, gameTeam),
            floodFill(x, y + 1, lockedTiles, visited, gameTeam),
            floodFill(x - 1, y, lockedTiles, visited, gameTeam),
            floodFill(x + 1, y, lockedTiles, visited, gameTeam)
        )

        if (results.any { it == null }) return null

        return visited
    }

    private fun isOutOfBounds(x: Int, y: Int): Boolean {
        return room.roomItems.values.none {
            it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                    it.position.x == x && it.position.y == y
        }
    }

    private fun isForeignLockedTile(x: Int, y: Int, gameTeam: GameTeam): Boolean {
        return room.roomItems.values.any {
            it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                    it.position.x == x && it.position.y == y &&
                    it.extraData.toIntOrNull()?.let { state ->
                        state % 3 == 2 && state / 3 != gameTeam.color
                    } == true
        }
    }

    private fun hasLockedTileAt(x: Int, y: Int, lockedTiles: Set<RoomItem>): Boolean {
        return lockedTiles.any { it.position.x == x && it.position.y == y }
    }

    private fun handleCounterInteraction(counter: RoomItem, state: Int) {
        when (state) {
            2 -> { // Botão Set Time
                if (!running) {
                    var currentTime = counter.extraData.toIntOrNull() ?: 0
                    // Normaliza para múltiplos de 30
                    currentTime = ((currentTime / 30) * 30)
                    if (currentTime == 0) currentTime = 30
                    else {
                        currentTime += 30
                        if (currentTime > 300) currentTime = 30
                    }
                    configuredTime = currentTime

                    // Atualiza TODOS os counters
                    room.roomItems.values
                        .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_COUNTER }
                        .forEach {
                            it.extraData = currentTime.toString()
                            it.update(updateDb = true, updateClient = true)
                        }
                }
            }

            1 -> { // Botão Start/Pause
                if (!running) {
                    if (tickCounter > 0) {
                        resume()
                    } else {
                        start()
                    }
                } else {
                    pause()
                    // Atualiza TODOS os counters
                    updateClock()
                }
            }
        }
    }

    override fun addScore(gameTeam: GameTeam, points: Int) {
        teamScores[gameTeam] = (teamScores[gameTeam] ?: 0) + points
        updateScoreboards(gameTeam)

        room.wiredHandler.triggerWired(WiredTriggerScoreAchieved::class, null, listOf(gameTeam, points))
    }

    override fun adjustClock(duration: Duration, mode: RoomGameClockAdjustMode) {
        val currentTotalSeconds = configuredTime
        val newConfiguredTime = when (mode) {
            RoomGameClockAdjustMode.INCREASE -> Duration.ofSeconds(currentTotalSeconds.toLong()).plus(duration)
                .toSeconds()

            RoomGameClockAdjustMode.DECREASE -> Duration.ofSeconds(currentTotalSeconds.toLong()).minus(duration)
                .toSeconds()

            RoomGameClockAdjustMode.SET -> duration.toSeconds()
        }.toInt()

        // Se o jogo está rodando, aplicamos o "delta" ao tempo atual
        if (running) {
            val delta = newConfiguredTime - configuredTime
            // Garante que o tempo restante não fique negativo
            timeRemaining = (timeRemaining + delta).coerceAtLeast(0)
        } else {
            // Se parado, o tempo restante é apenas o novo total
            timeRemaining = newConfiguredTime
        }

        configuredTime = newConfiguredTime

        updateClock()
    }

    override fun updateClock() {
        room.roomItems.values
            .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_COUNTER }
            .forEach { counter ->
                counter.extraData = timeRemaining.toString()
                counter.update(updateDb = false, updateClient = true)
            }
    }

    private fun updateScoreboards(gameTeam: GameTeam) {
        val score = teamScores[gameTeam] ?: 0
        val scoreboardType = when (gameTeam) {
            GameTeam.RED -> InteractionType.BATTLE_BANZAI_SCOREBOARD_RED
            GameTeam.GREEN -> InteractionType.BATTLE_BANZAI_SCOREBOARD_GREEN
            GameTeam.BLUE -> InteractionType.BATTLE_BANZAI_SCOREBOARD_BLUE
            GameTeam.YELLOW -> InteractionType.BATTLE_BANZAI_SCOREBOARD_YELLOW
            else -> return
        }

        room.roomItems.values
            .filter { it.furnishing.interactionType == scoreboardType }
            .forEach {
                it.extraData = score.toString()
                it.update(updateDb = false, updateClient = true)
            }
    }

    fun handlePuckKick(kicker: RoomUser, puck: RoomItem, velocity: Int) {
        // Se o puck já está agendado para parar ou mover, CANCELA AGORA.
        // Isso impede que o "Update" do final do movimento anterior seja enviado
        // e atropele o novo movimento que vamos iniciar.
        puckJobs[puck.id]?.cancel()
        puckJobs.remove(puck.id)

        val userId = kicker.habboSession?.userInformation?.id ?: return
        val team = userTeams[userId]

        // Direção do chute é a direção que o usuário está olhando
        val direction = kicker.bodyRotation

        // Pinta o tile de onde o puck está saindo (se tiver jogo rodando e time)
        if (team != null && running) {
            val newExtraData = team.color.toString()

            // Se a cor mudou, somos obrigados a atualizar.
            // Se a cor é igual, SÓ atualizamos se o puck estava parado (para garantir sincronia inicial).
            // Se estava andando (movingPucks continha o ID antes), NÃO mandamos update.
            if (puck.extraData != newExtraData) {
                puck.extraData = newExtraData
                puck.update(updateDb = false, updateClient = true)
            }

            // Pinta o tile de onde o puck saiu
            val startTile = room.roomItems.values.find {
                it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                        it.position.x == puck.position.x && it.position.y == puck.position.y
            }
            startTile?.let { handleTileWalk(kicker, it) }
        }

        // Marca puck como em movimento
        movingPucks.add(puck.id)

        // Atualiza timestamp de movimento (Deixa o puck "Quente")
        puckLastMoveTime[puck.id] = System.currentTimeMillis()

        // Inicia o movimento do puck com a velocidade especificada
        // Se não tiver time (jogo não rodando), passa null para currentTeam
        kickPuck(puck, kicker, direction, team, velocity, 0)
    }

    private fun kickPuck(
        puck: RoomItem,
        kicker: RoomUser,
        direction: Int,
        gameTeam: GameTeam?,
        totalSteps: Int,
        currentStep: Int
    ) {
        // Atualiza o timestamp a cada passo para manter o puck "Quente" durante o trajeto
        puckLastMoveTime[puck.id] = System.currentTimeMillis()

        if (currentStep >= totalSteps) {
            // Movimento final - reseta a cor para 0 (neutro) e salva no banco
            puck.extraData = "0"
            puck.update(updateDb = true, updateClient = true)
            movingPucks.remove(puck.id)
            return
        }

        // Calcula próximo tile baseado na direção
        val (dx, dy) = Direction.fromCode(direction).getOffset()

        val nextX = puck.position.x + dx
        val nextY = puck.position.y + dy

        // Verifica se pode mover
        val nextVector = Vector2(nextX, nextY)
        if (room.roomGamemap.isBlocked(nextVector, ignoreUsers = true)) {
            // Não pode mover - tenta direção inversa
            val inverseDirection = Direction.fromCode(direction).turnAround().code
            kickPuck(puck, kicker, inverseDirection, gameTeam, totalSteps, currentStep)
            return
        }

        // Incrementa step ANTES de calcular delay
        val nextStep = currentStep + 1

        // Calcula delay baseado no próximo step
        val delay = if (totalSteps == 1) 500L else 100L + (nextStep * 100L)

        room.setFloorItem(puck, nextVector, puck.rotation, null, rollerId = 0, rollerDelay = 0)

        // Pinta o tile apenas se o jogo estiver rodando e tiver time
        if (running && gameTeam != null) {
            val tile = room.roomItems.values.find {
                it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE &&
                        it.position.x == nextX && it.position.y == nextY
            }
            tile?.let {
                handleTileWalk(kicker, tile)
            }
        }

        val job = HabboServer.applicationScope.launch {
            delay(delay)
            // Se chegamos aqui, o job não foi cancelado, então podemos prosseguir
            kickPuck(puck, kicker, direction, gameTeam, totalSteps, nextStep)
        }

        // Registra o job para que handlePuckKick possa cancelá-lo se o usuário chutar de novo antes do delay acabar
        puckJobs[puck.id] = job
    }


    private fun resetTiles() {
        // Reseta todos os tiles para "1" (estado inicial do jogo)
        room.roomItems.values
            .filter { it.furnishing.interactionType == InteractionType.BATTLE_BANZAI_TILE }
            .forEach {
                it.extraData = "1"
                it.update(updateDb = false, updateClient = true)
            }
    }

    private fun resetScores() {
        teamScores.clear()
        TEAM_EFFECTS.keys.forEach { updateScoreboards(it) }
    }
}