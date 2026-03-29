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

package ovh.rwx.habbo.game.room.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.item.wired.trigger.UserActionTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerUserPerformsAction
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomChatMessageBubbles
import ovh.rwx.habbo.game.room.RoomChatType
import ovh.rwx.habbo.game.room.tasks.*
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.pathfinding.core.Path
import ovh.rwx.habbo.util.Direction
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class RoomUser(
    val habboSession: HabboSession?, // nullable, for in the future support bots
    val room: Room,
    val virtualID: Int,
    var currentVector3: Vector3,
    var headRotation: Int,
    var bodyRotation: Int
) : IHabboResponseSerialize {

    var updateNeeded: Boolean = false
    val statusMap: MutableMap<String, Pair<LocalDateTime?, String>> = ConcurrentHashMap()

    var objectiveVector2: Vector2? = null
    var objectiveRotation: Int = 0
    var objectiveItem: RoomItem? = null

    // Antigo stepSeatedVector3: Guarda a posição que o avatar assumirá no próximo ciclo
    var nextStepVector: Vector3? = null
    private val hasPendingStep: Boolean
        get() = nextStepVector != null

    val walking: Boolean
        get() = objectiveVector2 != null || ignoreBlocking && overrideBlocking && !walkingBlocked

    private var idleCount: Int = 0
    private var cycles: Int = 0
    private var currentCycles: Int = 0
    private var handItemCycle: Int = 0
    private var handItemCurrentCycles: Int = 0
    internal var headResetCycle: Int = 0

    var walkingBlocked: Boolean = false
    var frozen: Boolean = false
    var kicked: Boolean = false
    var ignoreBlocking: Boolean = false
    private var overrideBlocking: Boolean = false
    var rollerId: Int = -1
    var handleVendingId: Int = -1
    internal var path: MutableList<Path> = mutableListOf()

    // Buffer para teletransportes que devem ocorrer no início do próximo ciclo
    private var pendingTeleport: Vector3? = null
    private var pendingTeleportRotation: Int = -1

    var idle: Boolean = false
        set(newValue) {
            idleCount = if (newValue) {
                (TimeUnit.SECONDS.toMillis(HabboServer.habboConfig.timerConfig.roomIdleSeconds.toLong()) / HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toInt()
            } else 0

            if (field != newValue) {
                room.sendHabboResponse(Outgoing.ROOM_USER_IDLE, virtualID, newValue)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_IDLE, virtualID, newValue)
            }

            field = newValue
        }

    var typing: Boolean = false
        set(newValue) {
            if (field != newValue) {
                val state = if (newValue) 1 else 0
                room.sendHabboResponse(Outgoing.ROOM_USER_TYPING, virtualID, state)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_TYPING, virtualID, state)
            }

            field = newValue
        }

    var danceId: Int = 0
        set(newValue) {
            if (field != newValue) {
                room.sendHabboResponse(Outgoing.ROOM_USER_DANCE, virtualID, newValue)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_DANCE, virtualID, newValue)
            }

            field = newValue
        }

    var handItem: Int = 0
        set(newValue) {
            if (field != newValue) {
                room.sendHabboResponse(Outgoing.ROOM_USER_HANDITEM, virtualID, newValue)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_HANDITEM, virtualID, newValue)
            }

            field = newValue
        }

    var effect: RoomUserEffect? = null
        set(newValue) {
            if (field != newValue) {
                val effectId = newValue?.effectId ?: 0
                room.sendHabboResponse(Outgoing.ROOM_USER_EFFECT, virtualID, effectId)
                room.sendHabboResponse(OutgoingR63A.ROOM_USER_EFFECT, virtualID, effectId)
            }

            field = newValue
            lastEffect = newValue
        }

    private var lastEffect: RoomUserEffect? = null

    fun addStatus(key: String, value: String = "", milliseconds: Int = -1) {
        val hasStatus = statusMap.containsKey(key)

        val expiryTime = if (milliseconds == -1) null else {
            LocalDateTime.now().plusNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds.toLong()))
        }

        statusMap[key] = Pair(expiryTime, value)

        if (!hasStatus) {
            updateNeeded = true

            when (key) {
                "sit" -> triggerWiredAction(WiredTriggerUserPerformsAction.WiredUserAction.SIT)
                "lay" -> triggerWiredAction(WiredTriggerUserPerformsAction.WiredUserAction.LAY)
            }
        }
    }

    fun removeStatus(key: String) {
        val removed = statusMap.remove(key)

        updateNeeded = true

        if (removed != null && (key == "sit" || key == "lay")) {
            triggerWiredAction(WiredTriggerUserPerformsAction.WiredUserAction.STAND)
        }
    }

    private fun triggerWiredAction(action: WiredTriggerUserPerformsAction.WiredUserAction) {
        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerUserPerformsAction::class, this,
            UserActionTriggerData(action)
        )
    }

    fun onCycle() {
        processExpiredStatuses()

        // 1. Processa teleporte agendado no tick anterior (Dá tempo do cliente ver o piso aceso)
        processPendingTeleport()

        // 2. Efetiva o passo da caminhada normal
        commitPendingMovementStep()
        processTimers()

        // 3. Calcula o próximo passo
        if (walking) {
            processWalking()
        } else if (!idle) {
            handleIdleCounter()
        }
    }

    private fun processExpiredStatuses() {
        val now = LocalDateTime.now()
        statusMap.entries.forEach { (key, value) ->
            if (value.first != null && now.isAfter(value.first)) {
                removeStatus(key)
            }
        }
    }

    private fun commitPendingMovementStep() {
        if (!hasPendingStep) return

        val oldPos = currentVector3.copy()
        val nextPos = nextStepVector!!

        // 1. Oficializa a posição física
        this.currentVector3 = nextPos
        this.nextStepVector = null
        room.roomGamemap.updateRoomUserMovement(this, oldPos.vector2, currentVector3.vector2)

        val oldItem = room.roomGamemap.getHighestItem(oldPos.vector2)
        val newItem = room.roomGamemap.getHighestItem(currentVector3.vector2)

        // 2. DISPARO DE EVENTOS: Aqui você "pisou" oficialmente no gatilho (12, 6)
        if (oldItem != newItem) {
            oldItem?.onUserWalksOff(this, true)

            // Se o newItem for um gatilho Wired, ele chamará teleportTo()
            // Isso vai agendar o pendingTeleport para o PRÓXIMO ciclo
            newItem?.onUserWalksOn(this, true)
        }

        this.updateNeeded = true
    }

    private fun processTimers() {
        // Ciclo do item na mão (bebida/comida)
        if (handItemCycle > 0 && ++handItemCurrentCycles >= handItemCycle) {
            if (handItem > 0) {
                handItemCurrentCycles = 0
                handItemCycle = 0

                carryHandItem(0)
            }
        }

        // Reseta a rotação da cabeça se o usuário parou
        if (headResetCycle > 0 && --headResetCycle == 0) {
            if (!walking && !idle) {
                headRotation = bodyRotation
                updateNeeded = true
            }
        }

        // Efeitos temporários
        effect?.let {
            if (it.hasEffect && it.duration-- <= 0) effect = null
        }

        // Ciclos de ações agendadas (ex: máquina de vendas)
        if (cycles > 0 && ++currentCycles >= cycles) {
            if (handleVendingId > 0) {
                handItemCycle = 240

                carryHandItem(handleVendingId)

                handleVendingId = 0
            }

            walkingBlocked = false

            cycles = 0
            currentCycles = 0
        }
    }

    private fun processWalking() {
        if (frozen) {
            stopWalking()
            updateNeeded = true
            return
        }

        if (objectiveVector2 == currentVector3.vector2) {
            stopWalking()
            return
        }

        if (path.isEmpty()) calculatePath()

        if (path.isEmpty()) {
            stopWalking()
            return
        }

        var step = path.removeAt(0)
        var stepVector2 = Vector2(step.x, step.y)

        // Usa a função centralizada do Gamemap para avaliar tudo (itens, usuários, buracos no mapa)
        if (room.roomGamemap.isBlocked(
                stepVector2,
                ignoreUsers = ignoreBlocking,
                overrideBlocking = overrideBlocking
            )
        ) {
            calculatePath() // Recalcula a rota, pois algo bloqueou o caminho (usuário ou mobi)

            if (path.isEmpty()) {
                stopWalking()
                return
            }

            step = path.removeAt(0)
            stepVector2 = Vector2(step.x, step.y)
        }

        // ATENÇÃO: A checagem de altura CONTINUA necessária logo abaixo!
        // O isBlocked diz se o piso X,Y está livre, mas não avalia se o degrau é muito alto para subir.
        if (!ignoreBlocking && !overrideBlocking) {
            val currentHeight = room.roomGamemap.getAbsoluteHeight(currentVector3.x, currentVector3.y)
            val stepHeight = room.roomGamemap.getAbsoluteHeight(step.x, step.y)

            if (stepHeight - currentHeight > 3) {
                stopWalking()
                return
            }
        }

        handleTileTransition(stepVector2)
    }

    private fun handleTileTransition(stepVector2: Vector2) {
        // Apenas preparamos o terreno. O "pisar" oficial ocorre no commit.
        if (rollerId == -1) {
            bodyRotation = Direction.calculate(currentVector3.x, currentVector3.y, stepVector2.x, stepVector2.y)
            headRotation = bodyRotation
        }

        if (stepVector2 == room.roomModel.doorVector3.vector2) {
            room.userManager.removeUser(this, notifyClient = true, kickNotification = kicked)
            return
        }

        val z = room.roomGamemap.getAbsoluteHeight(stepVector2)

        // Prepara a posição para ser aplicada no próximo ciclo
        nextStepVector = Vector3(stepVector2, z)

        if (rollerId == -1 && walking) {
            removeUserStatuses()
            addStatus("mv", "${stepVector2.x},${stepVector2.y},$z")

            // ACH_LegDay: caminhar quadrados
            habboSession?.let {
                HabboServer.habboGame.achievementManager.progress(it, "ACH_LegDay", 1, accumulate = true)
            }
        }
    }

    private fun handleIdleCounter() {
        idleCount++
        val secondsIdle =
            TimeUnit.MILLISECONDS.toSeconds((idleCount * HabboServer.habboConfig.roomTaskConfig.delayMilliseconds).toLong())

        if (secondsIdle >= HabboServer.habboConfig.timerConfig.roomIdleSeconds) {
            idle = true
        }
    }

    fun moveTo(
        vector2: Vector2,
        rotation: Int = -1,
        rollerId: Int = -1,
        ignoreBlocking: Boolean = false,
        actingItem: RoomItem? = null
    ) = moveTo(vector2.x, vector2.y, rotation, rollerId, ignoreBlocking, actingItem)

    fun moveTo(
        x: Int,
        y: Int,
        rotation: Int = -1,
        rollerId: Int = -1,
        ignoreBlocking: Boolean = false,
        actingItem: RoomItem? = null
    ): Boolean {
        if (frozen || (!ignoreBlocking && !overrideBlocking && walkingBlocked)) return false

        val currentUserItem = room.roomGamemap.getHighestItem(currentVector3.vector2)
        var destinationVector2 = Vector2(x, y)

        if (currentUserItem != null) {
            val affectedTiles = HabboServer.habboGame.itemManager.getAffectedTiles(
                currentUserItem.position.x,
                currentUserItem.position.y,
                currentUserItem.rotation,
                currentUserItem.furnishing.width,
                currentUserItem.furnishing.length
            )

            if (affectedTiles.any { it == destinationVector2 }) {
                val xAxisChanged = currentVector3.x != x
                val yAxisChanged = currentVector3.y != y

                val shouldBlock = when (currentUserItem.rotation) {
                    0, 4 -> !xAxisChanged && yAxisChanged
                    2, 6 -> xAxisChanged && !yAxisChanged
                    else -> false
                }

                if (shouldBlock) return false
            }
        }

        val destinationItem = room.roomGamemap.getHighestItem(destinationVector2)
        if (destinationItem != null && destinationItem.furnishing.interactionType == InteractionType.BED) {
            destinationVector2 = when (destinationItem.rotation) {
                0, 4 -> Vector2(x, destinationItem.position.y)
                2, 6 -> Vector2(destinationItem.position.x, y)
                else -> Vector2(destinationItem.position.x, destinationItem.position.y)
            }
        }

        room.roomTask?.addTask(
            room,
            UserMoveTask(this, destinationVector2, rotation, actingItem, ignoreBlocking, rollerId)
        )

        return true
    }

    fun chat(
        virtualID: Int,
        message: String,
        bubble: RoomChatMessageBubbles,
        type: RoomChatType,
        skipCommands: Boolean
    ) {
        room.roomTask?.addTask(room, UserChatTask(this, virtualID, message, bubble, type, skipCommands))
    }

    fun action(action: UserAction) {
        room.roomTask?.addTask(room, UserActionTask(this, action))
    }

    fun sign(sign: Int) {
        room.roomTask?.addTask(room, UserSignTask(this, sign))
    }

    fun dance(danceId: Int) {
        room.roomTask?.addTask(room, UserDanceTask(this, danceId))
    }

    fun vendingMachine(handItem: Int) {
        room.roomTask?.addTask(room, UserVendingMachineTask(this, handItem))
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun carryHandItem(handItem: Int) {
        room.roomTask?.addTask(room, UserHandItemTask(this, handItem))
    }

    fun requestCycles(cycles1: Int) {
        if (currentCycles == 0 || cycles1 == 0) {
            cycles = cycles1
            currentCycles = 0
        }
    }

    fun stopWalking() {
        path.clear()
        nextStepVector = null
        objectiveVector2 = null
        ignoreBlocking = false

        removeStatus("mv")

        if (objectiveRotation != -1) {
            headRotation = objectiveRotation
            bodyRotation = objectiveRotation

            objectiveRotation = -1
        }

        objectiveItem?.let { item ->
            item.furnishing.interactor?.onTrigger(room, this, item, room.userManager.hasRights(habboSession, false), 0)
            objectiveItem = null
        }

        room.roomGamemap.getHighestItem(currentVector3.vector2)?.let { addUserStatuses(it) }
    }

    private fun calculatePath() {
        if (objectiveVector2 == null) return

        path = room.pathfinder.findPath(
            room.roomGamemap.grid,
            currentVector3.x,
            currentVector3.y,
            objectiveVector2!!.x,
            objectiveVector2!!.y,
            ignoreBlocking || overrideBlocking
        ).toMutableList()
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            // todo: bot
            habboSession?.let {
                writeInt(it.userInformation.id)
                writeUTF(it.userInformation.username)
                writeUTF(it.userInformation.motto)
                writeUTF(it.userInformation.figure)
                writeInt(virtualID)
                writeInt(currentVector3.x)
                writeInt(currentVector3.y)
                writeUTF(currentVector3.z.toString())
                writeInt(0) // 4 or 2 ?
                writeInt(1) // 1 for user, 2 for pet, 3 for bot.
                writeUTF(it.userInformation.gender.lowercase(Locale.getDefault()))

                val group = habboSession.userStats.favoriteGroup

                if (group == null) {
                    writeInt(-1)
                    writeInt(0)
                    writeUTF("")
                } else {
                    writeInt(group.groupData.id)
                    writeInt(0)
                    writeUTF(group.groupData.name)
                }

                writeUTF("")
                writeInt(habboSession.userStats.achievementScore)
                writeBoolean(false) // is member of builder club
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            habboSession?.let {
                writeInt(it.userInformation.id)
                writeUTF(it.userInformation.username)
                writeUTF(it.userInformation.motto)
                if (isVersionBefore(2009, 6, 17)) {
                    // todo - figure
                    writeUTF("hr-100.hd-180-7.ch-215-66.lg-270-79.sh-305-62.ha-1002-70.wa-2007")
                } else {
                    writeUTF(it.userInformation.figure)
                }
                writeInt(virtualID)
                writeInt(currentVector3.x)
                writeInt(currentVector3.y)
                writeUTF(currentVector3.z.toString())
                writeInt(bodyRotation)
                writeInt(1) // 1 for user, 2 for pet, 3 for bot.
                writeUTF(it.userInformation.gender.lowercase(Locale.getDefault()))

//                val group = habboSession.userStats.favoriteGroup

                writeInt(-1) // xp
//                    if (group == null) {
                writeInt(-1)
                writeInt(-1)
                writeUTF("")
                // bugged as fuck
//                    } else {
//                        writeInt(group.groupData.id)
//                        writeInt(-1)
//                        writeUTF(group.groupData.name)
//                    }

                if (isVersionAtLeast(2010, 12, 3)) {
                    writeInt(habboSession.userStats.achievementScore)
                }
            }
        }
    }

    fun removeUserStatuses() {
        removeStatus("sit")
        removeStatus("lay")
        // todo: remove effects
        updateNeeded = true
    }

    fun addUserStatuses(roomItem: RoomItem) {
        if (roomItem.furnishing.canSit || roomItem.furnishing.interactionType == InteractionType.BED) {
            addStatus(if (roomItem.furnishing.canSit) "sit" else "lay", roomItem.height.toString())
            bodyRotation = roomItem.rotation
            headRotation = roomItem.rotation
            // todo: add effects

            if (roomItem.furnishing.interactionType == InteractionType.BED) {
                val oldVector3 = currentVector3

                // Pegamos a posição atual onde o usuário parou (o tile que ele clicou)
                val userPosition = oldVector3.vector2

                // A posição final será:
                // 1. A altura (Z) do item.
                // 2. O X e Y dependem da rotação.
                val finalX: Int
                val finalY: Int

                when (roomItem.rotation) {
                    0, 4 -> {
                        // Cama vertical: Mantemos o X do usuário (lado) e forçamos o Y do item (cabeceira)
                        finalX = userPosition.x
                        finalY = roomItem.position.y
                    }

                    2, 6 -> {
                        // Cama horizontal: Mantemos o Y do usuário (lado) e forçamos o X do item (cabeceira)
                        finalX = roomItem.position.x
                        finalY = userPosition.y
                    }

                    else -> {
                        finalX = roomItem.position.x
                        finalY = roomItem.position.y
                    }
                }

                currentVector3 = Vector3(finalX, finalY, roomItem.position.z)

                // Atualiza o mapa de usuários para o novo tile (caso tenha mudado)
                if (oldVector3.x != finalX || oldVector3.y != finalY) {
                    room.roomGamemap.updateRoomUserMovement(this, oldVector3.vector2, Vector2(finalX, finalY))
                }
            }
        }

        updateNeeded = true
    }

    fun teleportTo(vector2: Vector2, rotation: Int = -1, showSlide: Boolean = false) {
        this.stopWalking()

        val z = room.roomGamemap.getAbsoluteHeight(vector2.x, vector2.y)
        this.pendingTeleport = Vector3(vector2, z)
        this.pendingTeleportRotation = rotation
        this.effect = RoomUserEffect(4, 5)
        this.updateNeeded = true
    }

    private fun processPendingTeleport() {
        val target = pendingTeleport ?: return
        pendingTeleport = null

        val oldPos = currentVector3.copy()
        val oldItem = room.roomGamemap.getHighestItem(oldPos.vector2)

        // Agora o oldItem será o seu gatilho, e a luz apagará!
        oldItem?.onUserWalksOff(this, true)
        this.removeUserStatuses()

        room.roomGamemap.updateRoomUserMovement(this, oldPos.vector2, target.vector2)
        this.currentVector3 = target

        val newItem = room.roomGamemap.getHighestItem(target.vector2)
        if (newItem != null) {
            this.addUserStatuses(newItem)
            newItem.onUserWalksOn(this, true)
        }

        this.updateNeeded = true
    }
}