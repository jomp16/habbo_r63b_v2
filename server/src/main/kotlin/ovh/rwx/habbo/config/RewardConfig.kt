package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty

data class RewardConfig(
        @param:JsonProperty("credits", required = true)
        val credits: Int,
        @param:JsonProperty("credits_max", required = true)
        val creditsMax: Int,
        @param:JsonProperty("pixels", required = true)
        val pixels: Int,
        @param:JsonProperty("pixels_max", required = true)
        val pixelsMax: Int,
        @param:JsonProperty("vip_points", required = true)
        val vipPoints: Int,
        @param:JsonProperty("vip_points_max", required = true)
        val vipPointsMax: Int,
        /** Outras moedas alternativas (duckets, conchas...) pagas no mesmo ciclo; opcional no config.yaml. */
        @param:JsonProperty("extra_currencies")
        val extraCurrencies: List<ExtraRewardCurrency> = emptyList()
)

/**
 * Recompensa periódica de uma moeda alternativa. [type] é o código do tipo (ver tabela `activity_point_types`);
 * [max] < 0 = sem teto; [vipOnly] limita a quem tem HC/VIP.
 */
data class ExtraRewardCurrency(
        @param:JsonProperty("type", required = true)
        val type: Int,
        @param:JsonProperty("amount", required = true)
        val amount: Int,
        @param:JsonProperty("max")
        val max: Int = -1,
        @param:JsonProperty("vip_only")
        val vipOnly: Boolean = false
)
