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

package ovh.rwx.habbo.admin

import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import java.util.concurrent.locks.ReentrantLock

/** Parâmetro aceito por uma ação. */
data class ActionParam(val name: String, val label: String, val required: Boolean, val maxLength: Int)

/** Descrição de uma ação da allowlist. */
data class ActionInfo(val name: String, val label: String, val description: String, val params: List<ActionParam>)

/** Resultado de [AdminActions.run]. */
sealed interface ActionOutcome {
    data class Ok(val action: String, val durationMs: Long, val message: String) : ActionOutcome
    data object Unknown : ActionOutcome
    data object Busy : ActionOutcome
    data class BadParam(val message: String) : ActionOutcome
    data class Failed(val message: String) : ActionOutcome
}

/**
 * Ações administrativas do hotel (recarregar catálogo/itens/etc. e alerta geral), numa lista fechada.
 * Usado tanto pelos comandos de chat quanto pela API admin do webapp, sem lógica duplicada.
 * As ações rodam serializadas: uma tentativa concorrente devolve [ActionOutcome.Busy].
 */
object AdminActions {
    const val MAX_ALERT_LENGTH = 500

    private val audit = LoggerFactory.getLogger("ovh.rwx.habbo.admin")
    private val lock = ReentrantLock()

    val actions: List<ActionInfo> = listOf(
        ActionInfo("reload_catalog", "Recarregar catálogo", "Recarrega o catálogo do banco e avisa todos os clientes logados.", emptyList()),
        ActionInfo("reload_items", "Recarregar itens", "Relê o furnidata.xml e os itens base do banco (furniture), interações e wireds; mobis novos no furnidata entram no banco. Pode levar alguns segundos.", emptyList()),
        ActionInfo(
            "reload_handlers", "Recarregar handlers",
            "Recarrega headers das releases e handlers de pacotes. Valida tudo antes de publicar; se faltar header ou override, mantém o estado atual e reporta o erro.",
            emptyList()
        ),
        ActionInfo("reload_navigator", "Recarregar navegador", "Recarrega as categorias do navegador.", emptyList()),
        ActionInfo("reload_permissions", "Recarregar permissões", "Recarrega as permissões por rank e por usuário.", emptyList()),
        ActionInfo("reload_landing", "Recarregar landing", "Recarrega as promoções e a recompensa da página inicial do cliente.", emptyList()),
        ActionInfo(
            "hotel_alert", "Alerta para o hotel", "Envia um alerta a todos os usuários logados.",
            listOf(ActionParam("message", "Mensagem", true, MAX_ALERT_LENGTH))
        ),
    )

    private val byName = actions.associateBy { it.name }

    fun isKnown(name: String): Boolean = name in byName

    /** Valida a mensagem do alerta (1..500 caracteres após o trim). Devolve a mensagem limpa ou null. */
    fun validateAlertMessage(message: String?): String? =
        message?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_ALERT_LENGTH }

    /**
     * Executa a ação [name] em nome de [actor] (só para o log de auditoria).
     * Roda na thread do chamador; nunca registra tokens.
     */
    fun run(name: String, params: Map<String, String?>, actor: String): ActionOutcome {
        if (name !in byName) return ActionOutcome.Unknown

        val alert = if (name == "hotel_alert") {
            validateAlertMessage(params["message"])
                ?: return ActionOutcome.BadParam("mensagem deve ter de 1 a $MAX_ALERT_LENGTH caracteres")
        } else null

        if (!lock.tryLock()) {
            audit.warn("admin-action actor={} action={} result=busy", actor, name)
            return ActionOutcome.Busy
        }

        val start = System.nanoTime()
        try {
            val message = execute(name, alert)
            val ms = (System.nanoTime() - start) / 1_000_000
            audit.info("admin-action actor={} action={} duration_ms={} result=ok", actor, name, ms)

            return ActionOutcome.Ok(name, ms, message)
        } catch (e: Exception) {
            val ms = (System.nanoTime() - start) / 1_000_000
            audit.error("admin-action actor={} action={} duration_ms={} result=error", actor, name, ms, e)

            if (e is ovh.rwx.habbo.communication.HandlerReloadException) return ActionOutcome.Failed(e.message ?: "validação falhou")

            return ActionOutcome.Failed("falha ao executar a ação: ${e.javaClass.simpleName}")
        } finally {
            lock.unlock()
        }
    }

    private fun execute(name: String, alert: String?): String {
        val game = HabboServer.habboGame

        return when (name) {
            "reload_catalog" -> {
                game.catalogManager.load()
                notifyCatalogUpdate()
                "Catálogo recarregado (${game.catalogManager.catalogPages.size - 2} páginas, ${game.catalogManager.catalogItems.size} itens)"
            }

            "reload_items" -> {
                game.itemManager.load()
                "Itens recarregados (${game.itemManager.furnishings.size} mobis)"
            }

            "reload_handlers" -> {
                HabboServer.habboHandler.load()
                "Handlers recarregados"
            }

            "reload_navigator" -> {
                game.navigatorManager.load()
                "Navegador recarregado (${game.navigatorManager.navigatorRoomCategories.size} categorias)"
            }

            "reload_permissions" -> {
                game.permissionManager.load()
                "Permissões recarregadas"
            }

            "reload_landing" -> {
                game.landingManager.load()
                "Landing recarregada (${game.landingManager.landingPromos.size} promoções)"
            }

            "hotel_alert" -> {
                val sessions = HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated }
                sessions.forEach { it.sendNotification(alert!!) }
                "Alerta enviado a ${sessions.size} usuários"
            }

            else -> error("ação desconhecida")
        }
    }

    /** Avisa os clientes logados (R63B e R63A) para recarregar o catálogo. */
    private fun notifyCatalogUpdate() {
        HabboServer.habboSessionManager.habboSessions.values.filter { it.authenticated }.forEach {
            if (!it.isPreShuffle) it.sendHabboResponse(Outgoing.CATALOG_UPDATE)
            else it.sendHabboResponse(OutgoingR63A.CATALOG_UPDATE)
        }
    }
}
