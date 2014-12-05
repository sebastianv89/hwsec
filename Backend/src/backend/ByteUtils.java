package backend;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
/**
 * 
 * @author Fitria
 * added this function to convert bytes to any types
 * from now long to byte is done
 * if necessary, will add bytes convertion to another types here
 *
 */

public class ByteUtils {
	public byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
	    buffer.putLong(x);
	    return buffer.array();
	}

	public long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}
	
	public short bytesToShort(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.put(bytes);
		bb.flip();
		return bb.getShort();
	}
	
	public byte[] shortToBytes(short x) {
	    ByteBuffer buffer = ByteBuffer.allocate(2);
	    buffer.putShort(x);
	    return buffer.array();
	}
	
	
	/** BigInteger to bytes (without leading zero) */
	byte[] getBytes(BigInteger big) {
		byte[] data = big.toByteArray();
		if (data[0] == 0) {
			byte[] tmp = data;
			data = new byte[tmp.length - 1];
			System.arraycopy(tmp, 1, data, 0, tmp.length - 1);
		}
		return data;
	}

	/** Get bytes in hexadecimal String */
	public String toHexString(byte[] in) {
		StringBuilder out = new StringBuilder(2 * in.length);
		for (int i = 0; i < in.length; i++) {
			out.append(String.format("%02x ", (in[i] & 0xFF)));
		}
		return out.toString().toUpperCase();
	}
	
	/**
	 * Generate a random Nonces for Mutual Auth
	 * @param length: the random bytes length
	 * @return random bytes array
	 */
	public byte[] GenerateRandomBytes(int length){
		byte[] random = new byte[length];
		
		new Random().nextBytes(random);
		//System.out.println(Arrays.toString(random));
		return random;
	}
	
	
	//convert long date to string date
	public String convertLongDateToString(long expDate){
	    Date date=new Date(expDate);
	    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");
	    String dateText = df2.format(date);
	    System.out.println(dateText);
	    return dateText;
	}
	
}
