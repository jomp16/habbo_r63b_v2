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

package ovh.rwx.habbo.game.item.wired.condition;

public enum WiredConditionType {
    MATCH_SSHOT(0),
    FURNI_HAVE_HABBO(1),
    TRIGGER_ON_FURNI(2),
    TIME_MORE_THAN(3),
    TIME_LESS_THAN(4),
    USER_COUNT(5),
    ACTOR_IN_TEAM(6),
    FURNI_HAS_FURNI(7),
    STUFF_IS(8),
    ACTOR_IN_GROUP(10),
    ACTOR_WEARS_BADGE(11),
    ACTOR_WEARS_EFFECT(12),
    NOT_MATCH_SSHOT(13),
    NOT_FURNI_HAVE_HABBO(14),
    NOT_ACTOR_ON_FURNI(15),
    NOT_USER_COUNT(16),
    NOT_ACTOR_IN_TEAM(17),
    NOT_FURNI_HAVE_FURNI(18),
    NOT_STUFF_IS(19),
    NOT_ACTOR_IN_GROUP(21),
    NOT_ACTOR_WEARS_BADGE(22),
    NOT_ACTOR_WEARS_EFFECT(23),
    DATE_RANGE(24),
    ACTOR_HAS_HANDITEM(25);

    public final int code;

    WiredConditionType(int code) {
        this.code = code;
    }
}
