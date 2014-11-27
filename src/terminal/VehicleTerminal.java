package terminal;

import java.util.List;
import java.util.Random;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import java.security.SecureRandom;

import applet.APDU;
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
			
			//generating random number (nounce)
			SecureRandom random = new SecureRandom();
			byte bytes[] = new bytes[20];
			random.nextBytes(bytes);
			
			Random rnd = Random.getInstance(Random.ALG_SECURE_RANDOM);
			rnd.generateData(RP, (short)0, (short)16);
		}
		

public VehicleTerminal getVehicleTerminal() throws Exception{
	VehicleTerminal vt = null;
	TerminalFactory terminalfactory = TerminalFactory.getDefault();
	VehicleTerminals vehicleTerminals = terminalFactory.terminals();
	
	if (vehicleTerminals.list().isEmpty() == false){
}

}

// communication with the smartcard
//have to create two temp buffer on smartcard for the process
private void readBuffer(APDU apdu, byte[] dest, short offset, short length) {
	byte[] buf = apdu.getBuffer();
	temp_short_1 = apdu.setIncomingAndReceive();
	temp_short_2 = 0;
	Util.arrayCopy(buf, OFFSET_CDATA, dest, offset, temp_short_1);
	while ((short) (temp_short_2 + temp_short_1) < length) {
		temp_short_2 += temp_short_1;
		offset += temp_short_1;
		temp_short_1 = (short) apdu.receiveBytes(OFFSET_CDATA);
		Util.arrayCopy(buf, OFFSET_CDATA, dest, offset, temp_short_1);
	}
}
