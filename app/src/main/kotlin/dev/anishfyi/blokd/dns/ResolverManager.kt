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

/**
 * Resolves allowed DNS queries with deadlines, fallback, and underlying-network
 * binding. Never blocks the packet thread longer than [QUERY_BUDGET_MS].
 */
class ResolverManager(
    private val useAdGuard: Boolean,
    private val strictEncryption: Boolean,
    private val networkTracker: UnderlyingNetworkTracker,
    private val protectUdp: (DatagramSocket) -> Boolean,
    private val protectTcp: (Socket) -> Boolean,
) : DnsResolver {

    private val pool = Executors.newCachedThreadPool()
    private val dot = DnsOverTlsResolver(
        protect = { socket ->
            networkTracker.bind(socket)
            protectTcp(socket)
        },
    )
    private val generation = AtomicInteger(0)

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
        if (useAdGuard) {
            dot.resolve(query)?.let { return it }
            if (strictEncryption) return null
        }
        for (server in networkTracker.dnsServers()) {
            resolveUdp(query, server)?.let { return it }
        }
        resolveUdp(query, InetAddress.getByName("1.1.1.1"))?.let { return it }
        return null
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
        dot.close()
    }

    fun upstreamLabel(): String = when {
        useAdGuard && strictEncryption -> "AdGuard DoT (strict)"
        useAdGuard -> "AdGuard DoT + fallback"
        else -> "System DNS"
    }

    override fun close() {
        dot.close()
        pool.shutdownNow()
    }

    companion object {
        const val QUERY_BUDGET_MS = 2_000L
    }
}
