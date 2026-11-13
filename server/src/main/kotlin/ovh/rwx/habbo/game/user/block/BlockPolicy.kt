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

package ovh.rwx.habbo.game.user.block

/** Visão de bloqueios de um usuário (o "dono" da visão): quem ele bloqueou e quem o bloqueou. */
interface BlockView {
    /** O dono da visão bloqueou [userId]. */
    fun isBlocking(userId: Int): Boolean

    /** [userId] bloqueou o dono da visão. */
    fun isBlockedBy(userId: Int): Boolean
}

/**
 * Decisões puras de bloqueio. Cada função recebe a [BlockView] de quem decide (ou do lado relevante) e ids simples,
 * sem tocar em sessão/rede, para serem testáveis. A direção checada em cada regra reflete o comportamento original.
 */
object BlockPolicy {
    /** Código de erro do mensageiro: o alvo bloqueou o solicitante. */
    const val FRIEND_REQUEST_BLOCKED_BY_THEM = 7

    /** Código de erro do mensageiro: o solicitante bloqueou o alvo. */
    const val FRIEND_REQUEST_BLOCKED_BY_YOU = 8

    /** Verdadeiro se um dos dois bloqueou o outro (as ações entre eles são recusadas). */
    fun isBlockedEitherWay(view: BlockView, otherId: Int): Boolean =
        view.isBlocking(otherId) || view.isBlockedBy(otherId)

    /** [listener] ouve o chat de [speakerId] apenas se não há bloqueio em nenhum dos sentidos. */
    fun canHearChat(listener: BlockView, speakerId: Int): Boolean = !isBlockedEitherWay(listener, speakerId)

    /** O sussurro de [sender] chega a [targetId] apenas se não há bloqueio nos dois sentidos (o remetente vê o próprio). */
    fun canWhisper(sender: BlockView, targetId: Int): Boolean = !isBlockedEitherWay(sender, targetId)

    /**
     * Entrada em quarto negada: o dono ([ownerId]) bloqueou quem entra (só esse sentido; quem entra ter bloqueado o dono
     * não impede). Staff com a permissão de acesso a qualquer quarto ([hasStaffBypass]) ignora, como no ban.
     */
    fun roomEntryDenied(view: BlockView, ownerId: Int, hasStaffBypass: Boolean): Boolean =
        view.isBlockedBy(ownerId) && !hasStaffBypass

    /**
     * Erro de pedido de amizade para [targetId]: [FRIEND_REQUEST_BLOCKED_BY_THEM] (7) se o alvo bloqueou o solicitante
     * (checado primeiro), [FRIEND_REQUEST_BLOCKED_BY_YOU] (8) se o solicitante bloqueou o alvo, ou null se permitido.
     */
    fun friendRequestError(view: BlockView, targetId: Int): Int? = when {
        view.isBlockedBy(targetId) -> FRIEND_REQUEST_BLOCKED_BY_THEM
        view.isBlocking(targetId) -> FRIEND_REQUEST_BLOCKED_BY_YOU
        else -> null
    }

    /** Aceitar pedido de amizade de [requesterId]: recusado se há bloqueio em qualquer sentido. */
    fun canAcceptFriendRequest(view: BlockView, requesterId: Int): Boolean = !isBlockedEitherWay(view, requesterId)

    /** Troca com [targetId] só é permitida sem bloqueio em nenhum sentido. */
    fun canTrade(view: BlockView, targetId: Int): Boolean = !isBlockedEitherWay(view, targetId)

    /**
     * Convite do mensageiro: [recipient] (a visão de quem recebe) só recebe se NÃO bloqueou o convidante
     * ([inviterId]). O sentido inverso (convidante bloqueou o destinatário, ou o destinatário foi bloqueado) não é checado.
     */
    fun canInvite(recipient: BlockView, inviterId: Int): Boolean = !recipient.isBlocking(inviterId)

    /** Mensagem do mensageiro para [targetId]: descartada em silêncio se há bloqueio em qualquer sentido. */
    fun canMessage(view: BlockView, targetId: Int): Boolean = !isBlockedEitherWay(view, targetId)

    /**
     * Presente: o destinatário ([receiverId]) que bloqueou o comprador é tratado como "não encontrado" (só esse
     * sentido; o comprador ter bloqueado o destinatário não impede).
     */
    fun giftReceiverHidden(buyer: BlockView, receiverId: Int): Boolean = buyer.isBlockedBy(receiverId)

    /** Navegador: esconde o quarto cujo dono ([ownerId]) bloqueou quem navega (só esse sentido). */
    fun hidesRoomOwner(viewer: BlockView, ownerId: Int): Boolean = viewer.isBlockedBy(ownerId)
}
