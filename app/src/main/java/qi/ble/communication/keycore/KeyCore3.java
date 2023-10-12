package qi.ble.communication.keycore;

// import android.util.Log;

import static qi.ble.communication.keycore.ConvertHexByte.bytesToHexString;

import android.util.Log;

import java.util.Arrays;

/*
 * Guidance page
 * */
public class KeyCore3 {

    private static final KeyCore3 instance = new KeyCore3();

    // private constructor to avoid client applications using the constructor
    private KeyCore3() {
    }

    public static KeyCore3 getInstance() {
        return instance;
    }

    /**
     * @param blueCarSig The Bluetooth verification string sent to the hardware for the first time,
     *                   Replace this method of sending messages BlueAdapter.getInstance().sendMessage(bytesToHexString(mess));
     */
    public void sendOne(String blueCarSig) {
        byte[] data16 = BlueVerfyUtils.getEncryptedData(blueCarSig);
        final byte[] mess = DataReceive.newBlueMessage((byte) 1, (byte) 0x72, data16);
        BlueAdapter.INSTANCE.sendMessage(mess);   //Send this to the hardware
//        BlueAdapter.INSTANCE.sendMessage(bytesToHexString(mess));   //Send this to the hardware
    }

    /**
     * This method is the byte array parsed from the first parameter onCharacteristicChanged after receiving the Bluetooth packet return method byte[] megebyte = characteristic.getValue();
     * The second parameter Bluetooth verification string carSig 16-bit string
     * Finally, replace the message-sending method in this method with BlueAdapter.getInstance().sendMessage(bytesToHexString(mess)). This message is the data that will eventually be sent to the hardware side for secondary verification. If this is sent to the hardware Bluetooth, it will not be disconnected. Connected
     **/
    public void sendTwo(final byte[] megebyte, String carSig) {
        //1. Check that the verification is appropriate
        final DataReceive data = new DataReceive();
        if (megebyte == null || megebyte.length < 4) {
            return;
        }
        //1 DataType
        data.dataType = megebyte[0];
        //2 Length
        data.length = megebyte[1];
        //3 data
        if (data.length == 0 || data.length > 32 || data.length < 0) {
            return;
        }
        if (megebyte.length < data.length + 3) {
            return;//Data is not completed, waiting for download Type+length+data+check
        }
        //If there is enough data, read the data into data first
        data.data = new byte[data.length];
        byte[] newMergeByte = new byte[megebyte.length];
        for (int i = 0; i < megebyte.length; i++) {
            newMergeByte[i] += megebyte[i] & 0xff;
        }
        System.arraycopy(newMergeByte, 2, data.data, 0, data.length);
        //4.check
        data.check = megebyte[data.length + 2];
        if (data.matchCheck()) {
            if (data.dataType == 0x03) {

                Log.i("Encryption link", "Data to be decrypted" + Arrays.toString(data.data));
                if (data.data.length == 17) {
                    byte[] decodeByte = DataReceive.subBytes(data.data, 1, 16);

                    Log.i("Encryption link", "Last 16 bits of data to be decrypted" + Arrays.toString(decodeByte));
                    Log.i("Encryption link", "Decrypted password" + carSig);
                    byte[] jiemihoushuzu = AES.decrypt(decodeByte, carSig);

                    Log.i("Encryption link", "Decrypted array" + Arrays.toString(jiemihoushuzu));
                    Log.i("Encryption link", "Decrypted string" + bytesToHexString(jiemihoushuzu));
                    byte[] byteAppTime = DataReceive.subBytes(jiemihoushuzu, 0, 8);
                    byte[] byteBlueTime = DataReceive.subBytes(jiemihoushuzu, 8, 8);
                    String appTime = new String(byteAppTime);
                    Log.i("Encryption link", "Decrypted app time" + appTime);
                    String BlueTime = new String(byteBlueTime);
                    Log.i("Encryption link", "Decrypted app Bluetooth time" + BlueTime);
                    try {
                        long longAppTime = Long.parseLong(appTime);
                        if (longAppTime != 0) {
                            long timeAddOne = Long.parseLong(BlueTime) + 1;
                            String jiamiTime = String.valueOf(timeAddOne);

                            Log.i("Encryption link", "Bluetooth time to be encrypted" + jiamiTime);
                            byte[] data16 = AES.AESgenerator(jiamiTime, carSig);

                            Log.i("Encryption link", "Encrypted Bluetooth time array" + Arrays.toString(data16));
                            final byte[] mess;

                            mess = DataReceive.newBlueMessage((byte) 1, (byte) 0x73, data16);
                            Log.i("Encryption link", "Final byte time + 1" + Arrays.toString(mess));
                            Log.i("Encryption link", "Final hextsr time + 1" + bytesToHexString(mess));
                            //Replace this method of sending messages and send the second verification
                            BlueAdapter.INSTANCE.sendMessage(mess);
//                            BlueAdapter.INSTANCE.sendMessage(bytesToHexString(mess));
                        }
                    } catch (NumberFormatException e) {
                        Log.e("Encryption link", "The decrypted app time encountered an error");
                    }
                }
            }
        }
    }

    public void sendCommand() {
        /*
         * Send command Send the following command to replace the following Bluetooth message method BlueAdapter.getInstance().sendMessage(controlCmdArr(0))
         *
         * */
        String[] controlCmdArr = new String[]{
                "0x82 02 00 10 6B",//start stop  Start flameout command
                "0x82 02 40 00 3B",//lock        Lock command
                "0x82 02 08 00 73",//unlock      Unlock command
                "0x82 02 00 80 FB",//backpag     tail box command
                "0x82 02 01 00 7A"//findcar      car search command
        };

        BlueAdapter.INSTANCE.sendMessage(ConvertHexByte.hexStringToBytes(controlCmdArr[0]));
//        BlueAdapter.INSTANCE.sendMessage(controlCmdArr[0]);
    }
}
