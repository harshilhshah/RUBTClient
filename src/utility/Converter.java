package utility;

/**
 *  @author Harshil Shah, Krupal Suthar, Aishwariya Gondhi
 */

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Converter {
	
	private final static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
	
	/**
	 * This method formats the given byte array into a string for the url
	 * @param byteArr
	 * @return String
	 */
	public static String bytesToURL(byte[] byteArr) {

		char[] charArr = new char[byteArr.length * 2];
		for (int i = 0; i < byteArr.length; i++) {
			int v = byteArr[i] & 0xFF;
			charArr[i * 2] = HEX_CHARS[v >>> 4];
			charArr[i * 2 + 1] = HEX_CHARS[v & 0x0F];
		}
		
		String hexStr =  new String(charArr);

		int len = hexStr.length();
		charArr = new char[len + (len / 2)];
		int i = 0;
		int j = 0;
		while (i < len) {
			charArr[j++] = '%';
			charArr[j++] = hexStr.charAt(i++);
			charArr[j++] = hexStr.charAt(i++);
		}
		return new String(charArr);
	}
    
	
	/**
	 * This method helps convert almost any object into a string
	 * @param Object
	 * @return String
	 */
	
	public static String objToStr(Object o){
		
		if(o instanceof byte[]){
			return Arrays.toString((byte[])o);
		}else if(o instanceof Integer){
			return String.valueOf(o);
		} else if(o instanceof ByteBuffer){
			try {
				return new String(((ByteBuffer) o).array(),"ASCII");
			} catch (UnsupportedEncodingException e) {
				return o.toString();
			}
		}else if(o instanceof Map<?,?>){
			
			String retStr = "";
			for (Object name: ((Map<?, ?>) o).keySet()){
	            String value = objToStr(((Map<?, ?>) o).get(name));  
	            retStr += objToStr(name) + ": " + value + "\n";  
			} 
			
			return retStr;
		}else if(o instanceof List){
			
			String retStr = "";
			for(Object elem: (List<?>)o){
				retStr += objToStr(elem) + "\n";
			}
			return retStr;
		}
		return o.toString();
	}
	
	/**
     * Converts int to 4 byte big-endian
     * @param value
     * @return byte[]
     */
    public static byte[] intToByteArr(int value){
        return ByteBuffer.allocate(4).putInt(value).array();
    }

}
