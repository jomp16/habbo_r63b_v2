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

package ovh.rwx.habbo.game.room.trading

/**
 * Máquina de estado para o sistema de Trading.
 * 
 * Fluxo normal:
 * OPEN -> ACCEPTED (ambos aceitam) -> CONFIRMING (confirmação final) -> COMPLETED
 * 
 * Qualquer estado -> CANCELLED (um dos usuários cancela ou desconecta)
 */
enum class TradeState {
    /**
     * Troca iniciada, aguardando itens e aceite dos usuários.
     * Itens podem ser adicionados/removidos livremente.
     */
    OPEN,

    /**
     * Um usuário aceitou a troca. O estado é resetado se o outro usuário
     * modificar a grade (adicionar/remover item).
     */
    ACCEPTED,

    /**
     * Ambos aceitaram. Agora aguardam confirmação final.
     * Neste ponto, a grade está travada.
     */
    CONFIRMING,

    /**
     * Troca completada com sucesso. Itens foram transferidos.
     * Estado terminal.
     */
    COMPLETED,

    /**
     * Troca cancelada. Itens devem ser destravados no inventário.
     * Estado terminal.
     */
    CANCELLED
}
