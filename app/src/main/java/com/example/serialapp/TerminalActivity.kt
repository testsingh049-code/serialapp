package com.example.serialapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.serialapp.databinding.ActivityTerminalBinding

class TerminalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtConnectionStatus.text = "Connected"

        binding.txtTx.text = "TX :"
        binding.txtRx.text = "RX :"

        binding.btnSend.setOnClickListener {

            val command = binding.etCommand.text.toString().trim()

            if (command.isEmpty()) {
                Toast.makeText(this, "Please enter a command", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simulate transmission
            binding.txtTx.text = "TX : $command"

            // Simulate reply
            binding.txtRx.text = "RX : Device replied: OK"

            binding.etCommand.text?.clear()
        }

        binding.btnClear.setOnClickListener {

            binding.txtTx.text = "TX :"
            binding.txtRx.text = "RX :"
            binding.etCommand.text?.clear()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}