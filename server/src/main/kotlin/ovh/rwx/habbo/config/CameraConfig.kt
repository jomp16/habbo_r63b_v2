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

package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty

data class CameraConfig(
    @param:JsonProperty("prices", required = true)
    val prices: CameraPrices,
    @param:JsonProperty("preview_timeout_minutes", required = true)
    val previewTimeoutMinutes: Long,
    @param:JsonProperty("assets_path", required = true)
    val assetsPath: String,
    /**
     * Opcional. A única origem aceita para imagens externas das fotos (sprites do JSON que vêm como URL), lidas direto de
     * `camera_data/` sem HTTP; as de qualquer outra URL são ignoradas. Deve ser a mesma do `HR_PHOTO_URL_PREFIX` do
     * habbo-render (ex.: `https://swf.habbo.rwx.ovh/api/camera/`). Ausente = nenhuma imagem externa é desenhada.
     */
    @param:JsonProperty("photo_url_prefix", required = false)
    val photoUrlPrefix: String = "",
    /** Opcional. Ausente = render 100% local (Kotlin). */
    @param:JsonProperty("renderer", required = false)
    val renderer: CameraRendererConfig = CameraRendererConfig()
)

/**
 * Delega a rasterização ao serviço `habbo-render` (`POST /api/v1/camera/render`), permitindo atualizá-lo sem
 * reiniciar o emulador. Com [fallbackLocal], erro/timeout cai no renderer Kotlin (mesma imagem, paridade garantida).
 */
data class CameraRendererConfig(
    @param:JsonProperty("enabled", required = false)
    val enabled: Boolean = false,
    @param:JsonProperty("url", required = false)
    val url: String = "http://127.0.0.1:8090/api/v1/camera/render",
    /** Bearer token (`HR_CAMERA_TOKEN` no habbo-render). Vazio = sem autenticação. */
    @param:JsonProperty("token", required = false)
    val token: String = "",
    @param:JsonProperty("timeout_ms", required = false)
    val timeoutMs: Long = 5000,
    @param:JsonProperty("fallback_local", required = false)
    val fallbackLocal: Boolean = true
)