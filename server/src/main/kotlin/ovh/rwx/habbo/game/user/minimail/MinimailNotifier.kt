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

package ovh.rwx.habbo.game.user.minimail

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.minimail.MinimailDao
import ovh.rwx.habbo.game.user.HabboSession
import org.slf4j.LoggerFactory

/**
 * O aviso do Minimail no jogo. As mensagens são do CMS; o CMS avisa (plugin_webapp) quando alguém recebe ou lê, e o
 * emulador manda ao client o número de não lidas tirado do banco (nunca um número vindo de fora). Só os clients de
 * 2012-02-29 em diante têm o ícone do Minimail; nos anteriores (e no R63A) nada é enviado.
 */
object MinimailNotifier {
    private val log = LoggerFactory.getLogger(MinimailNotifier::class.java)

    fun supports(habboSession: HabboSession): Boolean =
        !habboSession.isPreShuffle && habboSession.isVersionAtLeast(2012, 2, 29)

    /** O número atual de não lidas (no login e depois de ler no site). */
    fun sendUnreadCount(habboSession: HabboSession) {
        if (!supports(habboSession)) return
        habboSession.sendHabboResponse(Outgoing.MINIMAIL_UNREAD_COUNT, MinimailDao.countUnread(habboSession.userInformation.id))
    }

    /**
     * Avisa quem está online entre [userIds]: com [newMessage], o "mensagem nova" (o client soma 1 e destaca o ícone) e
     * em seguida o número certo. Devolve quantas sessões foram avisadas.
     */
    fun notify(userIds: Collection<Int>, newMessage: Boolean): Int {
        var notified = 0
        userIds.toSet().forEach { id ->
            val session = HabboServer.habboSessionManager.getHabboSessionById(id) ?: return@forEach
            if (!supports(session)) return@forEach
            try {
                if (newMessage) session.sendHabboResponse(Outgoing.MINIMAIL_NEW_MESSAGE)
                sendUnreadCount(session)
                notified++
            } catch (e: Exception) {
                log.warn("Minimail: aviso ao usuário {} falhou", id, e)
            }
        }
        return notified
    }

    /** Todos os online (a mensagem da equipe para o hotel). */
    fun notifyAllOnline(): Int =
        notify(HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated }.map { it.userInformation.id }, true)
}
