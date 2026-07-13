package dev.anishfyi.blokd.dns

import android.content.Context
import dev.anishfyi.blokd.stats.ProtectionMode

object DnsPreferences {
    private const val PREFERENCES_NAME = "blokd_dns"
    private const val ADGUARD_ENABLED = "adguard_enabled"
    private const val PROTECTION_MODE = "protection_mode"
    private const val STRICT_ENCRYPTION = "strict_encryption"

    fun isAdGuardEnabled(context: Context): Boolean =
        preferences(context).getBoolean(ADGUARD_ENABLED, true)

    fun setAdGuardEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(ADGUARD_ENABLED, enabled).apply()
    }

    fun protectionMode(context: Context): ProtectionMode {
        val raw = preferences(context).getString(PROTECTION_MODE, ProtectionMode.BERSERK.name)
        return runCatching { ProtectionMode.valueOf(raw ?: ProtectionMode.BERSERK.name) }
            .getOrDefault(ProtectionMode.BERSERK)
    }

    fun setProtectionMode(context: Context, mode: ProtectionMode) {
        preferences(context).edit().putString(PROTECTION_MODE, mode.name).apply()
    }

    fun isStrictEncryption(context: Context): Boolean =
        preferences(context).getBoolean(STRICT_ENCRYPTION, false)

    fun setStrictEncryption(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(STRICT_ENCRYPTION, enabled).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
