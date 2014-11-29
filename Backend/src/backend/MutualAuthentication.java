package backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MutualAuthentication {
	ByteUtils util = new ByteUtils();
	ConstantValues CV = new ConstantValues();
	
	int NOUNCE_LENGTH = 16;
	
	public MutualAuthentication(){
		
	}
	
	/**
	 * Rental Terminal Authentication:
	 * 1. Read RT Certificate from file
	 * 2. Generate Nonce
	 * 3. Combine those two and split them into two package
	 * 4. Received 2 package from smartcard
	 * 5. 
	 */
	
	public void RentalTerminalMutualAuth(){
		
		byte[] rtCert = readFiles("RTCert");  //Read Certificate for Rental Terminal
		byte[] nounce = util.GenerateRandomBytes(NOUNCE_LENGTH); //generate nonces
		
		//Split the certificate into two package
		byte[] pack1 = new byte[CV.PUBMODULUS + 1];
		System.arraycopy(rtCert, 0, pack1, 0, pack1.length); 
		byte[] pack2 = new byte[CV.SIG_LENGTH + NOUNCE_LENGTH];
		System.arraycopy(rtCert, CV.PUBMODULUS+1, pack2, 0, CV.SIG_LENGTH);
		System.arraycopy(nounce, 0, pack2, CV.SIG_LENGTH, nounce.length);
		
	}
	
	public static void main(String[] args) {
		MutualAuthentication ma = new MutualAuthentication();
		ma.RentalTerminalMutualAuth();
	}
	
	private byte[] readFiles(String filename) {
		FileInputStream file;
		byte[] bytes = null;
		try {
			file = new FileInputStream(filename);
			bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bytes;
	}

}
