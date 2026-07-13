package dev.anishfyi.blokd.dns

import android.content.Context

object DnsPreferences {
    private const val PREFERENCES_NAME = "blokd_dns"
    private const val ADGUARD_ENABLED = "adguard_enabled"

    fun isAdGuardEnabled(context: Context): Boolean {
        return preferences(context).getBoolean(ADGUARD_ENABLED, false)
    }

    fun setAdGuardEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(ADGUARD_ENABLED, enabled).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
