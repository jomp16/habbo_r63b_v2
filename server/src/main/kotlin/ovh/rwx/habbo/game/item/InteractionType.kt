/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item

enum class InteractionType(val type: String) {
    ALERT("alert"),
    BADGE_DISPLAY("badge_display"),
    BATTLE_BANZAI_COUNTER("bb_counter"),
    BATTLE_BANZAI_GATE_BLUE("bb_blue_gate"),
    BATTLE_BANZAI_GATE_GREEN("bb_green_gate"),
    BATTLE_BANZAI_GATE_RED("bb_red_gate"),
    BATTLE_BANZAI_GATE_YELLOW("bb_yellow_gate"),
    BATTLE_BANZAI_PUCK("bb_puck"),
    BATTLE_BANZAI_SCOREBOARD_BLUE("bb_score_b"),
    BATTLE_BANZAI_SCOREBOARD_GREEN("bb_score_g"),
    BATTLE_BANZAI_SCOREBOARD_RED("bb_score_r"),
    BATTLE_BANZAI_SCOREBOARD_YELLOW("bb_score_y"),
    BATTLE_BANZAI_TELEPORT("bb_teleport"),
    BATTLE_BANZAI_TILE("bb_patch"),
    BED("bed"),
    BOTTLE("bottle"),
    DEFAULT("default"),
    DICE("dice"),
    DIMMER("dimmer"),
    GATE("gate"),
    GIFT("gift"),
    GROUP_ITEM("gld_item"),
    HABBO_WHEEL("habbowheel"),
    JUKEBOX("jukebox"),
    LOVE_SHUFFLER("loveshuffler"),
    MANNEQUIN("mannequin"),
    NOT_FOUND("404"),
    ONE_WAY_GATE("onewaygate"),
    PET("pet"),
    POST_IT("postit"),
    PRESSURE_PAD("pressure_pad"),
    RENTALS("rentals"),
    ROLLER("roller"),
    ROOM_ADS("ads_mpu"),
    ROOM_EFFECT("room_effect"),
    SCOREBOARD("scoreboard"),
    TELEPORT("teleport"),
    TILE_STACK_MAGIC("tile_stack_magic"),
    TROPHY("trophy"),
    VENDING_MACHINE("vendingmachine"),
    WATER("water"),
    WIRED_EFFECT_CALL_STACK("wf_act_call_stacks"),
    WIRED_EFFECT_CHASE_USER("wf_act_chase"),
    WIRED_EFFECT_FLEE_USER("wf_act_flee"),
    WIRED_EFFECT_KICK_USER("wf_act_kick_user"),
    WIRED_EFFECT_MATCH_TO_SCREENSHOT("wf_act_match_to_sshot"),
    WIRED_EFFECT_MOVE_ROTATE("wf_act_move_rotate"),
    WIRED_EFFECT_ROTATE_ITEM("wf_act_move_to_dir"),
    WIRED_EFFECT_SHOW_MESSAGE("wf_act_show_message"),
    WIRED_EFFECT_TELEPORT_TO("wf_act_teleport_to"),
    WIRED_EFFECT_TOGGLE_STATE("wf_act_toggle_state"),
    WIRED_CONDITION_HAS_AVATARS("wf_cnd_furnis_hv_avtrs"),
    WIRED_CONDITION_HAS_NO_AVATARS("wf_cnd_not_hv_avtrs"),
    WIRED_CONDITION_IN_GROUP("wf_cnd_actor_in_group"),
    WIRED_CONDITION_NOT_IN_GROUP("wf_cnd_not_in_group"),
    WIRED_EXTRA_RANDOM("wf_xtra_random"),
    WIRED_EXTRA_UNSEEN("wf_xtra_unseen"),
    WIRED_TRIGGER_COLLISION("wf_trg_collision"),
    WIRED_TRIGGER_ENTER_ROOM("wf_trg_enter_room"),
    WIRED_TRIGGER_GAME_ENDS("wf_trg_game_ends"),
    WIRED_TRIGGER_GAME_STARTS("wf_trg_game_starts"),
    WIRED_TRIGGER_PERIODICALLY("wf_trg_periodically"),
    WIRED_TRIGGER_PERIODICALLY_LONG("wf_trg_period_long"),
    WIRED_TRIGGER_SAYS_SOMETHING("wf_trg_says_something"),
    WIRED_TRIGGER_STATE_CHANGED("wf_trg_state_changed"),
    WIRED_TRIGGER_WALKS_OFF_FURNI("wf_trg_walks_off_furni"),
    WIRED_TRIGGER_WALKS_ON_FURNI("wf_trg_walks_on_furni");

    companion object {
        fun fromString(value: String): InteractionType {
            if (value.isBlank()) return DEFAULT

            return values().firstOrNull { it.type == value } ?: NOT_FOUND
        }
    }
}