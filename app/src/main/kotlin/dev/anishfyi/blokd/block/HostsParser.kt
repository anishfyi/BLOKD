package dev.anishfyi.blokd.block

/**
 * Parses hosts-format and plain-domain block lists into a set of domains.
 * Accepts lines like "0.0.0.0 ads.example.com", "127.0.0.1 ads.example.com",
 * or a bare "ads.example.com". Comments (#) and loopback names are dropped.
 * Free of Android imports for plain-JVM unit testing.
 */
object HostsParser {

    private val validDomain = Regex("^[a-z0-9_-]+(\\.[a-z0-9_-]+)+$")

    private val skip = setOf(
        "localhost",
        "localhost.localdomain",
        "local",
        "broadcasthost",
        "ip6-localhost",
        "ip6-loopback",
    )

    fun parse(lines: Sequence<String>): Set<String> {
        val out = HashSet<String>()
        for (raw in lines) {
            var line = raw.trim()
            val hash = line.indexOf('#')
            if (hash >= 0) line = line.substring(0, hash).trim()
            if (line.isEmpty()) continue

            val parts = line.split(Regex("\\s+"))
            val domain = (if (parts.size == 1) parts[0] else parts[1])
                .lowercase()
                .trimEnd('.')

            if (domain in skip) continue
            if (validDomain.matches(domain)) out.add(domain)
        }
        return out
    }
}
