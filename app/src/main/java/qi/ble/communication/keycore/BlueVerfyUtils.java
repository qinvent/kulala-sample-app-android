package qi.ble.communication.keycore;


import android.util.Log;

public class BlueVerfyUtils {
    public static long appTime;

    /**
     * Take the third to the 10th position
     */
    public static String getTime() {
        long currentTime = System.currentTimeMillis();
        String theTime = String.valueOf(currentTime);

        Log.i("Encryption link", "Current time" + theTime);
        String jiqquTime = theTime.substring(2, 10);
        appTime = Long.parseLong(jiqquTime);

        Log.i("Encryption link", "Interception time" + jiqquTime);
        String eightTime = theTime.substring(2, 10);
//        String zeroTime=eightTime.replace("2","0");
//        String newTime=zeroTime+"00000000";
        return eightTime;
    }

    /**
     * The byte array encrypted based on time and Bluetooth name takes 16 bits
     */
    public static byte[] getEncryptedData(String passWord) {
        return AES.AESgenerator(getTime(), passWord);
    }

}
