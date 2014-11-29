package backend;

import java.nio.ByteBuffer;
import java.util.Arrays;
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
	
	
}
