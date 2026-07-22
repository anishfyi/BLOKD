package dev.anishfyi.blokd.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import dev.anishfyi.blokd.R
import dev.anishfyi.blokd.block.BlocklistRepository
import dev.anishfyi.blokd.dns.DnsFilter
import dev.anishfyi.blokd.dns.DnsPreferences
import dev.anishfyi.blokd.dns.ResolverManager
import dev.anishfyi.blokd.dns.ResolverTier
import dev.anishfyi.blokd.stats.HealthStatus
import dev.anishfyi.blokd.stats.ProtectionMode
import dev.anishfyi.blokd.ui.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.Socket
import kotlin.concurrent.thread

class BlokdVpnService : VpnService() {

    companion object {
        const val ACTION_START = "dev.anishfyi.blokd.START"
        const val ACTION_STOP = "dev.anishfyi.blokd.STOP"
        const val ACTION_RESTART = "dev.anishfyi.blokd.RESTART"

        private const val CHANNEL_ID = "blokd_vpn"
        private const val NOTIFICATION_ID = 1
        private const val TUN_V4 = "10.111.222.1"
        private const val DNS_V4 = "10.111.222.3"
        private const val TUN_V6 = "fd00:111:222::1"
        private const val DNS_V6 = "fd00:111:222::3"

        // Public resolvers that apps hardcode. Routed into the TUN so
        // PacketProcessor can filter UDP:53 (and drop DoH/DoT to these IPs).
        // AdGuard (94.140.x) and Mullvad (194.242.2.x) are deliberately excluded.
        private val PUBLIC_DNS_V4 = listOf(
            "8.8.8.8",
            "8.8.4.4",
            "1.1.1.1",
            "1.0.0.1",
            "9.9.9.9",
            "149.112.112.112",
            "208.67.222.222",
            "208.67.220.220",
        )
        private val PUBLIC_DNS_V6 = listOf(
            "2001:4860:4860::8888",
            "2001:4860:4860::8844",
            "2606:4700:4700::1111",
            "2606:4700:4700::1001",
            "2620:fe::fe",
            "2620:fe::9",
        )
    }

    private var tunnel: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private var worker: Thread? = null
    private var publisher: Thread? = null
    private var resolver: ResolverManager? = null
    private var networkTracker: UnderlyingNetworkTracker? = null

    private val filter = DnsFilter()
    private val stats = dev.anishfyi.blokd.stats.StatsCounter()
    private val blocklists = lazy { BlocklistRepository(this, filter) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTunnel()
                return START_NOT_STICKY
            }
            ACTION_RESTART -> {
                restartTunnel()
                return START_STICKY
            }
            else -> startTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        if (running) return
        startForeground(NOTIFICATION_ID, buildNotification())
        publish(HealthStatus.STARTING)

        val mode = DnsPreferences.protectionMode(this)
        blocklists.value.loadForMode(mode)

        val tracker = UnderlyingNetworkTracker(this) { network ->
            setUnderlyingNetworks(network?.let { arrayOf(it) } ?: emptyArray<Network>())
            resolver?.onNetworkChanged(network)
        }
        networkTracker = tracker
        tracker.start()

        val fd = Builder()
            .setSession("BLOKD")
            .setMtu(1280)
            .addAddress(TUN_V4, 32)
            .addAddress(TUN_V6, 128)
            .addDnsServer(DNS_V4)
            .addDnsServer(DNS_V6)
            .addRoute(DNS_V4, 32)
            .addRoute(DNS_V6, 128)
            .apply {
                PUBLIC_DNS_V4.forEach { addRoute(it, 32) }
                PUBLIC_DNS_V6.forEach { addRoute(it, 128) }
            }
            .allowFamily(OsConstants.AF_INET)
            .allowFamily(OsConstants.AF_INET6)
            .setBlocking(true)
            .setMetered(false)
            .setUnderlyingNetworks(tracker.currentNetwork()?.let { arrayOf(it) } ?: emptyArray<Network>())
            .establish() ?: run {
            publish(HealthStatus.ERROR)
            stopSelf()
            return
        }

        tunnel = fd
        running = true

        val activeResolver = ResolverManager(
            useAdGuard = DnsPreferences.isAdGuardEnabled(this),
            strictEncryption = DnsPreferences.isStrictEncryption(this),
            networkTracker = tracker,
            protectUdp = { socket: DatagramSocket -> protect(socket) },
            protectTcp = { socket: Socket -> protect(socket) },
        )
        resolver = activeResolver
        val processor = PacketProcessor(filter, activeResolver, stats)

        worker = thread(name = "blokd-packets", isDaemon = true) {
            runPacketLoop(fd, processor)
            if (running) {
                running = false
                publish(HealthStatus.ERROR)
            }
        }
        publisher = thread(name = "blokd-stats", isDaemon = true) {
            while (running) {
                publish(HealthStatus.HEALTHY)
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
        publish(HealthStatus.HEALTHY)
    }

    private fun runPacketLoop(fd: ParcelFileDescriptor, processor: PacketProcessor) {
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(65_535)
        try {
            while (running) {
                val n = input.read(buffer)
                if (n <= 0) continue
                val response = processor.process(buffer, n) ?: continue
                output.write(response)
            }
        } catch (_: Exception) {
            // TUN closed or interrupted.
        }
    }

    private fun restartTunnel() {
        if (!running) return
        stopTunnel(stopService = false)
        startTunnel()
    }

    private fun stopTunnel(stopService: Boolean = true) {
        running = false
        worker?.interrupt()
        publisher?.interrupt()
        resolver?.close()
        resolver = null
        networkTracker?.stop()
        networkTracker = null
        runCatching { tunnel?.close() }
        tunnel = null
        publish(HealthStatus.OFF)
        if (stopService) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel(stopService = false)
        super.onDestroy()
    }

    private fun publish(status: HealthStatus) {
        val mode = DnsPreferences.protectionMode(this)
        val adGuard = DnsPreferences.isAdGuardEnabled(this)
        val upstream = resolver?.upstreamLabel() ?: if (adGuard) "AdGuard DoT" else "System DNS"
        val tier = resolver?.currentTier()
        val effectiveStatus = when {
            status == HealthStatus.HEALTHY && networkTracker?.currentNetwork() == null -> HealthStatus.NO_NETWORK
            // AdGuard is on but the primary encrypted resolver is not the one
            // answering: protection is degraded (fell back to a secondary
            // encrypted or an unfiltered upstream). Surface it instead of a
            // false "Healthy".
            status == HealthStatus.HEALTHY && adGuard && tier != null && tier != ResolverTier.PRIMARY ->
                HealthStatus.DEGRADED
            else -> status
        }
        VpnController.publish(
            status = effectiveStatus,
            mode = mode,
            adGuardEnabled = adGuard,
            blocklist = blocklists.value.meta(),
            stats = stats.snapshot(),
            upstreamLabel = upstream,
        )
        val manager = getSystemService(NotificationManager::class.java)
        if (running) {
            manager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "BLOKD", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val intent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val mode = DnsPreferences.protectionMode(this)
        val count = filter.blockedCount()
        val text = buildString {
            append(
                when (mode) {
                    ProtectionMode.BERSERK -> "Berserk"
                    ProtectionMode.STANDARD -> "Standard"
                },
            )
            append(" · ")
            append(count)
            append(" domains")
            if (DnsPreferences.isAdGuardEnabled(this@BlokdVpnService)) {
                append(" · AdGuard DoT")
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLOKD protection is on")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }
}
