package qi.ble.communication.keycore;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Administrator on 2016/9/21.
 */
public class DataReceive {
    public int dataType;
    public int length;
    public byte[] data;
    public int check;//check code
    public boolean matchCheck(){
        byte result = 0;
        result+=dataType;
        result+=length;
        for(byte bt:data){
            result += bt;
        }
        result ^= 0xff;
        if(result == check)return true;
        return false;
    }

  /**
     * @param typeId instruction id
     * @param preId business id
     * @param data data
     * typeId + length(preId+data) + preId + data +OXff
     */
    public static byte[] newBlueMessage(byte typeId,byte preId,byte[] data){
        byte length = (byte) (data.length+1);
        byte[] result = new byte[length+3];
        result[0]= typeId;
        result[1]= length;
        result[2]= preId;
        System.arraycopy(data,0,result,3,data.length);
       //glue test
        byte ox = 0;
        for(byte bt:result){
            ox += bt;
        }
        ox ^= 0xff;
        result[length+2]= ox;
        return result;
    }
    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    public static byte[] calendar2Bytes() {
        Calendar calendar = new GregorianCalendar();
        int time = (int) (calendar.getTimeInMillis() / 1000);
        byte[] bytes = new byte[4];
        bytes[3] = (byte) (time & 0xFF);
        time >>= 8;

        bytes[2] = (byte) (time & 0xFF);
        time >>= 8;

        bytes[1] = (byte) (time & 0xFF);
        time >>= 8;

        bytes[0] = (byte) (time & 0xFF);
        time >>= 8;

        return bytes;
    }
}
