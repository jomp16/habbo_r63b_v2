/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.game.item

enum class InteractionType(val type: String) {
    DEFAULT("default"),
    VENDING_MACHINE("vendingmachine"),
    PET("pet"),
    POST_IT("postit"),
    GATE("gate"),
    BED("bed"),
    DIMMER("dimmer"),
    WATER("water"),
    JUKEBOX("jukebox"),
    HABBO_WHEEL("habbowheel"),
    TROPHY("trophy"),
    ROOM_EFFECT("room_effect"),
    ONE_WAY_GATE("onewaygate"),
    DICE("dice"),
    BOTTLE("bottle"),
    TELEPORT("teleport"),
    ROLLER("roller"),
    SCOREBOARD("scoreboard"),
    ALERT("alert"),
    LOVE_SHUFFLER("loveshuffler"),
    RENTALS("rentals"),
    MANNEQUIN("mannequin"),
    GIFT("gift"),
    WIRED_TRIGGER_ENTER_ROOM("wf_trg_enter_room"),
    WIRED_TRIGGER_SAYS_SOMETHING("wf_trg_says_something"),
    WIRED_TRIGGER_WALKS_ON_FURNI("wf_trg_walks_on_furni"),
    WIRED_TRIGGER_WALKS_OFF_FURNI("wf_trg_walks_off_furni"),
    WIRED_TRIGGER_STATE_CHANGED("wf_trg_state_changed"),
    WIRED_TRIGGER_PERIODICALLY("wf_trg_periodically"),
    WIRED_TRIGGER_PERIODICALLY_LONG("wf_trg_period_long"),
    WIRED_TRIGGER_GAME_STARTS("wf_trg_game_starts"),
    WIRED_TRIGGER_GAME_ENDS("wf_trg_game_ends"),
    WIRED_ACTION_SHOW_MESSAGE("wf_act_show_message"),
    WIRED_ACTION_MOVE_ROTATE("wf_act_move_rotate"),
    WIRED_ACTION_MATCH_TO_SCREENSHOT("wf_act_match_to_sshot"),
    WIRED_ACTION_TELEPORT_TO("wf_act_teleport_to"),
    WIRED_ACTION_TOGGLE_STATE("wf_act_toggle_state"),
    WIRED_CONDITION_HAS_AVATARS("wf_cnd_furnis_hv_avtrs"),
    WIRED_CONDITION_HAS_NO_AVATARS("wf_cnd_not_hv_avtrs"),
    WIRED_CONDITION_IN_GROUP("wf_cnd_actor_in_group"),
    WIRED_CONDITION_NOT_IN_GROUP("wf_cnd_not_in_group"),
    BATTLE_BANZAI_TELEPORT("bb_teleport"),
    BATTLE_BANZAI_GATE_BLUE("bb_blue_gate"),
    BATTLE_BANZAI_GATE_GREEN("bb_green_gate"),
    BATTLE_BANZAI_GATE_RED("bb_red_gate"),
    BATTLE_BANZAI_GATE_YELLOW("bb_yellow_gate"),
    BATTLE_BANZAI_COUNTER("bb_counter"),
    BATTLE_BANZAI_TILE("bb_patch"),
    BATTLE_BANZAI_SCOREBOARD_BLUE("bb_score_b"),
    BATTLE_BANZAI_SCOREBOARD_GREEN("bb_score_g"),
    BATTLE_BANZAI_SCOREBOARD_RED("bb_score_r"),
    BATTLE_BANZAI_SCOREBOARD_YELLOW("bb_score_y"),
    BATTLE_BANZAI_PUCK("bb_puck"),
    PRESSURE_PAD("pressure_pad"),
    BADGE_DISPLAY("badge_display"),
    ROOM_ADS("ads_mpu"),
    TILE_STACK_MAGIC("tile_stack_magic"),
    GROUP_ITEM("gld_item"),
    NOT_FOUND("404");

    companion object {
        fun fromString(value: String): InteractionType {
            if (value.isBlank()) return DEFAULT

            return values().firstOrNull { it.type == value } ?: DEFAULT
        }
    }
}