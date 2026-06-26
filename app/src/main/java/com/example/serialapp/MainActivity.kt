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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var devicesList = mutableListOf<String>()

    // This will become true once real USB connection succeeds
    private var isConnected = false

    private val baudRates = listOf(
        "9600",
        "38400",
        "115200"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtStatus.text = "Not Connected"

        // Initially hide progress bar
        binding.progressBar.visibility = View.GONE

        // Load baud rates
        binding.spinnerBaud.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            baudRates
        )

        // Refresh USB devices
        binding.btnRefresh.setOnClickListener {
            refreshDevices()
        }

        // Connect button
        binding.btnConnect.setOnClickListener {
            connectDevice()
        }

        // Testing button
        binding.btnNext.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    TerminalActivity::class.java
                )
            )
        }
    }

    /**
     * Refresh available USB devices
     */
    private fun refreshDevices() {

        val usbManager = getSystemService(USB_SERVICE) as UsbManager

        val devices = usbManager.deviceList

        devicesList.clear()

        if (devices.isEmpty()) {

            devicesList.add("No Devices Found")

        } else {

            devices.values.forEach {

                devicesList.add(it.deviceName)

            }

        }

        binding.spinnerCom.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            devicesList
        )

        Toast.makeText(
            this,
            "${devicesList.size} device(s) found",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Simulated connection
     */
    private fun connectDevice() {

        val com = binding.spinnerCom.selectedItem?.toString() ?: ""
        val baud = binding.spinnerBaud.selectedItem?.toString() ?: ""

        if (com.isEmpty() || com == "No Devices Found") {

            Toast.makeText(
                this,
                "Please refresh and select a COM Port.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (baud.isEmpty()) {

            Toast.makeText(
                this,
                "Please select a Baud Rate.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0

        binding.txtStatus.text = "Connecting..."

        lifecycleScope.launch {

            for (i in 1..100) {

                delay(20)

                binding.progressBar.progress = i

            }

            binding.progressBar.visibility = View.GONE

            // **************************************
            // Later this will become true when
            // the USB serial port actually connects.
            // **************************************
            isConnected = false

            if (isConnected) {

                binding.txtStatus.text = "Connected Successfully"

                startActivity(
                    Intent(
                        this@MainActivity,
                        TerminalActivity::class.java
                    )
                )

            } else {

                binding.txtStatus.text = "Connection Failed"

                Toast.makeText(
                    this@MainActivity,
                    "Unable to connect to device.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }
}