package qi.ble.communication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
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
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.exceptions.BleScanException
import com.polidea.rxandroidble3.scan.ScanResult
import com.polidea.rxandroidble3.scan.ScanSettings
import io.reactivex.rxjava3.core.Observable.empty
import io.reactivex.rxjava3.disposables.Disposable
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import qi.ble.communication.adapter.ScanResultsAdapter
import qi.ble.communication.databinding.ActivityMainBinding
import qi.ble.communication.keycore.BlueAdapter
import qi.ble.communication.permission.PermissionsHelper


class MainActivity : AppCompatActivity() {
    var bluetoothAdapter: BluetoothAdapter? = null

    private val rxBleClient = SampleApp.rxBleClient
    private var scanSubscription: Disposable? = null
    private var flowDisposable: Disposable? = null
    private var connectDisposable: Disposable? = null

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
        // binding.btnConnect.setOnClickListener { connect() }
        binding.btnSend.setOnClickListener {
            val bytes = binding.editText.text.toString().toByteArray(Charset.defaultCharset())
            // bluetoothConnection?.write(bytes)
        }
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

    private val resultsAdapter =
        ScanResultsAdapter { scanResult ->
            connect(scanResult)
        }

    private fun connect(scanResult: ScanResult) {
        val device = rxBleClient.getBleDevice(scanResult.bleDevice.macAddress)

        BlueAdapter.connectBleDevice(device)
        /*connectDisposable = device.establishConnection(false) // <-- autoConnect flag
            .subscribe(
                { rxBleConnection: RxBleConnection ->
                    Log.d("BLE Device ", rxBleConnection.toString())
                    getCharacteristicUuid(rxBleConnection)

                }
            ) { throwable: Throwable? ->
                Log.d("BLE Device ", throwable.toString())
            }*/

    }

    private lateinit var characteristicUuid: String
    private fun getCharacteristicUuid(rxBleConnection: RxBleConnection) {
        rxBleConnection.discoverServices(300, TimeUnit.SECONDS)
            .subscribe({ rxBleDeviceServices ->
                Log.d("BLE Device Services ", rxBleDeviceServices.toString())
                val services = rxBleDeviceServices.bluetoothGattServices
                services.forEach { bluetoothGattService ->
                    // here you can work with service's uuid
                    val serviceUuid: String = bluetoothGattService.uuid.toString()
                    Log.d("BLE Device ", "service UUID $serviceUuid")
                    // or with all characteristics in service
                    val characteristics = bluetoothGattService.characteristics
                    for (characteristic: BluetoothGattCharacteristic in characteristics) {
                        // here you have your characteristic's UUID
                        characteristicUuid = characteristic.uuid.toString()
                        Log.d("BLE Device ", "characteristic UUID $characteristicUuid")
                        rxBleConnection.readCharacteristic(characteristic).subscribe({
                            val str = it.toString(StandardCharsets.UTF_8)
                            Log.d("BLE Device ", "characteristic $str")
                        }) {
                            Log.d("BLE Device characteristic ", "characteristic error $it")
                        }
                    }
                }
            }) { throwable ->
                Log.d("BLE Device Services ", throwable.toString())
            }
    }

    private fun observerBleStateChange() {
        flowDisposable = rxBleClient.observeStateChanges()
            .switchMap<Any> { state: RxBleClient.State? ->
                when (state) {
                    RxBleClient.State.READY ->                 // everything should work
                        return@switchMap rxBleClient.scanBleDevices()
                    RxBleClient.State.BLUETOOTH_NOT_AVAILABLE, RxBleClient.State.LOCATION_PERMISSION_NOT_GRANTED, RxBleClient.State.BLUETOOTH_NOT_ENABLED, RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED -> return@switchMap empty()
                    else -> return@switchMap empty()

                }
            }
            .subscribe(
                { rxBleScanResult: Any? -> }
            ) { throwable: Throwable? -> }

    }


    override fun onDestroy() {
        super.onDestroy()
        dispose()
    }

    private val isScanning: Boolean
        get() = scanSubscription != null


    private fun scan() {
        Log.d(TAG, "scanning")
        scanSubscription = rxBleClient.scanBleDevices(
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                .build(), // add filters if needed

            // ScanFilter.Builder()
            //     .setDeviceAddress("20:39:56:F0:CF:54") // change if needed
            //     .build()
        )
            .subscribe(
                { scanResult: ScanResult ->
                    resultsAdapter.addScanResult(scanResult)
                }
            ) { throwable: Throwable ->
                onScanFailure(throwable)
            }
    }

    private fun dispose() {
        scanSubscription?.dispose()
        flowDisposable?.dispose()
        connectDisposable?.dispose()
        resultsAdapter.clearScanResults()
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) Log.e("ScanActivity", "Scan failed", throwable)
        else Log.w("ScanActivity", "Scan failed", throwable)
    }

    companion object {
        private const val TAG = "BLE Comm"
    }
}