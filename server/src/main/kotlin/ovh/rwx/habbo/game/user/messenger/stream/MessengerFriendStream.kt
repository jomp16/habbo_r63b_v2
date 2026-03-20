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

package ovh.rwx.habbo.game.user.messenger.stream

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize

/**
 * Representa uma entrada no "Friend Stream" (Mural de Atualizações) do Messenger.
 * * ### Estrutura do Protocolo R63:
 * O cliente Flash espera uma sequência fixa de dados antes da parte variável (ExtraData).
 * A ordem correta é: `id`, `actionId`, `accountId`, `accountName`, `accountGender`, `imagePath`,
 * `minutesSince`, `linkTarget`, `numLikes`, `isLikable`.
 *
 * ### Tipos de Ações (StreamType):
 * - **FRIEND (0)**: Requer `friendId` e `friendName` no ExtraData.
 * - **ROOM (1)**: Requer `roomId` e `roomName`. Usado para "curtidas" em quartos.
 * - **ACHIEVEMENT (2)**: Requer `achievementCode`. O cliente baixa o ícone do emblema automaticamente.
 * - **MOTTO (3)**: Requer apenas o texto da missão (`motto`).
 * - **HOTEL_ALERT (4)**: Mensagem administrativa. O ícone costuma ser definido em `imageFilePath`.
 *
 * ### Tipos de Alvos de Link (StreamLinkTarget):
 * Define o comportamento do clique na notificação:
 * - `NONE (0)`: Sem ação.
 * - `FRIEND_PROFILE (1)`: Abre o perfil do usuário (usa `accountId`).
 * - `ROOM (2)`: Abre o navegador ou entra no quarto (usa `data1` como ID).
 * - `ACHIEVEMENTS (3)`: Abre a janela de conquistas do usuário.
 * - `MOTTO_CHANGER (4)`: Abre o editor de missão (apenas se for o próprio usuário).
 * - `FRIEND_FOLLOW (5)`: Tenta seguir o amigo até o quarto.
 *
 * @property id ID único da entrada no banco de dados.
 * @property type Define qual o gatilho social disparou este evento.
 * @property imageFilePath Pode ser uma String de look (hr-115...) ou o nome de um asset (ex: album_ads).
 * @property minutesAgo Delta de tempo em minutos; o Flash converte para "horas" ou "dias" atrás.
 * @property linkTargetType Define para onde o link da notificação aponta.
 */
data class MessengerFriendStream(
    val id: Int,
    val type: StreamType,
    val accountId: String,
    val userName: String,
    val imageFilePath: String,
    val userGender: String,
    val minutesAgo: Int,
    val likesCount: Int,
    val linkTargetType: StreamLinkTarget,
    val canLike: Boolean,
    val data1: String = "",
    val data2: String = ""
) : IHabboResponseSerialize {

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(id)
            writeInt(type.id)
            writeUTF(accountId)
            writeUTF(userName)
            writeUTF(userGender)
            writeUTF(imageFilePath) // Look ou caminho do asset (badge/icon)
            writeInt(minutesAgo)
            writeInt(linkTargetType.id)
            writeInt(likesCount)
            writeBoolean(canLike)

            // Processamento do ExtraData conforme o actionId/type
            when (type) {
                StreamType.FRIEND -> {
                    writeUTF(data1) // friendId
                    writeUTF(data2) // friendName
                }

                StreamType.ROOM -> {
                    writeUTF(data1) // roomId
                    writeUTF(data2) // roomName
                }

                StreamType.ACHIEVEMENT -> {
                    writeUTF(data1) // achievementCode
                }

                StreamType.MOTTO -> {
                    writeUTF(data1) // motto
                }

                StreamType.HOTEL_ALERT -> {
                    writeUTF(data1) // message (exibido como corpo do texto)

                    // Alertas de Staff podem conter links externos ou redirecionamento de quarto
                    // baseado na lógica de reutilização do campo likesCount do Habbo original.
                    when (linkTargetType) {
                        StreamLinkTarget.NONE, StreamLinkTarget.URL_LINK, StreamLinkTarget.ROOM -> {
                            // Nessas condições, o cliente executa readString() novamente
                            writeUTF(data2)
                        }

                        else -> {
                            // Outros tipos (Profile, Achievements, etc) só leem a data1
                        }
                    }
                }

                StreamType.STATUS, StreamType.EXTENDED_STATUS -> {
                    writeUTF(data1) // statusMessage
                }
            }
        }
    }
}