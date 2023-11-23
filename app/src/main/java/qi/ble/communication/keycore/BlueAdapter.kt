package qi.ble.communication.keycore

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.polidea.rxandroidble2.RxBleClient.State
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask
import qi.ble.communication.BuildConfig
import qi.ble.communication.SampleApp
import qi.ble.communication.keycore.ConvertHexByte.bytesToHexString
import qi.ble.communication.keycore.ConvertHexByte.hexStringToBytes


@SuppressLint("StaticFieldLeak")
object BlueAdapter {

    const val TAG = "BLE Device"

    private val UUID_WRITE_SERVICE = ParcelUuid.fromString("00001000-0000-1000-8000-00805f9b34fb")
    private val UUID_WRITE_CHARACTERISTIC = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb")
    private val UUID_NOTIFY = UUID.fromString("00001002-0000-1000-8000-00805f9b34fb")
    private val UUID_NOTIFY_DES = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val rxBleClient = SampleApp.rxBleClient
    private var rxBleConnection: RxBleConnection? = null
    private var rxBleDevice: RxBleDevice? = null

    private var rxBleState: State? = null
    private var scanDisposable: Disposable? = null
    private var connectDisposable: Disposable? = null
    private var discoveryDisposable: Disposable? = null

    private var bleConnectionDisposable: Disposable? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val carSign = ""

    private var context: Context? = null

    private val isScanning: Boolean
        get() = scanDisposable != null

    fun init(context: Context) {
        this.context = context
    }

    fun scan(result: KcResult<ScanResult>) {
        Log.d(TAG, "scanning")
        val scanSetting = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
            // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
            .build()

        val scanFilter = ScanFilter.Builder()
            .setDeviceAddress("F0:F8:F2:E2:3E:06")// Kulala device address "F0:F8:F2:E2:3E:06"
            // .setDeviceAddress("00:07:80:C5:BB:47") // KC1 device address "00:07:80:C5:BB:47"
            .build()

        scanDisposable = rxBleClient.observeStateChanges()
            .startWith(rxBleClient.state)
            .flatMap {
                rxBleState = it
                if (rxBleState == State.READY) {
                    rxBleClient.scanBleDevices(scanSetting, scanFilter)
                        .timeout(10, TimeUnit.SECONDS)
                } else {
                    io.reactivex.Observable.empty()
                }

            }.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { scanResult: ScanResult ->
                    if (rxBleState == State.READY) {
                        result.onSuccess(scanResult)
                        rxBleDevice = scanResult.bleDevice
                    } else {
                        result.onError(Throwable(rxBleState?.name))
                        Log.d(TAG, rxBleState?.name.toString())
                    }
                    Log.d(TAG, scanResult.toString())
                    if (isScanning) scanDisposable?.dispose()
                }
            ) { throwable: Throwable ->
                onScanFailure(throwable)
                result.onError(throwable)
                scanDisposable?.dispose()
            }

    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) Log.e(TAG, "Scan failed", throwable)
        else Log.w(TAG, "Scan failed", throwable)
    }

    fun connect() {
        connectDisposable = rxBleDevice?.establishConnection(false)
            ?.flatMapSingle {
                Log.d(TAG, "BLE connection established!")
                it.discoverServices()
            }?.subscribe({
                Log.d(TAG, "BLE Device Service discovered!")
                onServiceDiscovered(it)
                connectDisposable?.dispose()
            }, {
                connectDisposable?.dispose()
                Log.e(TAG, "BLE connection failed: $it")
            })
    }

    private fun onServiceDiscovered(rxBleDeviceServices: RxBleDeviceServices) {
        discoveryDisposable = rxBleDeviceServices.getService(UUID_WRITE_SERVICE.uuid)
            .subscribe({
                Log.d(TAG, "Get write service success!")
                getCharacteristics(it)
                discoveryDisposable?.dispose()
            }, {
                Log.e(TAG, "Get write service failed: $it")
                discoveryDisposable?.dispose()
            })
    }


    private fun getCharacteristics(bluetoothGattService: BluetoothGattService) {
        Log.d(TAG, "Get characteristic for notification : $UUID_NOTIFY")
        val notificationCharacteristic = bluetoothGattService.getCharacteristic(UUID_NOTIFY)
        Log.d(TAG, "Setup for notifications change for characteristic : $UUID_NOTIFY")
        rxBleConnection?.setupNotification(notificationCharacteristic)
            ?.doOnNext {
                val descriptor = notificationCharacteristic.getDescriptor(UUID_NOTIFY_DES)
                if (descriptor != null) {
                    Log.d(TAG, "Notification channel description settings: $UUID_NOTIFY_DES")
                }
            }?.flatMap { it }
            ?.subscribe({
                Log.i(TAG, "NotificationReceived for  $UUID_NOTIFY_DES");
                onNotificationReceived(it)
            }, {
                Log.e(TAG, "Notification channel description settings: $it")
            }).let { it?.dispose() }

        writeCharacteristic = bluetoothGattService.getCharacteristic(UUID_WRITE_CHARACTERISTIC)
        if (writeCharacteristic != null) {
            Log.e(TAG, "Write channel selection:" + writeCharacteristic?.uuid)
            rxBleDevice?.name?.let { onDiscoverCharacteristic(carSign, it)  }

        }
    }


    private fun onDiscoverCharacteristic(carSign: String, deviceName: String) {
        if (carSign.isNotEmpty() && (deviceName.startsWith("NFC") || deviceName.startsWith("AKL") || deviceName.startsWith(
                "MIN"
            ))
        ) {
            Timer().schedule(timerTask {
                if (carSign.isNotEmpty()) {
                    if (BuildConfig.DEBUG) Log.e(
                        "Encryption link",
                        "Encrypt passwords $carSign"
                    )
                    val data16 = BlueVerfyUtils.getEncryptedData(carSign)
                    if (BuildConfig.DEBUG) Log.e("Encryption link", Arrays.toString(data16))
                    if (data16 != null) {
                        val mess = DataReceive.newBlueMessage(1.toByte(), 0x72.toByte(), data16)
                        if (BuildConfig.DEBUG) Log.e(
                            "Encryption link ",
                            "final byte" + Arrays.toString(mess)
                        )
                        if (BuildConfig.DEBUG) Log.e(
                            "Encryption link",
                            "final hextsr" + bytesToHexString(mess)
                        )
                        sendMessage(bytesToHexString(mess))
                    }
                }
            }, 500)
        } else {
            Timer().schedule(timerTask {
                if (carSign.isNotEmpty()) {
                    val bytes: ByteArray = byteArrayOf()//dataCar.carSign.getBytes()
                    val mess = DataReceive.newBlueMessage(1.toByte(), 1.toByte(), bytes)
                    Log.d(
                        "blue",
                        "onDiscoverOK sendmessage:" + bytesToHexString(mess)
                    ) //The UI thread cannot be used, and the screen is turned off
                    sendMessage(bytesToHexString(mess))
                }
            }, 500)
        }
    }

    @SuppressLint("NewApi")
    fun sendMessage(message: String) {
        Log.i("Host Bluetooth", "Send Message")
        bleConnectionDisposable =
            writeCharacteristic?.let {
                rxBleConnection?.writeCharacteristic(it, ConvertHexByte.hexStringToBytes(message))
                    ?.doFinally { bleConnectionDisposable?.dispose() }
                    ?.subscribe({ characteristicValue ->
                        Log.d(TAG, "write characteristic $characteristicValue ")
                        onMessageSent(characteristicValue)
                    }) { throwable ->
                        Log.d(TAG, "write characteristic $throwable")
                    }
            }
    }

    fun sendMessage(message: ByteArray) {
        Log.i("Host Bluetooth", "Send Message")
        bleConnectionDisposable =
            writeCharacteristic?.let {
                rxBleConnection?.writeCharacteristic(it, message)
                    ?.doFinally { bleConnectionDisposable?.dispose() }
                    ?.subscribe({ characteristicValue ->
                        Log.d(TAG, "write characteristic $characteristicValue ")
                        onMessageSent(characteristicValue)
                    }) { throwable ->
                        Log.d(TAG, "write characteristic $throwable")
                    }
            }
    }


    private fun onMessageSent(bytes: ByteArray?) {
        if (BuildConfig.DEBUG) Log.e("The delivery of the mod was successful", "123 ")
        if (bytes == null) return
        val byteStr = bytesToHexString(bytes) ?: return
        if (BuildConfig.DEBUG) Log.e("blue", "onMessageSended length:" + bytes.size + " " + byteStr)
        if (BuildConfig.DEBUG) Log.e(
            "------------",
            "onMessageSended length:" + bytes.size + " " + byteStr
        )
        val s6s = bytesToHexString(hexStringToBytes(BlueStaticValue.getControlCmdByID(6)))
        if (BuildConfig.DEBUG) Log.e(
            "------------",
            "onMessageSended length: s6s$s6s"
        )
        if (bytes.size >= 16) {
            Timer().schedule(timerTask {
                if (BuildConfig.DEBUG) Log.e(
                    "bluestate",
                    "onMessageSended Vibrate indicates that the connection is successful!"
                )
                // val dataCar: DataCarBlue = BlueLinkControl.getInstance().getDataCar()
                val vibratorOpen  = true
                if (BuildConfig.DEBUG) Log.d(
                    "------------",
                    "datacar vibratorOpen $vibratorOpen"
                )
                if (vibratorOpen) {
                    if (BuildConfig.DEBUG) Log.d(
                        "------------",
                        "Vibrate indicates that the connection is successful"
                    )
                    // showVibrator()
                }
            }, 500)

        } else if (bytes.size == 5 && bytes[0] == hexStringToBytes("AA")[0] && bytes[1].toInt() == 2 && bytes[2] == hexStringToBytes(
                "55"
            )[0] && bytes[3] == hexStringToBytes("0A")[0] && bytes[4] == hexStringToBytes("F4")[0]
        ) {
        } else {
            if (byteStr == bytesToHexString(
                    hexStringToBytes(
                        BlueStaticValue.getControlCmdByID(1)
                    )
                )
            ) {
                Log.d(
                    "blueSound",
                    "bytes:" + byteStr + "  " + BlueStaticValue.getControlCmdByID(1)
                )
                if (BuildConfig.DEBUG) Log.e("SoundPlay", "ServiceA play_start")
                // SoundPlay.getInstance().play_start(this@KulalaServiceA)
            } else if (byteStr == bytesToHexString(
                    hexStringToBytes(
                        BlueStaticValue.getControlCmdByID(
                            5
                        )
                    )
                )
            ) {
                // SoundPlay.getInstance().play_backpag(this@KulalaServiceA)
            } else if (byteStr == s6s) {
                if (BuildConfig.DEBUG) Log.e(
                    "------------",
                    "The sound of the wave looking for the car"
                )
                // SoundPlay.getInstance().play_findcar(this@KulalaServiceA)
            } else if (byteStr == bytesToHexString(
                    hexStringToBytes(
                        BlueStaticValue.getControlCmdByID(
                            3
                        )
                    )
                ) || byteStr == bytesToHexString(
                    hexStringToBytes(
                        BlueStaticValue.getControlCmdByID(
                            4
                        )
                    )
                )
            ) {

                // if (System.currentTimeMillis() - OShakeBlueNoScreenOnOrOff.controlSuccessTime < 2000L) {
                //     showVibrator()
                //     SoundPlay.getInstance().play_lock(this@KulalaServiceA)
                // } else {
                //     SoundPlay.getInstance().play_lock(this@KulalaServiceA)
                // }
            }
        }
    }

    private fun onNotificationReceived(bytes: ByteArray) {
        findMege(bytes)
    }


    private var cachecheck: ByteArray? = null
    var countMegeTime: Long = 0

    private fun findMege(megebyte: ByteArray) {
        Log.e(TAG, "Initial value" + megebyte.contentToString())
        cachecheck = ConvertHexByte.bytesMege(cachecheck, megebyte)
        if ((cachecheck?.size ?: 0) > 200) {
            cachecheck = null //Too much more data
            return
        }
        val now = System.currentTimeMillis()
        //        if (now - countMegeTime >= 50L) {
        countMegeTime = now
        //             if (BuildConfig.DEBUG) Log.e("blue", "2108 cachecheck:"+ConvertHexByte.bytesToHexString(cachecheck));
        Log.e(TAG, "cachecheck" + Arrays.toString(cachecheck))
        findNext(cachecheck)
        cachecheck = null
//        }
    }

    private fun findNext(megebyte: ByteArray?) {
        //1.Check that the calibration is appropriate
        val data = DataReceive()
        if (megebyte == null || megebyte.size < 4) {
            cachecheck = megebyte
            return
        }
        //1 DataType
        data.dataType = megebyte[0].toInt()
        //2 Length
        data.length = megebyte[1].toInt()
        //3 data
        if (data.length == 0 || data.length > 32 || data.length < 0) {
            findNext(ConvertHexByte.bytesCut(megebyte, 1)) //Cut it out 1byte;
            return
        }
        if (megebyte.size < data.length + 3) {
            cachecheck = megebyte
            return  //The data is not completed, waiting for the downlink Type+length+data+check
        }
        //If you have enough data, read it in first data
        data.data = ByteArray(data.length)
       val newMergeByte = ByteHelper.bytesMege(megebyte)
        System.arraycopy(newMergeByte, 2, data.data, 0, data.length)
        //4.check
        data.check = megebyte[data.length + 2].toInt()
        if (data.matchCheck()) {
                Log.d(
                    TAG,
                    "data。data" + Arrays.toString(data.data)
                        .toString() + "data.type" + data.dataType
                )
               onDataReceived(data)
        }
        val uncheck = ConvertHexByte.bytesCut(megebyte, data.length + 3) //Cut off the duplicated
        findNext(uncheck)
    }

    private fun onDataReceived(data: DataReceive?) {
        if (data == null) return
        if (data.dataType == 0x03) {
            if (BuildConfig.DEBUG) Log.e(
                "Encryption link",
                "Data to be decrypted" + Arrays.toString(data.data)
            )
            if (data.data.size == 17) {
                val decodeByte = DataReceive.subBytes(data.data, 1, 16)
                if (BuildConfig.DEBUG) Log.e(
                    "Encryption link",
                    "After the data to be decrypted 16 bit" + Arrays.toString(decodeByte)
                )
                // val dataCar: DataCarBlue = BlueLinkControl.getInstance().getDataCar()
                val deviceName = rxBleDevice?.name?:""
                if (deviceName.isNotEmpty() && (deviceName.startsWith(
                        "NFC"
                    ) || deviceName.startsWith("AKL") || deviceName.startsWith("MIN"))
                ) {
                    if (BuildConfig.DEBUG) Log.d(
                        "Encrypted Link",
                        "Decrypted Password carSign"
                    )
                    if (carSign.isNotEmpty()) {
                        val jiemihoushuzu = AES.decrypt(decodeByte, carSign)
                        if (BuildConfig.DEBUG) Log.e(
                            "Encrypted Link",
                            "Decrypted Array" + Arrays.toString(jiemihoushuzu)
                        )
                        if (BuildConfig.DEBUG) Log.e(
                            "Encrypted Link",
                            "Decrypted String." + bytesToHexString(jiemihoushuzu)
                        )
                        val byteAppTime = DataReceive.subBytes(jiemihoushuzu, 0, 8)
                        val byteBlueTime = DataReceive.subBytes(jiemihoushuzu, 8, 8)
                        val appTime = String(byteAppTime)
                        if (BuildConfig.DEBUG) Log.e(
                            "Encryption link",
                            "decrypted app time$appTime"
                        )
                        val blueTime = String(byteBlueTime)
                        if (BuildConfig.DEBUG) Log.e(
                            "Encrypted link",
                            "decrypted app Bluetooth time$blueTime"
                        )
                        try {
                            val longAppTime = appTime.toLong()
                            if (BlueVerfyUtils.appTime != 0L && longAppTime != 0L) {
                                val timeAddOne = blueTime.toLong() + 1
                                val jiamiTime = timeAddOne.toString()
                                if (BuildConfig.DEBUG) Log.e(
                                    "Encrypt Link",
                                    "Bluetooth Time to Encrypt$jiamiTime"
                                )
                                val data16 = AES.AESgenerator(jiamiTime, carSign)
                                if (BuildConfig.DEBUG) Log.e(
                                    "Encrypted Link",
                                    "Encrypted Bluetooth Time Array" + Arrays.toString(data16)
                                )
                                val isMyCar = 1
                                // val mess: ByteArray = if (dataCar.isMyCar == 1) {
                                val mess: ByteArray = if (isMyCar == 1) {
                                    DataReceive.newBlueMessage(1.toByte(), 0x73.toByte(), data16)
                                } else {
                                    DataReceive.newBlueMessage(1.toByte(), 0x74.toByte(), data16)
                                }
                                if (BuildConfig.DEBUG) Log.e(
                                    "Encryption Link",
                                    "Final byte time +1" + mess.contentToString()
                                )
                                if (BuildConfig.DEBUG) Log.e(
                                    "Encryption link",
                                    "Final hextsr time +1" + bytesToHexString(mess)
                                )
                                sendMessage(bytesToHexString(mess))
                            }
                        } catch (e: NumberFormatException) {
                            Log.e("Encryption link", "Decrypted app time encountered error")
                        }
                    }
                }
            }
        }
        if (data.dataType == 0x21) {
            val infos =
                ConvertHexByte.getBitArray(data.data[1]) //Basic information about the vehicle
            Log.d(TAG, "Car status isLock ${infos[6]}")
            // BlueLinkControl.getInstance().getDataCar().isLock =
            //     infos[6] //Lock status 0b: Unlock 1b: Locked
            Log.d(TAG, "Car status isTheft ${infos[2] * 2 + infos[1]}")
            // BlueLinkControl.getInstance().getDataCar().isTheft =
            //     infos[2] * 2 + infos[1] //Anti-theft
            Log.d(TAG, "Car status isON ${infos[0]}")
            // BlueLinkControl.getInstance().getDataCar().isON =
            //     infos[0] //ONSwitch status            0b: OFF 1b: ON
            // if (ManagerCurrentCarStatus.getInstance().getCarStatus() != null) {
            //     ManagerCurrentCarStatus.getInstance().setIsOn(infos[0])
            // }
        }
    }

    fun getDataCar(): DataCarBlue {
        if (BuildConfig.DEBUG) Log.e(
            "bluestate",
            "BlueAdapter.current_blue_state"
        )
        return DataCarBlue.loadLocal(context)
    }

    /**
     * Wrapper for slick result
     */
    interface KcResult<T> {
        fun onSuccess(result: T)
        fun onError(error: Throwable)
    }
}
