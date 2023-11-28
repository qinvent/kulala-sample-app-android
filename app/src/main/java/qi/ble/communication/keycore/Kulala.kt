package qi.ble.communication.keycore

import android.content.Context

enum class Kulala {

    instance;

    var blueAdapter: BlueAdapter? = null


    private var context: Context? = null

    private fun throwNotInitializedError(result: BlueResult<KulalaState>) =
        result.onError(Throwable("Call `Kulala.instance.init()` first."))

    fun init(ctx: Context) {
        context = ctx
        blueAdapter = BlueAdapter(ctx)
    }

    fun isInitialized() = context != null

    fun connectToVehicle(result: BlueResult<KulalaState>) {
        if (!isInitialized()) {
            throwNotInitializedError(result)
            return
        }
        blueAdapter?.connectToVehicle(result)
    }

    fun lockDoors(result: BlueResult<KulalaState>) {
        if (!isInitialized()) {
            throwNotInitializedError(result)
            return
        }

        blueAdapter?.lockDoors(result)
    }

    fun unlockDoors(result: BlueResult<KulalaState>) {
        if (!isInitialized()) {
            throwNotInitializedError(result)
            return
        }
        blueAdapter?.unlockDoors(result)
    }

    fun startEngine(result: BlueResult<KulalaState>) {
        if (!isInitialized()) {
            throwNotInitializedError(result)
            return
        }
        blueAdapter?.startEngine(result)
    }

    fun stopEngine(result: BlueResult<KulalaState>) {
        if (!isInitialized()) {
            throwNotInitializedError(result)
            return
        }
        blueAdapter?.stopEngine(result)
    }

    fun disconnectFromVehicle(result: BlueResult<KulalaState>) {
        if (!isInitialized()) {
            throwNotInitializedError(result)
            return
        }
        blueAdapter?.disconnectFromVehicle(result)
    }

    interface BlueResult<T> {
        fun onSuccess(result: T)
        fun onError(error: Throwable)
    }
}