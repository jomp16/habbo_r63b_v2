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

package ovh.rwx.habbo.communication.outgoing.wired

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class WiredAllVariablesDiffsResponse {
    @Response(Outgoing.WIRED_ALL_VARIABLES_DIFFS)
    fun response(habboResponse: HabboResponse) {
        // TODO: Implementar quando refazer wired 2.0
        // Estrutura do pacote:
        // - allVariablesHash (Int): Hash de todas as variáveis
        // - isLastChunk (Boolean): Se é o último chunk de dados
        // - removedVariablesCount (Int): Quantidade de variáveis removidas
        //   - removedVariables (String[]): Array com IDs das variáveis removidas
        // - addedOrUpdatedCount (Int): Quantidade de variáveis adicionadas/atualizadas
        //   Para cada variável:
        //   - variableIdHash (Int): Hash do ID da variável
        //   - WiredVariable:
        //     - variableId (String): ID da variável
        //     - variableType (Int): Tipo da variável
        //     - variableName (String): Nome da variável
        //     - availabilityType (Int): Tipo de disponibilidade
        //     - variableTarget (Int): Target da variável
        //     - alwaysAvailable (Boolean): Se está sempre disponível
        //     - canCreateAndDelete (Boolean): Se pode criar e deletar
        //     - hasValue (Boolean): Se tem valor
        //     - canWriteValue (Boolean): Se pode escrever valor
        //     - canInterceptChanges (Boolean): Se pode interceptar mudanças
        //     - isInvisible (Boolean): Se é invisível
        //     - canReadCreationTime (Boolean): Se pode ler tempo de criação
        //     - canReadLastUpdateTime (Boolean): Se pode ler tempo de última atualização
        //     - hasTextConnector (Boolean): Se tem conector de texto
        //       Se hasTextConnector = true:
        //       - textConnectorCount (Int): Quantidade de conectores
        //         Para cada conector:
        //         - connectorId (Int): ID do conector
        //         - connectorValue (String): Valor do conector

        habboResponse.apply {
            writeInt(0) // allVariablesHash
            writeBoolean(true) // isLastChunk
            writeInt(0) // removedVariablesCount (empty array)
            writeInt(0) // addedOrUpdatedCount (empty array)
        }
    }
}
