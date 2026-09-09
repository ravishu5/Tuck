package com.tuck.app.data.travel

/**
 * Which continent each country sits on.
 *
 * Bundled rather than looked up: it is a few hundred bytes of facts that never change, and a
 * travel history that needs the network to tell it how many continents it covers would not be
 * an on-device travel history.
 *
 * Transcontinental countries are filed under the landmass most of the country is on, which is
 * a choice rather than a fact — the alternative is counting a single trip twice.
 */
object Continents {

    const val AFRICA = "Africa"
    const val ASIA = "Asia"
    const val EUROPE = "Europe"
    const val NORTH_AMERICA = "North America"
    const val SOUTH_AMERICA = "South America"
    const val OCEANIA = "Oceania"
    const val ANTARCTICA = "Antarctica"

    fun of(countryCode: String?): String? =
        countryCode?.trim()?.uppercase()?.let(BY_CODE::get)

    private val BY_CODE: Map<String, String> = buildMap {
        // Named `assign` rather than `put`: a local `put` here shadows `MutableMap.put`, so the
        // body calls itself instead of the map and the whole table becomes infinite recursion.
        fun assign(continent: String, vararg codes: String) {
            codes.forEach { this[it] = continent }
        }

        assign(
            AFRICA,
            "AO", "BF", "BI", "BJ", "BW", "CD", "CF", "CG", "CI", "CM", "CV", "DJ", "DZ", "EG",
            "EH", "ER", "ET", "GA", "GH", "GM", "GN", "GQ", "GW", "KE", "KM", "LR", "LS", "LY",
            "MA", "MG", "ML", "MR", "MU", "MW", "MZ", "NA", "NE", "NG", "RE", "RW", "SC", "SD",
            "SH", "SL", "SN", "SO", "SS", "ST", "SZ", "TD", "TG", "TN", "TZ", "UG", "YT", "ZA",
            "ZM", "ZW"
        )
        assign(
            ASIA,
            "AE", "AF", "AM", "AZ", "BD", "BH", "BN", "BT", "CN", "CY", "GE", "HK", "ID", "IL",
            "IN", "IQ", "IR", "JO", "JP", "KG", "KH", "KP", "KR", "KW", "KZ", "LA", "LB", "LK",
            "MM", "MN", "MO", "MV", "MY", "NP", "OM", "PH", "PK", "PS", "QA", "SA", "SG", "SY",
            "TH", "TJ", "TL", "TM", "TW", "UZ", "VN", "YE"
        )
        assign(
            EUROPE,
            "AD", "AL", "AT", "AX", "BA", "BE", "BG", "BY", "CH", "CZ", "DE", "DK", "EE", "ES",
            "FI", "FO", "FR", "GB", "GG", "GI", "GR", "HR", "HU", "IE", "IM", "IS", "IT", "JE",
            "LI", "LT", "LU", "LV", "MC", "MD", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO",
            "RS", "RU", "SE", "SI", "SJ", "SK", "SM", "TR", "UA", "VA", "XK"
        )
        assign(
            NORTH_AMERICA,
            "AG", "AI", "AW", "BB", "BL", "BM", "BQ", "BS", "BZ", "CA", "CR", "CU", "CW", "DM",
            "DO", "GD", "GL", "GP", "GT", "HN", "HT", "JM", "KN", "KY", "LC", "MF", "MQ", "MS",
            "MX", "NI", "PA", "PM", "PR", "SV", "SX", "TC", "TT", "US", "VC", "VG", "VI"
        )
        assign(
            SOUTH_AMERICA,
            "AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PE", "PY", "SR", "UY", "VE"
        )
        assign(
            OCEANIA,
            "AS", "AU", "CK", "FJ", "FM", "GU", "KI", "MH", "MP", "NC", "NF", "NR", "NU", "NZ",
            "PF", "PG", "PW", "SB", "TK", "TO", "TV", "VU", "WF", "WS"
        )
        assign(ANTARCTICA, "AQ", "BV", "GS", "HM", "TF")
    }
}
