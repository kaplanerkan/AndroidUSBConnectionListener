package com.lotuspecas.usbdeviceslistener

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lotuspecas.usbdeviceslistener.databinding.ItemUsbDeviceBinding

class UsbDeviceAdapter :
    ListAdapter<UsbDeviceInfo, UsbDeviceAdapter.UsbDeviceViewHolder>(UsbDeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsbDeviceViewHolder {
        val binding =
            ItemUsbDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsbDeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsbDeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UsbDeviceViewHolder(private val binding: ItemUsbDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(device: UsbDeviceInfo) {
            Log.d("Adapter", "Binding device: ${device.deviceName}, VendorID: ${device.vendorId}, ProductID: ${device.productId}, HasPermission: ${device.hasPermission}")
            binding.textDeviceName.text = device.deviceName
            binding.textVendorId.text = "Vendor ID: ${device.vendorId}"
            binding.textProductId.text = "Product ID: ${device.productId}"
            binding.textDeviceId.text = "Device ID: ${device.deviceId}"
            binding.textPermissionStatus.text = "Permission: ${if (device.hasPermission) "Granted" else "Denied"}"
        }
    }
}

class UsbDeviceDiffCallback : DiffUtil.ItemCallback<UsbDeviceInfo>() {
    override fun areItemsTheSame(oldItem: UsbDeviceInfo, newItem: UsbDeviceInfo): Boolean {
        return oldItem.deviceId == newItem.deviceId
    }

    override fun areContentsTheSame(oldItem: UsbDeviceInfo, newItem: UsbDeviceInfo): Boolean {
        return oldItem == newItem
    }
}