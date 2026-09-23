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

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.wired.WiredVariableDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.wired.addon.WiredAddonType
import ovh.rwx.habbo.game.item.wired.trigger.VariableTriggerData
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerVariableChanged
import ovh.rwx.habbo.game.item.wired.variable.*
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.games.GameTeam
import ovh.rwx.habbo.game.room.user.*
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class WiredVariableManager(private val room: Room) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Definições de variáveis registradas no quarto
    private val definitions: MutableMap<String, WiredVariable> = ConcurrentHashMap()

    // Valores das variáveis por escopo
    private val roomValues: MutableMap<String, WiredVariableValue> = ConcurrentHashMap()
    private val userValues: MutableMap<Int, MutableMap<String, WiredVariableValue>> = ConcurrentHashMap()
    private val furniValues: MutableMap<Int, MutableMap<String, WiredVariableValue>> = ConcurrentHashMap()

    // Cache de hash
    @Volatile
    private var cachedHash: Int = 0

    @Volatile
    private var hashDirty: Boolean = true

    // Histórico de versões por variável para cálculo de diffs
    private val variableHashes: MutableMap<String, Int> = ConcurrentHashMap()

    companion object {
        const val MAX_PERMANENT_FURNI_VARIABLES = 1000
        const val MAX_PERMANENT_USER_VARIABLES = 1000
        const val MAX_PERMANENT_GLOBAL_VARIABLES = 100
    }

    fun getDefinitions(): List<WiredVariable> =
        definitions.values.toList() + InternalVariableDefinition.ALL_VARIABLES

    fun getDefinition(variableId: String): WiredVariable? =
        definitions[variableId] ?: InternalVariableDefinition.find(variableId)?.toWiredVariable()

    fun registerVariable(
        variable: WiredVariable,
        initialValue: Any = 0,
        ownerId: Int = room.roomData.id,
        ownerType: VariableOwnerType = VariableOwnerType.ROOM
    ) {
        definitions[variable.variableId] = variable
        variableHashes[variable.variableId] = computeSingleVariableHash(variable)
        setVariableValue(variable.variableId, initialValue, ownerId, ownerType)
        markDirty()
    }

    fun getFurniVariables(furniId: Int): Map<String, WiredVariableValue> {
        val map = mutableMapOf<String, WiredVariableValue>()
        furniValues[furniId]?.let { map.putAll(it) }
        InternalVariableDefinition.entries.filter { it.variableTarget == WiredVariableTarget.FURNI }.forEach { def ->
            getFurniInternalVariable(def, furniId)?.let { map[def.variableId] = it }
        }
        return map
    }

    fun getUserVariables(userId: Int): Map<String, WiredVariableValue> {
        val map = mutableMapOf<String, WiredVariableValue>()
        userValues[userId]?.let { map.putAll(it) }
        InternalVariableDefinition.entries.filter { it.variableTarget == WiredVariableTarget.USER }.forEach { def ->
            getUserInternalVariable(def, userId)?.let { map[def.variableId] = it }
        }
        return map
    }

    fun getRoomVariables(): Map<String, WiredVariableValue> {
        val map = mutableMapOf<String, WiredVariableValue>()
        roomValues.let { map.putAll(it) }
        InternalVariableDefinition.entries.filter { it.variableTarget == WiredVariableTarget.GLOBAL }.forEach { def ->
            map[def.variableId] = getRoomInternalVariable(def)
        }
        return map
    }

    fun getHoldersForVariable(variableId: String): List<Pair<Int, Int>> {
        val internalDef = InternalVariableDefinition.find(variableId)
        if (internalDef != null) {
            val result = mutableListOf<Pair<Int, Int>>()
            when (internalDef.variableTarget) {
                WiredVariableTarget.FURNI -> {
                    room.itemManager.items.values.forEach { item ->
                        val v = getFurniInternalVariable(internalDef, item.id)
                        if (v != null) {
                            val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                            result.add(item.id to intVal)
                        }
                    }
                }

                WiredVariableTarget.USER -> {
                    room.userManager.entities.values.forEach { entity ->
                        val v = getUserInternalVariable(internalDef, entity.virtualID)
                        if (v != null) {
                            val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                            val id = (entity as? RoomUser)?.habboSession?.userInformation?.id ?: entity.virtualID
                            result.add(id to intVal)
                        }
                    }
                }

                WiredVariableTarget.GLOBAL -> {
                    val v = getRoomInternalVariable(internalDef)
                    val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                    result.add(room.roomData.id to intVal)
                }

                WiredVariableTarget.CONTEXT -> {
                    result.add(0 to 0)
                }
            }
            return result
        }

        val def = definitions[variableId] ?: return emptyList()
        val result = mutableListOf<Pair<Int, Int>>()
        when (def.variableTarget) {
            WiredVariableTarget.FURNI -> {
                furniValues.forEach { (furniId, vars) ->
                    val v = vars[variableId]
                    if (v != null) {
                        val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                        result.add(furniId to intVal)
                    }
                }
            }

            WiredVariableTarget.USER -> {
                userValues.forEach { (userId, vars) ->
                    val v = vars[variableId]
                    if (v != null) {
                        val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                        result.add(userId to intVal)
                    }
                }
            }

            WiredVariableTarget.GLOBAL -> {
                val v = roomValues[variableId]
                if (v != null) {
                    val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                    result.add(room.roomData.id to intVal)
                }
            }

            WiredVariableTarget.CONTEXT -> {
                val v = roomValues[variableId]
                if (v != null) {
                    val intVal = (v.value as? Number)?.toInt() ?: v.value.toString().toIntOrNull() ?: 0
                    result.add(0 to intVal)
                }
            }
        }
        return result
    }

    fun deleteAllHoldersForVariable(variableId: String) {
        val internalDef = InternalVariableDefinition.find(variableId)
        if (internalDef != null) return // Não é permitido deletar holders de variáveis internas

        val def = definitions[variableId]
        roomValues.remove(variableId)
        userValues.values.forEach { it.remove(variableId) }
        furniValues.values.forEach { it.remove(variableId) }
        markDirty()
        if (def != null && def.availabilityType.isPersistent) {
            WiredVariableDao.deleteVariable(variableId, room.roomData.id, VariableOwnerType.ROOM)
        }
    }

    fun getVariableValue(
        variableId: String,
        ownerId: Int = room.roomData.id,
        ownerType: VariableOwnerType = VariableOwnerType.ROOM
    ): WiredVariableValue? {
        val internalDef = InternalVariableDefinition.find(variableId)
        if (internalDef != null) {
            return when (ownerType) {
                VariableOwnerType.FURNI -> getFurniInternalVariable(internalDef, ownerId)
                VariableOwnerType.USER -> getUserInternalVariable(internalDef, ownerId)
                VariableOwnerType.ROOM, VariableOwnerType.GLOBAL -> getRoomInternalVariable(internalDef)
            } ?: when (internalDef.variableTarget) {
                WiredVariableTarget.FURNI -> getFurniInternalVariable(internalDef, ownerId)
                WiredVariableTarget.USER -> getUserInternalVariable(internalDef, ownerId)
                WiredVariableTarget.GLOBAL -> getRoomInternalVariable(internalDef)
                WiredVariableTarget.CONTEXT -> WiredVariableValue(
                    internalDef.variableId,
                    0,
                    ownerId = ownerId,
                    ownerType = ownerType
                )
            }
        }

        return when (ownerType) {
            VariableOwnerType.ROOM -> roomValues[variableId]
            VariableOwnerType.USER -> userValues[ownerId]?.get(variableId)
            VariableOwnerType.FURNI -> furniValues[ownerId]?.get(variableId)
            VariableOwnerType.GLOBAL -> roomValues[variableId]
        }
    }

    fun getFurniInternalVariable(def: InternalVariableDefinition, furniId: Int): WiredVariableValue? {
        val item = room.itemManager.items[furniId] ?: return null
        val value: Any = when (def) {
            InternalVariableDefinition.FURNI_ID -> item.id
            InternalVariableDefinition.FURNI_CLASS_ID -> item.furnishing.spriteId
            InternalVariableDefinition.FURNI_STATE -> item.extraData.toIntOrNull() ?: 0
            InternalVariableDefinition.FURNI_POS_X -> item.position.x
            InternalVariableDefinition.FURNI_POS_Y -> item.position.y
            InternalVariableDefinition.FURNI_ALTITUDE -> (item.position.z * 100).toInt()
            InternalVariableDefinition.FURNI_HEIGHT -> (item.height * 100).toInt()
            InternalVariableDefinition.FURNI_ROTATION -> item.rotation
            InternalVariableDefinition.FURNI_IS_INVISIBLE -> 0
            InternalVariableDefinition.FURNI_TYPE -> if (item.furnishing.type == ItemType.WALL) 1 else 0
            InternalVariableDefinition.FURNI_IS_STACKABLE -> if (item.furnishing.canStack) 1 else 0
            InternalVariableDefinition.FURNI_CAN_STAND_ON -> if (item.furnishing.walkable) 1 else 0
            InternalVariableDefinition.FURNI_CAN_SIT_ON -> if (item.furnishing.canSit) 1 else 0
            InternalVariableDefinition.FURNI_CAN_LAY_ON -> if (item.furnishing.canLay) 1 else 0
            InternalVariableDefinition.FURNI_OWNER_ID -> item.userId
            InternalVariableDefinition.FURNI_WALLITEM_OFFSET -> 0
            else -> 0
        }
        return WiredVariableValue(def.variableId, value, ownerId = furniId, ownerType = VariableOwnerType.FURNI)
    }

    fun getUserInternalVariable(def: InternalVariableDefinition, userIdOrIndex: Int): WiredVariableValue? {
        val entity = room.userManager.entities.values.firstOrNull {
            it.virtualID == userIdOrIndex || (it as? RoomUser)?.habboSession?.userInformation?.id == userIdOrIndex
        } ?: return null

        val value: Any = when (def) {
            InternalVariableDefinition.USER_INDEX -> entity.virtualID
            InternalVariableDefinition.USER_TYPE -> when (entity) {
                is RoomUser -> 1
                is RoomPet -> 2
                is RoomBot -> 4
                else -> 1
            }

            InternalVariableDefinition.USER_GENDER -> {
                if (entity is RoomUser && entity.habboSession.userInformation.gender.equals(
                        "F",
                        ignoreCase = true
                    )
                ) 1 else 0
            }

            InternalVariableDefinition.USER_LEVEL -> {
                if (entity is RoomUser) entity.habboSession.userInformation.achievementUsers.sumOf { it.level } else 0
            }

            InternalVariableDefinition.USER_ACHIEVEMENT_SCORE -> {
                if (entity is RoomUser) entity.habboSession.userInformation.achievementUsers.sumOf { it.level * 10 } else 0
            }

            InternalVariableDefinition.USER_IS_HC -> {
                if (entity is RoomUser && entity.habboSession.userInformation.vip) 1 else 0
            }

            InternalVariableDefinition.USER_HAS_RIGHTS -> {
                if (entity is RoomUser && room.userManager.hasRights(entity.habboSession)) 1 else 0
            }

            InternalVariableDefinition.USER_IS_GROUP_ADMIN -> {
                if (entity is RoomUser) {
                    val group = HabboServer.habboGame.groupManager.groups[room.roomData.groupId]
                    if (group?.admins?.any { it.userId == entity.habboSession.userInformation.id } == true) 1 else 0
                } else 0
            }

            InternalVariableDefinition.USER_IS_OWNER -> {
                if (entity is RoomUser && entity.habboSession.userInformation.id == room.roomData.ownerId) 1 else 0
            }

            InternalVariableDefinition.USER_POS_X -> entity.currentVector3.x
            InternalVariableDefinition.USER_POS_Y -> entity.currentVector3.y
            InternalVariableDefinition.USER_ALTITUDE -> (entity.currentVector3.z * 100).toInt()
            InternalVariableDefinition.USER_DIRECTION -> entity.bodyRotation
            InternalVariableDefinition.USER_HANDITEM_ID -> (entity as? RoomHumanoid)?.handItem ?: 0
            InternalVariableDefinition.USER_EFFECT_ID -> entity.effect?.effectId ?: 0
            InternalVariableDefinition.USER_IS_FROZEN -> if (entity.frozen) 1 else 0
            InternalVariableDefinition.USER_IS_MUTED -> {
                if (entity is RoomUser) {
                    if (room.roomData.muteSettings > 0 && !room.userManager.hasRights(entity.habboSession)) 1
                    else 0
                } else 0
            }

            InternalVariableDefinition.USER_FAVOURITE_GROUP_ID -> {
                if (entity is RoomUser) entity.habboSession.userInformation.groups.firstOrNull()?.groupData?.id
                    ?: 0 else 0
            }

            InternalVariableDefinition.USER_DANCE -> (entity as? RoomHumanoid)?.danceId ?: 0
            InternalVariableDefinition.USER_SIGN -> 0
            InternalVariableDefinition.USER_IS_IDLE -> if ((entity as? RoomHumanoid)?.idle == true) 1 else 0
            InternalVariableDefinition.USER_USER_ID -> if (entity is RoomUser) entity.habboSession.userInformation.id else 0
            InternalVariableDefinition.USER_PET_ID -> (entity as? RoomPet)?.petData?.id ?: 0
            InternalVariableDefinition.USER_BOT_ID -> (entity as? RoomBot)?.botId ?: 0
            InternalVariableDefinition.USER_PET_OWNER_ID -> (entity as? RoomPet)?.petData?.userId ?: 0
            else -> 0
        }
        return WiredVariableValue(def.variableId, value, ownerId = userIdOrIndex, ownerType = VariableOwnerType.USER)
    }

    fun getRoomInternalVariable(def: InternalVariableDefinition): WiredVariableValue {
        val value: Any = when (def) {
            InternalVariableDefinition.ROOM_FURNI_COUNT -> room.itemManager.items.size
            InternalVariableDefinition.ROOM_USER_COUNT -> room.userManager.entities.values.count { it is RoomUser }
            InternalVariableDefinition.ROOM_WIRED_TIMER -> (System.currentTimeMillis() / 500).toInt()
            InternalVariableDefinition.ROOM_TEAM_RED_SCORE -> room.gameManager.games.values.sumOf {
                it.teamScores[GameTeam.RED] ?: 0
            }

            InternalVariableDefinition.ROOM_TEAM_GREEN_SCORE -> room.gameManager.games.values.sumOf {
                it.teamScores[GameTeam.GREEN] ?: 0
            }

            InternalVariableDefinition.ROOM_TEAM_BLUE_SCORE -> room.gameManager.games.values.sumOf {
                it.teamScores[GameTeam.BLUE] ?: 0
            }

            InternalVariableDefinition.ROOM_TEAM_YELLOW_SCORE -> room.gameManager.games.values.sumOf {
                it.teamScores[GameTeam.YELLOW] ?: 0
            }

            InternalVariableDefinition.ROOM_TEAM_RED_SIZE -> room.userManager.entities.values.count {
                it.effect?.effectId in listOf(
                    33,
                    40
                )
            }

            InternalVariableDefinition.ROOM_TEAM_GREEN_SIZE -> room.userManager.entities.values.count {
                it.effect?.effectId in listOf(
                    34,
                    41
                )
            }

            InternalVariableDefinition.ROOM_TEAM_BLUE_SIZE -> room.userManager.entities.values.count {
                it.effect?.effectId in listOf(
                    35,
                    42
                )
            }

            InternalVariableDefinition.ROOM_TEAM_YELLOW_SIZE -> room.userManager.entities.values.count {
                it.effect?.effectId in listOf(
                    36,
                    43
                )
            }

            InternalVariableDefinition.ROOM_ROOM_ID -> room.roomData.id
            InternalVariableDefinition.ROOM_GROUP_ID -> room.roomData.groupId
            else -> 0
        }
        return WiredVariableValue(def.variableId, value, ownerId = room.roomData.id, ownerType = VariableOwnerType.ROOM)
    }

    fun setVariableValue(
        variableId: String,
        value: Any,
        ownerId: Int = room.roomData.id,
        ownerType: VariableOwnerType = VariableOwnerType.ROOM
    ) {
        val internalDef = InternalVariableDefinition.find(variableId)
        if (internalDef != null) {
            if (!internalDef.canWriteValue) return
            val intVal = (value as? Number)?.toInt() ?: value.toString().toIntOrNull() ?: 0
            when (internalDef) {
                InternalVariableDefinition.FURNI_POS_X -> {
                    val item = room.itemManager.items[ownerId] ?: return
                    room.itemManager.setFloorItem(item, Vector2(intVal, item.position.y), item.rotation, null)
                }

                InternalVariableDefinition.FURNI_POS_Y -> {
                    val item = room.itemManager.items[ownerId] ?: return
                    room.itemManager.setFloorItem(item, Vector2(item.position.x, intVal), item.rotation, null)
                }

                InternalVariableDefinition.FURNI_ALTITUDE -> {
                    val item = room.itemManager.items[ownerId] ?: return
                    val targetZ = intVal.toDouble() / 100.0
                    room.itemManager.setFloorItem(item, item.position.vector2, item.rotation, null, overrideZ = targetZ)
                }

                InternalVariableDefinition.FURNI_ROTATION -> {
                    val item = room.itemManager.items[ownerId] ?: return
                    room.itemManager.setFloorItem(item, item.position.vector2, intVal, null)
                }

                InternalVariableDefinition.FURNI_STATE -> {
                    val item = room.itemManager.items[ownerId] ?: return
                    item.extraData = intVal.toString()
                    item.update(updateDb = true, updateClient = true)
                }

                InternalVariableDefinition.USER_POS_X -> {
                    val entity = room.userManager.entities.values.firstOrNull {
                        it.virtualID == ownerId || (it as? RoomUser)?.habboSession?.userInformation?.id == ownerId
                    } ?: return
                    entity.teleportTo(Vector2(intVal, entity.currentVector3.y))
                }

                InternalVariableDefinition.USER_POS_Y -> {
                    val entity = room.userManager.entities.values.firstOrNull {
                        it.virtualID == ownerId || (it as? RoomUser)?.habboSession?.userInformation?.id == ownerId
                    } ?: return
                    entity.teleportTo(Vector2(entity.currentVector3.x, intVal))
                }

                InternalVariableDefinition.USER_ALTITUDE -> {
                    val entity = room.userManager.entities.values.firstOrNull {
                        it.virtualID == ownerId || (it as? RoomUser)?.habboSession?.userInformation?.id == ownerId
                    } ?: return
                    entity.currentVector3 =
                        Vector3(entity.currentVector3.x, entity.currentVector3.y, intVal.toDouble() / 100.0)
                    entity.updateNeeded = true
                }

                InternalVariableDefinition.USER_DIRECTION -> {
                    val entity = room.userManager.entities.values.firstOrNull {
                        it.virtualID == ownerId || (it as? RoomUser)?.habboSession?.userInformation?.id == ownerId
                    } ?: return
                    entity.bodyRotation = intVal
                    entity.headRotation = intVal
                    entity.updateNeeded = true
                }

                InternalVariableDefinition.USER_HANDITEM_ID -> {
                    val entity = room.userManager.entities.values.firstOrNull {
                        it.virtualID == ownerId || (it as? RoomUser)?.habboSession?.userInformation?.id == ownerId
                    } as? RoomHumanoid ?: return
                    entity.carryHandItem(intVal)
                }

                InternalVariableDefinition.USER_EFFECT_ID -> {
                    val entity = room.userManager.entities.values.firstOrNull {
                        it.virtualID == ownerId || (it as? RoomUser)?.habboSession?.userInformation?.id == ownerId
                    } ?: return
                    entity.effect = if (intVal > 0) RoomUserEffect(intVal, Integer.MAX_VALUE) else null
                }

                InternalVariableDefinition.USER_DANCE -> {
                    val entity = room.userManager.entities.values.firstOrNull {
                        it.virtualID == ownerId || (it as? RoomUser)?.habboSession?.userInformation?.id == ownerId
                    } as? RoomHumanoid ?: return
                    entity.dance(intVal)
                }

                InternalVariableDefinition.ROOM_TEAM_RED_SCORE -> {
                    room.gameManager.games.values.forEach { it.teamScores[GameTeam.RED] = intVal }
                }

                InternalVariableDefinition.ROOM_TEAM_GREEN_SCORE -> {
                    room.gameManager.games.values.forEach { it.teamScores[GameTeam.GREEN] = intVal }
                }

                InternalVariableDefinition.ROOM_TEAM_BLUE_SCORE -> {
                    room.gameManager.games.values.forEach { it.teamScores[GameTeam.BLUE] = intVal }
                }

                InternalVariableDefinition.ROOM_TEAM_YELLOW_SCORE -> {
                    room.gameManager.games.values.forEach { it.teamScores[GameTeam.YELLOW] = intVal }
                }

                else -> {}
            }

            room.itemManager.wiredHandler.triggerWired(
                WiredTriggerVariableChanged::class,
                null,
                VariableTriggerData(variableId, newValue = value)
            )
            return
        }

        val now = LocalDateTime.now()
        when (ownerType) {
            VariableOwnerType.ROOM -> {
                roomValues.compute(variableId) { _, existing ->
                    if (existing != null) {
                        existing.value = value
                        existing.updatedAt = now
                        existing
                    } else {
                        WiredVariableValue(
                            variableId,
                            value,
                            createdAt = now,
                            updatedAt = now,
                            ownerId = ownerId,
                            ownerType = ownerType
                        )
                    }
                }
            }

            VariableOwnerType.USER -> {
                val userMap = userValues.computeIfAbsent(ownerId) { ConcurrentHashMap() }
                userMap.compute(variableId) { _, existing ->
                    if (existing != null) {
                        existing.value = value
                        existing.updatedAt = now
                        existing
                    } else {
                        WiredVariableValue(
                            variableId,
                            value,
                            createdAt = now,
                            updatedAt = now,
                            ownerId = ownerId,
                            ownerType = ownerType
                        )
                    }
                }
            }

            VariableOwnerType.FURNI -> {
                val furniMap = furniValues.computeIfAbsent(ownerId) { ConcurrentHashMap() }
                furniMap.compute(variableId) { _, existing ->
                    if (existing != null) {
                        existing.value = value
                        existing.updatedAt = now
                        existing
                    } else {
                        WiredVariableValue(
                            variableId,
                            value,
                            createdAt = now,
                            updatedAt = now,
                            ownerId = ownerId,
                            ownerType = ownerType
                        )
                    }
                }
            }

            VariableOwnerType.GLOBAL -> {
                roomValues.compute(variableId) { _, existing ->
                    if (existing != null) {
                        existing.value = value
                        existing.updatedAt = now
                        existing
                    } else {
                        WiredVariableValue(
                            variableId,
                            value,
                            createdAt = now,
                            updatedAt = now,
                            ownerId = ownerId,
                            ownerType = ownerType
                        )
                    }
                }
            }
        }

        if (!variableHashes.containsKey(variableId)) {
            val varDef = definitions[variableId]
            variableHashes[variableId] = computeSingleVariableHash(varDef)
            markDirty()
        }

        room.itemManager.wiredHandler.triggerWired(
            WiredTriggerVariableChanged::class,
            null,
            VariableTriggerData(variableId, newValue = value)
        )
    }

    fun deleteVariable(
        variableId: String,
        ownerId: Int = room.roomData.id,
        ownerType: VariableOwnerType = VariableOwnerType.ROOM
    ) {
        val internalDef = InternalVariableDefinition.find(variableId)
        if (internalDef != null) return // Não é permitido deletar variáveis internas

        val def = definitions[variableId]
        definitions.remove(variableId)
        when (ownerType) {
            VariableOwnerType.ROOM, VariableOwnerType.GLOBAL -> roomValues.remove(variableId)
            VariableOwnerType.USER -> userValues[ownerId]?.remove(variableId)
            VariableOwnerType.FURNI -> furniValues[ownerId]?.remove(variableId)
        }
        variableHashes.remove(variableId)
        markDirty()

        if (def != null && def.availabilityType.isPersistent) {
            WiredVariableDao.deleteVariable(variableId, ownerId, ownerType)
        }
    }

    fun onUserEnter(userId: Int) {
        loadUserVariables(userId)
    }

    fun onUserLeave(userId: Int) {
        // Limpa apenas variáveis temporárias do usuário
        val userMap = userValues[userId] ?: return
        val iterator = userMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val def = definitions[entry.key]
            if (def == null || !def.availabilityType.isPersistent) {
                iterator.remove()
            }
        }
        if (userMap.isEmpty()) {
            userValues.remove(userId)
        }
    }

    fun cleanupFurni(furniId: Int) {
        // Limpa apenas variáveis temporárias do mobi ao ser recolhido
        val furniMap = furniValues[furniId] ?: return
        val iterator = furniMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val def = definitions[entry.key]
            if (def == null || !def.availabilityType.isPersistent) {
                iterator.remove()
            }
        }
        if (furniMap.isEmpty()) {
            furniValues.remove(furniId)
        }
    }

    fun getVariableIdHash(variableId: String): Int {
        variableHashes[variableId]?.let { return it }
        val internalDef = InternalVariableDefinition.find(variableId)
        if (internalDef != null) {
            return computeSingleVariableHash(internalDef.toWiredVariable())
        }
        return variableId.hashCode()
    }

    fun getAllVariablesHash(): Int {
        if (!hashDirty) return cachedHash

        var hash = 1
        val allDefs = (definitions.values + InternalVariableDefinition.ALL_VARIABLES).distinctBy { it.variableId }
        allDefs.sortedBy { it.variableId }.forEach { def ->
            val vHash = variableHashes[def.variableId] ?: computeSingleVariableHash(def)
            hash = 31 * hash + def.variableId.hashCode() + vHash
        }
        cachedHash = hash
        hashDirty = false
        return cachedHash
    }

    private fun markDirty() {
        hashDirty = true
    }

    private fun computeSingleVariableHash(variable: WiredVariable?): Int {
        var h = 1
        h = 31 * h + variable?.variableId.hashCode()
        h = 31 * h + variable?.variableName.hashCode()
        h = 31 * h + (variable?.variableType?.code ?: 0)
        h = 31 * h + (variable?.availabilityType?.code ?: 0)
        return h
    }

    // Contadores para WiredRoomStats
    fun getPermanentFurniVariablesCount(): Int {
        return furniValues.values.sumOf { map ->
            map.count { (varId, _) ->
                val def = definitions[varId]
                def != null && def.availabilityType.isPersistent
            }
        }
    }

    fun getPermanentUserVariablesCount(): Int {
        return userValues.values.sumOf { map ->
            map.count { (varId, _) ->
                val def = definitions[varId]
                def != null && def.availabilityType.isPersistent
            }
        }
    }

    fun getPermanentGlobalVariablesCount(): Int {
        return roomValues.count { (varId, _) ->
            val def = definitions[varId]
            def != null && def.availabilityType.isPersistent
        }
    }

    fun loadUserVariables(userId: Int) {
        try {
            val loaded = WiredVariableDao.getVariablesForOwner(userId, VariableOwnerType.USER)
            loaded.forEach { (variable, value) ->
                definitions.putIfAbsent(variable.variableId, variable)
                userValues.computeIfAbsent(userId) { ConcurrentHashMap() }[variable.variableId] = value
                variableHashes.putIfAbsent(variable.variableId, computeSingleVariableHash(variable))
            }
        } catch (e: Exception) {
            log.error("Erro ao carregar variáveis persistentes do usuário {} no quarto {}", userId, room.roomData.id, e)
        }
    }

    fun loadPersistentVariables() {
        try {
            val loaded = WiredVariableDao.getVariablesForOwner(room.roomData.id, VariableOwnerType.ROOM)
            loaded.forEach { (variable, value) ->
                definitions[variable.variableId] = variable
                roomValues[variable.variableId] = value
                variableHashes[variable.variableId] = computeSingleVariableHash(variable)
            }

            val furniIds = room.itemManager.items.keys
            if (furniIds.isNotEmpty()) {
                val furniLoaded = WiredVariableDao.getVariablesForOwners(furniIds, VariableOwnerType.FURNI)
                furniLoaded.forEach { (variable, value) ->
                    definitions.putIfAbsent(variable.variableId, variable)
                    furniValues.computeIfAbsent(value.ownerId) { ConcurrentHashMap() }[variable.variableId] = value
                    variableHashes.putIfAbsent(variable.variableId, computeSingleVariableHash(variable))
                }
            }

            markDirty()
        } catch (e: Exception) {
            log.error("Erro ao carregar variáveis persistentes do quarto {}", room.roomData.id, e)
        }
    }

    fun savePersistentVariables() {
        try {
            val toSave = mutableListOf<Pair<WiredVariable, WiredVariableValue>>()
            // 1. Variáveis de sala / globais
            definitions.values.filter { it.availabilityType.isPersistent }.forEach { def ->
                val value = roomValues[def.variableId]
                if (value != null) {
                    toSave += def to value
                }
            }
            // 2. Variáveis de usuários
            userValues.forEach { (_, map) ->
                map.forEach { (varId, value) ->
                    val def = definitions[varId]
                    if (def != null && def.availabilityType.isPersistent) {
                        toSave += def to value
                    }
                }
            }
            // 3. Variáveis de mobis
            furniValues.forEach { (_, map) ->
                map.forEach { (varId, value) ->
                    val def = definitions[varId]
                    if (def != null && def.availabilityType.isPersistent) {
                        toSave += def to value
                    }
                }
            }
            if (toSave.isNotEmpty()) {
                WiredVariableDao.saveVariables(toSave)
            }
        } catch (e: Exception) {
            log.error("Erro ao salvar variáveis persistentes do quarto {}", room.roomData.id, e)
        }
    }

    /**
     * Busca variáveis compartilhadas persistentes de outros quartos do dono da sala.
     */
    fun getSharedVariablesForOwner(
        ownerId: Int = room.roomData.ownerId,
        excludeRoomId: Int = room.roomData.id
    ): List<SharedVariable> {
        if (ownerId <= 0) return emptyList()

        val ownerRooms = HabboServer.habboGame.roomManager.rooms.values
            .filter { it.roomData.ownerId == ownerId && it.roomData.id != excludeRoomId }

        if (ownerRooms.isEmpty()) return emptyList()

        val result = mutableListOf<SharedVariable>()
        val seenRoomAndVarIds = mutableSetOf<Pair<Int, String>>()

        for (otherRoom in ownerRooms) {
            if (otherRoom.initialized) {
                val defs = otherRoom.wiredVariableManager.getDefinitions()
                    .filter { it.availabilityType.isPersistent }
                for (def in defs) {
                    if (seenRoomAndVarIds.add(otherRoom.roomData.id to def.variableId)) {
                        result.add(SharedVariable(otherRoom.roomData.id, otherRoom.roomData.name, def))
                    }
                }
            }
        }

        val uninitializedRoomIds = ownerRooms.filter { !it.initialized }.map { it.roomData.id }
        if (uninitializedRoomIds.isNotEmpty()) {
            try {
                val dbVars = WiredVariableDao.getVariablesForOwners(uninitializedRoomIds, VariableOwnerType.ROOM)
                val roomMap = ownerRooms.associateBy { it.roomData.id }
                for ((variable, value) in dbVars) {
                    val r = roomMap[value.ownerId]
                    if (r != null && variable.availabilityType.isPersistent) {
                        if (seenRoomAndVarIds.add(r.roomData.id to variable.variableId)) {
                            result.add(SharedVariable(r.roomData.id, r.roomData.name, variable))
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Erro ao buscar variáveis persistentes de quartos descarregados do usuário {}", ownerId, e)
            }
        }

        return result
    }

    /**
     * Busca placeholders globais disponíveis em outros quartos do dono da sala.
     */
    fun getSharedGlobalPlaceholdersForOwner(
        ownerId: Int = room.roomData.ownerId,
        excludeRoomId: Int = room.roomData.id
    ): List<SharedGlobalPlaceholder> {
        if (ownerId <= 0) return emptyList()
        val ownerRooms = HabboServer.habboGame.roomManager.rooms.values
            .filter { it.roomData.ownerId == ownerId && it.roomData.id != excludeRoomId }

        if (ownerRooms.isEmpty()) return emptyList()

        val result = mutableListOf<SharedGlobalPlaceholder>()
        val seen = mutableSetOf<Pair<Int, String>>()

        for (otherRoom in ownerRooms) {
            if (otherRoom.initialized) {
                for (item in otherRoom.itemManager.items.values) {
                    val isGlobalPlaceholder =
                        item.furnishing.interactionType == InteractionType.WIRED_EXTRA_TEXT_INPUT_VARIABLE ||
                                item.furnishing.interactionType == InteractionType.WIRED_EXTRA_TEXT_OUTPUT_VARIABLE ||
                                item.furnishing.interactionType.name.contains("PLACEHOLDER") ||
                                item.furnishing.interactionType.name.contains("GLOBAL_PLACEHOLDER")

                    val wiredInstance = HabboServer.habboGame.itemManager.getWiredInstance(otherRoom, item)
                    val isCode2000 =
                        wiredInstance?.code() == WiredAddonType.GLOBAL_PLACEHOLDER.code || wiredInstance?.code() == 2000

                    if (isGlobalPlaceholder || isCode2000) {
                        val rawMsg = item.wiredData?.message ?: ""
                        val placeholderName = rawMsg.split("\t").firstOrNull()?.trim() ?: ""
                        if (placeholderName.isNotEmpty() && seen.add(otherRoom.roomData.id to placeholderName)) {
                            result.add(
                                SharedGlobalPlaceholder(
                                    otherRoom.roomData.id,
                                    otherRoom.roomData.name,
                                    placeholderName
                                )
                            )
                        }
                    }
                }

                // Placeholders definidos em textConnectors das variáveis
                for (def in otherRoom.wiredVariableManager.getDefinitions()) {
                    for ((_, placeholderName) in def.textConnectors) {
                        val name = placeholderName.trim()
                        if (name.isNotEmpty() && seen.add(otherRoom.roomData.id to name)) {
                            result.add(SharedGlobalPlaceholder(otherRoom.roomData.id, otherRoom.roomData.name, name))
                        }
                    }
                }
            }
        }

        val uninitializedRoomIds = ownerRooms.filter { !it.initialized }.map { it.roomData.id }
        if (uninitializedRoomIds.isNotEmpty()) {
            try {
                val dbPlaceholders = WiredVariableDao.getGlobalPlaceholderNamesForRooms(uninitializedRoomIds)
                val roomMap = ownerRooms.associateBy { it.roomData.id }
                for ((rId, name) in dbPlaceholders) {
                    val r = roomMap[rId]
                    if (r != null && seen.add(rId to name)) {
                        result.add(SharedGlobalPlaceholder(r.roomData.id, r.roomData.name, name))
                    }
                }
            } catch (e: Exception) {
                log.error("Erro ao buscar placeholders de quartos descarregados do usuário {}", ownerId, e)
            }
        }

        return result
    }
}
