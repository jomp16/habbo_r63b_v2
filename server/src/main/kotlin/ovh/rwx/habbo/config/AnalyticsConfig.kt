package ovh.rwx.habbo.config

import com.fasterxml.jackson.annotation.JsonProperty

data class AnalyticsConfig(
        @JsonProperty("unique_id", required = true)
        val uniqueId: Boolean,
        @JsonProperty("ip", required = true)
        val ip: Boolean
)