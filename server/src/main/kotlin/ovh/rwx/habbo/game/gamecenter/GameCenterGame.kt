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

package ovh.rwx.habbo.game.gamecenter

/**
 * Jogo do Game Center (tabela `game_center_games`).
 *
 * Os paths são compostos a partir do `dir`:
 * ícones do gamecenter em `${c_images_url}/<dir>/`; jogo e assets em
 * `${games_url}/<dir>/<swf>` e `${games_url}/<dir>/<assets_swf>`. A 6ª string
 * do GameList (`playUrl`) é o link externo aberto ao clicar em "jogar no site".
 */
data class GameCenterGame(
    val gameTypeId: Int,
    val name: String,
    val bgColor: String,
    val textColor: String,
    val dir: String,
    val swf: String,
    val assetsSwf: String?,
    val playUrl: String,
)
