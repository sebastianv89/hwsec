package terminal;

import java.util.List;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import java.security.SecureRandom;

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
	// VT -> S:cert_VT, nounce
	// S -> RT:{|cert_S,{<nounce, K_tmp>}SK_s|}EK_VT
	// RT : checks that expiration <= current time
	// RT -> S:{K_VT,S}K_tmp
	
		
		public boolean isCardPresent() {
			//check if the card is present in the vehicle terminal
			ReadWrite rw = new ReadWrite();
			List<ReadWrite> rw;
			try {
				rw = ct.list(ReadWrite.State.CARD_PRESENT);
				for (VehicleTerminal v : rw) {
					if (v.isCardPresent()){
						return this.ready;
						//if present it returns true and performs mutual authentication
					}
				}
			} catch (CardException e) {
				// Do nothing
			}
			
			return false;
		}

		public boolean MutualAuthenticationVT_S(){
			boolean status = true;
			//check mutual authentication between vehicle terminal and card
		
			return status;
			
			SecureRandom random = new SecureRandom();
			byte bytes[] = new bytes[20];
			random.nextBytes(bytes);
			
			RandomData rnd = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
			rnd.generateData(RP, (short)0, (short)16);
		}
		

	

}
