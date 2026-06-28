package com.example.serialapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.serialapp.databinding.ActivityTerminalBinding
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
class TerminalActivity : AppCompatActivity() {

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get connection mode from MainActivity
        val mode = intent.getStringExtra(MainActivity.EXTRA_MODE) ?: MainActivity.MODE_SERIAL

        // Display connection status
        if (mode == MainActivity.MODE_BLE) {
            binding.txtConnectionStatus.text = "Bluetooth Connected"
            binding.txtConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.txtConnectionStatus.text = "USB Connected"
            binding.txtConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
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

            val response = "Device replied: OK"

            appendColoredText(
                "\nTX : $command\n",
                getColor(android.R.color.holo_blue_dark)
            )
            appendColoredText(
                "RX : $response\n\n",
                getColor(android.R.color.holo_green_dark)
            )

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
    }
}