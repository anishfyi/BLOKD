package dev.anishfyi.blokd.dns

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Resolves DNS through AdGuard's public DNS-over-TLS service. The TCP socket is
 * protected before connecting so its traffic cannot loop back into BLOKD.
 *
 * A connection is reused while AdGuard keeps it open. On a stale or failed
 * connection, the query is retried once against the other published endpoint.
 */
class DnsOverTlsResolver(
    private val hostname: String = "dns.adguard-dns.com",
    private val addresses: List<String> = listOf("94.140.14.14", "94.140.15.15"),
    private val port: Int = 853,
    private val timeoutMs: Int = 5000,
    private val protect: (Socket) -> Unit,
) : DnsResolver {

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var nextAddress = 0

    @Synchronized
    override fun resolve(query: ByteArray): ByteArray? {
        repeat(2) {
            try {
                ensureConnected()
                DnsOverTlsFraming.writeQuery(output ?: error("Missing DNS output"), query)
                return DnsOverTlsFraming.readResponse(input ?: error("Missing DNS input"))
            } catch (e: Exception) {
                closeConnection()
            }
        }
        return null
    }

    @Synchronized
    override fun close() {
        closeConnection()
    }

    private fun ensureConnected() {
        if (socket?.isConnected == true && socket?.isClosed == false) return

        var lastFailure: Exception? = null
        repeat(addresses.size) {
            val address = addresses[nextAddress]
            nextAddress = (nextAddress + 1) % addresses.size
            val rawSocket = Socket()
            try {
                protect(rawSocket)
                rawSocket.soTimeout = timeoutMs
                rawSocket.tcpNoDelay = true
                rawSocket.connect(InetSocketAddress(address, port), timeoutMs)

                val tlsFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val tlsSocket = tlsFactory
                    .createSocket(rawSocket, hostname, port, true) as SSLSocket
                tlsSocket.soTimeout = timeoutMs
                tlsSocket.useClientMode = true
                tlsSocket.sslParameters = tlsSocket.sslParameters.apply {
                    endpointIdentificationAlgorithm = "HTTPS"
                    serverNames = listOf(SNIHostName(hostname))
                }
                tlsSocket.startHandshake()

                socket = tlsSocket
                input = DataInputStream(BufferedInputStream(tlsSocket.inputStream))
                output = DataOutputStream(BufferedOutputStream(tlsSocket.outputStream))
                return
            } catch (e: Exception) {
                lastFailure = e
                runCatching { rawSocket.close() }
            }
        }
        throw IOException("Unable to connect to AdGuard DNS", lastFailure)
    }

    private fun closeConnection() {
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }
}

internal object DnsOverTlsFraming {
    private const val MIN_DNS_MESSAGE_SIZE = 12
    private const val MAX_DNS_MESSAGE_SIZE = 65_535

    fun writeQuery(output: DataOutputStream, query: ByteArray) {
        require(query.size in MIN_DNS_MESSAGE_SIZE..MAX_DNS_MESSAGE_SIZE) {
            "Invalid DNS query size"
        }
        output.writeShort(query.size)
        output.write(query)
        output.flush()
    }

    fun readResponse(input: DataInputStream): ByteArray {
        val length = input.readUnsignedShort()
        if (length !in MIN_DNS_MESSAGE_SIZE..MAX_DNS_MESSAGE_SIZE) {
            throw IOException("Invalid DNS-over-TLS response size: $length")
        }
        return ByteArray(length).also(input::readFully)
    }
}
