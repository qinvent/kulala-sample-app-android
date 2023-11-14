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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Flowable.empty
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.nio.charset.Charset
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import qi.ble.communication.adapter.ScanResultsAdapter
import qi.ble.communication.databinding.ActivityMainBinding
import qi.ble.communication.keycore.BlueAdapter
import qi.ble.communication.keycore.BlueAdapter.TAG
import qi.ble.communication.permission.PermissionsHelper


class MainActivity : AppCompatActivity() {
    var bluetoothAdapter: BluetoothAdapter? = null

    private val rxBleClient = SampleApp.rxBleClient
    private var scanDisposable: Disposable? = null
    private var flowDisposable: Disposable? = null
    private var rxBleDevice: RxBleDevice? = null

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
    private val broadcastReceiver1: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> Log.d(TAG, "onReceive: STATE OFF")
                    BluetoothAdapter.STATE_TURNING_OFF -> Log.d(
                        TAG, "mBroadcastReceiver1: STATE TURNING OFF"
                    )
                    BluetoothAdapter.STATE_ON -> Log.d(TAG, "mBroadcastReceiver1: STATE ON")
                    BluetoothAdapter.STATE_TURNING_ON -> Log.d(
                        TAG, "mBroadcastReceiver1: STATE TURNING ON"
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        binding.btnScan.setOnClickListener { scan() }
        binding.btnConnect.setOnClickListener { connect() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermission()
        }

        configureResultList(binding)
        observerBleStateChange()
    }

    fun enableBluetooth(view: View) {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.")
        }
        if (bluetoothAdapter?.isEnabled == false && ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "enableDisableBT: enabling BT.")
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(btIntent)
            val intent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(broadcastReceiver1, intent)
        }
        if (bluetoothAdapter?.isEnabled == true) {
            val intent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(broadcastReceiver1, intent)
        }
    }

    private fun configureResultList(binding: ActivityMainBinding) {
        with(binding.scanResults) {
            setHasFixedSize(true)
            itemAnimator = null
            adapter = resultsAdapter
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }

    private val resultsAdapter = ScanResultsAdapter()

    private fun connect() {
        rxBleDevice?.let { BlueAdapter.connectBleDevice(it) }
    }

    private fun observerBleStateChange() {
        flowDisposable = rxBleClient.observeStateChanges()
            .switchMap<Any> { state: RxBleClient.State? ->
                when (state) {
                    RxBleClient.State.READY ->                 // everything should work
                        return@switchMap rxBleClient.scanBleDevices()
                    RxBleClient.State.BLUETOOTH_NOT_AVAILABLE, RxBleClient.State.LOCATION_PERMISSION_NOT_GRANTED, RxBleClient.State.BLUETOOTH_NOT_ENABLED, RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED -> return@switchMap io.reactivex.Observable.empty()
                    else -> return@switchMap io.reactivex.Observable.empty()

                }
            }
            .subscribe(
                { rxBleScanResult: Any? -> }
            ) { throwable: Throwable? -> }

    }


    override fun onDestroy() {
        super.onDestroy()
        scanDisposable?.dispose()
    }

    public override fun onPause() {
        super.onPause()
        if (isScanning) scanDisposable?.dispose()
    }

    private val isScanning: Boolean
        get() = scanDisposable != null


    private fun scan() {
        Log.d(TAG, "scanning")
        rxBleClient.scanBleDevices(
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                .build(), // add filters if needed

            ScanFilter.Builder()
                // .setDeviceAddress("F0:F8:F2:E2:3E:06")// Kulala device address "F0:F8:F2:E2:3E:06"
                .setDeviceAddress("00:07:80:C5:BB:47") // KC1 device address "00:07:80:C5:BB:47"
                .build()
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { scanResult: ScanResult ->
                    resultsAdapter.addScanResult(scanResult)
                    rxBleDevice = scanResult.bleDevice
                    Log.d(TAG, scanResult.toString())
                    if(isScanning) scanDisposable?.dispose()
                }
            ) { throwable: Throwable ->
                onScanFailure(throwable)
            }.let { scanDisposable = it }
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) Log.e(TAG, "Scan failed", throwable)
        else Log.w(TAG, "Scan failed", throwable)
        scanDisposable?.dispose()
    }
}