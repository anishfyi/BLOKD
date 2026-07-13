package dev.anishfyi.blokd.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anishfyi.blokd.vpn.VpnController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlokdApp(onToggle: (Boolean) -> Unit) {
    val running by VpnController.running.collectAsStateWithLifecycle()
    val stats by VpnController.stats.collectAsStateWithLifecycle()

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("BLOKD") }) }) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = if (running) "Blocking on" else "Blocking off",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(checked = running, onCheckedChange = onToggle)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Blocked: ${stats.blocked}")
                        Text("Allowed: ${stats.allowed}")
                    }
                }
                Text(
                    text = "No root. Blocks ad and tracker domains across every app.",
                    fontSize = 13.sp,
                )
            }
        }
    }
}
