package com.lotuspecas.usbdeviceslistener

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lotuspecas.usbdeviceslistener.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var usbDeviceHelper: UsbDeviceHelper
    private lateinit var usbDeviceAdapter: UsbDeviceAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        initViews()
    }


    private fun initViews() {
        // USB Helper'ı başlat
        usbDeviceHelper = UsbDeviceHelper(this@MainActivity)

        // RecyclerView ayarları
        usbDeviceAdapter = UsbDeviceAdapter()
        binding.rvUSbDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = usbDeviceAdapter
        }

        // USB cihazlarını gözlemle
        usbDeviceHelper.usbDevices.observe(this) { devices ->
            Log.d("MainActivity", "Received ${devices.size} devices")
            lifecycleScope.launch(Dispatchers.Default) {
                val deviceList = devices.toList()
                withContext(Dispatchers.Main) {
                    usbDeviceAdapter.submitList(deviceList)
                }
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        // Temizlik
        usbDeviceHelper.cleanup()
    }







}