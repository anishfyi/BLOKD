package dev.anishfyi.blokd.vpn

import dev.anishfyi.blokd.dns.DnsCodec
import dev.anishfyi.blokd.dns.DnsFilter
import dev.anishfyi.blokd.dns.UpstreamResolver
import dev.anishfyi.blokd.stats.StatsCounter

/**
 * Turns a single inbound IPv4 UDP DNS packet into a response packet. Blocked
 * names get an NXDOMAIN answer built locally; everything else is forwarded to
 * the upstream resolver. Returns null when the packet is not IPv4 UDP to port 53
 * or when no reply is available.
 *
 * Only DNS is routed into the tunnel (see BlokdVpnService), so this handler does
 * not need a full IP stack. It reuses the request headers and swaps addresses
 * and ports to form the reply.
 */
class PacketProcessor(
    private val filter: DnsFilter,
    private val resolver: UpstreamResolver,
    private val stats: StatsCounter,
) {
    fun process(packet: ByteArray, length: Int): ByteArray? {
        if (length < 28) return null
        val version = (packet[0].toInt() and 0xF0) ushr 4
        if (version != 4) return null

        val ihl = (packet[0].toInt() and 0x0F) * 4
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // UDP

        val udpStart = ihl
        val dstPort = ((packet[udpStart + 2].toInt() and 0xFF) shl 8) or
            (packet[udpStart + 3].toInt() and 0xFF)
        if (dstPort != 53) return null

        val udpLen = ((packet[udpStart + 4].toInt() and 0xFF) shl 8) or
            (packet[udpStart + 5].toInt() and 0xFF)
        val payloadStart = udpStart + 8
        val payloadLen = udpLen - 8
        if (payloadLen <= 0 || payloadStart + payloadLen > length) return null

        val dnsQuery = packet.copyOfRange(payloadStart, payloadStart + payloadLen)
        val domain = DnsCodec.extractDomain(dnsQuery)

        val responsePayload: ByteArray = if (domain != null && filter.isBlocked(domain)) {
            stats.onBlocked()
            DnsCodec.buildNxDomainResponse(dnsQuery)
        } else {
            stats.onAllowed()
            resolver.resolve(dnsQuery) ?: return null
        }

        return buildIpv4UdpResponse(packet, ihl, udpStart, responsePayload)
    }

    /** Wraps a DNS payload in an IPv4 + UDP packet with src and dst swapped from the request. */
    private fun buildIpv4UdpResponse(
        request: ByteArray,
        ihl: Int,
        udpStart: Int,
        payload: ByteArray,
    ): ByteArray {
        val totalLen = ihl + 8 + payload.size
        val out = ByteArray(totalLen)

        // IP header: copy, then fix total length, reset id and flags, set TTL,
        // swap addresses, and recompute the header checksum.
        System.arraycopy(request, 0, out, 0, ihl)
        out[2] = ((totalLen ushr 8) and 0xFF).toByte()
        out[3] = (totalLen and 0xFF).toByte()
        out[4] = 0; out[5] = 0; out[6] = 0; out[7] = 0
        out[8] = 64 // TTL
        for (i in 0 until 4) {
            out[12 + i] = request[16 + i]
            out[16 + i] = request[12 + i]
        }
        out[10] = 0; out[11] = 0
        val ipChk = checksum(out, 0, ihl)
        out[10] = ((ipChk ushr 8) and 0xFF).toByte()
        out[11] = (ipChk and 0xFF).toByte()

        // UDP header: swap ports, set length, clear then compute the checksum.
        val srcPort = ((request[udpStart].toInt() and 0xFF) shl 8) or
            (request[udpStart + 1].toInt() and 0xFF)
        val dstPort = ((request[udpStart + 2].toInt() and 0xFF) shl 8) or
            (request[udpStart + 3].toInt() and 0xFF)
        out[ihl] = ((dstPort ushr 8) and 0xFF).toByte()
        out[ihl + 1] = (dstPort and 0xFF).toByte()
        out[ihl + 2] = ((srcPort ushr 8) and 0xFF).toByte()
        out[ihl + 3] = (srcPort and 0xFF).toByte()
        val udpLen = 8 + payload.size
        out[ihl + 4] = ((udpLen ushr 8) and 0xFF).toByte()
        out[ihl + 5] = (udpLen and 0xFF).toByte()
        out[ihl + 6] = 0; out[ihl + 7] = 0
        System.arraycopy(payload, 0, out, ihl + 8, payload.size)
        val udpChk = udpChecksum(out, ihl, udpLen)
        out[ihl + 6] = ((udpChk ushr 8) and 0xFF).toByte()
        out[ihl + 7] = (udpChk and 0xFF).toByte()

        return out
    }

    private fun checksum(data: ByteArray, offset: Int, len: Int): Int {
        var sum = 0
        var i = offset
        val end = offset + len
        while (i + 1 < end) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv() and 0xFFFF
    }

    private fun udpChecksum(packet: ByteArray, udpStart: Int, udpLen: Int): Int {
        var sum = 0
        // Pseudo-header: source and destination addresses (already swapped in packet).
        var i = 12
        while (i < 20) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        sum += 17 // protocol
        sum += udpLen
        i = udpStart
        val end = udpStart + udpLen
        while (i + 1 < end) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) sum += (packet[i].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        val result = sum.inv() and 0xFFFF
        return if (result == 0) 0xFFFF else result
    }
}
