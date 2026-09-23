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

INSERT INTO `users_snowwar_stats` (`user_id`, `total_score`, `weekly_score`, `games_played`, `weekly_games_played`,
                                   `skill_level`, `last_week_number`, `last_year`)
VALUES (:user_id, :total_score, :weekly_score, :games_played, :weekly_games_played, :skill_level, :last_week_number,
        :last_year)
ON DUPLICATE KEY UPDATE `total_score`         = VALUES(`total_score`),
                        `weekly_score`        = VALUES(`weekly_score`),
                        `games_played`        = VALUES(`games_played`),
                        `weekly_games_played` = VALUES(`weekly_games_played`),
                        `skill_level`         = VALUES(`skill_level`),
                        `last_week_number`    = VALUES(`last_week_number`),
                        `last_year`           = VALUES(`last_year`);
