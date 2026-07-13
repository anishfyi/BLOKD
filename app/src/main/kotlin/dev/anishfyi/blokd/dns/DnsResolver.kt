package dev.anishfyi.blokd.dns

/** Resolves a raw DNS wire-format query and returns its raw response. */
interface DnsResolver : AutoCloseable {
    fun resolve(query: ByteArray): ByteArray?

    override fun close() = Unit
}
