package dev.anishfyi.blokd.block

object BlocklistSources {
    const val HAGEZI_PRO_PLUS_MINI =
        "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/pro.plus.mini-onlydomains.txt"
    const val OISD_BIG = "https://big.oisd.nl/domainswild2"
    const val HAGEZI_DOH =
        "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/doh-onlydomains.txt"

    val STANDARD = listOf(HAGEZI_PRO_PLUS_MINI, OISD_BIG, HAGEZI_DOH)

    /** Aggressive profile adds the full HaGeZi Pro hosts list for extra coverage. */
    val BERSERK = listOf(
        HAGEZI_PRO_PLUS_MINI,
        "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/pro.plus-onlydomains.txt",
        OISD_BIG,
        HAGEZI_DOH,
    )

    /**
     * Always-on set of well-known encrypted-DNS bootstrap hostnames. Apps that
     * try their own DoH/DoT get NXDOMAIN and fall back to plaintext DNS that
     * BLOKD filters. AdGuard and Mullvad are deliberately excluded: they are
     * BLOKD's own upstreams, reached by IP.
     */
    val ANTI_BYPASS = setOf(
        "dns.google",
        "cloudflare-dns.com",
        "one.one.one.one",
        "dns.quad9.net",
        "dns9.quad9.net",
        "dns10.quad9.net",
        "dns11.quad9.net",
        "doh.opendns.com",
        "dns.opendns.com",
        "dns.nextdns.io",
        "doh.dns.sb",
        "dns.controld.com",
        "freedns.controld.com",
        "use-application-dns.net",
    )

    /** Never block Android/OEM connectivity validation hostnames. */
    val CONNECTIVITY_ALLOW = setOf(
        "connectivitycheck.gstatic.com",
        "clients3.google.com",
        "clients.google.com",
        "www.google.com",
        "google.com",
        "mtalk.google.com",
        "play.googleapis.com",
        "android.googleapis.com",
    )
}
