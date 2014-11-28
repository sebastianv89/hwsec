package backend;

import java.nio.ByteBuffer;
/**
 * 
 * @author Fitria
 * added this function to convert bytes to any types
 * from now long to byte is done
 * if necessary, will add bytes convertion to another types here
 *
 */

public class ByteUtils {
	static public byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
	    buffer.putLong(x);
	    return buffer.array();
	}

	static public long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}
	
}
