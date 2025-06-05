package com.lotuspecas.usbdeviceslistener

data class UsbDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val deviceId: Int,
    val hasPermission: Boolean // Yeni alan
)
