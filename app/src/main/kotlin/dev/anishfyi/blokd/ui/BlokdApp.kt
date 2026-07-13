package dev.anishfyi.blokd.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anishfyi.blokd.dns.DnsPreferences
import dev.anishfyi.blokd.vpn.VpnController
import java.text.NumberFormat

@Composable
fun BlokdApp(
    onToggle: (Boolean) -> Unit,
    onDnsSettingsChanged: () -> Unit,
) {
    val context = LocalContext.current
    val running by VpnController.running.collectAsStateWithLifecycle()
    val stats by VpnController.stats.collectAsStateWithLifecycle()
    var adGuardEnabled by remember(context) {
        mutableStateOf(DnsPreferences.isAdGuardEnabled(context))
    }

    BlokdTheme {
        BlokdHome(
            running = running,
            blocked = stats.blocked,
            allowed = stats.allowed,
            adGuardEnabled = adGuardEnabled,
            onToggle = onToggle,
            onAdGuardChange = { enabled ->
                DnsPreferences.setAdGuardEnabled(context, enabled)
                adGuardEnabled = enabled
                onDnsSettingsChanged()
            },
        )
    }
}

@Composable
private fun BlokdHome(
    running: Boolean,
    blocked: Long,
    allowed: Long,
    adGuardEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onAdGuardChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            AppHeader(running = running)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = when {
                        running && adGuardEnabled -> "DUAL PROTECTION ACTIVE"
                        running -> "PROTECTION ACTIVE"
                        adGuardEnabled -> "TWO LAYERS, ONE TAP"
                        else -> "READY WHEN YOU ARE"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(20.dp))
                ProtectionButton(enabled = running, onToggle = onToggle)
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = if (running) "You’re protected" else "Protection is off",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        running && adGuardEnabled ->
                            "BLOKD and encrypted AdGuard DNS are protecting every app."
                        running ->
                            "Ads and trackers are being blocked across every app."
                        adGuardEnabled ->
                            "Tap once to start both protection layers."
                        else ->
                            "Tap the button to protect this device."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            StatsPanel(
                running = running,
                blocked = blocked,
                allowed = allowed,
                adGuardEnabled = adGuardEnabled,
                onAdGuardChange = onAdGuardChange,
            )
            Text(
                text = "NO ROOT  ·  NO ACCOUNT  ·  OPEN SOURCE",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

@Composable
private fun AppHeader(running: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            color = MaterialTheme.colorScheme.onBackground,
            shape = RoundedCornerShape(11.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "B",
                    color = MaterialTheme.colorScheme.background,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "BLOKD",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = "PRIVATE DNS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }
        StatusPill(running = running)
    }
}

@Composable
private fun StatusPill(running: Boolean) {
    val background by animateColorAsState(
        targetValue = if (running) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "status background",
    )
    val foreground by animateColorAsState(
        targetValue = if (running) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "status foreground",
    )

    Surface(color = background, shape = RoundedCornerShape(100.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(foreground, CircleShape),
            )
            Text(
                text = if (running) "ON" else "OFF",
                color = foreground,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun ProtectionButton(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val buttonColor by animateColorAsState(
        targetValue = if (enabled) accent else MaterialTheme.colorScheme.surface,
        label = "protection button",
    )
    val iconColor by animateColorAsState(
        targetValue = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "power icon",
    )
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.96f,
        label = "protection button scale",
    )

    Box(
        modifier = Modifier
            .size(184.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "BLOKD protection"
                stateDescription = if (enabled) "On" else "Off"
            }
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = accent.copy(alpha = if (enabled) 0.12f else 0.05f),
                radius = size.minDimension * 0.5f,
            )
            drawCircle(
                color = accent.copy(alpha = if (enabled) 0.34f else 0.14f),
                radius = size.minDimension * 0.43f,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
                color = accent,
                radius = 3.5.dp.toPx(),
                center = Offset(size.width * 0.86f, size.height * 0.25f),
            )
        }
        Surface(
            modifier = Modifier
                .size(132.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            color = buttonColor,
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) accent else MaterialTheme.colorScheme.outline,
            ),
            shadowElevation = if (enabled) 18.dp else 5.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(49.dp)) {
                    val strokeWidth = 5.dp.toPx()
                    drawArc(
                        color = iconColor,
                        startAngle = -45f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(
                            width = size.width - strokeWidth,
                            height = size.height - strokeWidth,
                        ),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height * 0.46f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsPanel(
    running: Boolean,
    blocked: Long,
    allowed: Long,
    adGuardEnabled: Boolean,
    onAdGuardChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Stat(
                    label = "BLOCKED",
                    value = blocked,
                    valueColor = MaterialTheme.colorScheme.primary,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(MaterialTheme.colorScheme.outline),
                )
                Stat(
                    label = "ALLOWED",
                    value = allowed,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "DNS",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = "AdGuard DNS",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when {
                            adGuardEnabled && running -> "Encrypted filtering is active"
                            adGuardEnabled -> "Starts with the main button"
                            else -> "Add encrypted upstream filtering"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Switch(
                    checked = adGuardEnabled,
                    onCheckedChange = onAdGuardChange,
                )
            }
        }
    }
}

@Composable
private fun RowScope.Stat(
    label: String,
    value: Long,
    valueColor: Color,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = NumberFormat.getIntegerInstance().format(value),
            color = valueColor,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F3EC)
@Composable
private fun BlokdHomePreview() {
    BlokdTheme(darkTheme = false) {
        BlokdHome(
            running = true,
            blocked = 1_284,
            allowed = 8_931,
            adGuardEnabled = true,
            onToggle = {},
            onAdGuardChange = {},
        )
    }
}
