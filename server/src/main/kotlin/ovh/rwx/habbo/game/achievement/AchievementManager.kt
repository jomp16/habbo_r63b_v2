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

package ovh.rwx.habbo.game.achievement

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.achievement.AchievementDao
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.ActivityPointType
import java.util.concurrent.ConcurrentHashMap

class AchievementManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    val achievementGroups: MutableMap<String, AchievementGroup> = mutableMapOf()
    private val achievements: MutableList<Achievement> = mutableListOf()
    private val saveQueue: MutableSet<AchievementUser> = ConcurrentHashMap.newKeySet()
    val achievementLevels: Map<Int, List<Achievement>>
        get() = achievements.filter { it.enabled }.groupBy { it.groupId }
    val groupedAchievements: Map<AchievementGroup, List<Achievement>>
        get() = achievements.filter { it.enabled }.groupBy { it.group }

    fun load() {
        log.info("Loading achievements...")

        achievementGroups.clear()
        achievements.clear()

        achievementGroups += AchievementDao.loadAchievementGroups()
        achievements += AchievementDao.loadAchievements()

        log.info("Loaded {} achievement groups!", achievementGroups.size)
        log.info("Loaded {} achievements!", achievements.size)
    }

    fun progress(
        habboSession: HabboSession?,
        userId: Int,
        achievementName: String,
        amount: Int,
        accumulate: Boolean = true
    ) {
        val group = achievementGroups[achievementName] ?: return

        // Busca achievement do usuário (do banco se sessão offline)
        val achievementUsers = habboSession?.userInformation?.achievementUsers
            ?: AchievementDao.loadUserAchievements(userId).toMutableList()

        var userData = achievementUsers.find { it.groupId == group.id }

        if (userData == null) {
            userData = AchievementDao.insertUserAchievement(userId, group.id, 0, 0)
            achievementUsers.add(userData)
        }

        val levels = achievementLevels[group.id] ?: return
        if (userData.level >= levels.size) return

        if (accumulate) {
            userData.progress += amount
        } else {
            if (amount > userData.progress) {
                userData.progress = amount
            }
        }

        // Se sessão online, faz level up e envia pacotes
        if (habboSession != null) {
            checkLevelUp(habboSession, group, userData, levels)
        }

        // Adiciona na queue ao invés de salvar imediatamente
        saveQueue.add(userData)
    }

    // Sobrecarga para manter compatibilidade
    fun progress(habboSession: HabboSession, achievementName: String, amount: Int, accumulate: Boolean = true) {
        progress(habboSession, habboSession.userInformation.id, achievementName, amount, accumulate)
    }

    fun saveQueuedAchievements() {
        if (saveQueue.isEmpty()) return

        val toSave = saveQueue.toList()
        saveQueue.clear()

        AchievementDao.saveUserAchievements(toSave)

        log.debug("Saved {} queued achievements", toSave.size)
    }

    private fun checkLevelUp(
        habboSession: HabboSession,
        group: AchievementGroup,
        userData: AchievementUser,
        levels: List<Achievement>
    ) {
        var leveledUp = false
        var lastUnlockedLevel: Achievement? = null
        val startLevel = userData.level

        while (true) {
            val nextLevel = userData.level + 1
            val nextLevelData = levels.find { it.level == nextLevel } ?: break

            if (userData.progress >= nextLevelData.progressRequirement) {
                userData.level = nextLevel
                leveledUp = true
                lastUnlockedLevel = nextLevelData

                habboSession.userStats.achievementScore += nextLevelData.rewardAchievementPoints

                if (nextLevelData.rewardActivityPoints > 0) {
                    habboSession.userInformation.activityPointsCurrencies.merge(
                        ActivityPointType.PIXELS,
                        nextLevelData.rewardActivityPoints,
                        Int::plus
                    )
                }
            } else {
                break
            }
        }

        if (leveledUp) {
            // Remove badge anterior (se houver) e adiciona apenas o badge final
            if (startLevel > 0 && group.badgeAppendLevel) {
                val oldBadgeCode = "${group.name}$startLevel"
                habboSession.habboBadge.badges[oldBadgeCode]?.let {
                    habboSession.habboBadge.removeBadge(oldBadgeCode)
                }
            }

            val newBadgeCode = if (group.badgeAppendLevel) "${group.name}${userData.level}" else group.name
            if (!habboSession.habboBadge.badges.containsKey(newBadgeCode)) {
                habboSession.habboBadge.addBadge(newBadgeCode)
            }

            if (habboSession.release == "R63A") {
                habboSession.sendHabboResponse(
                    OutgoingR63A.ACTIVITY_POINTS_BALANCE,
                    habboSession.userInformation.activityPointsCurrencies
                )
                habboSession.sendHabboResponse(OutgoingR63A.ACHIEVEMENT_SCORE, habboSession.userStats.achievementScore)
                habboSession.sendHabboResponse(OutgoingR63A.ACHIEVEMENT_UNLOCKED, userData, lastUnlockedLevel!!)
                habboSession.sendHabboResponse(OutgoingR63A.ACHIEVEMENT_PROGRESS, userData)

                habboSession.sendHabboResponse(
                    OutgoingR63A.ACHIEVEMENT_LIST,
                    habboSession.userInformation.achievementUsers,
                    groupedAchievements
                )
            } else {
                habboSession.sendHabboResponse(
                    Outgoing.ACTIVITY_POINTS_BALANCE,
                    habboSession.userInformation.activityPointsCurrencies
                )
                habboSession.sendHabboResponse(Outgoing.ACHIEVEMENT_SCORE, habboSession.userStats.achievementScore)
                habboSession.sendHabboResponse(Outgoing.ACHIEVEMENT_UNLOCKED, userData, lastUnlockedLevel!!)
                habboSession.sendHabboResponse(Outgoing.ACHIEVEMENT_PROGRESS, userData)

                habboSession.sendHabboResponse(
                    Outgoing.ACHIEVEMENT_LIST,
                    habboSession.userInformation.achievementUsers,
                    groupedAchievements
                )
            }
        } else {
            if (habboSession.release == "R63A") {
                habboSession.sendHabboResponse(OutgoingR63A.ACHIEVEMENT_PROGRESS, userData)
            } else {
                habboSession.sendHabboResponse(Outgoing.ACHIEVEMENT_PROGRESS, userData)
            }
        }
    }
}