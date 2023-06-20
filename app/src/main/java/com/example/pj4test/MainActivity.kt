package com.example.pj4test

import android.Manifest
import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.Timer
import kotlin.concurrent.schedule
import java.util.*

class GlobalSocket {
    companion object {
        lateinit var myGlobalSocket: BluetoothSocket
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    // permissions
    private val permissions = arrayOf(RECORD_AUDIO, CAMERA, BLUETOOTH,  BLUETOOTH_CONNECT, BLUETOOTH_ADMIN)
    private val PERMISSIONS_REQUEST = 0x0000001;

    private val REQUEST_ENABLE_BT = 1

    private var connectionAttempts = 0
    private val maxConnectionAttempts = 3


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()


        val DEVICE_ADDRESS = "68:5A:CF:86:F8:27"
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        lateinit var bluetoothSocket: BluetoothSocket

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "This device does not support Bluetooth", Toast.LENGTH_LONG)
            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS)


        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(permissions, PERMISSIONS_REQUEST)
            }
            device?.let {
                while (connectionAttempts < maxConnectionAttempts && !bluetoothSocket.isConnected) {
                    runOnUiThread {
                        setContentView(R.layout.loading_page)
                    }
                    try {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                        Log.d(TAG, "Socket obtained")
                        bluetoothSocket.connect()
                        Log.d(TAG, "Socket connected")
                        runOnUiThread {
                            setContentView(R.layout.activity_main)
                        }
                    } catch (e: IOException) {
                        // Handle connection error
                        Log.d(TAG, "Connection error: ${e.message}")
                        connectionAttempts++
                        Thread.sleep(1000) // Wait for a second before retrying
                    }

                    if (!bluetoothSocket.isConnected) {
                        runOnUiThread {
                            setContentView(R.layout.bluetooth_activity)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle connection error
            Log.d(TAG, "some error")
            setContentView(R.layout.loading_page)
            Timer().schedule(3000){
                runOnUiThread {
                    setContentView(R.layout.activity_main)
                }
            }
        }

    }

    private fun checkPermissions() {
        if (permissions.all{ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED}){
            Log.d(TAG, "All Permission Granted")
        }
        else{
            requestPermissions(permissions, PERMISSIONS_REQUEST)
        }
    }

}