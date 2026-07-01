package com.example.serialapp

import android.hardware.usb.UsbDeviceConnection
import com.hoho.android.usbserial.driver.UsbSerialPort

object SerialManager {

    var serialPort: UsbSerialPort? = null
    var connection: UsbDeviceConnection? = null

    fun disconnect() {
        try {
            serialPort?.close()
        } catch (_: Exception) {
        }

        connection?.close()

        serialPort = null
        connection = null
    }
}
