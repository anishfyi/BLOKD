package dev.anishfyi.blokd.dns

import android.net.Network
import dev.anishfyi.blokd.vpn.UnderlyingNetworkTracker
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Resolves allowed DNS queries with deadlines, fallback, and underlying-network
 * binding. Never blocks the packet thread longer than [QUERY_BUDGET_MS].
 *
 * When AdGuard is enabled the resolver stays on encrypted, ad-filtering upstreams
 * for as long as it can: it tries AdGuard DNS-over-TLS first, then an independent
 * ad-filtering operator (Mullvad) so a single-provider outage does not silently
 * drop protection. Plaintext system DNS is used only as a last resort, and only
 * when the user has not asked for strict encryption. The tier that actually
 * answered is recorded so the UI can show honestly when protection has fallen
 * back off the primary resolver.
 */
class ResolverManager(
    private val useAdGuard: Boolean,
    private val strictEncryption: Boolean,
    private val networkTracker: UnderlyingNetworkTracker,
    private val protectUdp: (DatagramSocket) -> Boolean,
    private val protectTcp: (Socket) -> Boolean,
) : DnsResolver {

    private val pool = Executors.newCachedThreadPool()

    private val protectTls: (Socket) -> Unit = { socket ->
        networkTracker.bind(socket)
        protectTcp(socket)
    }

    private val primaryDot = DnsOverTlsResolver(protect = protectTls)

    /**
     * Independent encrypted ad-filtering fallback. A different operator from the
     * primary so an AdGuard outage still leaves an ad-blocking resolver. No
     * account or config id required.
     */
    private val secondaryDot = DnsOverTlsResolver(
        hostname = "adblock.dns.mullvad.net",
        addresses = listOf("194.242.2.3", "194.242.2.4"),
        protect = protectTls,
    )

    private val generation = AtomicInteger(0)
    private val tier = AtomicReference(
        if (useAdGuard) ResolverTier.PRIMARY else ResolverTier.UNFILTERED,
    )

    override fun resolve(query: ByteArray): ByteArray? {
        val task = Callable {
            resolveBlocking(query)
        }
        val future = pool.submit(task)
        return try {
            future.get(QUERY_BUDGET_MS, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            future.cancel(true)
            null
        }
    }

    private fun resolveBlocking(query: ByteArray): ByteArray? {
        val steps = ArrayList<Pair<ResolverTier, () -> ByteArray?>>()
        if (useAdGuard) {
            steps += ResolverTier.PRIMARY to { primaryDot.resolve(query) }
            steps += ResolverTier.SECONDARY to { secondaryDot.resolve(query) }
        }
        // Plaintext fallback does no ad filtering, so it is reached only when the
        // user has not demanded strict encryption (or has AdGuard turned off).
        if (!useAdGuard || !strictEncryption) {
            for (server in networkTracker.dnsServers()) {
                steps += ResolverTier.UNFILTERED to { resolveUdp(query, server) }
            }
            steps += ResolverTier.UNFILTERED to {
                resolveUdp(query, InetAddress.getByName("1.1.1.1"))
            }
        }

        val outcome = ResolverChain.run(steps)
        tier.set(outcome.tier)
        return outcome.response
    }

    private fun resolveUdp(query: ByteArray, server: InetAddress): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                networkTracker.bind(socket)
                if (!protectUdp(socket)) return null
                socket.soTimeout = 1_500
                socket.send(
                    java.net.DatagramPacket(
                        query,
                        query.size,
                        InetSocketAddress(server, 53),
                    ),
                )
                val buffer = ByteArray(4096)
                val reply = java.net.DatagramPacket(buffer, buffer.size)
                socket.receive(reply)
                buffer.copyOf(reply.length)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun onNetworkChanged(network: Network?) {
        generation.incrementAndGet()
        primaryDot.close()
        secondaryDot.close()
    }

    /** The tier that answered the most recent query. */
    fun currentTier(): ResolverTier = tier.get()

    fun upstreamLabel(): String = when (tier.get()) {
        ResolverTier.PRIMARY ->
            if (strictEncryption) "AdGuard DoT (strict)" else "AdGuard DoT"
        ResolverTier.SECONDARY -> "Mullvad DoT (fallback)"
        ResolverTier.UNFILTERED ->
            if (useAdGuard) "System DNS (unfiltered fallback)" else "System DNS"
        ResolverTier.FAILED -> "No resolver"
    }

    override fun close() {
        primaryDot.close()
        secondaryDot.close()
        pool.shutdownNow()
    }

    companion object {
        const val QUERY_BUDGET_MS = 2_000L
    }
}
