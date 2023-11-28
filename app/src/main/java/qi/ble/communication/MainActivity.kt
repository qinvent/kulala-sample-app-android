package qi.ble.communication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import qi.ble.communication.databinding.ActivityMainBinding
import qi.ble.communication.keycore.Kulala
import qi.ble.communication.keycore.KulalaState
import qi.ble.communication.permission.PermissionsHelper


class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val TAG = "Kulala"

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermission() {
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
                    requestBluetoothPermission()
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermission()
        }

        Kulala.instance.init(this)
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
                requestBluetoothPermission()
            }
        }

        if (bluetoothAdapter?.isEnabled == true) {
            val intent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(broadcastReceiver, intent)
            showToast("Bluetooth already enabled!")
        }
    }

    fun connectToVehicle(view: View) {
        Kulala.instance.connectToVehicle(object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.vehicle_connected))
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error.toString())
            }
        })
    }

    fun lockDoors(view: View) {
        Kulala.instance.lockDoors(object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.doors_locked))
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error.toString())
            }
        })
    }

    fun unlockDoors(view: View) {
        Kulala.instance.unlockDoors(object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.doors_unlocked))
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error.toString())
            }
        })
    }

    fun startEngine(view: View) {
        Kulala.instance.startEngine(object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.engine_started))
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error.toString())
            }
        })
    }


    fun stopEngine(view: View) {
        Kulala.instance.stopEngine(object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.engine_stopped))
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error.toString())
            }
        })
    }

    fun disconnectFromVehicle(view: View) {
        Kulala.instance.disconnectFromVehicle(object : Kulala.BlueResult<KulalaState> {
            override fun onSuccess(result: KulalaState) {
                showToast(getString(R.string.vehicle_disconnected))
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, error.toString())
                showToast(error.toString())
            }
        })
    }
}