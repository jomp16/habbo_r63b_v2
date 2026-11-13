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

package ovh.rwx.habbo.game.item

/**
 * Categorias de mobília no inventário (com.sulake.habbo.inventory.enum.FurniCategory no client).
 */
enum class FurniCategory(val id: Int) {
    DEFAULT(1),
    WALL_PAPER(2),
    FLOOR(3),
    LANDSCAPE(4),
    POST_IT(5),
    POSTER(6),
    SOUND_SET(7),
    TRAX_SONG(8),
    PRESENT(9),
    ECOTRON_BOX(10),
    TROPHY(11),
    CREDIT_FURNI(12),
    PET_SHAMPOO(13),
    PET_CUSTOM_PART(14),
    PET_CUSTOM_PART_SHAMPOO(15),
    PET_SADDLE(16),
    GUILD_FURNI(17),
    GAME_FURNI(18),
    MONSTERPLANT_SEED(19),
    MONSTERPLANT_REVIVAL(20),
    MONSTERPLANT_REBREED(21),
    MONSTERPLANT_FERTILIZE(22),
    FIGURE_PURCHASABLE_SET(23),
    FURNI_CHEST(24),
    COINS_CHEST(25);
}
