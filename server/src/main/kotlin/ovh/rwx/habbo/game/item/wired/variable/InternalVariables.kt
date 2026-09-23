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

package ovh.rwx.habbo.game.item.wired.variable

enum class InternalVariableDefinition(
    val variableId: String,
    val variableName: String,
    val variableTarget: WiredVariableTarget,
    val canWriteValue: Boolean = false,
    val textConnectors: Map<Int, String> = emptyMap(),
    val aliases: List<String> = emptyList()
) {
    // --- FURNI INTERNAL VARIABLES ---
    FURNI_ID(
        variableId = "@furni.id",
        variableName = "@furni.id",
        variableTarget = WiredVariableTarget.FURNI,
        aliases = listOf("@id", "furni.id", "id")
    ),
    FURNI_CLASS_ID(
        variableId = "@furni.class_id",
        variableName = "@furni.class_id",
        variableTarget = WiredVariableTarget.FURNI,
        aliases = listOf("@class_id", "furni.class_id", "class_id")
    ),
    FURNI_STATE(
        variableId = "@furni.state",
        variableName = "@furni.state",
        variableTarget = WiredVariableTarget.FURNI,
        canWriteValue = true,
        aliases = listOf("@state", "furni.state", "state")
    ),
    FURNI_POS_X(
        variableId = "@furni.position_x",
        variableName = "@furni.position_x",
        variableTarget = WiredVariableTarget.FURNI,
        canWriteValue = true,
        aliases = listOf("@position_x", "furni.position_x", "position_x", "x")
    ),
    FURNI_POS_Y(
        variableId = "@furni.position_y",
        variableName = "@furni.position_y",
        variableTarget = WiredVariableTarget.FURNI,
        canWriteValue = true,
        aliases = listOf("@position_y", "furni.position_y", "position_y", "y")
    ),
    FURNI_ALTITUDE(
        variableId = "@furni.altitude",
        variableName = "@furni.altitude",
        variableTarget = WiredVariableTarget.FURNI,
        canWriteValue = true,
        aliases = listOf("@altitude", "furni.altitude", "altitude", "z")
    ),
    FURNI_HEIGHT(
        variableId = "@furni.height",
        variableName = "@furni.height",
        variableTarget = WiredVariableTarget.FURNI,
        aliases = listOf("@height", "furni.height", "height")
    ),
    FURNI_ROTATION(
        variableId = "@furni.rotation",
        variableName = "@furni.rotation",
        variableTarget = WiredVariableTarget.FURNI,
        canWriteValue = true,
        textConnectors = mapOf(
            0 to "North",
            2 to "East",
            4 to "South",
            6 to "West"
        ),
        aliases = listOf("@rotation", "furni.rotation", "rotation")
    ),
    FURNI_IS_INVISIBLE(
        variableId = "@furni.is_invisible",
        variableName = "@furni.is_invisible",
        variableTarget = WiredVariableTarget.FURNI,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_invisible", "furni.is_invisible", "is_invisible")
    ),
    FURNI_TYPE(
        variableId = "@furni.type",
        variableName = "@furni.type",
        variableTarget = WiredVariableTarget.FURNI,
        textConnectors = mapOf(
            0 to "Floor",
            1 to "Wall"
        ),
        aliases = listOf("@type", "furni.type", "type")
    ),
    FURNI_IS_STACKABLE(
        variableId = "@furni.is_stackable",
        variableName = "@furni.is_stackable",
        variableTarget = WiredVariableTarget.FURNI,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_stackable", "furni.is_stackable", "is_stackable")
    ),
    FURNI_CAN_STAND_ON(
        variableId = "@furni.can_stand_on",
        variableName = "@furni.can_stand_on",
        variableTarget = WiredVariableTarget.FURNI,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@can_stand_on", "furni.can_stand_on", "can_stand_on")
    ),
    FURNI_CAN_SIT_ON(
        variableId = "@furni.can_sit_on",
        variableName = "@furni.can_sit_on",
        variableTarget = WiredVariableTarget.FURNI,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@can_sit_on", "furni.can_sit_on", "can_sit_on")
    ),
    FURNI_CAN_LAY_ON(
        variableId = "@furni.can_lay_on",
        variableName = "@furni.can_lay_on",
        variableTarget = WiredVariableTarget.FURNI,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@can_lay_on", "furni.can_lay_on", "can_lay_on")
    ),
    FURNI_OWNER_ID(
        variableId = "@furni.owner_id",
        variableName = "@furni.owner_id",
        variableTarget = WiredVariableTarget.FURNI,
        aliases = listOf("@owner_id", "furni.owner_id", "owner_id")
    ),
    FURNI_WALLITEM_OFFSET(
        variableId = "@furni.wallitem_offset",
        variableName = "@furni.wallitem_offset",
        variableTarget = WiredVariableTarget.FURNI,
        aliases = listOf("@wallitem_offset", "furni.wallitem_offset", "wallitem_offset")
    ),

    // --- USER INTERNAL VARIABLES ---
    USER_INDEX(
        variableId = "@user.index",
        variableName = "@user.index",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@index", "user.index", "index")
    ),
    USER_TYPE(
        variableId = "@user.type",
        variableName = "@user.type",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            1 to "User",
            2 to "Pet",
            4 to "Bot"
        ),
        aliases = listOf("@type", "user.type", "type")
    ),
    USER_GENDER(
        variableId = "@user.gender",
        variableName = "@user.gender",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "Male",
            1 to "Female"
        ),
        aliases = listOf("@gender", "user.gender", "gender")
    ),
    USER_LEVEL(
        variableId = "@user.level",
        variableName = "@user.level",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@level", "user.level", "level")
    ),
    USER_ACHIEVEMENT_SCORE(
        variableId = "@user.achievement_score",
        variableName = "@user.achievement_score",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@achievement_score", "user.achievement_score", "achievement_score")
    ),
    USER_IS_HC(
        variableId = "@user.is_hc",
        variableName = "@user.is_hc",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_hc", "user.is_hc", "is_hc")
    ),
    USER_HAS_RIGHTS(
        variableId = "@user.has_rights",
        variableName = "@user.has_rights",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@has_rights", "user.has_rights", "has_rights")
    ),
    USER_IS_GROUP_ADMIN(
        variableId = "@user.is_group_admin",
        variableName = "@user.is_group_admin",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_group_admin", "user.is_group_admin", "is_group_admin")
    ),
    USER_IS_OWNER(
        variableId = "@user.is_owner",
        variableName = "@user.is_owner",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_owner", "user.is_owner", "is_owner")
    ),
    USER_POS_X(
        variableId = "@user.position_x",
        variableName = "@user.position_x",
        variableTarget = WiredVariableTarget.USER,
        canWriteValue = true,
        aliases = listOf("@position_x", "user.position_x", "position_x", "x")
    ),
    USER_POS_Y(
        variableId = "@user.position_y",
        variableName = "@user.position_y",
        variableTarget = WiredVariableTarget.USER,
        canWriteValue = true,
        aliases = listOf("@position_y", "user.position_y", "position_y", "y")
    ),
    USER_ALTITUDE(
        variableId = "@user.altitude",
        variableName = "@user.altitude",
        variableTarget = WiredVariableTarget.USER,
        canWriteValue = true,
        aliases = listOf("@altitude", "user.altitude", "altitude", "z")
    ),
    USER_DIRECTION(
        variableId = "@user.direction",
        variableName = "@user.direction",
        variableTarget = WiredVariableTarget.USER,
        canWriteValue = true,
        textConnectors = mapOf(
            0 to "North",
            1 to "North-East",
            2 to "East",
            3 to "South-East",
            4 to "South",
            5 to "South-West",
            6 to "West",
            7 to "North-West"
        ),
        aliases = listOf("@direction", "user.direction", "direction")
    ),
    USER_HANDITEM_ID(
        variableId = "@user.handitem_id",
        variableName = "@user.handitem_id",
        variableTarget = WiredVariableTarget.USER,
        canWriteValue = true,
        aliases = listOf("@handitem_id", "user.handitem_id", "handitem_id")
    ),
    USER_EFFECT_ID(
        variableId = "@user.effect_id",
        variableName = "@user.effect_id",
        variableTarget = WiredVariableTarget.USER,
        canWriteValue = true,
        aliases = listOf("@effect_id", "user.effect_id", "effect_id")
    ),
    USER_IS_FROZEN(
        variableId = "@user.is_frozen",
        variableName = "@user.is_frozen",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_frozen", "user.is_frozen", "is_frozen")
    ),
    USER_IS_MUTED(
        variableId = "@user.is_muted",
        variableName = "@user.is_muted",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_muted", "user.is_muted", "is_muted")
    ),
    USER_FAVOURITE_GROUP_ID(
        variableId = "@user.favourite_group_id",
        variableName = "@user.favourite_group_id",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@favourite_group_id", "user.favourite_group_id", "favourite_group_id")
    ),
    USER_DANCE(
        variableId = "@user.dance",
        variableName = "@user.dance",
        variableTarget = WiredVariableTarget.USER,
        canWriteValue = true,
        aliases = listOf("@dance", "user.dance", "dance")
    ),
    USER_SIGN(
        variableId = "@user.sign",
        variableName = "@user.sign",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@sign", "user.sign", "sign")
    ),
    USER_IS_IDLE(
        variableId = "@user.is_idle",
        variableName = "@user.is_idle",
        variableTarget = WiredVariableTarget.USER,
        textConnectors = mapOf(
            0 to "No",
            1 to "Yes"
        ),
        aliases = listOf("@is_idle", "user.is_idle", "is_idle")
    ),
    USER_USER_ID(
        variableId = "@user.user_id",
        variableName = "@user.user_id",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@user_id", "user.user_id", "user_id")
    ),
    USER_PET_ID(
        variableId = "@user.pet_id",
        variableName = "@user.pet_id",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@pet_id", "user.pet_id", "pet_id")
    ),
    USER_BOT_ID(
        variableId = "@user.bot_id",
        variableName = "@user.bot_id",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@bot_id", "user.bot_id", "bot_id")
    ),
    USER_PET_OWNER_ID(
        variableId = "@user.pet_owner_id",
        variableName = "@user.pet_owner_id",
        variableTarget = WiredVariableTarget.USER,
        aliases = listOf("@pet_owner_id", "user.pet_owner_id", "pet_owner_id")
    ),

    // --- ROOM / GLOBAL INTERNAL VARIABLES ---
    ROOM_FURNI_COUNT(
        variableId = "@room.furni_count",
        variableName = "@room.furni_count",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@furni_count", "room.furni_count", "furni_count")
    ),
    ROOM_USER_COUNT(
        variableId = "@room.user_count",
        variableName = "@room.user_count",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@user_count", "room.user_count", "user_count")
    ),
    ROOM_WIRED_TIMER(
        variableId = "@room.wired_timer",
        variableName = "@room.wired_timer",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@wired_timer", "room.wired_timer", "wired_timer")
    ),
    ROOM_TEAM_RED_SCORE(
        variableId = "@room.team_red_score",
        variableName = "@room.team_red_score",
        variableTarget = WiredVariableTarget.GLOBAL,
        canWriteValue = true,
        aliases = listOf("@team_red_score", "room.team_red_score", "team_red_score")
    ),
    ROOM_TEAM_GREEN_SCORE(
        variableId = "@room.team_green_score",
        variableName = "@room.team_green_score",
        variableTarget = WiredVariableTarget.GLOBAL,
        canWriteValue = true,
        aliases = listOf("@team_green_score", "room.team_green_score", "team_green_score")
    ),
    ROOM_TEAM_BLUE_SCORE(
        variableId = "@room.team_blue_score",
        variableName = "@room.team_blue_score",
        variableTarget = WiredVariableTarget.GLOBAL,
        canWriteValue = true,
        aliases = listOf("@team_blue_score", "room.team_blue_score", "team_blue_score")
    ),
    ROOM_TEAM_YELLOW_SCORE(
        variableId = "@room.team_yellow_score",
        variableName = "@room.team_yellow_score",
        variableTarget = WiredVariableTarget.GLOBAL,
        canWriteValue = true,
        aliases = listOf("@team_yellow_score", "room.team_yellow_score", "team_yellow_score")
    ),
    ROOM_TEAM_RED_SIZE(
        variableId = "@room.team_red_size",
        variableName = "@room.team_red_size",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@team_red_size", "room.team_red_size", "team_red_size")
    ),
    ROOM_TEAM_GREEN_SIZE(
        variableId = "@room.team_green_size",
        variableName = "@room.team_green_size",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@team_green_size", "room.team_green_size", "team_green_size")
    ),
    ROOM_TEAM_BLUE_SIZE(
        variableId = "@room.team_blue_size",
        variableName = "@room.team_blue_size",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@team_blue_size", "room.team_blue_size", "team_blue_size")
    ),
    ROOM_TEAM_YELLOW_SIZE(
        variableId = "@room.team_yellow_size",
        variableName = "@room.team_yellow_size",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@team_yellow_size", "room.team_yellow_size", "team_yellow_size")
    ),
    ROOM_ROOM_ID(
        variableId = "@room.room_id",
        variableName = "@room.room_id",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@room_id", "room.room_id", "room_id")
    ),
    ROOM_GROUP_ID(
        variableId = "@room.group_id",
        variableName = "@room.group_id",
        variableTarget = WiredVariableTarget.GLOBAL,
        aliases = listOf("@group_id", "room.group_id", "group_id")
    ),

    // --- CONTEXT INTERNAL VARIABLES ---
    CONTEXT_SELECTOR_FURNI_COUNT(
        variableId = "@context.selector_furni_count",
        variableName = "@context.selector_furni_count",
        variableTarget = WiredVariableTarget.CONTEXT,
        aliases = listOf("@selector_furni_count", "context.selector_furni_count", "selector_furni_count")
    ),
    CONTEXT_SELECTOR_USER_COUNT(
        variableId = "@context.selector_user_count",
        variableName = "@context.selector_user_count",
        variableTarget = WiredVariableTarget.CONTEXT,
        aliases = listOf("@selector_user_count", "context.selector_user_count", "selector_user_count")
    ),
    CONTEXT_SIGNAL_FURNI_COUNT(
        variableId = "@context.signal_furni_count",
        variableName = "@context.signal_furni_count",
        variableTarget = WiredVariableTarget.CONTEXT,
        aliases = listOf("@signal_furni_count", "context.signal_furni_count", "signal_furni_count")
    ),
    CONTEXT_ANTENNA_ID(
        variableId = "@context.antenna_id",
        variableName = "@context.antenna_id",
        variableTarget = WiredVariableTarget.CONTEXT,
        aliases = listOf("@antenna_id", "context.antenna_id", "antenna_id")
    ),
    CONTEXT_CHAT_TYPE(
        variableId = "@context.chat_type",
        variableName = "@context.chat_type",
        variableTarget = WiredVariableTarget.CONTEXT,
        aliases = listOf("@chat_type", "context.chat_type", "chat_type")
    ),
    CONTEXT_CHAT_STYLE(
        variableId = "@context.chat_style",
        variableName = "@context.chat_style",
        variableTarget = WiredVariableTarget.CONTEXT,
        aliases = listOf("@chat_style", "context.chat_style", "chat_style")
    );

    fun toWiredVariable(): WiredVariable = WiredVariable(
        variableId = variableId,
        variableType = WiredVariableType.INTERNAL,
        variableName = variableName,
        availabilityType = VariableAvailabilityType.NOT_APPLICABLE,
        variableTarget = variableTarget,
        alwaysAvailable = true,
        canCreateAndDelete = false,
        hasValue = true,
        canWriteValue = canWriteValue,
        canInterceptChanges = true,
        isInvisible = false,
        canReadCreationTime = false,
        canReadLastUpdateTime = false,
        textConnectors = textConnectors
    )

    companion object {
        private val aliasMap: Map<String, InternalVariableDefinition> = buildMap {
            InternalVariableDefinition.entries.forEach { def ->
                put(def.variableId.lowercase(), def)
                def.aliases.forEach { alias ->
                    put(alias.lowercase(), def)
                }
            }
        }

        fun find(id: String): InternalVariableDefinition? = aliasMap[id.trim().lowercase()]

        val ALL_VARIABLES: List<WiredVariable> by lazy { InternalVariableDefinition.entries.map { it.toWiredVariable() } }
    }
}
