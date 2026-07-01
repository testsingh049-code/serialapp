package com.example.serialapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.serialapp.databinding.ActivityTerminalBinding
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
class TerminalActivity : AppCompatActivity() {
    private var readThread: Thread? = null
    private lateinit var binding: ActivityTerminalBinding

    private fun appendColoredText(text: String, color: Int) {
        val spannable = SpannableString(text)

        spannable.setSpan(
            ForegroundColorSpan(color),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.txtTerminal.append(spannable)
    }

    private fun startReading() {
        val port = SerialManager.serialPort ?: return
        readThread = Thread {
            val buffer = ByteArray(1024)

            while (!Thread.currentThread().isInterrupted) {
                try {
                    val len = port.read(buffer, 100)
                    if (len > 0) {
                        val received = String(buffer, 0, len)
                        runOnUiThread {
                            appendColoredText(
                                "RX : $received\n",
                                getColor(android.R.color.holo_green_dark)
                            )
                        }
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
        readThread?.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get connection mode from MainActivity
        val mode = intent.getStringExtra(MainActivity.EXTRA_MODE) ?: MainActivity.MODE_SERIAL
        val connected = intent.getBooleanExtra(MainActivity.EXTRA_CONNECTED, false)

        // Display connection status
        if (mode == MainActivity.MODE_SERIAL) {
            if (connected) {
                binding.txtConnectionStatus.text = "USB Connected"
                binding.txtConnectionStatus.setTextColor(
                    getColor(android.R.color.holo_green_dark)

                )
                binding.viewConnectionStatus.setBackgroundResource(R.drawable.green_circle)
            } else {
                binding.txtConnectionStatus.text = "USB Disconnected"
                binding.txtConnectionStatus.setTextColor(
                    getColor(android.R.color.holo_red_dark)
                )
                binding.viewConnectionStatus.setBackgroundResource(R.drawable.red_circle)

            }
        } else {
            if (connected) {
                binding.txtConnectionStatus.text = "Bluetooth Connected"
                binding.txtConnectionStatus.setTextColor(
                    getColor(android.R.color.holo_green_dark)
                )
                binding.viewConnectionStatus.setBackgroundResource(R.drawable.green_circle)
            } else {
                binding.txtConnectionStatus.text = "Bluetooth Disconnected"
                binding.txtConnectionStatus.setTextColor(
                    getColor(android.R.color.holo_red_dark)
                )
                binding.viewConnectionStatus.setBackgroundResource(R.drawable.red_circle)
            }
        }

        binding.txtTerminal.text = ""

        // Send Button
        binding.btnSend.setOnClickListener {

            val command = binding.etCommand.text.toString().trim()

            if (command.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter a command",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            // response
            val port = SerialManager.serialPort
            if (port == null) {
                Toast.makeText(this, "No serial connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                port.write(
                    (command + "\r\n").toByteArray(),
                    1000
                )
                appendColoredText(
                    "\nTX : $command\n",
                    getColor(android.R.color.holo_blue_dark)
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.etCommand.text?.clear()
        }

        // Clear Button
        binding.btnClear.setOnClickListener {
            binding.txtTerminal.text = ""
            binding.etCommand.text?.clear()
        }

        // Back Button
        binding.btnBack.setOnClickListener {
            finish()
        }
        startReading()
    }
    override fun onDestroy() {
        readThread?.interrupt()
        super.onDestroy()
    }
}