package dev.anishfyi.blokd.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import dev.anishfyi.blokd.R
import dev.anishfyi.blokd.block.BlocklistRepository
import dev.anishfyi.blokd.dns.DnsFilter
import dev.anishfyi.blokd.dns.UpstreamResolver
import dev.anishfyi.blokd.stats.StatsCounter
import dev.anishfyi.blokd.ui.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import kotlin.concurrent.thread

/**
 * The local VPN. It routes only the sentinel DNS address into the tunnel, so all
 * other traffic flows normally and no full IP stack is required. Each captured
 * DNS packet is filtered by [PacketProcessor] and either answered with NXDOMAIN
 * or forwarded upstream.
 */
class BlokdVpnService : VpnService() {

    companion object {
        const val ACTION_START = "dev.anishfyi.blokd.START"
        const val ACTION_STOP = "dev.anishfyi.blokd.STOP"

        private const val CHANNEL_ID = "blokd_vpn"
        private const val NOTIFICATION_ID = 1
        private const val TUN_ADDRESS = "10.111.222.1"
        private const val DNS_SENTINEL = "10.111.222.3"
    }

    private var tunnel: ParcelFileDescriptor? = null

    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var publisher: Thread? = null

    private val filter = DnsFilter()
    private val stats = StatsCounter()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTunnel()
                return START_NOT_STICKY
            }
            else -> startTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        if (running) return
        BlocklistRepository(this, filter).loadCachedOrSeed()

        val fd = Builder()
            .setSession("BLOKD")
            .addAddress(TUN_ADDRESS, 24)
            .addDnsServer(DNS_SENTINEL)
            .addRoute(DNS_SENTINEL, 32)
            .setBlocking(true)
            .establish() ?: run { stopSelf(); return }

        tunnel = fd
        running = true
        startForeground(NOTIFICATION_ID, buildNotification())
        VpnController.running.value = true

        val resolver = UpstreamResolver(protect = { socket: DatagramSocket -> protect(socket) })
        val processor = PacketProcessor(filter, resolver, stats)

        worker = thread(name = "blokd-packets", isDaemon = true) {
            runPacketLoop(fd, processor)
        }
        publisher = thread(name = "blokd-stats", isDaemon = true) {
            while (running) {
                VpnController.stats.value = stats.snapshot()
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun runPacketLoop(fd: ParcelFileDescriptor, processor: PacketProcessor) {
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)
        try {
            while (running) {
                val n = input.read(buffer)
                if (n <= 0) continue
                val response = processor.process(buffer, n) ?: continue
                output.write(response)
            }
        } catch (e: Exception) {
            // Interface torn down or read interrupted. Exit the loop quietly.
        }
    }

    private fun stopTunnel() {
        running = false
        worker?.interrupt()
        publisher?.interrupt()
        try {
            tunnel?.close()
        } catch (e: Exception) {
            // Already closed.
        }
        tunnel = null
        VpnController.running.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLOKD is on")
            .setContentText("Blocking ad and tracker domains")
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }
}
