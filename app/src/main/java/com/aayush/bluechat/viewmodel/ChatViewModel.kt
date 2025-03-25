package com.aayush.bluechat.viewmodel

import android.app.Application
import android.bluetooth.*
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.util.*
import kotlin.collections.ArrayList

data class ChatMessage(
    val content: String,
    val timestamp: String,
    val isOutgoing: Boolean
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val CHARACTERISTIC_UUID = UUID.fromString("00001102-0000-1000-8000-00805F9B34FB")
    
    val messages = mutableStateListOf<ChatMessage>()
    var deviceName by mutableStateOf<String?>(null)
        private set
    
    var isConnected by mutableStateOf(false)
        private set
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false
                bluetoothGatt?.close()
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                }
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val message = String(value)
            val timestamp = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            messages.add(ChatMessage(message, timestamp, false))
        }
    }
    
    fun connectToDevice(address: String) {
        val device = bluetoothAdapter.getRemoteDevice(address)
        deviceName = device.name
        bluetoothGatt = device.connectGatt(getApplication(), false, gattCallback)
    }
    
    fun sendMessage(message: String) {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
        
        if (characteristic != null) {
            characteristic.value = message.toByteArray()
            bluetoothGatt?.writeCharacteristic(characteristic)
            
            val timestamp = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            messages.add(ChatMessage(message, timestamp, true))
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothGatt?.close()
    }
} 