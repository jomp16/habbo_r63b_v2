/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class IpInfo(
        @param:JsonProperty("ip")
        val ip: String? = "",
        @param:JsonProperty("hostname")
        val hostname: String? = "",
        @param:JsonProperty("type")
        val type: String? = "",
        @param:JsonProperty("continent_code")
        val continentCode: String? = "",
        @param:JsonProperty("continent_name")
        val continentName: String? = "",
        @param:JsonProperty("country_code")
        val countryCode: String? = "",
        @param:JsonProperty("country_name")
        val countryName: String? = "",
        @param:JsonProperty("region_code")
        val regionCode: String? = "",
        @param:JsonProperty("region_name")
        val regionName: String? = "",
        @param:JsonProperty("city")
        val city: String? = "",
        @param:JsonProperty("latitude")
        val latitude: Double? = 0.toDouble(),
        @param:JsonProperty("longitude")
        val longitude: Double? = 0.toDouble()
)