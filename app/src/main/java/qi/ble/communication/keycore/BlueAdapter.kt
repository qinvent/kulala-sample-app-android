package qi.ble.communication.keycore

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.RxBleDevice
import java.util.concurrent.TimeUnit

object BlueAdapter {

    private var bleDevice: RxBleDevice? = null
    private var bleConnection: RxBleConnection? = null
    fun connectBleDevice(rxBleDevice: RxBleDevice) {
        bleDevice = rxBleDevice
        bleDevice?.establishConnection(false) // <-- autoConnect flag
            ?.subscribe(
                { rxBleConnection: RxBleConnection ->
                    Log.d("BLE Device ", rxBleConnection.toString())
                    bleConnection = rxBleConnection
                    KeyCore3.getInstance().sendOne("dd")

                }
            ) { throwable: Throwable? ->
                Log.d("BLE Device ", throwable.toString())
            }
    }

    fun sendMessage(message: ByteArray) {

        bleConnection?.discoverServices(300, TimeUnit.SECONDS)
            ?.subscribe({ rxBleDeviceServices ->
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
                        val characteristicUuid = characteristic.uuid.toString()
                        Log.d("BLE Device ", "characteristic UUID $characteristicUuid")
                        write(characteristic, message)
                    }
                }
            }) { throwable ->
                Log.d("BLE Device Services ", throwable.toString())
            }
    }

    private fun write(characteristic: BluetoothGattCharacteristic, message: ByteArray) {
        bleConnection?.writeCharacteristic(characteristic, message)
            ?.subscribe({ characteristicValue ->
                Log.d("BLE Device ", "write characteristic $characteristicValue ")
                KeyCore3.getInstance().sendTwo(characteristicValue, "dd")
            }) { throwable ->
                Log.d("BLE Device  ", "Write ${throwable.toString()}")
            }
    }
}