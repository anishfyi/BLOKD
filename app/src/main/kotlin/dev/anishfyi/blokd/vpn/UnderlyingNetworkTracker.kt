package dev.anishfyi.blokd.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference

/**
 * Tracks the best underlying physical network for upstream DNS and keeps the VPN
 * informed via [onNetworkChanged].
 */
class UnderlyingNetworkTracker(
    context: Context,
    private val onNetworkChanged: (Network?) -> Unit,
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val active = AtomicReference<Network?>(null)

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = pickBestNetwork()
        override fun onLost(network: Network) = pickBestNetwork()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = pickBestNetwork()
    }

    fun start() {
        connectivity.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback,
        )
        pickBestNetwork()
    }

    fun stop() {
        runCatching { connectivity.unregisterNetworkCallback(callback) }
        active.set(null)
        onNetworkChanged(null)
    }

    fun currentNetwork(): Network? = active.get()

    fun dnsServers(): List<InetAddress> {
        val network = active.get() ?: return emptyList()
        val props: LinkProperties = connectivity.getLinkProperties(network) ?: return emptyList()
        return props.dnsServers
    }

    private fun pickBestNetwork() {
        val networks = connectivity.allNetworks
        val best = networks.firstOrNull { network ->
            val caps = connectivity.getNetworkCapabilities(network) ?: return@firstOrNull false
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: networks.firstOrNull { network ->
            val caps = connectivity.getNetworkCapabilities(network) ?: return@firstOrNull false
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        if (active.getAndSet(best) != best) {
            onNetworkChanged(best)
        }
    }

    fun bind(socket: Socket) {
        val network = active.get() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            network.bindSocket(socket)
        }
    }

    fun bind(socket: DatagramSocket) {
        val network = active.get() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            network.bindSocket(socket)
        }
    }
}
