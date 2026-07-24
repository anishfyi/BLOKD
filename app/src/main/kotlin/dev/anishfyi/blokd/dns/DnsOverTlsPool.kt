package dev.anishfyi.blokd.dns

import java.util.concurrent.atomic.AtomicInteger

/**
 * A small pool of DNS resolvers that spreads queries round-robin across several
 * connections. Each underlying [DnsOverTlsResolver] serializes its own queries
 * on one socket, so without a pool a burst of unique lookups (a fresh page load)
 * queues behind a single connection. With N connections up to N queries run in
 * parallel. Pure JVM: only depends on the [DnsResolver] interface.
 */
class DnsOverTlsPool(
    size: Int,
    factory: () -> DnsResolver,
) : DnsResolver {

    private val resolvers: List<DnsResolver> = List(size.coerceAtLeast(1)) { factory() }
    private val next = AtomicInteger(0)

    override fun resolve(query: ByteArray): ByteArray? {
        val index = (next.getAndIncrement() % resolvers.size + resolvers.size) % resolvers.size
        return resolvers[index].resolve(query)
    }

    override fun close() {
        resolvers.forEach { runCatching { it.close() } }
    }
}
