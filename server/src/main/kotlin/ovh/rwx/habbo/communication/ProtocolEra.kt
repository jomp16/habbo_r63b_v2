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

package ovh.rwx.habbo.communication

import java.time.LocalDateTime

/**
 * Era do protocolo, detectada empiricamente na fronteira do wire (HabboNettyDecoder):
 * o cliente pré-shuffle não reporta a versão na conexão (só nas misc client variables,
 * via flash.client.url), então a família é inferida pelo framing dos primeiros bytes.
 *
 * Fronteiras empíricas verificadas nos 1.567 dumps (habbo-timeline):
 * - Wedgie B64 delimitado: r34/2009-06-16 até RELEASE63-35336-34886-201108141653 (último build Wedgie puro).
 * - Pré-shuffle com encoder moderno (int length + short header, headers fixos):
 *   RELEASE63-35436-34886-201108172310 (primeiro) até RELEASE63-201110040910 (última release estática).
 * - Pós-shuffle (headers embaralhados por build): a partir de RELEASE63-201110051250.
 */
enum class ProtocolEra(
    /** Âncora temporal conservadora usada até as misc client variables refinarem a versão exata. */
    val anchorVersion: HabboVersion
) {
    /** Framing Wedgie B64 (delimiter '@'), r32..r63 2009-06 → 2011-08-14. Headers fixos. */
    WEDGIE_PRE_SHUFFLE(HabboVersion(LocalDateTime.of(2011, 8, 14, 16, 53), 63)),

    /** Encoder moderno, mas headers ainda fixos. 2011-08-17 → 2011-10-04. */
    PRE_SHUFFLE_MODERN(HabboVersion(LocalDateTime.of(2011, 10, 4, 9, 10), 63)),

    /** Headers embaralhados por build, versão reportada na conexão (ReleaseCheck 4000). ≥ 2011-10-05. */
    SHUFFLED(HabboVersion(LocalDateTime.of(2011, 10, 5, 12, 50), 63));

    /** Cliente da era pré-shuffle (Wedgie ou encoder moderno): tabelas de header estáticas, sem ReleaseCheck com versão. */
    val isPreShuffle: Boolean get() = this != SHUFFLED
}
