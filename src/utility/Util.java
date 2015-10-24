package utility;

import java.util.Random;

public class Util {
	
	
	/**
	 * This method generates a random string
	 * @param int: length of the string to be generated
	 * @return String
	 */
	public static byte[] generateMyId(int len){
		final String alphaStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ) 
			sb.append(alphaStr.charAt(rnd.nextInt(alphaStr.length())));
		return sb.toString().getBytes();
	}

}
