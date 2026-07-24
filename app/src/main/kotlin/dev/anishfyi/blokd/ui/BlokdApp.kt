package dev.anishfyi.blokd.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anishfyi.blokd.dns.DnsPreferences
import dev.anishfyi.blokd.stats.HealthStatus
import dev.anishfyi.blokd.stats.ProtectionMode
import dev.anishfyi.blokd.stats.ProtectionState
import dev.anishfyi.blokd.vpn.VpnController
import java.text.NumberFormat

@Composable
fun BlokdApp(
    onToggle: (Boolean) -> Unit,
    onSettingsChanged: () -> Unit,
    onUpdateLists: () -> Unit,
    initialAllowlist: List<String> = emptyList(),
    onAddAllow: (String) -> Unit = {},
    onRemoveAllow: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val protection by VpnController.state.collectAsStateWithLifecycle()
    var adGuardEnabled by remember(context) {
        mutableStateOf(DnsPreferences.isAdGuardEnabled(context))
    }
    var mode by remember(context) {
        mutableStateOf(DnsPreferences.protectionMode(context))
    }
    var allowlist by remember { mutableStateOf(initialAllowlist) }

    BlokdTheme {
        BlokdHome(
            protection = protection,
            mode = mode,
            adGuardEnabled = adGuardEnabled,
            onToggle = onToggle,
            onModeChange = { selected ->
                DnsPreferences.setProtectionMode(context, selected)
                mode = selected
                onSettingsChanged()
            },
            onAdGuardChange = { enabled ->
                DnsPreferences.setAdGuardEnabled(context, enabled)
                adGuardEnabled = enabled
                onSettingsChanged()
            },
            onUpdateLists = onUpdateLists,
            allowlist = allowlist,
            onAddAllow = { raw ->
                val d = normalizeDomain(raw)
                if (d != null && d !in allowlist) {
                    onAddAllow(d)
                    allowlist = (allowlist + d).sorted()
                }
            },
            onRemoveAllow = { d ->
                onRemoveAllow(d)
                allowlist = allowlist - d
            },
        )
    }
}

private val DOMAIN_REGEX = Regex("^[a-z0-9_-]+(\\.[a-z0-9_-]+)+$")

/** Trim, lowercase, and validate a user-entered domain, or null if it is not one. */
private fun normalizeDomain(raw: String): String? {
    val d = raw.trim().lowercase().trimEnd('.')
    return if (DOMAIN_REGEX.matches(d)) d else null
}

@Composable
private fun BlokdHome(
    protection: ProtectionState,
    mode: ProtectionMode,
    adGuardEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onModeChange: (ProtectionMode) -> Unit,
    onAdGuardChange: (Boolean) -> Unit,
    onUpdateLists: () -> Unit,
    allowlist: List<String>,
    onAddAllow: (String) -> Unit,
    onRemoveAllow: (String) -> Unit,
) {
    val running = protection.status == HealthStatus.HEALTHY ||
        protection.status == HealthStatus.DEGRADED ||
        protection.status == HealthStatus.STARTING

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            AppHeader()
            Spacer(modifier = Modifier.height(12.dp))
            HealthBanner(protection = protection)
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProtectionButton(enabled = running, onToggle = onToggle)
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = statusHeadline(protection.status, running),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusSubtitle(protection, running, adGuardEnabled),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Mode",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeCard(
                    title = "Standard",
                    subtitle = "DNS filtering",
                    selected = mode == ProtectionMode.STANDARD,
                    onClick = { onModeChange(ProtectionMode.STANDARD) },
                    modifier = Modifier.weight(1f),
                )
                ModeCard(
                    title = "Berserk",
                    subtitle = "Maximum lists",
                    selected = mode == ProtectionMode.BERSERK,
                    onClick = { onModeChange(ProtectionMode.BERSERK) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            LayersPanel(
                domainCount = protection.blocklist.domainCount,
                adGuardEnabled = adGuardEnabled,
                running = running,
                upstream = protection.upstreamLabel,
                onAdGuardChange = onAdGuardChange,
                onUpdateLists = onUpdateLists,
            )

            Spacer(modifier = Modifier.height(16.dp))
            AllowlistCard(
                entries = allowlist,
                onAdd = onAddAllow,
                onRemove = onRemoveAllow,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SessionStatsPanel(protection = protection)

            Spacer(modifier = Modifier.height(16.dp))
            LimitsCard()

            Text(
                text = "No root · Open source · MIT",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun statusHeadline(status: HealthStatus, running: Boolean): String = when {
    status == HealthStatus.ERROR -> "Protection interrupted"
    status == HealthStatus.NO_NETWORK -> "Waiting for network"
    status == HealthStatus.DEGRADED -> "Protection is degraded"
    running -> "You are protected"
    else -> "Protection is off"
}

private fun statusSubtitle(
    protection: ProtectionState,
    running: Boolean,
    adGuardEnabled: Boolean,
): String = when {
    running && adGuardEnabled ->
        "Blocking ${formatCount(protection.blocklist.domainCount)} domains with local lists and AdGuard DNS."
    running ->
        "Blocking ${formatCount(protection.blocklist.domainCount)} domains across every app."
    adGuardEnabled ->
        "Tap once to start local lists and encrypted AdGuard DNS."
    else -> "Tap the button to start protection."
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            color = MaterialTheme.colorScheme.onBackground,
            shape = RoundedCornerShape(11.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = buildAnnotatedString {
                        append("B")
                        withStyle(SpanStyle(color = BlokdOrange)) { append(".") }
                    },
                    color = MaterialTheme.colorScheme.background,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append("BLOKD")
                    withStyle(SpanStyle(color = BlokdOrange)) { append(".") }
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Text("Private DNS ad blocker", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HealthBanner(protection: ProtectionState) {
    val (label, bg, fg) = when (protection.status) {
        HealthStatus.HEALTHY -> Triple(
            "Healthy · ${formatCount(protection.blocklist.domainCount)} domains",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
        )
        HealthStatus.DEGRADED -> Triple(
            "Degraded · ${protection.upstreamLabel}",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HealthStatus.NO_NETWORK -> Triple(
            "No network",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HealthStatus.ERROR -> Triple(
            "Error · tap to retry",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
        )
        HealthStatus.STARTING -> Triple(
            "Starting protection…",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HealthStatus.OFF -> Triple(
            "Ready · ${formatCount(protection.blocklist.domainCount)} domains loaded",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(color = bg, shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(fg, CircleShape))
            Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LayersPanel(
    domainCount: Int,
    adGuardEnabled: Boolean,
    running: Boolean,
    upstream: String,
    onAdGuardChange: (Boolean) -> Unit,
    onUpdateLists: () -> Unit,
) {
    PanelCard {
        LayerRow(
            title = "Local blocklists",
            subtitle = "${formatCount(domainCount)} domains · tap to update",
            trailing = { Text(if (running) "On" else "Ready", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) },
            onClick = onUpdateLists,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        LayerRow(
            title = "AdGuard DNS",
            subtitle = if (adGuardEnabled) upstream else "Encrypted upstream filtering",
            trailing = { Switch(checked = adGuardEnabled, onCheckedChange = onAdGuardChange) },
        )
    }
}

@Composable
private fun SessionStatsPanel(protection: ProtectionState) {
    PanelCard {
        Row(modifier = Modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.CenterVertically) {
            Stat("Blocked", protection.stats.blocked, MaterialTheme.colorScheme.primary)
            Divider()
            Stat("Allowed", protection.stats.allowed, MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "CNAME ${protection.stats.blockedCname} · Cached ${protection.stats.cacheHits} · Failed ${protection.stats.failed}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun LimitsCard() {
    PanelCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("What BLOKD cannot block", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("• In-stream video ads stitched into the content server", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("• Apps using hardcoded IPs or certificate-pinned DoH", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("• Android Private DNS set to strict without full tunnel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AllowlistCard(
    entries: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    PanelCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Allowlist", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "Domains you never want blocked. Applied on top of every list.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("example.com", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        onAdd(input)
                        input = ""
                    },
                    enabled = input.isNotBlank(),
                ) {
                    Text("Add")
                }
            }
            if (entries.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No custom entries yet.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                entries.forEach { domain ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(domain, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(
                            "Remove",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onRemove(domain) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column { content() }
    }
}

@Composable
private fun LayerRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun RowScope.Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(42.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

@Composable
private fun RowScope.Stat(label: String, value: Long, color: Color) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            formatCount(value),
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
    }
}

@Composable
private fun ProtectionButton(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val buttonColor by animateColorAsState(if (enabled) accent else MaterialTheme.colorScheme.surface, label = "btn")
    val iconColor by animateColorAsState(if (enabled) Color.White else MaterialTheme.colorScheme.onSurface, label = "icon")
    val scale by animateFloatAsState(if (enabled) 1f else 0.96f, label = "scale")

    Box(
        modifier = Modifier
            .size(184.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "BLOKD protection"
                stateDescription = if (enabled) "On" else "Off"
            }
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(accent.copy(alpha = if (enabled) 0.12f else 0.05f), size.minDimension * 0.5f)
            drawCircle(accent.copy(alpha = if (enabled) 0.34f else 0.14f), size.minDimension * 0.43f, style = Stroke(1.5.dp.toPx()))
        }
        Surface(
            modifier = Modifier.size(132.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            color = buttonColor,
            shape = CircleShape,
            border = BorderStroke(1.dp, if (enabled) accent else MaterialTheme.colorScheme.outline),
            shadowElevation = if (enabled) 18.dp else 5.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(49.dp)) {
                    val sw = 5.dp.toPx()
                    drawArc(iconColor, -45f, 270f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                    drawLine(iconColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height * 0.46f), sw, cap = StrokeCap.Round)
                }
            }
        }
    }
}

private fun formatCount(value: Long): String = NumberFormat.getIntegerInstance().format(value)
private fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

@Preview(showBackground = true, backgroundColor = 0xFFF6F3EC)
@Composable
private fun BlokdHomePreview() {
    BlokdTheme {
        BlokdHome(
            protection = ProtectionState(),
            mode = ProtectionMode.BERSERK,
            adGuardEnabled = true,
            onToggle = {},
            onModeChange = {},
            onAdGuardChange = {},
            onUpdateLists = {},
            allowlist = listOf("analytics.example.com"),
            onAddAllow = {},
            onRemoveAllow = {},
        )
    }
}
