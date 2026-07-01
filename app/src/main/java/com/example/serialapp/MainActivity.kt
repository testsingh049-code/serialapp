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
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val devicesList = mutableListOf<String>()
    private val usbDevices = mutableListOf<UsbDevice>()
    private val bleDevicesList = mutableListOf<String>()

    // Will become true once real USB connection succeeds
    private var isConnected = false

    companion object {
        const val EXTRA_MODE = "MODE"
        const val EXTRA_CONNECTED = "CONNECTED"
        const val MODE_SERIAL = "SERIAL"
        const val MODE_BLE = "BLE"
    }

    private val baudRates = listOf(
        "9600",
        "38400",
        "115200"
    )

    private val ACTION_USB_PERMISSION =
        "com.example.serialapp.USB_PERMISSION"

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

        // Next button to navigate to next screen without connection
        binding.btnNext.setOnClickListener {
            val intent = Intent(
                this,
                TerminalActivity::class.java
            )
            intent.putExtra(EXTRA_MODE, MODE_SERIAL)
            intent.putExtra(EXTRA_CONNECTED, false)
            startActivity(intent)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                usbPermissionReceiver,
                IntentFilter(ACTION_USB_PERMISSION),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                usbPermissionReceiver,
                IntentFilter(ACTION_USB_PERMISSION)
            )
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

//    Broadcast Receiver
    private val usbPermissionReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_USB_PERMISSION) {
            synchronized(this) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED,
                        false
                    )
                ) {
                    if (device != null) {
                        Toast.makeText(
                            this@MainActivity,
                            "USB Permission Granted",
                            Toast.LENGTH_SHORT
                        ).show()
                        openUsbConnection(device)
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "USB Permission Denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
    // open usb connection
    private var serialPort: UsbSerialPort? = null
    private fun openUsbConnection(device: UsbDevice) {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val connection = usbManager.openDevice(device)
        if (connection == null) {
            Toast.makeText(this, "Failed to open USB Device", Toast.LENGTH_SHORT).show()
            return
        }

        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            Toast.makeText(this, "No USB Serial Driver Found", Toast.LENGTH_SHORT).show()
            return
        }

        serialPort = driver.ports[0]
        try {
            serialPort?.open(connection)

            SerialManager.serialPort = serialPort
            SerialManager.connection = connection

            serialPort?.setParameters(
                binding.spinnerBaud.selectedItem.toString().toInt(),
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )

            binding.txtStatus.text = "USB Connected"
            binding.txtStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            Toast.makeText(this, "USB Connected Successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, TerminalActivity::class.java)
            intent.putExtra(EXTRA_MODE, MODE_SERIAL)
            intent.putExtra(EXTRA_CONNECTED, true)
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Connection Failed", Toast.LENGTH_LONG).show()
        }
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
        val usbManager = getSystemService(USB_SERVICE) as UsbManager

        if (!usbManager.hasPermission(selectedDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(selectedDevice, permissionIntent)
            return
        }
 // if permission is already granted
        openUsbConnection(selectedDevice)

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

//        binding.progressBar.visibility = View.VISIBLE
//        binding.progressBar.progress = 0
//
//        binding.txtStatus.text = "Connecting to USB..."
//        lifecycleScope.launch {
//            for (i in 1..100) {
//                delay(20)
//                binding.progressBar.progress = i
//
//            }
//
//            binding.progressBar.visibility = View.GONE
//
//            // Replace this later with actual USB connection result
//            isConnected = false
//
//            if (isConnected) {
//                binding.txtStatus.text = "USB Connected"
//                binding.txtStatus.setTextColor(getColor(android.R.color.holo_green_dark))
//
//                val intent = Intent(
//                    this@MainActivity,
//                    TerminalActivity::class.java
//                )
//
//                intent.putExtra(EXTRA_MODE, MODE_SERIAL)
//                startActivity(intent)
//
//            } else {
//                binding.txtStatus.text = "USB Connection Failed"
//                binding.txtStatus.setTextColor(getColor(android.R.color.holo_red_dark))
//
//                Toast.makeText(
//                    this@MainActivity,
//                    "Unable to connect to device.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbPermissionReceiver)
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
            intent.putExtra(EXTRA_CONNECTED, true)

            startActivity(intent)
        }
    }
}