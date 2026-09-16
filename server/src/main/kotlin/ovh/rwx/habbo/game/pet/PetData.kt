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
import ovh.rwx.habbo.communication.*
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

            writePetFigureData()

            if (isVersionAtLeast(2012, 4, 30)) {
                writeInt(level)
            }

            if (isAir && isVersionAtLeast(2026, 5, 18)) {
                writeInt(0) // todo: rarityLevel
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        serializeHabboResponse(habboResponse, *params)
    }

    private fun HabboResponse.writePetFigureData() {
        // Pré-2011: Protocolo legado baseado em string de look
        if (isVersionBefore(2011, 2, 15)) {
            writeUTF(look)
            if (isVersionAtLeast(2009, 11, 13)) {
                writeInt(type)
            }
            return
        }

        // Base comum a partir de 2011-02-15
        writeInt(type)
        writeInt(race)
        writeUTF(color)

        // A partir de 2011-09-14 foi adicionado o breedId antes das partes customizadas
        if (isVersionAtLeast(2011, 9, 14)) {
            writeInt(0) // todo: breedId
        }

        // A partir de 2011-09-08 foi introduzido o count de custom parts
        if (isVersionAtLeast(2011, 9, 8)) {
            writeInt(0) // custom part count (loop)
            // Se count > 0: loop de writeInt, writeInt, writeInt
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
