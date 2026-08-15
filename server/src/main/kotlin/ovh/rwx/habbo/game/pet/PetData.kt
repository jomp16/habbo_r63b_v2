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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.database.user.UserInformationDao

data class PetData(
    val id: Int,
    val userId: Int,
    var roomId: Int,
    val name: String,
    val race: Int,
    val type: Int,
    val color: String,
    var happiness: Int,
    var experience: Int,
    var energy: Int,
    var hunger: Int,
    var thirst: Int,
    var respect: Int,
    val createdAt: Long,
    var x: Int,
    var y: Int,
    var z: Double,
    var rot: Int,
    var extraDataJson: String = "{}"
) : IHabboResponseSerialize {
    val ownerName: String by lazy { UserInformationDao.getUserInformationById(userId)?.username ?: "" }

    val level: Int get() = PetLevel.getLevel(experience)

    val experienceGoal: Int get() = PetLevel.getExperienceGoal(level)

    val maxEnergy: Int get() = 100

    val age: Int
        get() {
            val now = System.currentTimeMillis() / 1000
            return ((now - createdAt) / 86400).toInt()
        }

    val look: String get() = "$type $race $color"

    val isHorse: Boolean get() = type == 15

    val isMonsterPlant: Boolean get() = type == 16

    // Lazy-parsed typed extra data
    val horseData: HorseExtraData? by lazy {
        if (!isHorse) null else mapper.readValue(extraDataJson)
    }

    val monsterPlantData: MonsterPlantExtraData? by lazy {
        if (!isMonsterPlant) null else mapper.readValue(extraDataJson)
    }

    fun serializeExtraData(): String = when {
        isHorse -> horseData?.let { mapper.writeValueAsString(it) } ?: "{}"
        isMonsterPlant -> monsterPlantData?.let { mapper.writeValueAsString(it) } ?: "{}"
        else -> "{}"
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id)
            writeUTF(name)
            writeInt(type)
            writeInt(race)
            writeUTF(color)
            writeInt(0) // unknown
            writeInt(0) // custom part count
            // start - figureString
//                writeInt(0)
//                writeInt(0)
//                writeInt(0)
            // end - qtd figureString
            writeInt(level)
            if (habboResponse.isVersionAtLeast(2026, 8, 6)) {
                writeInt(0) // todo: rarityLevel
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id)
            writeUTF(name)

            if (isVersionAtLeast(2011, 9, 14)) {
                writeInt(type)
                writeInt(race)
                writeUTF(color)
                writeInt(0) // Geralmente 'experience' ou 'nature'
                writeInt(0) // Custom part count (LOOP start)
                // Se o count acima for > 0, aqui entraria o loop de Int, Int, Int
            } else if (isVersionAtLeast(2011, 9, 8)) {
                // Build 20110908: Um Int a menos antes do loop
                writeInt(type)
                writeInt(race)
                writeUTF(color)
                writeInt(0) // Este já é o Count do Loop segundo o trace
                // Se o count acima for > 0, aqui entraria o loop de Int, Int, Int
            } else if (isVersionAtLeast(2011, 2, 15)) {
                writeInt(type)
                writeInt(race)
                writeUTF(color)
            } else {
                writeUTF(look)
                if (isVersionAtLeast(2009, 11, 13)) {
                    writeInt(type)
                }
            }
        }
    }

    companion object {
        private val mapper = jacksonObjectMapper()
    }
}

object PetLevel {
    private val experienceThresholds = intArrayOf(
        100, 200, 400, 600, 900, 1300, 1800, 2400, 3200,
        4300, 5700, 7600, 10100, 13300, 17500, 23000, 30200, 39600, 51900
    )

    const val MAX_LEVEL = 20
    const val MAX_HAPPINESS = 100
    const val MAX_NUTRITION = 150

    fun getLevel(experience: Int): Int {
        for (i in experienceThresholds.indices) {
            if (experience < experienceThresholds[i]) return i + 1
        }
        return MAX_LEVEL
    }

    fun getExperienceGoal(level: Int): Int {
        val index = (level - 1).coerceIn(experienceThresholds.indices)
        return experienceThresholds[index]
    }
}
