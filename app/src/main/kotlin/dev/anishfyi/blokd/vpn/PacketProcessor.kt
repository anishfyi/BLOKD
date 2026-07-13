package dev.anishfyi.blokd.vpn

import dev.anishfyi.blokd.dns.DnsCodec
import dev.anishfyi.blokd.dns.DnsFilter
import dev.anishfyi.blokd.dns.DnsResolver
import dev.anishfyi.blokd.dns.DnsResponseFilter
import dev.anishfyi.blokd.stats.StatsCounter

/**
 * Converts inbound IPv4/IPv6 UDP DNS packets into responses. Blocked names and
 * CNAME-cloaked answers get NXDOMAIN; resolver failures get SERVFAIL.
 */
class PacketProcessor(
    private val filter: DnsFilter,
    private val resolver: DnsResolver,
    private val stats: StatsCounter,
) {
    fun process(packet: ByteArray, length: Int): ByteArray? {
        if (length < 28) return null
        val version = (packet[0].toInt() and 0xF0) ushr 4
        return when (version) {
            4 -> processIpv4(packet, length)
            6 -> if (length >= 48) processIpv6(packet, length) else null
            else -> null
        }
    }

    private fun processIpv4(packet: ByteArray, length: Int): ByteArray? {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || packet[9].toInt() and 0xFF != 17) return null

        val udpStart = ihl
        val dstPort = readPort(packet, udpStart + 2)
        if (dstPort != 53) return null

        val udpLen = readU16(packet, udpStart + 4)
        val payloadStart = udpStart + 8
        val payloadLen = udpLen - 8
        if (payloadLen <= 0 || payloadStart + payloadLen > length) return null

        val query = packet.copyOfRange(payloadStart, payloadStart + payloadLen)
        val responsePayload = resolveQuery(query) ?: return null
        return buildIpv4UdpResponse(packet, ihl, udpStart, responsePayload)
    }

    private fun processIpv6(packet: ByteArray, length: Int): ByteArray? {
        if ((packet[6].toInt() and 0xFF) != 17) return null
        val udpStart = 40
        if (udpStart + 8 > length) return null
        val dstPort = readPort(packet, udpStart + 2)
        if (dstPort != 53) return null

        val udpLen = readU16(packet, udpStart + 4)
        val payloadStart = udpStart + 8
        val payloadLen = udpLen - 8
        if (payloadLen <= 0 || payloadStart + payloadLen > length) return null

        val query = packet.copyOfRange(payloadStart, payloadStart + payloadLen)
        val responsePayload = resolveQuery(query) ?: return null
        return buildIpv6UdpResponse(packet, udpStart, responsePayload)
    }

    private fun resolveQuery(query: ByteArray): ByteArray? {
        val domain = DnsCodec.extractDomain(query)
        if (domain != null && filter.isBlocked(domain)) {
            stats.onBlocked()
            return DnsCodec.buildNxDomainResponse(query)
        }

        val upstream = resolver.resolve(query)
        if (upstream == null) {
            stats.onFailed()
            return DnsCodec.buildServFailResponse(query)
        }
        if (DnsResponseFilter.shouldBlockResponse(filter, upstream)) {
            stats.onBlockedCname()
            return DnsCodec.buildNxDomainResponse(query)
        }
        stats.onAllowed()
        return upstream
    }

    private fun buildIpv4UdpResponse(
        request: ByteArray,
        ihl: Int,
        udpStart: Int,
        payload: ByteArray,
    ): ByteArray {
        val totalLen = ihl + 8 + payload.size
        val out = ByteArray(totalLen)
        System.arraycopy(request, 0, out, 0, ihl)
        out[2] = ((totalLen ushr 8) and 0xFF).toByte()
        out[3] = (totalLen and 0xFF).toByte()
        out[4] = 0; out[5] = 0; out[6] = 0; out[7] = 0
        out[8] = 64
        for (i in 0 until 4) {
            out[12 + i] = request[16 + i]
            out[16 + i] = request[12 + i]
        }
        out[10] = 0; out[11] = 0
        val ipChk = checksum(out, 0, ihl)
        out[10] = ((ipChk ushr 8) and 0xFF).toByte()
        out[11] = (ipChk and 0xFF).toByte()

        val srcPort = readPort(request, udpStart)
        val dstPort = readPort(request, udpStart + 2)
        out[ihl] = ((dstPort ushr 8) and 0xFF).toByte()
        out[ihl + 1] = (dstPort and 0xFF).toByte()
        out[ihl + 2] = ((srcPort ushr 8) and 0xFF).toByte()
        out[ihl + 3] = (srcPort and 0xFF).toByte()
        val udpLen = 8 + payload.size
        out[ihl + 4] = ((udpLen ushr 8) and 0xFF).toByte()
        out[ihl + 5] = (udpLen and 0xFF).toByte()
        out[ihl + 6] = 0; out[ihl + 7] = 0
        System.arraycopy(payload, 0, out, ihl + 8, payload.size)
        val udpChk = ipv4UdpChecksum(out, ihl, udpLen)
        out[ihl + 6] = ((udpChk ushr 8) and 0xFF).toByte()
        out[ihl + 7] = (udpChk and 0xFF).toByte()
        return out
    }

    private fun buildIpv6UdpResponse(
        request: ByteArray,
        udpStart: Int,
        payload: ByteArray,
    ): ByteArray {
        val udpLen = 8 + payload.size
        val totalLen = 40 + udpLen
        val out = ByteArray(totalLen)
        System.arraycopy(request, 0, out, 0, 40)
        out[4] = ((totalLen ushr 8) and 0xFF).toByte()
        out[5] = (totalLen and 0xFF).toByte()
        out[7] = 64
        for (i in 0 until 16) {
            out[8 + i] = request[24 + i]
            out[24 + i] = request[8 + i]
        }

        val srcPort = readPort(request, udpStart)
        val dstPort = readPort(request, udpStart + 2)
        out[40] = ((dstPort ushr 8) and 0xFF).toByte()
        out[41] = (dstPort and 0xFF).toByte()
        out[42] = ((srcPort ushr 8) and 0xFF).toByte()
        out[43] = (srcPort and 0xFF).toByte()
        out[44] = ((udpLen ushr 8) and 0xFF).toByte()
        out[45] = (udpLen and 0xFF).toByte()
        out[46] = 0; out[47] = 0
        System.arraycopy(payload, 0, out, 48, payload.size)
        val udpChk = ipv6UdpChecksum(out, udpLen)
        out[46] = ((udpChk ushr 8) and 0xFF).toByte()
        out[47] = (udpChk and 0xFF).toByte()
        return out
    }

    private fun readPort(packet: ByteArray, offset: Int): Int {
        return readU16(packet, offset)
    }

    private fun readU16(packet: ByteArray, offset: Int): Int {
        return ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)
    }

    private fun checksum(data: ByteArray, offset: Int, len: Int): Int {
        var sum = 0
        var i = offset
        val end = offset + len
        while (i + 1 < end) {
            sum += readU16(data, i)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv() and 0xFFFF
    }

    private fun ipv4UdpChecksum(packet: ByteArray, udpStart: Int, udpLen: Int): Int {
        var sum = 0
        var i = 12
        while (i < 20) {
            sum += readU16(packet, i)
            i += 2
        }
        sum += 17
        sum += udpLen
        i = udpStart
        val end = udpStart + udpLen
        while (i + 1 < end) {
            sum += readU16(packet, i)
            i += 2
        }
        if (i < end) sum += (packet[i].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        val result = sum.inv() and 0xFFFF
        return if (result == 0) 0xFFFF else result
    }

    private fun ipv6UdpChecksum(packet: ByteArray, udpLen: Int): Int {
        var sum = 0
        var i = 8
        while (i < 40) {
            sum += readU16(packet, i)
            i += 2
        }
        sum += 17
        sum += udpLen
        i = 40
        val end = 40 + udpLen
        while (i + 1 < end) {
            sum += readU16(packet, i)
            i += 2
        }
        if (i < end) sum += (packet[i].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        val result = sum.inv() and 0xFFFF
        return if (result == 0) 0xFFFF else result
    }
}
