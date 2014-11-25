package terminal;

import backend.Backend;
import backend.InitData;

public class VehicleTerminal {
	
	public static void main(String[] args){
		Backend bk = new Backend();
		InitData id = bk.registerVehicleTerminal();
		
		byte[] c = id.certificate;
		byte[] vk = id.caVerifKey;
		byte[] sk = id.secretKey;
		byte[] pk = id.privateKey;
		
		
	}
	

}
