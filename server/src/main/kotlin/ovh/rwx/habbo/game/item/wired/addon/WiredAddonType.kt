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

package ovh.rwx.habbo.game.item.wired.addon

enum class WiredAddonType(val code: Int) {
    CONDITION_EVALUATION(0), // wf_xtra_or_eval
    UNSEEN(1), // wf_xtra_unseen
    RANDOM(2), // wf_xtra_random
    EXECUTION_LIMIT(5), // wf_xtra_execution_limit
    NO_MOVE_ANIMATION(6), // wf_xtra_mov_no_animation
    MOVEMENT_PHYSICS(7), // wf_xtra_mov_physics
    CARRY_USERS(8), // wf_xtra_mov_carry_users
    ANIMATION_TIME(9), // wf_xtra_anim_time
    FURNI_SELECTOR_FILTER(10), // wf_xtra_filter_furni
    USER_SELECTOR_FILTER(11), // wf_xtra_filter_users
    FURNI_VARIABLE_FILTER(12), // wf_xtra_filter_furni_by_var
    USER_VARIABLE_FILTER(13), // wf_xtra_filter_users_by_var
    USERNAME_PLACEHOLDER(14), // wf_xtra_text_output_username
    VARIABLE_PLACEHOLDER(15), // wf_xtra_text_output_variable
    VARIABLE_CAPTURER(16), // wf_xtra_text_input_variable
    EXECUTE_IN_ORDER(17), // wf_xtra_exec_in_order
    CHEST_ITEM_TYPE_SCANNER(18), // wf_xtra_scan_chest_furni_by_type
    FURNI_NAME_PLACEHOLDER(19), // wf_xtra_text_output_furni_name
    CUSTOM_CONTRACT(20), // wf_xtra_custom_contract
    PROJECTILE(21), // wf_xtra_rotate_to_dir
    JUMP_STRENGTH(22), // wf_xtra_mov_curve
    VARIABLE_TEXT_CONVERTER(1000), // wf_xtra_var_text_connector
    VARIABLE_LEVEL_UP(1001), // wf_xtra_var_lvlup_system
    VARIABLE_TIME_UTIL(1002), // wf_xtra_var_time_util
    VARIABLE_FX_HEALTH_POINTS(1200), // wf_xtra_varfx_hp
    VARIABLE_FX_PROGRESS_BAR(1201), // wf_xtra_varfx_prog
    VARIABLE_FX_LEVELLING_PROGRESS(1202), // wf_xtra_varfx_levelling
    VARIABLE_FX_STATUS_BAR(1203), // wf_xtra_varfx_status
    VARIABLE_FX_BOSS_BAR(1204), // wf_xtra_varfx_boss
    VARIABLE_FX_NUMBER_DISPLAY(1205), // wf_xtra_varfx_number
    GLOBAL_PLACEHOLDER(2000), // wf_xtra_global_placeholder
    ACHIEVEMENT_ENABLER(2001), // wf_xtra_achievement_enabler
    VARIABLES_WEB_API(2002); // wf_xtra_var_web_api

    companion object {
        fun fromCode(code: Int): WiredAddonType? = entries.firstOrNull { it.code == code }
    }
}