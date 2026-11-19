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

package ovh.rwx.habbo.game.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.isVersionBefore
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.room.RoomUpdateUserData
import ovh.rwx.habbo.communication.outgoing.user.UserUpdateFigureData
import ovh.rwx.habbo.database.user.UserInformationDao
import java.util.*

/**
 * A troca de visual: a do jogo (UserChangeFigureHandler) e a do site (o editor de visual do CMS, pela API do
 * plugin_webapp). Um usuário online troca em memória (o write-behind grava) e o novo visual vai para ele e para o
 * quarto em que está; um offline troca direto no banco.
 */
object UserFigureChanger {

    enum class Result { CHANGED, UNCHANGED, INVALID, NOT_FOUND }

    /**
     * Aplica [figure]/[gender] à sessão se o visual é válido para ela, e manda o visual atual (o novo, ou o antigo
     * se inválido: o client desfaz a troca) ao usuário e ao quarto.
     */
    fun change(habboSession: HabboSession, figure: String, gender: String): Result {
        val g = gender.uppercase(Locale.getDefault())
        if (figure == habboSession.userInformation.figure && g == habboSession.userInformation.gender) return Result.UNCHANGED

        val valid = HabboServer.habboGame.figureManager.isValidFigure(figure, g, habboSession)
        if (valid) {
            habboSession.userInformation.figure = figure
            habboSession.userInformation.gender = g
            habboSession.userInformation.markDirty()

            // ACH_AvatarLooks: mudar visual
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_AvatarLooks", 1, accumulate = true)
        }

        broadcast(habboSession)
        return if (valid) Result.CHANGED else Result.INVALID
    }

    /**
     * Pelo id: online, como [change]; offline, valida pelas peças comuns (e de clube, se VIP; tudo para rank 7+)
     * e grava no banco.
     */
    fun changeById(userId: Int, figure: String, gender: String): Result {
        HabboServer.habboSessionManager.getHabboSessionById(userId)?.let { return change(it, figure, gender) }

        val user = UserInformationDao.getUserInformationById(userId) ?: return Result.NOT_FOUND
        val g = gender.uppercase(Locale.getDefault())
        if (figure == user.figure && g == user.gender) return Result.UNCHANGED
        val anyClothing = user.rank >= 7
        val valid = HabboServer.habboGame.figureManager.isValidFigureSet(
            figure, g, clubLevel = if (user.vip || anyClothing) 2 else 0, allowAnyClothing = anyClothing
        )
        if (!valid) return Result.INVALID

        UserInformationDao.updateFigure(userId, figure, g)
        return Result.CHANGED
    }

    /** O visual atual ao próprio usuário e ao quarto. */
    private fun broadcast(habboSession: HabboSession) {
        val currentFigure = habboSession.userInformation.figure
        val currentGender = habboSession.userInformation.gender

        if (habboSession.isVersionBefore(2015, 12, 1)) {
            habboSession.sendResponse(
                Outgoing.USER_UPDATE,
                OutgoingR63A.USER_UPDATE,
                RoomUpdateUserData(
                    virtualId = -1,
                    figure = currentFigure,
                    gender = currentGender,
                    motto = habboSession.userInformation.motto,
                    achievementScore = habboSession.userStats.achievementScore
                )
            )
        } else {
            habboSession.sendHabboResponse(
                Outgoing.USER_UPDATE_FIGURE,
                UserUpdateFigureData(
                    figure = currentFigure,
                    gender = currentGender,
                )
            )
        }

        habboSession.roomUser?.let { roomUser ->
            habboSession.currentRoom?.sendResponse(
                Outgoing.USER_UPDATE,
                OutgoingR63A.USER_UPDATE,
                RoomUpdateUserData(
                    virtualId = roomUser.virtualID,
                    figure = currentFigure,
                    gender = currentGender,
                    motto = habboSession.userInformation.motto,
                    achievementScore = habboSession.userStats.achievementScore
                )
            )
        }
    }
}
