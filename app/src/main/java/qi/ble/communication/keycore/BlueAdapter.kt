package qi.ble.communication.keycore

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.ParcelUuid
import android.util.Log
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleDeviceServices
import io.reactivex.disposables.Disposable
import java.util.*
import qi.ble.communication.keycore.ConvertHexByte.bytesToHexString


object BlueAdapter {

    const val TAG = "BLE Device"

    private val UUID_WRITE_SERVICE = ParcelUuid.fromString("00001000-0000-1000-8000-00805f9b34fb")
    private val UUID_WRITE_CHARACTERISTIC = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb")
    private val UUID_NOTIFY = UUID.fromString("00001002-0000-1000-8000-00805f9b34fb")
    private val UUID_NOTIFY_DES = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bleDevice: RxBleDevice? = null
    private var bleConnection: RxBleConnection? = null

    fun connectBleDevice(rxBleDevice: RxBleDevice) {
        bleDevice = rxBleDevice
        rxBleDevice.establishConnection(false)
            .flatMapSingle { it.discoverServices() }
            .subscribe({
                onServiceDiscovered(it)
            }, {
                Log.e(TAG, "BLE connection failed: $it")
            }).dispose()
    }

    private fun onServiceDiscovered(rxBleDeviceServices: RxBleDeviceServices) {
        rxBleDeviceServices.getService(UUID_WRITE_SERVICE.uuid)
            .subscribe({
                displayGattServices(it)
            }, {
                Log.e(TAG, "Service discoveries failed: $it")
            }).dispose()
    }


    private fun displayGattServices(bluetoothGattService: BluetoothGattService) {
        val notificationCharacteristic = bluetoothGattService.getCharacteristic(UUID_NOTIFY)
        bleConnection?.setupNotification(notificationCharacteristic)
            ?.subscribe({
                val descriptor = notificationCharacteristic.getDescriptor(UUID_NOTIFY_DES)
                if (descriptor != null) {
                    Log.d(TAG, "Notification channel description settings: $UUID_NOTIFY_DES")
                }
            }, {
                Log.e(TAG, "Notification channel description settings: $it")
            }).let { it?.dispose() }

        val writeCharacteristic = bluetoothGattService.getCharacteristic(UUID_WRITE_CHARACTERISTIC)
        if (writeCharacteristic != null) {
            Log.e(TAG, "Write channel selection:" + writeCharacteristic.uuid)
            val carSign = ""
            onDiscoverOK(writeCharacteristic, carSign)
        }
    }


    private fun onDiscoverOK(writeCharacteristic: BluetoothGattCharacteristic, carSign: String) {
        if (carSign.isNotEmpty()) {
            Log.e("Encryption link", "Encrypt passwords$carSign")
            val data16 = BlueVerfyUtils.getEncryptedData(carSign)
            Log.e("Encryption link", Arrays.toString(data16))
            if (data16 != null) {
                val mess = DataReceive.newBlueMessage(1.toByte(), 0x72.toByte(), data16)
                Log.e(
                    "Encryption link ",
                    "final byte" + Arrays.toString(mess)
                )
                Log.e(
                    "Encryption link",
                    "final hextsr" + bytesToHexString(mess)
                )
                sendMessage(writeCharacteristic, mess)
            }
        }
    }

    fun sendMessage(writeCharacteristic: BluetoothGattCharacteristic, message: ByteArray) {
        write(writeCharacteristic, message)
    }

    private var bleConnectionDisposable: Disposable? = null
    private fun write(characteristic: BluetoothGattCharacteristic, message: ByteArray) {
        bleConnectionDisposable = bleConnection?.writeCharacteristic(characteristic, message)
            ?.subscribe({ characteristicValue ->
                Log.d(TAG, "write characteristic $characteristicValue ")

            }) { throwable ->
                Log.d(TAG, "Write $throwable")
            }
    }
}