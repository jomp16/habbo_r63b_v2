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

package ovh.rwx.habbo.encryption

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import ovh.rwx.habbo.encryption.wedgie.HabboRandom
import ovh.rwx.habbo.encryption.wedgie.WedgieWrapper
import kotlin.random.Random

class WedgieRC4SnapshotTest {
    @Test
    fun `WedgieRC4 snapshot e fromSnapshot mantem paridade byte a byte`() {
        val random = Random(99)
        val key = random.nextBytes(32)
        val continuous = WedgieRC4(key)
        val original = WedgieRC4(key)

        // Avanca o estado com N bytes (cruzando multiplos de 64 para acionar o shuffle step)
        val prefix = random.nextBytes(300)
        assertArrayEquals(continuous.parse(prefix), original.parse(prefix))

        val snapshot = original.snapshot()
        val restored = WedgieRC4.fromSnapshot(snapshot)

        assertEquals(snapshot, restored.snapshot())

        // Mutar original nao pode alterar o snapshot nem a instancia restaurada
        original.parse(random.nextBytes(128))
        assertEquals(snapshot, restored.snapshot())

        repeat(10) {
            val chunk = random.nextBytes(random.nextInt(1, 512))
            assertArrayEquals(continuous.parse(chunk), restored.parse(chunk))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WedgieRC4 fromSnapshot rejeita sBox com tamanho diferente de 256`() {
        WedgieRC4.fromSnapshot(WedgieRC4.Snapshot(IntArray(128), 0, 0))
    }

    @Test
    fun `HabboRandom e WedgieWrapper snapshot e fromSnapshot mantem paridade completa`() {
        val random = Random(2026)
        val innerKey = random.nextBytes(24)
        val outerKey = random.nextBytes(24)

        val continuous = WedgieWrapper(
            WedgieRC4(innerKey),
            WedgieRC4(outerKey),
            HabboRandom(seed = 12345, modulus = 65536)
        )
        val original = WedgieWrapper(
            WedgieRC4(innerKey),
            WedgieRC4(outerKey),
            HabboRandom(seed = 12345, modulus = 65536)
        )

        repeat(5) {
            val header = random.nextBytes(4)
            val payload = random.nextBytes(random.nextInt(10, 200))
            assertArrayEquals(continuous.inner.parse(header), original.inner.parse(header))
            assertArrayEquals(continuous.outer.parse(payload), original.outer.parse(payload))
            assertEquals(continuous.rng.nextInt(), original.rng.nextInt())
        }

        val snapshot = original.snapshot()
        val restored = WedgieWrapper.fromSnapshot(snapshot)

        assertEquals(snapshot, restored.snapshot())

        // Avanca original para garantir isolamento do snapshot
        original.inner.parse(random.nextBytes(16))
        original.outer.parse(random.nextBytes(64))
        original.rng.nextInt()

        repeat(15) {
            val header = random.nextBytes(4)
            val payload = random.nextBytes(random.nextInt(10, 400))
            assertArrayEquals(continuous.inner.parse(header), restored.inner.parse(header))
            assertArrayEquals(continuous.outer.parse(payload), restored.outer.parse(payload))
            assertEquals(continuous.rng.nextInt(), restored.rng.nextInt())
        }
    }
}
