package com.lotuspecas.usbdeviceslistener

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class UsbDeviceHelper(private val context: Context) {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val _usbDevices = MutableLiveData<List<UsbDeviceInfo>>()
    val usbDevices: LiveData<List<UsbDeviceInfo>> get() = _usbDevices

    // Otomatik izin için Vendor ID ve Product ID listesi
    private val allowedDevices = listOf(
        Pair(1921, 21905), // Önceki tarayıcı
        Pair(8746, 1),     // 222a:0001
        Pair(1155, 30016),  // 0483:7540
        Pair(1155, 30016),
        Pair(13421,22136)
    )

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast received: ${intent?.action}")
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    @Suppress("DEPRECATION")
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    Log.d(TAG, "USB_DEVICE_ATTACHED: Device = $device")
                    device?.let { handleDeviceAttached(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(TAG, "USB_DEVICE_DETACHED")
                    updateDeviceList()
                }
                ACTION_USB_PERMISSION -> {
                    @Suppress("DEPRECATION")
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    Log.d(TAG, "USB_PERMISSION: Device = $device, Granted = $permissionGranted")
                    if (permissionGranted) {
                        Log.d(TAG, "Permission granted for device: ${device?.deviceName}, VendorID: ${device?.vendorId}")
                        device?.let { updateDeviceList() }
                    } else {
                        Log.w(TAG, "Permission denied for device: ${device?.deviceName}")
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.lotuspecas.usbdeviceslistener.USB_PERMISSION"
        private const val TAG = "UsbDeviceHelper"
    }

    init {
        Log.d(TAG, "Initializing UsbDeviceHelper")
        // BroadcastReceiver'ı kaydet
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                usbReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
            Log.d(TAG, "Receiver registered with RECEIVER_NOT_EXPORTED")
        } else {
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            Log.d(TAG, "Receiver registered with RECEIVER_EXPORTED")
        }
        // Mevcut cihazlar için izin iste
        requestPermissionForExistingDevices()
        // İlk cihaz listesini yükle
        updateDeviceList()
    }

    private fun requestPermissionForExistingDevices() {
        val deviceList = usbManager.deviceList
        Log.d(TAG, "Requesting permission for ${deviceList.size} existing devices")
        deviceList.values.forEach { device ->
            val isAllowed = allowedDevices.any { it.first == device.vendorId && it.second == device.productId }
            if (isAllowed && !usbManager.hasPermission(device)) {
                Log.d(TAG, "Requesting permission for existing device: ${device.deviceName}, VendorID: ${device.vendorId}, ProductID: ${device.productId}")
                requestPermission(device)
            } else if (usbManager.hasPermission(device)) {
                Log.d(TAG, "Existing device already has permission: ${device.deviceName}")
            }
        }
    }

    private fun handleDeviceAttached(device: UsbDevice) {
        Log.d(TAG, "Device attached: ${device.deviceName}, VendorID: ${device.vendorId}, ProductID: ${device.productId}")
        Log.d(TAG, "Has permission: ${usbManager.hasPermission(device)}")
        // Vendor ID ve Product ID kontrolü
        val isAllowed = allowedDevices.any { it.first == device.vendorId && it.second == device.productId }
        Log.d(TAG, "Is allowed device: $isAllowed")
        if (isAllowed) {
            if (usbManager.hasPermission(device)) {
                Log.d(TAG, "Device already has permission: ${device.deviceName}")
                updateDeviceList()
            } else {
                Log.d(TAG, "Requesting permission for device: ${device.deviceName}")
                requestPermission(device)
            }
        }
    }

    private fun requestPermission(device: UsbDevice) {
        Log.d(TAG, "Creating PendingIntent for device: ${device.deviceName}")
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName) // Explicit Intent
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        usbManager.requestPermission(device, pendingIntent)
        Log.d(TAG, "Requested permission for device: ${device.deviceName}")
    }

    private fun updateDeviceList() {
        val deviceList = usbManager.deviceList
        Log.d(TAG, "Raw device list size: ${deviceList.size}")
        val usbDeviceInfos = deviceList.values
            .filter {
                Log.d(TAG, "Checking permission for device: ${it.deviceName}, VendorID: ${it.vendorId}, ProductID: ${it.productId}, HasPermission: ${usbManager.hasPermission(it)}")
                true // Tüm cihazları listele, izin durumu hasPermission ile gösterilecek
            }
            .map { device ->
                UsbDeviceInfo(
                    deviceName = device.deviceName,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    deviceId = device.deviceId,
                    hasPermission = usbManager.hasPermission(device) // İzin durumu
                )
            }
        Log.d(TAG, "Filtered device list: ${usbDeviceInfos.size} devices")
        _usbDevices.postValue(usbDeviceInfos)
    }

    fun cleanup() {
        context.unregisterReceiver(usbReceiver)
        Log.d(TAG, "UsbDeviceHelper cleaned up")
    }
}