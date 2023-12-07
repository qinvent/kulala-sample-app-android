package com.qi.kulala.sample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.qi.kulala.sample.databinding.ActivityMainBinding
import com.qi.kulala.sample.permission.PermissionsHelper
import com.qi.kulala.sdk.Kulala
import com.qi.kulala.sdk.enumerator.Command
import com.qi.kulala.sdk.enumerator.KulalaState
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val TAG = "Kulala"

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermissions() {
        Log.d("isRequesting=", "${PermissionsHelper.isRequesting}")
        if (PermissionsHelper.isRequesting) return
        PermissionsHelper.requestPermissions(
            this, listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        ) { result ->
            // UX is best not to automatically trigger the block if granting of perm is from Settings
            Log.d("isForwardedToSettings=", "${result.isForwardedToSettings}")
            if (!result.allGranted && result.isForwardedToSettings) {
                lifecycleScope.launch {
                    delay(200)
                    requestPermissions()
                }
            }
        }
    }


    // Create a BroadcastReceiver for ACTION_FOUND
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> Log.d(TAG, "onReceive: STATE OFF")
                    BluetoothAdapter.STATE_TURNING_OFF -> Log.d(
                        TAG, "mBroadcastReceiver: STATE TURNING OFF"
                    )
                    BluetoothAdapter.STATE_ON -> Log.d(TAG, "mBroadcastReceiver: STATE ON")
                    BluetoothAdapter.STATE_TURNING_ON -> Log.d(
                        TAG, "mBroadcastReceiver: STATE TURNING ON"
                    )
                }
            }
        }
    }

    private fun showToast(throwable: Throwable) {
        Toast.makeText(this, throwable.message ?: throwable.toString(), Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    }

    private fun setVehicleState(state: KulalaState) {
        val currentState = getString(
            R.string.vehicleState, when (state) {
                KulalaState.CONNECTED -> getString(R.string.connected)
                KulalaState.UNLOCKED -> getString(R.string.doorsUnlocked)
                KulalaState.LOCKED -> getString(R.string.doorsLocked)
                KulalaState.ENGINE_STARTED -> getString(R.string.engineStarted)
                KulalaState.ENGINE_STOPPED -> getString(R.string.engineStopped)
                KulalaState.DISCONNECTED -> getString(R.string.disconnected)
                else -> "Unknown"
            }
        )
        binding?.vehicleState?.text = currentState
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions()
        }

        Kulala.instance.init(this)
        setVehicleState(KulalaState.UNKNOWN)
    }

    fun enableBluetooth(view: View) {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.")
            showToast("Does not have BT capabilities")
        }
        if (bluetoothAdapter?.isEnabled == false && ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "enableDisableBT: enabling BT.")
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(btIntent)
            val intent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(broadcastReceiver, intent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions()
            }
        }

        if (bluetoothAdapter?.isEnabled == true) {
            val intent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(broadcastReceiver, intent)
            showToast(getString(R.string.bluetooth_enabled))
        }
    }

    fun connectToVehicle(view: View) {
        if (bluetoothAdapter?.isEnabled == false) {
            showToast(getString(R.string.enable_bluetooth))
            return
        }

        if (!isLocationEnabled()) {
            showToast(getString(R.string.turn_on_location_service))
            return
        }

        // Kulala device address "A4:34:F1:11:F8:8B"
        // KC1 device address "00:07:80:C5:BB:47"
        val macAddress = "A4:34:F1:11:F8:8B"
        binding?.btnConnect?.text = getString(R.string.connectingVehicle)
        Kulala.instance.connectDevice(macAddress, object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.vehicle_connected))
                setVehicleState(result)
                binding?.btnConnect?.text = getString(R.string.connectToVehicle)
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error)
                binding?.btnConnect?.text = getString(R.string.connectToVehicle)
            }
        })
    }

    fun lockDoors(view: View) {
        binding?.btnLockDoors?.text = getString(R.string.lockingDoors)
        Kulala.instance.sendCommand(Command.LOCK, object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.doors_locked))
                setVehicleState(result)
                binding?.btnLockDoors?.text = getString(R.string.lockDoors)
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error)
                binding?.btnLockDoors?.text = getString(R.string.lockDoors)
            }
        })
    }

    fun unlockDoors(view: View) {
        binding?.btnUnlockDoors?.text = getString(R.string.unlockingDoors)
        Kulala.instance.sendCommand(Command.UNLOCK, object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.doors_unlocked))
                setVehicleState(result)
                binding?.btnUnlockDoors?.text = getString(R.string.unlockDoors)
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error)
                binding?.btnUnlockDoors?.text = getString(R.string.unlockDoors)
            }
        })
    }

    fun startEngine(view: View) {
        binding?.btnStartEngine?.text = getString(R.string.startingEngine)
        Kulala.instance.sendCommand(Command.START_ENGINE, object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.engine_started))
                setVehicleState(result)
                binding?.btnStartEngine?.text = getString(R.string.startEngine)
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error)
                binding?.btnStartEngine?.text = getString(R.string.startEngine)
            }
        })
    }


    fun stopEngine(view: View) {
        binding?.btnStopEngine?.text = getString(R.string.stoppingEngine)
        Kulala.instance.sendCommand(Command.STOP_ENGINE, object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.engine_stopped))
                setVehicleState(result)
                binding?.btnStopEngine?.text = getString(R.string.stopEngine)
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error)
                binding?.btnStopEngine?.text = getString(R.string.stopEngine)
            }
        })
    }

    fun disconnectFromVehicle(view: View) {
        binding?.btnDisconnect?.text = getString(R.string.disconnectingVehicle)
        Kulala.instance.disconnectDevice(object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.vehicle_disconnected))
                setVehicleState(result)
                binding?.btnDisconnect?.text = getString(R.string.disconnectFromVehicle)
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error)
                binding?.btnDisconnect?.text = getString(R.string.disconnectFromVehicle)
            }
        })
    }
}