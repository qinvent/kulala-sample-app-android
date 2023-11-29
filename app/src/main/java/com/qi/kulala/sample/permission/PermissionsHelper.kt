package com.qi.kulala.sample.permission

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX
import com.qi.kulala.sample.R

/**
 * A helper class to handle runtime permissions
 */
object PermissionsHelper {

    /**
     * A flag to indicate if it went into Settings page
     */
    var isForwardedToSettings = false

    /**
     * A flag to indicate if it is currently requesting or not
     * This is useful to not have duplicates asking for permissions
     */
    var isRequesting = false


    /**
     * Utility function that request runtime permissions
     *
     * @param activity
     * @param permissions list of permission to request
     * @param callBack to call if all permissions are granted
     * @receiver
     */
    fun requestPermissions(
        activity: FragmentActivity,
        permissions: List<String>,
        callBack: (result: RequestPermissionResult) -> Unit
    ) {

        Log.d("Requesting for", "$permissions")
        isForwardedToSettings = false // reset
        isRequesting = true // going to request now
        PermissionX.init(activity)
            .permissions(permissions)
            .onForwardToSettings { scope, deniedList ->
                Log.d("deniedList=", "$deniedList")
                isForwardedToSettings = true
                scope.showForwardToSettingsDialog(
                    deniedList,
                    activity.getString(R.string.allow_in_settings),
                    activity.getString(R.string.ok),
                    activity.getString(R.string.cancel)
                )
            }
            .request { allGranted, grantedList, deniedList ->
                isRequesting = false // not anymore since result has been returned
                callBack.invoke(
                    RequestPermissionResult(
                        allGranted,
                        grantedList,
                        deniedList,
                        isForwardedToSettings
                    )
                )
            }
    }
}

/**
 *  Data class for requested permission result
 */
data class RequestPermissionResult(
    var allGranted: Boolean,
    var grantedList: List<String>,
    var deniedList: List<String>,
    var isForwardedToSettings: Boolean
)
