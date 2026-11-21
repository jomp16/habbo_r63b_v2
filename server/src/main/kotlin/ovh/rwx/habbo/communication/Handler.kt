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

package ovh.rwx.habbo.communication

import ovh.rwx.habbo.communication.incoming.Incoming

/**
 * [dedupeMs] > 0: um pacote IDÊNTICO (mesmo header e mesmo payload) que chega de novo na mesma sessão menos de
 * [dedupeMs] depois do último atendido é descartado ([RequestDedupe]). Só para pedidos idempotentes
 * que o cliente repete em rajada (ex.: o 2016 abre o quarto recém-criado 3x); nunca em ações (andar, chat, compra).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
internal annotation class Handler(vararg val headers: Incoming, val requiredAuth: Boolean = true, val dedupeMs: Int = 0)