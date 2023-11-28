package qi.ble.communication.keycore;

import android.text.TextUtils;

/**
 * Created by Administrator on 2018/5/11.
 */

public class BlueStaticValue {
    public static final String[] controlCmdArr = new String[]{
            "0x82 02 00 10 6B",//start stop
            "0x82 02 40 00 3B",//lock
            "0x82 02 08 00 73",//unlock
            "0x82 02 00 80 FB",//backpag
            "0x82 02 01 00 7A"//findcar
    };
    public static boolean isControlCmd(byte[] bytes,String preCmd){
        if(TextUtils.isEmpty(preCmd) || bytes == null || bytes.length == 0)return false;//空对像
        byte[] byt = ByteHelper.hexStringToBytes(preCmd);
        if(ByteHelper.bytesToHexString(byt).equals(ByteHelper.bytesToHexString(bytes)))return true;
        return false;
    }
    public static String getBlueTimeCmd(int time){
        switch (time) {
            case 0: return "E1 02 10 00 0C";
            case 1: return "E1 02 30 00 EC";
            case 2: return "E1 02 50 00 CC";
            case 3: return "E1 02 70 00 AC";
            case 4: return "E1 02 90 00 8C";
            case 5: return "E1 02 B0 00 6C";
            case 6: return "E1 02 D0 00 4C";
            case 7: return "E1 02 F0 00 2C";
        }
        return "";
    }
    /**
     * Control the vehicle instruction control command 1: On 2: Turn off the engine 3: Fortify 4: Disarm 5: Tail box change 6: Find the car The time only has 0 to 7 gears, each gear is 5 minutes, 0 gear is 5 minutes
     */
    public static String getControlCmdByID(int controlCmd) {
        switch (controlCmd) {
            case 1:
            case 2:
                return controlCmdArr[0];
            case 3: return controlCmdArr[1];
            case 4: return controlCmdArr[2];
            case 5: return controlCmdArr[3];
            case 6: return controlCmdArr[4];
        }
        return "";
    }

    public static String BLUETOOTH_NEED_SENDMESSAGE        = "BLUETOOTH_NEED_SENDMESSAGE";
    public static String BLUETOOTH_NEED_STOPCONN_CLEARDATA = "BLUETOOTH_NEED_STOPCONN_CLEARDATA";
    public static String BLUETOOTH_NEED_CHANGE_CAR         = "BLUETOOTH_NEED_CHANGE_CAR";
    public static String BLUETOOTH_NEED_CHANGE_CAR_DATA         = "BLUETOOTH_NEED_CHANGE_CAR_DATA";
    public static String BLUETOOTH_NEED_GET_CARINFO         = "BLUETOOTH_NEED_GET_CARINFO";
    public static String BLUETOOTH_NEED_SENDMESSAGE_LCD        = "BLUETOOTH_NEED_SENDMESSAGE_LCD";
    public static String BLUETOOTH_NEED_STOPCONN_CLEARDATA_LCD = "BLUETOOTH_NEED_STOPCONN_CLEARDATA_LCD";
    public static String BLUETOOTH_NEED_SEND_CARINFO_TO_LCD         = "BLUETOOTH_NEED_SEND_CARINFO_TO_LCD";
    public static String BLUETOOTH_NEED_CHANGE_LCDKEY        = "BLUETOOTH_NEED_CHANGE_LCDKEY";
    public static String BLUETOOTH_NEED__LCDKEY_CONTROL_CAR        = "BLUETOOTH_NEED__LCDKEY_CONTROL_CAR";
    public static String BLUETOOTH_NEED_GET_CARINFO_FOR_LCDKEY         = "BLUETOOTH_NEED_GET_CARINFO_FOR_LCDKEY";
    public static final String ACTION_CONNECTION_STATE_CHANGED =      "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
    public static String BLUETOOTH_NEED_CLEAR_BLUETOOTH         = "BLUETOOTH_NEED_CLEAR_BLUETOOTH";
    //    public static String SERVICE_1_SOUND_CONTROL = "SERVICE_1_SOUND_CONTROL";
    public static String SERVICE_1_HEART_BEET = "SERVICE_1_HEART_BEET";
    public static String SERVICE_2_HEART_BEET = "SERVICE_2_HEART_BEET";

    public static String ONCONNECTEDFAILED = "ONCONNECTEDFAILED";
    public static String ONDISCOVEROK      = "ONDISCOVEROK";
    public static String ONMESSAGESENDED   = "ONMESSAGESENDED";
    public static String ONDATARECEIVED    = "ONDATARECEIVED";
    public static String ONBLUECONNCHANGE  = "ONBLUECONNCHANGE";

    public static String ONCONNECTEDFAILEDLCDKEY = "ONCONNECTEDFAILEDLCDKEY";
    public static String ONDISCOVEROKLCD      = "ONDISCOVEROKLCD";


//    public static String OSHAKE_ACTION_SCREEN_ON  = "OSHAKE_ACTION_SCREEN_ON";
//    public static String OSHAKE_ACTION_SCREEN_OFF = "OSHAKE_ACTION_SCREEN_OFF";
    //soki
    public static String SERVICE_INIT_SOCKET_GET  = "SERVICE_INIT_SOCKET_GET";
    public static String SERVICE_INIT_SOCKET_POST = "SERVICE_INIT_SOCKET_POST";
    public static String SERVICE_INIT_NOTIFI_GET  = "SERVICE_INIT_NOTIFI_GET";
    public static String SERVICE_INIT_NOTIFI_POST = "SERVICE_INIT_NOTIFI_POST";
    public static String SERVICE_SEND_MESSAGE     = "SERVICE_SEND_MESSAGE";
    public static String SERVICE_RECEIVE_MESSAGE  = "SERVICE_RECEIVE_MESSAGE";
    public static String SERVICE_IS_CONNECT  = "SERVICE_IS_CONNECT";
//    public static String IS_ACTIVITYMAIN_FORGROUND  = "IS_ACTIVITYMAIN_FORGROUND";
    public static String IS_NEED_SERVICEA_TO_SERVICE_C  = "IS_NEED_SERVICEA_TO_SERVICE_C";
    public static String SEND_CAR_STATUS_BLUETOOTH_PROGRESS  = "SEND_CAR_INFO_BLUETOOTH_PROGRESS";
    public static String SEND_CAR_STATUS_SERVICEC_TO_SERVICECA  = "SEND_CAR_STATUS_SERVICEC_TO_SERVICECA";
    public static String BLUE_STATE_CHANGE  = "android.bluetooth.adapter.action.STATE_CHANGED";
}
