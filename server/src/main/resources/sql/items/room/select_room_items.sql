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

SELECT `i`.`id`,
       `i`.`room_id`,
       `i`.`item_name`,
       `i`.`extra_data`,
       `i`.`x`,
       `i`.`y`,
       `i`.`z`,
       `i`.`rot`,
       `i`.`wall_pos`,
       `i`.`user_id`,
       `i`.`is_builders_club`,
       (`il`.`id` is not null) as `is_limited`
FROM `items` `i`
         left join `items_limited` `il` on `i`.`id` = `il`.`item_id`
WHERE `i`.`room_id` = :room_id
ORDER BY `i`.`id` DESC