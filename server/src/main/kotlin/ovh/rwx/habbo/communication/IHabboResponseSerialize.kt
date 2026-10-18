/*
 * Copyright (C) 2015-2021 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication

interface IHabboResponseSerialize {
    fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any)

    /**
     * Serialização da era pré-shuffle. O default delega à era moderna: o corpo do wire só difere
     * entre eras quando existe um override — o override É a documentação da divergência real
     * (ex.: RoomUser, UserItem, CatalogItem). Não crie delegações manuais; delete-as.
     */
    fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) =
        serializeHabboResponse(habboResponse, *params)
}