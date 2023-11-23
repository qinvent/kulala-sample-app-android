package qi.ble.communication.keycore;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
/**
 * Created by Administrator on 2018/5/11.
 */

public class DataCarBlue {
    public long carId;
    public String deviceName;
    public String deviceAddress;
    public String carSign;//蓝牙验证券
    public boolean isUseBlueModel = false;//蓝牙模式
    public boolean isShakeOpen =  false;//摇一摇开关
    public boolean vibratorOpen =  true;//震动提醒开关
    public int isBindBluetooth;
    public String carsig;
    public int isMyCar;
    public String shakeLevel;

    //for shake
    public int isON =  0;
    public int isTheft =  0;
    public int isLock =  0;
    public static void saveLocal(Context context,DataCarBlue car){
        if(context == null || car == null)return;
        SharedPreferences settings  = context.getSharedPreferences("carpods_share_blue", Activity.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        if (editor == null)return;
        editor.putLong("carId", car.carId);
        editor.putString("deviceName", car.deviceName);
        editor.putString("deviceAddress", car.deviceAddress);
        editor.putString("carSign", car.carSign);
        editor.putBoolean("isUseBlueModel", car.isUseBlueModel);
        editor.putBoolean("isShakeOpen", car.isShakeOpen);
        editor.putBoolean("vibratorOpen", car.vibratorOpen);
        editor.putInt("isON", car.isON);
        editor.putInt("isTheft", car.isTheft);
        editor.putInt("isLock", car.isLock);
        editor.putInt("isBindBluetooth", car.isBindBluetooth);
        editor.putString("carsig", car.carsig);
        editor.putInt("isMyCar", car.isMyCar);
        editor.putString("shakeLevel", car.shakeLevel);
        editor.commit();
    }
    public static DataCarBlue loadLocal(Context context){
        SharedPreferences settings  = context.getSharedPreferences("carpods_share_blue", Activity.MODE_MULTI_PROCESS);
        if (settings == null)return null;
        DataCarBlue car = new DataCarBlue();
        car.carId = settings.getLong("carId", 0);
        car.deviceName = settings.getString("deviceName", null);
        car.deviceAddress = settings.getString("deviceAddress", null);
        car.carSign = settings.getString("carSign", null);
        car.isUseBlueModel = settings.getBoolean("isUseBlueModel", false);
        car.isShakeOpen = settings.getBoolean("isShakeOpen", false);
        car.vibratorOpen = settings.getBoolean("vibratorOpen", false);
        car.isON = settings.getInt("isON", 0);
        car.isTheft = settings.getInt("isTheft", 0);
        car.isLock = settings.getInt("isLock", 0);
        car.isBindBluetooth = settings.getInt("isBindBluetooth", 0);
        car.carsig = settings.getString("carsig", null);
        car.isMyCar = settings.getInt("isMyCar", 0);
        car.shakeLevel = settings.getString("shakeLevel", null);
        return car;
    }
}
