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

package ovh.rwx.habbo.database.sequence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.database.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe Hi-Lo sequence ID generator.
 * Allocates blocks of IDs from database to generate unique integer IDs in memory at nanosecond speed.
 *
 * hi e lo vivem num único [AtomicLong] (hi nos 32 bits altos, lo nos baixos): cada `nextId()` lê os dois do MESMO
 * estado. Antes eram dois campos, e uma thread que pegou o último lo de um bloco e foi pausada antes de ler o hi podia
 * montar o id com o hi do bloco SEGUINTE, gerando um id que sairia de novo depois (INSERT duplicado, linha perdida).
 */
open class HiLoSequence(
    val sequenceName: String,
    val tableName: String = sequenceName,
    val idColumn: String = "id",
    val blockSize: Int = 1000
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lock = Any()

    // começa sem bloco (hi = NO_BLOCK): o primeiro nextId()/warmUp() reserva
    private val state = AtomicLong(pack(NO_BLOCK, blockSize))

    init {
        require(blockSize > 0) { "blockSize must be positive" }
        all += this
    }

    /**
     * Reserva o primeiro bloco no boot, evitando uma transação síncrona (round-trip ao banco)
     * na primeira chamada a [nextId] — normalmente dentro da thread de um quarto/event loop.
     */
    fun warmUp() {
        synchronized(lock) {
            if (exhausted(state.get())) reserveBlock()
        }
    }

    fun nextId(): Int {
        while (true) {
            // incrementa só o lo (32 bits baixos); hi e lo saem do mesmo valor lido
            val s = state.getAndIncrement()
            val hi = hiOf(s)
            val lo = loOf(s)
            if (hi != NO_BLOCK && lo < blockSize) return hi * blockSize + lo.toInt()

            synchronized(lock) {
                if (exhausted(state.get())) reserveBlock()
            }
        }
    }

    /**
     * Shutdown limpo: devolve o bloco atual se nenhum id dele saiu (lo = 0), para o próximo boot reusar o mesmo hi
     * em vez de queimar um bloco por sequência a cada restart. Só devolve se ninguém reservou depois (o `UPDATE` exige
     * `next_hi = hi + 1`). Depois disso a sequência fica sem bloco; um `nextId()` tardio reserva de novo.
     */
    fun releaseUnusedBlock(): Boolean = synchronized(lock) {
        val s = state.get()
        val hi = hiOf(s)
        if (hi == NO_BLOCK || loOf(s) != 0L) return false
        // nextId() incrementa fora do lock: se alguém pegou o lo 0 neste meio-tempo, o bloco já está em uso
        if (!state.compareAndSet(s, pack(NO_BLOCK, blockSize))) return false

        val released = returnBlock(hi)
        if (released) log.info("Released unused Hi-Lo block for '{}': hi={}", sequenceName, hi)
        released
    }

    private fun reserveBlock() {
        val hi = fetchNextHi()
        state.set(pack(hi, 0))
        log.info("Reserved Hi-Lo block for '{}': hi={}, range=[{}..{}]", sequenceName, hi, hi * blockSize, (hi + 1) * blockSize - 1)
    }

    private fun exhausted(s: Long) = hiOf(s) == NO_BLOCK || loOf(s) >= blockSize

    /** Reserva o próximo hi no banco (transação com `FOR UPDATE`). Aberto para os testes. */
    protected open fun fetchNextHi(): Int {
        return dbTransaction {
            val existing = queryOne<Int>(
                "SELECT `next_hi` FROM `sequences` WHERE `sequence_name` = :name FOR UPDATE",
                mapOf("name" to sequenceName)
            )

            if (existing == null) {
                val maxId = try {
                    queryOne<Int>("SELECT COALESCE(MAX(`$idColumn`), 0) FROM `$tableName`") ?: 0
                } catch (e: Exception) {
                    0
                }
                val initialHi = (maxId / blockSize) + 1
                update(
                    "INSERT INTO `sequences` (`sequence_name`, `next_hi`) VALUES (:name, :hi)",
                    mapOf("name" to sequenceName, "hi" to initialHi + 1)
                )
                initialHi
            } else {
                update(
                    "UPDATE `sequences` SET `next_hi` = `next_hi` + 1 WHERE `sequence_name` = :name",
                    mapOf("name" to sequenceName)
                )
                existing
            }
        }
    }

    /** Devolve [hi] ao banco se ele ainda é o último bloco reservado. Aberto para os testes. */
    protected open fun returnBlock(hi: Int): Boolean = db {
        update(
            "UPDATE `sequences` SET `next_hi` = :hi WHERE `sequence_name` = :name AND `next_hi` = :next",
            mapOf("hi" to hi, "next" to hi + 1, "name" to sequenceName)
        ) > 0
    }

    companion object {
        private const val NO_BLOCK = -1

        private val all = CopyOnWriteArrayList<HiLoSequence>()

        private fun pack(hi: Int, lo: Int): Long = (hi.toLong() shl 32) or (lo.toLong() and 0xFFFF_FFFFL)
        private fun hiOf(s: Long): Int = (s ushr 32).toInt()
        private fun loOf(s: Long): Long = s and 0xFFFF_FFFFL

        /** Shutdown: devolve os blocos não usados de todas as sequências em paralelo (falha de uma não impede as outras). */
        fun releaseAllUnusedBlocks() {
            val log = LoggerFactory.getLogger(HiLoSequence::class.java)
            runBlocking(Dispatchers.IO.limitedParallelism(8)) {
                for (seq in all) {
                    launch {
                        try {
                            seq.releaseUnusedBlock()
                        } catch (e: Exception) {
                            log.warn("Failed to release Hi-Lo block of '{}'", seq.sequenceName, e)
                        }
                    }
                }
            }
        }
    }
}
