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

package ovh.rwx.habbo.game.snowwar

import ovh.rwx.habbo.communication.outgoing.gamecenter.Game2FullGameStatusData
import ovh.rwx.habbo.communication.outgoing.gamecenter.Game2GameStatusData
import ovh.rwx.habbo.game.snowwar.enums.SnowWarActivityState
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.snowwar.objects.SnowWarGameObject
import ovh.rwx.habbo.game.snowwar.objects.SnowWarMachine
import ovh.rwx.habbo.game.snowwar.objects.SnowWarPile
import ovh.rwx.habbo.game.snowwar.objects.SnowWarSnowball
import ovh.rwx.habbo.game.snowwar.objects.SnowWarTree
import ovh.rwx.habbo.game.snowwar.utils.QuickRandom
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simulação determinística da arena SnowWar: campo (heightmap, blockedTiles, objetos),
 * física em 3 subturnos por tick de 150ms, aplicação de eventos e checksum.
 *
 * Sem rede, sem executor, sem config — os adapters atravessam o seam `tick()` /
 * `queueEvent()` / `fullStatusData()`: o [SnowWarGame] liga broadcast + relógio + config,
 * e os testes fazem replay headless (checksum vira assert, não crash de cliente).
 */
class SnowWarArena(val users: ConcurrentHashMap<Int, SnowWarUser>) {
    val blockedTiles = HashSet<Pair<Int, Int>>()
    val machines = CopyOnWriteArrayList<SnowWarMachine>()
    val piles = CopyOnWriteArrayList<SnowWarPile>()
    val trees = CopyOnWriteArrayList<SnowWarTree>()
    val snowballs = CopyOnWriteArrayList<SnowWarSnowball>()
    val nextObjectId = AtomicInteger(100)
    var arenaHeightMapRows = emptyList<String>()

    /**
     * Produtor de eventos síncrono ao tick (ex.: SnowWarBotAI), invocado dentro do lock
     * com o turno corrente. Null em replay headless.
     */
    var onBeforeTick: ((turn: Int) -> Unit)? = null

    @Volatile
    var lastCompletedTurn = 0
        private set

    @Volatile
    var lastCompletedChecksum = 0
        private set

    private val turn = AtomicInteger()
    private val pendingEvents = ConcurrentLinkedQueue<Pair<Int, ISnowWarGameEvent>>()
    private val deferredEvents = ConcurrentLinkedQueue<Pair<Int, ISnowWarGameEvent>>()

    fun queueEvent(subTurn: Int, event: ISnowWarGameEvent) {
        pendingEvents.add(subTurn.coerceIn(0, 2) to event)
    }

    /**
     * Inicializa o estado do campo a partir do mapa da arena (máquinas, pilhas, árvores,
     * blockedTiles) e posiciona os spawns dos usuários.
     */
    fun setupField(arena: SnowWarArenaData) {
        arenaHeightMapRows = arena.heightMap.split("\r")

        machines.clear()
        piles.clear()
        trees.clear()
        snowballs.clear()
        blockedTiles.clear()
        nextObjectId.set(100)

        for (obj in arena.fuseObjects) {
            when {
                obj.name in listOf("snowball_machine", "s_snowball_machine") -> {
                    machines.add(SnowWarMachine(nextObjectId.getAndIncrement(), obj.x, obj.y, obj.direction, 5, obj.id))
                }

                obj.name.startsWith("snst_tree") -> {
                    trees.add(SnowWarTree(nextObjectId.getAndIncrement(), obj.x, obj.y, obj.direction, 3200, obj.id, 3))
                }

                obj.name == "snst_ballpile" -> {
                    piles.add(SnowWarPile(nextObjectId.getAndIncrement(), obj.x, obj.y, 12, obj.id))
                }
            }
        }

        for (tree in trees) {
            blockedTiles.add(tree.x to tree.y)
        }
        for (machine in machines) {
            blockedTiles.add(machine.x to machine.y)
        }
        for (obj in arena.fuseObjects) {
            val furnishing = obj.furnishing
            if (furnishing != null && !furnishing.walkable) {
                var w = furnishing.width
                var l = furnishing.length
                if (obj.direction == 2 || obj.direction == 6) {
                    val tmp = w
                    w = l
                    l = tmp
                }
                for (dx in 0 until w) {
                    for (dy in 0 until l) {
                        blockedTiles.add((obj.x + dx) to (obj.y + dy))
                    }
                }
            }
        }

        val blueSpawns = (arena.blueSpawns.takeIf { it.isNotEmpty() } ?: listOf(22 to 9)).shuffled()
        val redSpawns = (arena.redSpawns.takeIf { it.isNotEmpty() } ?: listOf(30 to 43)).shuffled()

        var blueIndex = 0
        var redIndex = 0

        users.values.toList().forEachIndexed { index, user ->
            val spawnTile = if (user.team == SnowWarTeam.BLUE) {
                blueSpawns[blueIndex++ % blueSpawns.size]
            } else {
                redSpawns[redIndex++ % redSpawns.size]
            }
            user.setupSpawn(index, spawnTile, arena.width, arena.height)
        }
    }

    /**
     * Reinicia o relógio de turnos; retorna o checksum do turno 0.
     */
    fun resetTurns(): Int {
        turn.set(0)
        pendingEvents.clear()
        deferredEvents.clear()
        lastCompletedTurn = 0
        lastCompletedChecksum = calculateChecksum(0)
        return lastCompletedChecksum
    }

    /**
     * Um tick de 150ms (3 subturnos). Mutex com [fullStatusData] para impedir drift de
     * turno (ver GLOSSARY / snowwar_desync_manual). Retorna null quando a arena não tem
     * jogadores — o adapter deve parar o relógio.
     */
    fun tick(): Game2GameStatusData? = synchronized(this) {
        if (users.isEmpty()) return null

        val currentTurn = turn.getAndIncrement()

        // 1. Process bot AI decisions (produtores de eventos, dentro do mesmo lock)
        onBeforeTick?.invoke(currentTurn)

        val subTurns = mutableListOf<MutableList<ISnowWarGameEvent>>()
        repeat(3) { subTurns.add(mutableListOf()) }

        val eventsToApply = mutableListOf<MutableList<ISnowWarGameEvent>>()
        repeat(3) { eventsToApply.add(mutableListOf()) }

        val eventsToSend = mutableListOf<MutableList<ISnowWarGameEvent>>()
        repeat(3) { eventsToSend.add(mutableListOf()) }

        // 2. Process deferred events: aplica agora o que foi enviado no turno passado
        while (true) {
            val item = deferredEvents.poll() ?: break
            val subIndex = item.first.coerceIn(0, 2)
            eventsToApply[subIndex].add(item.second)
        }

        // 3. Process pending user/bot events: envia agora, mas aplica na física só no próximo turno
        while (true) {
            val item = pendingEvents.poll() ?: break
            val subIndex = item.first.coerceIn(0, 2)
            deferredEvents.add(subIndex to item.second)
            eventsToSend[subIndex].add(item.second)
        }

        // Simulação física em 3 subturnos
        for (sub in 0 until 3) {
            val subturnEvents = subTurns[sub]

            // Phase 0: Aplica ações sincronizadas no cliente
            for (ev in eventsToApply[sub]) {
                ev.apply(this)
            }

            // Phase 1: Move players (em ordem determinística por objectId, idêntico ao AS3 SynchronizedGameArena)
            for (user in users.values.sortedBy { it.objectId }) {
                user.subturn { tx, ty -> isTileWalkable(tx, ty, user) }
            }

            // Phase 2: Move snowballs & test collisions
            for (ball in snowballs.toList()) {
                if (!ball.alive) continue
                ball.calculateFrameMovement()
                if (checkObjectCollision(ball) || ball.hasFloorCollision(arenaHeightMapRows)) {
                    ball.kill()
                    snowballs.remove(ball)
                }
            }

            // Phase 3: Generator ticks das máquinas
            for (machine in machines) {
                if (machine.processGeneratorTick()) {
                    deferredEvents.add(sub to MachineCreatesSnowballGameEvent(machine.objectId))
                    eventsToSend[sub].add(MachineCreatesSnowballGameEvent(machine.objectId))
                }
            }

            // Phase 4: Coleta de bolas em máquinas e pilhas (em ordem determinística por objectId)
            for (user in users.values.sortedBy { it.objectId }) {
                if (!user.tickSnowballPickupTimer()) continue

                val targetMachine = machines.find { it.canPlayerPickup(user) }
                if (targetMachine != null) {
                    targetMachine.reservePickup()
                    deferredEvents.add(
                        sub to HumanGetsSnowballsFromMachineGameEvent(
                            user.objectId,
                            targetMachine.objectId
                        )
                    )
                    eventsToSend[sub].add(
                        HumanGetsSnowballsFromMachineGameEvent(
                            user.objectId,
                            targetMachine.objectId
                        )
                    )
                    continue
                }

                val targetPile = piles.find { it.canPlayerPickup(user) }
                if (targetPile != null) {
                    targetPile.reservePickup()
                    deferredEvents.add(
                        sub to HumanGetsSnowballsFromMachineGameEvent(
                            user.objectId,
                            targetPile.objectId
                        )
                    )
                    eventsToSend[sub].add(
                        HumanGetsSnowballsFromMachineGameEvent(
                            user.objectId,
                            targetPile.objectId
                        )
                    )
                }
            }
        }

        // Phase 5: Checksum sempre por último!
        val cs = calculateChecksum(currentTurn)

        for (sub in 0 until 3) {
            eventsToSend[sub].addAll(subTurns[sub])
        }

        lastCompletedTurn = currentTurn
        lastCompletedChecksum = cs

        Game2GameStatusData(
            turn = currentTurn,
            checksum = cs,
            subTurns = eventsToSend
        )
    }

    /**
     * Snapshot completo para FullGameStatus — mesmo lock do tick: serializa o turno
     * completado + eventos deferidos pendentes, sem dropar eventos em voo.
     */
    fun fullStatusData(durationSeconds: Int, numberOfTeams: Int): Game2FullGameStatusData = synchronized(this) {
        val currentTurn = lastCompletedTurn
        val cs = lastCompletedChecksum
        val elapsedSeconds = (currentTurn * 150) / 1000
        val remaining = (durationSeconds - elapsedSeconds).coerceAtLeast(0)

        val eventsBySubTurn = listOf(
            mutableListOf<ISnowWarGameEvent>(),
            mutableListOf(),
            mutableListOf()
        )
        for ((sub, ev) in deferredEvents) {
            eventsBySubTurn[sub.coerceIn(0, 2)].add(ev)
        }

        val statusData = Game2GameStatusData(
            turn = currentTurn,
            checksum = cs,
            subTurns = eventsBySubTurn
        )

        val allObjects = mutableListOf<SnowWarGameObject>()
        allObjects.addAll(machines)
        allObjects.addAll(piles)
        allObjects.addAll(trees)
        allObjects.addAll(users.values.sortedBy { it.objectId })
        allObjects.addAll(snowballs.filter { it.alive })

        Game2FullGameStatusData(
            remainingTimeSeconds = remaining,
            durationInSeconds = durationSeconds,
            objects = allObjects,
            numberOfTeams = numberOfTeams,
            status = statusData
        )
    }

    fun isTileWalkable(tileX: Int, tileY: Int, movingUser: SnowWarUser): Boolean {
        if (tileY !in 0 until arenaHeightMapRows.size) return false
        val row = arenaHeightMapRows[tileY]
        if (tileX !in row.indices) return false
        val c = row[tileX]
        if (c == 'x' || c == 'X') return false
        if ((tileX to tileY) in blockedTiles) return false
        for (pile in piles) {
            if (pile.snowballCount > 0 && pile.x == tileX && pile.y == tileY) return false
        }
        for (u in users.values) {
            if (u.objectId == movingUser.objectId || !u.isAlive()) continue
            val occX = if (u.nextTileX != -1) u.nextTileX else u.currentTileX
            val occY = if (u.nextTileY != -1) u.nextTileY else u.currentTileY
            if (occX == tileX && occY == tileY) return false
        }
        return true
    }

    fun isValidTile(tileX: Int, tileY: Int): Boolean {
        if (tileY !in arenaHeightMapRows.indices) return false
        val row = arenaHeightMapRows[tileY]
        if (tileX !in row.indices) return false
        val c = row[tileX]
        return c != 'x' && c != 'X'
    }

    fun calculateChecksum(turnNumber: Int): Int {
        var checksum = QuickRandom.iterateSeed(turnNumber)

        for (machine in machines) {
            if (machine.isAlive()) {
                checksum += machine.getChecksumContribution()
            }
        }

        for (pile in piles) {
            if (pile.isAlive()) {
                checksum += pile.getChecksumContribution()
            }
        }

        for (tree in trees) {
            if (tree.isAlive()) {
                checksum += tree.getChecksumContribution()
            }
        }

        for (user in users.values.sortedBy { it.objectId }) {
            if (user.isAlive()) {
                checksum += user.getChecksumContribution()
            }
        }

        for (ball in snowballs) {
            if (ball.alive) {
                checksum += ball.getChecksumContribution()
            }
        }

        return checksum
    }

    private fun checkObjectCollision(ball: SnowWarSnowball): Boolean {
        val collisionTiles = getCollisionTiles(ball)
        for (tile in collisionTiles) {
            for (tree in trees) {
                if (tree.x == tile.first && tree.y == tile.second && tree.testCollision(ball)) {
                    tree.hit()
                    ball.thrower.treeHits.incrementAndGet()
                    return true
                }
            }
            for (machine in machines) {
                if (machine.x == tile.first && machine.y == tile.second && machine.testCollision(ball)) {
                    return true
                }
            }
            for (pile in piles) {
                if (pile.x == tile.first && pile.y == tile.second && pile.testCollision(ball)) {
                    return true
                }
            }
            for (user in users.values.sortedBy { it.objectId }) {
                // No AS3, quando um avatar está andando, ele desocupa o currentTile e ocupa o nextTile
                val occX = if (user.nextTileX != -1) user.nextTileX else user.currentTileX
                val occY = if (user.nextTileY != -1) user.nextTileY else user.currentTileY
                if (occX != tile.first || occY != tile.second || !user.testCollision(ball)) {
                    continue
                }
                if (user.team != ball.thrower.team) {
                    applyAvatarHit(user, ball)
                }
                return true
            }
        }
        return false
    }

    private fun applyAvatarHit(player: SnowWarUser, ball: SnowWarSnowball) {
        if (player.hitPoints > 0) {
            val thrower = ball.thrower
            if (player.hitPoints == 1) {
                // AS3 playerFallsDown: HP decrementa para 0, atordoado e adiciona 1 (hit) + 5 (knockdown) = 6 pontos
                player.hitPoints = 0
                player.stopWalking()
                player.activityState = SnowWarActivityState.STUNNED
                player.activityTimer = SnowWarMath.STUNNED_TIMER
                player.bodyDirection = (SnowWarMath.direction360To8(ball.direction) + 4) % 8
                thrower.score.addAndGet(6)
                thrower.hits.incrementAndGet()
                thrower.kills.incrementAndGet()
                player.deaths.incrementAndGet()
            } else {
                player.hitPoints -= 1
                thrower.score.addAndGet(1)
                thrower.hits.incrementAndGet()
            }
        }
    }

    private fun getCollisionTiles(ball: SnowWarSnowball): List<Pair<Int, Int>> {
        val currentX = SnowWarMath.worldToTile(ball.locH)
        val currentY = SnowWarMath.worldToTile(ball.locV)
        // No AS3 SnowBallGameObject.subturn():
        // var tile = arena.getTileAt(tileX, tileY);
        // if (tile) { ... testCollisions ... }
        // Se a bola estiver voando sobre um buraco ('x'), getTileAt retorna null e nenhum teste de colisão é executado!
        if (!isValidTile(currentX, currentY)) {
            return emptyList()
        }

        val direction = SnowWarMath.direction360To8(ball.direction)
        val offsetX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val offsetY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
        val left = (direction - 1 + 8) % 8
        val right = (direction + 1) % 8

        val tiles = mutableListOf(currentX to currentY)
        val dirX = currentX + offsetX[direction]
        val dirY = currentY + offsetY[direction]
        if (isValidTile(dirX, dirY)) tiles.add(dirX to dirY)

        val leftX = currentX + offsetX[left]
        val leftY = currentY + offsetY[left]
        if (isValidTile(leftX, leftY)) tiles.add(leftX to leftY)

        val rightX = currentX + offsetX[right]
        val rightY = currentY + offsetY[right]
        if (isValidTile(rightX, rightY)) tiles.add(rightX to rightY)

        return tiles
    }
}
