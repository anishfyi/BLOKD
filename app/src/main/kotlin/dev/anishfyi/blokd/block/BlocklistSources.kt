package dev.anishfyi.blokd.block

object BlocklistSources {
    const val HAGEZI_PRO_PLUS_MINI =
        "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/pro.plus.mini-onlydomains.txt"
    const val OISD_BIG = "https://big.oisd.nl/domainswild2"

    val STANDARD = listOf(HAGEZI_PRO_PLUS_MINI, OISD_BIG)

    /** Aggressive profile adds the full HaGeZi Pro hosts list for extra coverage. */
    val BERSERK = listOf(
        HAGEZI_PRO_PLUS_MINI,
        "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/pro.plus-onlydomains.txt",
        OISD_BIG,
    )

    /** Never block Android/OEM connectivity validation hostnames. */
    val CONNECTIVITY_ALLOW = setOf(
        "connectivitycheck.gstatic.com",
        "clients3.google.com",
        "clients.google.com",
        "www.google.com",
        "google.com",
        "dns.google",
        "one.one.one.one",
        "cloudflare-dns.com",
        "mtalk.google.com",
        "play.googleapis.com",
        "android.googleapis.com",
    )
}
