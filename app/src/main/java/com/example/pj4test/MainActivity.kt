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
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                GlobalSocket.myGlobalSocket = bluetoothSocket
                Log.d(TAG, "socket obtained")
                GlobalSocket.myGlobalSocket.connect()
                Log.d(TAG, "socket connected")
                runOnUiThread {
                    setContentView(R.layout.activity_main)
                }
            }
        } catch (e: Exception) {
            // Handle connection error
            Log.d(TAG, "some error")
            setContentView(R.layout.bluetooth_activity)
            Timer().schedule(5000){
                setContentView(R.layout.activity_main)
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