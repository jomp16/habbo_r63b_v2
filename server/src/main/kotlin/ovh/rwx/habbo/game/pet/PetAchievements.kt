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

package ovh.rwx.habbo.game.pet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Gatilhos de conquistas de mascote. O dono do mascote pode estar offline (o progresso vai ao banco), então tudo roda
 * em IO, fora das threads de rede e de sala.
 */
object PetAchievements {
    private fun progressOwner(ownerId: Int, name: String, amount: Int) {
        HabboServer.applicationScope.launch(Dispatchers.IO) {
            HabboServer.habboGame.achievementManager.progress(
                HabboServer.habboSessionManager.getHabboSessionById(ownerId), ownerId, name, amount, accumulate = true
            )
        }
    }

    /** Mascote comprado: +1 no ACH_PetLover (o contador total vem do backfill no login). */
    fun onPetAcquired(ownerId: Int) = progressOwner(ownerId, "ACH_PetLover", 1)

    /** Carinho: só em mascote de outro usuário (quem acaricia e o dono ganham). */
    fun onPetRespected(giver: HabboSession, petData: PetData) {
        if (petData.userId == giver.userInformation.id) return
        progressOwner(giver.userInformation.id, "ACH_PetRespectGiver", 1)
        progressOwner(petData.userId, "ACH_PetRespectReceiver", 1)
    }

    /** O mascote subiu de nível: o dono acumula os níveis treinados. */
    fun onPetLevelUp(petData: PetData, oldLevel: Int) {
        val gained = petData.level - oldLevel
        if (gained > 0) progressOwner(petData.userId, "ACH_PetLevelUp", gained)
    }
}
