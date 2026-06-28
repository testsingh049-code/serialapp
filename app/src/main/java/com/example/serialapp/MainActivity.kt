package com.example.serialapp

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.serialapp.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.hardware.usb.UsbDevice

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val devicesList = mutableListOf<String>()
    private val usbDevices = mutableListOf<UsbDevice>()
    private val bleDevicesList = mutableListOf<String>()

    // Will become true once real USB connection succeeds
    private var isConnected = false

    companion object {
        const val EXTRA_MODE = "MODE"
        const val MODE_SERIAL = "SERIAL"
        const val MODE_BLE = "BLE"
    }

    private val baudRates = listOf(
        "9600",
        "38400",
        "115200"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initial UI
        binding.txtStatus.text = "USB Not Connected"
        binding.txtStatus.setTextColor(getColor(android.R.color.holo_red_dark))

        binding.progressBar.visibility = View.GONE

        binding.radioSerial.isChecked = true
        binding.layoutSerial.visibility = View.VISIBLE
        binding.layoutBle.visibility = View.GONE
        binding.btnRefresh.visibility = View.VISIBLE

        // Baud Rates
        binding.spinnerBaud.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            baudRates
        )

        // Load USB devices initially
        refreshDevices()

        // Refresh USB Devices
        binding.btnRefresh.setOnClickListener {
            refreshDevices()
        }

        // Radio Buttons
        binding.radioGroupMode.setOnCheckedChangeListener { _, checkedId ->

            when (checkedId) {

                R.id.radioSerial -> {

                    binding.layoutSerial.visibility = View.VISIBLE
                    binding.layoutBle.visibility = View.GONE
                    binding.btnRefresh.visibility = View.VISIBLE

                    binding.txtStatus.text = "USB Not Connected"
                    binding.txtStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                }

                R.id.radioBle -> {

                    binding.layoutSerial.visibility = View.GONE
                    binding.layoutBle.visibility = View.VISIBLE
                    binding.btnRefresh.visibility = View.GONE

                    binding.txtStatus.text = "Bluetooth Not Connected"
                    binding.txtStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                }
            }
        }

        // Connect
        binding.btnConnect.setOnClickListener {

            if (binding.radioSerial.isChecked) {
                connectSerial()
            } else {
                connectBle()
            }
        }

        // Dummy BLE Scan
        binding.btnScanBle.setOnClickListener {

            bleDevicesList.clear()

            bleDevicesList.add("ESP32 Sensor")
            bleDevicesList.add("Nordic Thingy")
            bleDevicesList.add("Mi Band")
            bleDevicesList.add("BLE Heart Rate")

            binding.spinnerBle.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                bleDevicesList
            )

            Toast.makeText(
                this,
                "BLE Scan Complete",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Testing only
        binding.btnNext.setOnClickListener {

            val intent = Intent(
                this,
                TerminalActivity::class.java
            )

            intent.putExtra(EXTRA_MODE, MODE_SERIAL)

            startActivity(intent)
        }
    }

    /**
     * Refresh USB Devices
     */
    private fun refreshDevices() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList

        devicesList.clear()
        usbDevices.clear()

        if (devices.isEmpty()) {
            devicesList.add("No Devices Found")
        } else {
            devices.values.forEach { device ->
                usbDevices.add(device)
                val displayName =
                    "${device.productName ?: "Unknown Device"} " +
                            "(VID:${device.vendorId}, PID:${device.productId})"
                devicesList.add(displayName)
            }

            Toast.makeText(
                this,
                "${devices.size} device(s) found",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.spinnerCom.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            devicesList
        )
    }

    /**
     * USB Serial Connection
     */
    private fun connectSerial() {
        val selectedIndex = binding.spinnerCom.selectedItemPosition

        if (selectedIndex == -1 || usbDevices.isEmpty()) {
            Toast.makeText(
                this,
                "No USB device selected.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val selectedDevice = usbDevices[selectedIndex]
        val baud = binding.spinnerBaud.selectedItem?.toString() ?: ""

        if (baud.isEmpty()) {
            Toast.makeText(
                this,
                "Please select a Baud Rate.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Toast.makeText(
            this,
            "Selected: ${selectedDevice.productName}",
            Toast.LENGTH_SHORT
        ).show()

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0

        binding.txtStatus.text = "Connecting to USB..."

        lifecycleScope.launch {

            for (i in 1..100) {

                delay(20)
                binding.progressBar.progress = i

            }

            binding.progressBar.visibility = View.GONE

            // Replace this later with actual USB connection result
            isConnected = false

            if (isConnected) {

                binding.txtStatus.text = "USB Connected"
                binding.txtStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                val intent = Intent(
                    this@MainActivity,
                    TerminalActivity::class.java
                )

                intent.putExtra(EXTRA_MODE, MODE_SERIAL)

                startActivity(intent)

            } else {

                binding.txtStatus.text = "USB Connection Failed"
                binding.txtStatus.setTextColor(getColor(android.R.color.holo_red_dark))

                Toast.makeText(
                    this@MainActivity,
                    "Unable to connect to device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * BLE Connection
     */
    private fun connectBle() {

        if (bleDevicesList.isEmpty()) {

            Toast.makeText(
                this,
                "Please scan for BLE devices first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val device = binding.spinnerBle.selectedItem?.toString() ?: ""

        if (device.isEmpty()) {

            Toast.makeText(
                this,
                "Please select a BLE device.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0

        binding.txtStatus.text = "Connecting to Bluetooth..."

        lifecycleScope.launch {

            for (i in 1..100) {

                delay(20)
                binding.progressBar.progress = i

            }

            binding.progressBar.visibility = View.GONE

            binding.txtStatus.text = "Bluetooth Connected"
            binding.txtStatus.setTextColor(getColor(android.R.color.holo_green_dark))

            val intent = Intent(
                this@MainActivity,
                TerminalActivity::class.java
            )

            intent.putExtra(EXTRA_MODE, MODE_BLE)

            startActivity(intent)
        }
    }
}