package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val screenshotProtection by viewModel.screenshotProtection.collectAsState()
    val autolockTimeout by viewModel.autolockTimeout.collectAsState()
    val themeSelection by viewModel.themeSelection.collectAsState()
    val driveConnected by viewModel.googleDriveConnected.collectAsState()
    val driveAccount by viewModel.googleAccountName.collectAsState()
    val logs by viewModel.securityLogs.collectAsState()
    val threshold by viewModel.intruderThreshold.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val backupWifiOnly by viewModel.backupWifiOnly.collectAsState()
    val backupHistoryList by viewModel.backupHistoryList.collectAsState()

    var activeSettingTab by remember { mutableStateOf("General") } // General, Intruder Logs

    // Form inputs
    var showDecoyDialog by remember { mutableStateOf(false) }
    var decoyPinInput by remember { mutableStateOf("") }

    var showGoogleConnectDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Security Hub") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Settings Segment Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                listOf("General", "Intruder Logs").forEach { tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeSettingTab == tab) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeSettingTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSettingTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (activeSettingTab == "General") {
                // GENERAL SETTINGS VIEW
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Encryption & Security", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    item {
                        // Decoy PIN Setup Option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDecoyDialog = true }
                                .testTag("setup_decoy_option")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Decoy Profile / Fake PIN", fontWeight = FontWeight.Bold)
                                    Text("Entering this PIN opens an entirely separate decoy vault profile", fontSize = 12.sp)
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }

                    item {
                        // Screenshot protection switch
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Shutter Lock (FLAG_SECURE)", fontWeight = FontWeight.Bold)
                                        Text("Block screenshot and screen recording", fontSize = 12.sp)
                                    }
                                }
                                Switch(
                                    checked = screenshotProtection,
                                    onCheckedChange = { viewModel.setScreenshotProtection(it) },
                                    modifier = Modifier.testTag("screenshot_switch")
                                )
                            }
                        }
                    }

                    item {
                        // Inactivity Auto Lock Selection
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Auto-Lock Timeout", fontWeight = FontWeight.Bold)
                                }
                                Text("Inactivity seconds before locking vault: ${if (autolockTimeout == 0) "Instant" else "$autolockTimeout sec"}", fontSize = 12.sp)
                                Slider(
                                    value = autolockTimeout.toFloat(),
                                    onValueChange = { viewModel.setAutolockTimeout(it.toInt()) },
                                    valueRange = 10f..300f,
                                    steps = 10
                                )
                            }
                        }
                    }

                    item {
                        // Intruder Selfie Threshold Setup
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Failed Attempts Threshold", fontWeight = FontWeight.Bold)
                                }
                                Text("Capture silent intruder log after: $threshold wrong attempts", fontSize = 12.sp)
                                Slider(
                                    value = threshold.toFloat(),
                                    onValueChange = { viewModel.setIntruderThreshold(it.toInt()) },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                            }
                        }
                    }

                    item {
                        Text("Custom Theme & Visuals", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("LIGHT", "DARK", "AMOLED").forEach { mode ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (themeSelection == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable { viewModel.setThemeSelection(mode) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode,
                                        fontWeight = FontWeight.Bold,
                                        color = if (themeSelection == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("Cloud Sync Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Google Backup Sync", fontWeight = FontWeight.Bold)
                                            Text(if (driveConnected) "Logged into: $driveAccount" else "Offline (Manual backup)", fontSize = 12.sp)
                                        }
                                    }

                                    if (driveConnected) {
                                        Button(onClick = { viewModel.disconnectGoogleDrive() }) {
                                            Text("Log out")
                                        }
                                    } else {
                                        Button(onClick = { showGoogleConnectDialog = true }) {
                                            Text("Connect")
                                        }
                                    }
                                }

                                if (driveConnected) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                    // Wifi preference toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Back up over Wi-Fi only", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text("Restricts background transfers to Wi-Fi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                        }
                                        Switch(
                                            checked = backupWifiOnly,
                                            onCheckedChange = { viewModel.setBackupWifiOnly(it) }
                                        )
                                    }

                                    // Backup Now Action Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Force Sync Backup", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Button(
                                            onClick = { viewModel.syncBackup() },
                                            enabled = !isSyncing,
                                            modifier = Modifier.testTag("backup_now_button")
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Syncing...", fontSize = 12.sp)
                                            } else {
                                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back up Now", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Backup logs & files overview
                                    Text("Encrypted Backup Vault Log History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        backupHistoryList.forEach { historyLog ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = historyLog, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        // Simulated Restore Action
                                                    },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) {
                                                    Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // INTRUDER LOGS VIEW LIST
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(48.dp))
                            Text("No intruders caught yet. Perfect security!")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Caught Intruder Snapshots", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.clearSecurityLogs() }) {
                            Text("Shred Logs", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(logs) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simulated Photo preview circle
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.Red)
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = "Attempted: \"${log.attemptedPin}\"",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Decoy Setup PIN popup dialog
    if (showDecoyDialog) {
        AlertDialog(
            onDismissRequest = { showDecoyDialog = false },
            title = { Text("Configure Decoy Profile PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Type a 4+ digit Decoy PIN. Unlocking with this PIN loads fake data profile to protect your primary contents from coercion:")
                    OutlinedTextField(
                        value = decoyPinInput,
                        onValueChange = { decoyPinInput = it },
                        label = { Text("Decoy PIN") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decoy_pin_field")
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (decoyPinInput.length >= 4) {
                        viewModel.setDecoyPin(decoyPinInput)
                        showDecoyDialog = false
                    }
                }) {
                    Text("Register Decoy PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecoyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Google Connect popup dialog
    if (showGoogleConnectDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleConnectDialog = false },
            title = { Text("Link Google Account for Drive Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Secure OAuth2 Simulation. Google Drive files will be fully encrypted locally before upload to protect your absolute privacy:")
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Email Account") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (emailInput.contains("@")) {
                        viewModel.setGoogleAccount(emailInput)
                        showGoogleConnectDialog = false
                    }
                }) {
                    Text("Secure Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleConnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
