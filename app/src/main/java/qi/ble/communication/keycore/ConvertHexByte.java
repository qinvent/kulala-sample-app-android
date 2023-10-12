package qi.ble.communication.keycore;

public class ConvertHexByte {
	
/**
* Convert byte to a byte array of length 8, each value of the array represents a bit
* positive bit value
*/
	public static byte[] getBitArray(byte b) {
		byte[] array = getBitArrayAnti(b);
		byte[] result = new byte[8];
		for (int i = 0; i <= 7; i++) {
			result[i] = array[7-i];
		}
		return result;
	}
	
/**
* Convert byte to a byte array of length 8, each value of the array represents a bit
* bit inverse value
*/
	public static byte[] getBitArrayAnti(byte b) {
		byte[] array = new byte[8];
		for (int i = 7; i >= 0; i--) {
			array[i] = (byte)(b & 1);
			b = (byte) (b >> 1);
		}
		return array;
	}
	//Are the two byte arrays equal?
	public static boolean isByteEqual(byte[] data1, byte[] data2) {
		if (data1 == null || data2 == null)return false;
		if (data1 == data2) return true;//Same address
		if(data1.length != data2.length)return false;
		for (int i = 0; i < data1.length; i++) {
			if (data1[i] != data2[i])return false;
		}
		return true;
	}
	//Convert String to uninterrupted binary array
	public static byte[] hexStringToBytesNoSpace(String hexStr) {
		if (hexStr == null || hexStr.length() == 0) return null;
		
//1 removes 0X
		String cut0X = hexStr.replace("0X", "");
		String cut0x = cut0X.replace("0x", "");
		//2Remove spaces
		String cutSpace = cut0x.replace(" ", "");
		byte[] bytes    = hexStringToBytes(cutSpace);
		return bytes;
	}



	//================================================================================
	
//Convert hexadecimal string to byte
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		
//1 removes 0X
		String cut0X = hexString.replace("0X", "");
		String cut0x = cut0X.replace("0x", "");
		//2Remove spaces
		String cutSpace = cut0x.replace(" ", "");
		String result = cutSpace.toUpperCase();
		int    length   = result.length() / 2;
		char[] hexChars = result.toCharArray();
		byte[] d        = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}


//byte to hexadecimal string
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int    v  = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString().toUpperCase();
	}
	
//byte merge
	public static byte[] bytesMege(byte[] cache,byte[] mege) {
		if(cache == null && mege == null)return null;
		if(cache == null)return mege;
		if(mege == null)return cache;
		byte[] result = new byte[cache.length+mege.length];
		System.arraycopy(cache, 0, result, 0, cache.length);
		System.arraycopy(mege, 0, result, cache.length, mege.length);
		return result;
	}
	
//byte cutting
	public static byte[] bytesCut(byte[] cache,int fromPos) {
		if(cache == null)return cache;
		if(cache.length<fromPos-1)return cache;
		byte[] result = new byte[cache.length-fromPos];
		System.arraycopy(cache, fromPos, result, 0, cache.length-fromPos);
		return result;
	}
}
