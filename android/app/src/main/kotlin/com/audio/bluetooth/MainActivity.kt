package com.audio.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    private val channel = "bluetooth_channel"
    private lateinit var bluetoothHandler: BluetoothHandler

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Initialize handler
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        bluetoothHandler = BluetoothHandler(this, adapter)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            channel
        ).setMethodCallHandler { call, result ->

            when (call.method) {

                "checkBluetoothStatus" -> {
                    result.success(bluetoothHandler.checkStatus())
                }

                "scanForDevices" -> {
                    bluetoothHandler.scanForDevices { devices ->
                        result.success(devices)
                    }
                }

                "connectToDevice" -> {
                    val address = call.argument<String>("address")
                    if (address == null) {
                        result.error("INVALID_ARGUMENT", "Address cannot be null", null)
                        return@setMethodCallHandler
                    }
                    bluetoothHandler.connectToDevice(address) { connected ->
                        runOnUiThread {
                            if (connected) {
                                result.success(true)
                            } else {
                                result.error("CONNECT_FAILED", "Unable to connect", null)
                            }
                        }
                    }
                }

                else -> result.notImplemented()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        bluetoothHandler.onRequestPermissionsResult(requestCode, grantResults)
    }
}
