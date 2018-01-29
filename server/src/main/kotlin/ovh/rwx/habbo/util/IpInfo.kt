package ovh.rwx.habbo.util

import com.fasterxml.jackson.annotation.JsonProperty

data class IpInfo(
        @JsonProperty("ip")
        val ip: String = "",
        @JsonProperty("country_code")
        val countryCode: String = "",
        @JsonProperty("country_name")
        val countryName: String = "",
        @JsonProperty("region_code")
        val regionCode: String = "",
        @JsonProperty("region_name")
        val regionName: String = "",
        @JsonProperty("city")
        val city: String = "",
        @JsonProperty("zip_code")
        val zipCode: String = "",
        @JsonProperty("time_zone")
        val timeZone: String = "",
        @JsonProperty("latitude")
        val latitude: Double = 0.toDouble(),
        @JsonProperty("longitude")
        val longitude: Double = 0.toDouble(),
        @JsonProperty("metro_code")
        val metroCode: Int = 0
)