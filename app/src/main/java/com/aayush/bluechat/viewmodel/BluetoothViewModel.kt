package com.aayush.bluechat.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import java.util.*

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner
    
    private val SERVICE_UUID = ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
    private val CHARACTERISTIC_UUID = UUID.fromString("00001102-0000-1000-8000-00805F9B34FB")
    
    val discoveredDevices = mutableStateListOf<BluetoothDevice>()
    val pairedDevices = mutableStateListOf<BluetoothDevice>()
    val connectedDevices = mutableStateListOf<BluetoothDevice>()
    
    var isScanning by mutableStateOf(false)
        private set
    
    var isServerStarted by mutableStateOf(false)
        private set
    
    private var bluetoothGattServer: BluetoothGattServer? = null
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!discoveredDevices.contains(device)) {
                discoveredDevices.add(device)
            }
        }
    }
    
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (!connectedDevices.contains(device)) {
                    connectedDevices.add(device)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevices.remove(device)
            }
        }
        
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int,
            offset: Int, characteristic: BluetoothGattCharacteristic
        ) {
            bluetoothGattServer?.sendResponse(
                device, requestId,
                BluetoothGatt.GATT_SUCCESS, offset,
                characteristic.value
            )
        }
        
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray
        ) {
            bluetoothGattServer?.sendResponse(
                device, requestId,
                BluetoothGatt.GATT_SUCCESS, offset,
                value
            )
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
            hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getDeviceName(device: BluetoothDevice): String {
        return if (hasRequiredPermissions()) {
            device.name ?: "Unknown Device"
        } else {
            "Unknown Device"
        }
    }
    
    fun updatePairedDevices() {
        if (!hasRequiredPermissions()) return
        
        pairedDevices.clear()
        pairedDevices.addAll(bluetoothAdapter.bondedDevices)
        
        // Update connected devices
        updateConnectedDevices()
    }
    
    private fun updateConnectedDevices() {
        if (!hasRequiredPermissions()) return
        
        connectedDevices.clear()
        val connectedProfiles = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        connectedDevices.addAll(connectedProfiles)
    }
    
    fun startScan() {
        if (isScanning || !hasRequiredPermissions()) return
        
        discoveredDevices.clear()
        
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(SERVICE_UUID)
            .build()
            
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
            
        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        isScanning = true
    }
    
    fun stopScan() {
        if (!isScanning) return
        bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
    }
    
    fun makeDeviceVisible() {
        if (!hasRequiredPermissions()) return
        
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        discoverableIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        getApplication<Application>().startActivity(discoverableIntent)
    }
    
    fun startServer(): Boolean {
        if (!hasRequiredPermissions()) return false
        if (!bluetoothAdapter.isEnabled) return false
        
        try {
            // Stop any existing server
            stopServer()
            
            // Initialize GATT server
            bluetoothGattServer = bluetoothManager.openGattServer(getApplication(), gattServerCallback)
                ?: return false // Return false if server creation fails
            
            val service = BluetoothGattService(
                SERVICE_UUID.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            
            val characteristic = BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            
            service.addCharacteristic(characteristic)
            val serviceAdded = bluetoothGattServer?.addService(service) ?: false
            if (!serviceAdded) {
                stopServer()
                return false
            }
            
            // Start advertising
            try {
                startAdvertising()
            } catch (e: Exception) {
                e.printStackTrace()
                stopServer()
                return false
            }
            
            isServerStarted = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            stopServer()
            return false
        }
    }
    
    private fun startAdvertising() {
        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            ?: throw IllegalStateException("Bluetooth LE Advertiser not available")
            
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
            
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(SERVICE_UUID)
            .build()
            
        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
            }
            
            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                stopServer()
                throw IllegalStateException("Failed to start advertising with error code: $errorCode")
            }
        })
    }
    
    private fun stopServer() {
        try {
            bluetoothGattServer?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bluetoothGattServer = null
            isServerStarted = false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopScan()
        stopServer()
    }
} 