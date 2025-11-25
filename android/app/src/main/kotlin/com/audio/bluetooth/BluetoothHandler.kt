package com.audio.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import android.provider.Settings
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class BluetoothHandler(
    private val activity: Activity,
    private val adapter: BluetoothAdapter?
) {
    companion object {
        const val PERMISSION_REQUEST_CODE = 881
    }

    private var permissionCallback: (() -> Unit)? = null
    private var permissionDeniedCallback: (() -> Unit)? = null
    private var discoveryReceiver: BroadcastReceiver? = null

    private var bluetoothUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredAndroid12Permissions()
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requiredAndroid12Permissions() : Array<String> {
        return arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissionsThen(onGranted: () -> Unit, onDenied: (() -> Unit)? = null) {
        if (hasPermissions()) {
            onGranted()
            return
        }

        permissionCallback = onGranted
        permissionDeniedCallback = onDenied
        ActivityCompat.requestPermissions(
            activity,
            requiredPermissions(),
            PERMISSION_REQUEST_CODE
        )
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                permissionCallback?.invoke()
            } else {
                permissionDeniedCallback?.invoke()
            }
            permissionCallback = null
            permissionDeniedCallback = null
        }
    }

    @SuppressLint("MissingPermissions")
    fun checkStatus() : Boolean {
        return adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun scanForDevices(onResult: (List<Map<String, String>>) -> Unit) {
        requestPermissionsThen(
            onGranted = {
                if (adapter == null) {
                    android.util.Log.d("BluetoothHandler", "Adapter is null")
                    onResult(emptyList())
                    return@requestPermissionsThen
                }

                val devices = ArrayList<Map<String, String>>()

                if (adapter.isDiscovering) {
                    android.util.Log.d("BluetoothHandler", "Canceling existing discovery")
                    adapter.cancelDiscovery()
                }

                discoveryReceiver?.let {
                    try {
                        activity.unregisterReceiver(it)
                    } catch (_: Exception) {}
                }

                discoveryReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {

                        val action = intent.action
                        android.util.Log.d("BluetoothHandler", "onReceive action: $action")

                        if (BluetoothDevice.ACTION_FOUND == action) {
                            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                            android.util.Log.d("BluetoothHandler", "Device found: ${device?.name} - ${device?.address}")
                            if (device != null) {
                                devices.add(
                                    mapOf(
                                        "name" to (device.name ?: "Unknown"),
                                        "address" to device.address
                                    )
                                )
                            }
                        }

                        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                            android.util.Log.d("BluetoothHandler", "Discovery finished. Found ${devices.size} devices")
                            onResult(devices)
                        }
                    }
                }

                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }

                activity.registerReceiver(discoveryReceiver, filter)

                val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
                                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                android.util.Log.d("BluetoothHandler", "Location Services Enabled: $isLocationEnabled")
                android.util.Log.d("BluetoothHandler", "Bluetooth Adapter Enabled: ${adapter.isEnabled}")

                if (!isLocationEnabled) {
                    AlertDialog.Builder(activity)
                        .setTitle("Location Required")
                        .setMessage("Please enable location services to scan for Bluetooth devices.")
                        .setPositiveButton("Enable") { _, _ ->
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            activity.startActivity(intent)
                            onResult(emptyList())
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            onResult(emptyList())
                        }
                        .show()
                    return@requestPermissionsThen
                }

                val started = adapter.startDiscovery()
                android.util.Log.d("BluetoothHandler", "startDiscovery returned: $started")
                if (!started) {
                    // Failed to start discovery (e.g. Bluetooth off, Location off)
                    onResult(emptyList())
                }
            },
            onDenied = {
                android.util.Log.d("BluetoothHandler", "Permissions denied")
                onResult(emptyList())
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, onComplete: (Boolean) -> Unit) {
        requestPermissionsThen(
            onGranted = {
                adapter ?: return@requestPermissionsThen onComplete(false)

                val device = adapter.getRemoteDevice(address)

                Thread {
                    try {
                        val socket = device.createRfcommSocketToServiceRecord(bluetoothUUID)
                        adapter.cancelDiscovery()
                        socket.connect()
                        onComplete(true)
                    } catch (_: IOException) {
                        onComplete(false)
                    }
                }.start()
            },
            onDenied = {
                onComplete(false)
            }
        )
    }
}