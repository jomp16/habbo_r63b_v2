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

package ovh.rwx.habbo.game.figure

import org.junit.Assert.*
import org.junit.Test

class FigureManagerTest {

    @Test
    fun testFigureGeneratorFallback() {
        val manager = FigureManager()
        // With empty data, it should return fallback valid figures
        val figureM = manager.generateRandomFigure("M")
        assertTrue("Figure M fallback contains hd", figureM.contains("hd-"))
        assertTrue("Figure M fallback contains hr", figureM.contains("hr-"))
        assertTrue("Figure M fallback contains ch", figureM.contains("ch-"))
        assertTrue("Figure M fallback contains lg", figureM.contains("lg-"))
        assertTrue("Figure M fallback contains sh", figureM.contains("sh-"))

        val figureF = manager.generateRandomFigure("F")
        assertTrue("Figure F fallback contains hd", figureF.contains("hd-"))
        assertTrue("Figure F fallback contains hr", figureF.contains("hr-"))
        assertTrue("Figure F fallback contains ch", figureF.contains("ch-"))
        assertTrue("Figure F fallback contains lg", figureF.contains("lg-"))
        assertTrue("Figure F fallback contains sh", figureF.contains("sh-"))
    }

    @Test
    fun testFigureGeneratorWithMockData() {
        val manager = FigureManager()

        // Populate palettes
        manager.figurePalette[1] = mutableListOf(FigureColor(id = 1, selectable = true, club = 0))
        manager.figurePalette[2] = mutableListOf(FigureColor(id = 42, selectable = true, club = 0))
        manager.figurePalette[3] = mutableListOf(FigureColor(id = 62, selectable = true, club = 0))

        // Populate set types
        val hd = FigureSetType(
            "hd",
            1,
            sets = mutableListOf(
                FigureSet(
                    id = 101,
                    type = "hd",
                    paletteId = 1,
                    colors = 1,
                    gender = "U",
                    selectable = true
                )
            )
        )
        val hr = FigureSetType(
            "hr",
            2,
            sets = mutableListOf(
                FigureSet(
                    id = 102,
                    type = "hr",
                    paletteId = 2,
                    colors = 1,
                    gender = "M",
                    selectable = true
                )
            )
        )
        val ch = FigureSetType(
            "ch",
            3,
            sets = mutableListOf(
                FigureSet(
                    id = 103,
                    type = "ch",
                    paletteId = 3,
                    colors = 1,
                    gender = "U",
                    selectable = true
                )
            )
        )
        val lg = FigureSetType(
            "lg",
            3,
            sets = mutableListOf(
                FigureSet(
                    id = 104,
                    type = "lg",
                    paletteId = 3,
                    colors = 1,
                    gender = "U",
                    selectable = true
                )
            )
        )
        val sh = FigureSetType(
            "sh",
            3,
            sets = mutableListOf(
                FigureSet(
                    id = 105,
                    type = "sh",
                    paletteId = 3,
                    colors = 1,
                    gender = "U",
                    selectable = true
                )
            )
        )

        manager.figureSetTypes["hd"] = hd
        manager.figureSetTypes["hr"] = hr
        manager.figureSetTypes["ch"] = ch
        manager.figureSetTypes["lg"] = lg
        manager.figureSetTypes["sh"] = sh

        listOf(hd, hr, ch, lg, sh).forEach { st ->
            st.sets.forEach { manager.figureSets[it.id] = it }
        }

        val generated = manager.generateRandomFigure("M")
        assertTrue("Must contain hd-101-1: $generated", generated.contains("hd-101-1"))
        assertTrue("Must contain hr-102-42: $generated", generated.contains("hr-102-42"))
        assertTrue("Must contain ch-103-62: $generated", generated.contains("ch-103-62"))
        assertTrue("Must contain lg-104-62: $generated", generated.contains("lg-104-62"))
        assertTrue("Must contain sh-105-62: $generated", generated.contains("sh-105-62"))
    }

    @Test
    fun testIsValidFigureSetRobust() {
        val manager = FigureManager()

        // Palettes
        manager.figurePalette[1] = mutableListOf(
            FigureColor(id = 1, selectable = true, club = 0),
            FigureColor(id = 2, selectable = true, club = 1) // HC color
        )
        manager.figurePalette[2] = mutableListOf(FigureColor(id = 42, selectable = true, club = 0))
        manager.figurePalette[3] = mutableListOf(
            FigureColor(id = 62, selectable = true, club = 0),
            FigureColor(id = 99, selectable = false, club = 0) // unselectable color
        )

        // SetTypes with AS3 mandatory definitions
        val hd = FigureSetType(
            "hd", 1, mandM0 = true, mandF0 = true, mandM1 = true, mandF1 = true,
            sets = mutableListOf(
                FigureSet(
                    id = 101,
                    type = "hd",
                    paletteId = 1,
                    colors = 1,
                    gender = "U",
                    selectable = true
                )
            )
        )
        val hr = FigureSetType(
            "hr", 2, mandM0 = false, mandF0 = false, mandM1 = false, mandF1 = false,
            sets = mutableListOf(
                FigureSet(id = 102, type = "hr", paletteId = 2, colors = 1, gender = "M", selectable = true, club = 0),
                FigureSet(id = 103, type = "hr", paletteId = 2, colors = 1, gender = "F", selectable = true, club = 0),
                FigureSet(
                    id = 104,
                    type = "hr",
                    paletteId = 2,
                    colors = 1,
                    gender = "M",
                    selectable = true,
                    club = 1
                ), // Club hair
                FigureSet(
                    id = 105,
                    type = "hr",
                    paletteId = 2,
                    colors = 1,
                    gender = "M",
                    selectable = false,
                    club = 0
                ) // Unselectable hair
            )
        )
        val ch = FigureSetType(
            "ch", 3, mandM0 = true, mandF0 = true, mandM1 = false, mandF1 = true, // male shirtless allowed on club
            sets = mutableListOf(
                FigureSet(id = 201, type = "ch", paletteId = 3, colors = 1, gender = "U", selectable = true, club = 0),
                FigureSet(
                    id = 202,
                    type = "ch",
                    paletteId = 3,
                    colors = 1,
                    gender = "U",
                    selectable = true,
                    club = 0,
                    sellable = true
                ) // Sellable clothing
            )
        )
        val lg = FigureSetType(
            "lg", 3, mandM0 = true, mandF0 = true, mandM1 = true, mandF1 = true,
            sets = mutableListOf(
                FigureSet(
                    id = 301,
                    type = "lg",
                    paletteId = 3,
                    colors = 1,
                    gender = "U",
                    selectable = true
                )
            )
        )
        val sh = FigureSetType(
            "sh", 3, mandM0 = false, mandF0 = false, mandM1 = false, mandF1 = false,
            sets = mutableListOf(
                FigureSet(
                    id = 401,
                    type = "sh",
                    paletteId = 3,
                    colors = 1,
                    gender = "U",
                    selectable = true
                )
            )
        )

        listOf(hd, hr, ch, lg, sh).forEach { st ->
            manager.figureSetTypes[st.type] = st
            st.sets.forEach { manager.figureSets[it.id] = it }
        }

        val validMale = "hd-101-1.hr-102-42.ch-201-62.lg-301-62.sh-401-62"
        assertTrue(manager.isValidFigureSet(validMale, "M", clubLevel = 0))

        // 1. Male shirtless without club -> invalid (ch is mand_m_0)
        val maleShirtless = "hd-101-1.hr-102-42.lg-301-62.sh-401-62"
        assertFalse(manager.isValidFigureSet(maleShirtless, "M", clubLevel = 0))

        // 2. Male shirtless WITH club -> valid (mand_m_1 = 0)
        assertTrue(manager.isValidFigureSet(maleShirtless, "M", clubLevel = 1))

        // 3. Female shirtless WITH club -> invalid (mand_f_1 = 1, always mandatory)
        val femaleShirtless = "hd-101-1.hr-103-42.lg-301-62.sh-401-62"
        assertFalse(manager.isValidFigureSet(femaleShirtless, "F", clubLevel = 2))

        // 4. Missing mandatory head or legs -> invalid
        val noHead = "hr-102-42.ch-201-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(noHead, "M", clubLevel = 2))
        val noLegs = "hd-101-1.hr-102-42.ch-201-62"
        assertFalse(manager.isValidFigureSet(noLegs, "M", clubLevel = 2))

        // 5. Gender mismatch: male wearing female hair (103)
        val maleWearingFemaleHair = "hd-101-1.hr-103-42.ch-201-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(maleWearingFemaleHair, "M", clubLevel = 2))

        // 6. Club item without club
        val clubHair = "hd-101-1.hr-104-42.ch-201-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(clubHair, "M", clubLevel = 0))
        assertTrue(manager.isValidFigureSet(clubHair, "M", clubLevel = 1))

        // 7. Club color without club
        val clubColor = "hd-101-2.hr-102-42.ch-201-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(clubColor, "M", clubLevel = 0))
        assertTrue(manager.isValidFigureSet(clubColor, "M", clubLevel = 1))

        // 8. Unselectable set or color
        val unselectableHair = "hd-101-1.hr-105-42.ch-201-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(unselectableHair, "M", clubLevel = 2))
        val unselectableColor = "hd-101-1.hr-102-42.ch-201-99.lg-301-62"
        assertFalse(manager.isValidFigureSet(unselectableColor, "M", clubLevel = 2))

        // 9. Sellable clothing furni: not owned vs owned vs allowAnyClothing
        val sellableFigure = "hd-101-1.hr-102-42.ch-202-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(sellableFigure, "M", clubLevel = 2, unlockedSetIds = emptySet()))
        assertTrue(manager.isValidFigureSet(sellableFigure, "M", clubLevel = 0, unlockedSetIds = setOf(202)))
        assertTrue(manager.isValidFigureSet(sellableFigure, "M", clubLevel = 0, allowAnyClothing = true))

        // 10. Duplicate part types (e.g. 2 heads)
        val duplicateHead = "hd-101-1.hd-101-1.ch-201-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(duplicateHead, "M", clubLevel = 2))

        // 11. Type mismatch (using chest id 201 as head type hd)
        val typeMismatch = "hd-201-62.ch-201-62.lg-301-62"
        assertFalse(manager.isValidFigureSet(typeMismatch, "M", clubLevel = 2))

        // 12. resolveClubLevel
        assertEquals(0, manager.resolveClubLevel(validMale, "M"))
        assertEquals(1, manager.resolveClubLevel(clubHair, "M"))
        assertEquals(1, manager.resolveClubLevel(clubColor, "M"))
        assertEquals(1, manager.resolveClubLevel(maleShirtless, "M"))
    }
}
