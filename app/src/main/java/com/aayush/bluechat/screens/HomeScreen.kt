package com.aayush.bluechat.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.bluechat.viewmodel.BluetoothViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToSettings: () -> Unit,
    navigateToChat: (String) -> Unit,
    darkTheme: Boolean,
    onThemeUpdated: (Boolean) -> Unit,
    viewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager.adapter }
    
    var showDrawer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.updatePairedDevices()
            viewModel.startScan()
        }
    }
    
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == -1) {
            viewModel.updatePairedDevices()
            viewModel.startScan()
        }
    }
    
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    LaunchedEffect(showDrawer) {
        if (showDrawer) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFBBDEFB), // Light Blue 100
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            showDrawer = false
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF2196F3),
                        unselectedContainerColor = Color.White.copy(alpha = 0.5f),
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color(0xFF2196F3),
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color(0xFF2196F3)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            showDrawer = false
                            navigateToSettings()
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF2196F3),
                        unselectedContainerColor = Color.White.copy(alpha = 0.5f),
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color(0xFF2196F3),
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color(0xFF2196F3)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bluechat", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { showDrawer = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2196F3), // Material Blue 500
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bluetooth Toggle Button
                    ElevatedButton(
                        onClick = {
                            if (bluetoothAdapter.isEnabled) {
                                bluetoothAdapter.disable()
                            } else {
                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                enableBluetoothLauncher.launch(enableBtIntent)
                            }
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Icon(
                            if (bluetoothAdapter.isEnabled) Icons.Default.BluetoothDisabled else Icons.Default.Bluetooth,
                            contentDescription = "Toggle Bluetooth"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (bluetoothAdapter.isEnabled) "Off" else "On",
                            maxLines = 1
                        )
                    }
                    
                    // Scan Button
                    ElevatedButton(
                        onClick = { viewModel.startScan() },
                        enabled = bluetoothAdapter.isEnabled,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Scan")
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Scan",
                            maxLines = 1
                        )
                    }
                    
                    // Make Visible Button
                    ElevatedButton(
                        onClick = { viewModel.makeDeviceVisible() },
                        enabled = bluetoothAdapter.isEnabled,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Make Visible")
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Visible",
                            maxLines = 1
                        )
                    }
                }

                // Discovered Devices Section
                Text(
                    "Discovered Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (darkTheme) Color.White else Color.Black
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(viewModel.discoveredDevices) { device ->
                        DeviceItem(
                            name = viewModel.getDeviceName(device),
                            address = device.address,
                            onClick = { navigateToChat(device.address) },
                            darkTheme = darkTheme
                        )
                    }
                }

                // Connected Devices Section
                Text(
                    "Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (darkTheme) Color.White else Color.Black
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(viewModel.connectedDevices) { device ->
                        DeviceItem(
                            name = viewModel.getDeviceName(device),
                            address = device.address,
                            onClick = { navigateToChat(device.address) },
                            darkTheme = darkTheme
                        )
                    }
                }
                
                // Paired Devices Section
                Text(
                    "Paired Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (darkTheme) Color.White else Color.Black
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(viewModel.pairedDevices) { device ->
                        DeviceItem(
                            name = viewModel.getDeviceName(device),
                            address = device.address,
                            onClick = { navigateToChat(device.address) },
                            darkTheme = darkTheme
                        )
                    }
                }
                
                ElevatedButton(
                    onClick = { navigateToChat("server") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Start Server")
                    Spacer(Modifier.width(8.dp))
                    Text("Start Chat Server")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceItem(
    name: String,
    address: String,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) MaterialTheme.colorScheme.surfaceVariant else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (darkTheme) Color.White else Color.Black
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
            )
        }
    }
} 