package dev.anishfyi.blokd.dns

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Forwards an allowed DNS query to an upstream resolver over UDP and returns the
 * raw reply. The socket is handed to [protect] before use so its traffic bypasses
 * the VPN tunnel instead of looping back into it.
 */
class UpstreamResolver(
    private val upstream: String = "1.1.1.1",
    private val port: Int = 53,
    private val timeoutMs: Int = 5000,
    private val protect: (DatagramSocket) -> Unit,
) : DnsResolver {
    override fun resolve(query: ByteArray): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = timeoutMs
                val address = InetAddress.getByName(upstream)
                socket.send(DatagramPacket(query, query.size, address, port))
                val buffer = ByteArray(4096)
                val reply = DatagramPacket(buffer, buffer.size)
                socket.receive(reply)
                buffer.copyOf(reply.length)
            }
        } catch (e: Exception) {
            null
        }
    }
}
