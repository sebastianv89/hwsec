package terminal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.TerminalFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import applet.APDU;
import applet.Cipher;
import applet.Signature;
import backend.Backend;
import backend.InitData;

public class VehicleTerminal {
	
	private static final String RP = null;
	private boolean ready;
	private Object rsaHandler;
	byte[] tmp;
		short temp_short_1;
		short temp_short_2;
		
	Cipher cipher; //for encryption and decryption
	Signature signature_instance; // Signature instance for signing and verifying

	

	
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
	
	
	TerminalFactory factory = TerminalFactory.getDefault();
	CardTerminals ct = factory.terminals();
	List<CardTerminal> cs;
	
	try {
		cs = ct.list(CardTerminals.State.CARD_PRESENT);
		for (CardTerminal c : cs) {
			if (c.isCardPresent()){
				return this.ready;
			}
		}
	} catch (CardException e) {
		// Do nothing
	}
			
			return false;
		}
	public void IssuingCommandsHandler(VehicleTerminal vt) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
	}

		

		public boolean MutualAuthenticationVT_S(){
			boolean status = true;
			//check mutual authentication between vehicle terminal and card
		
			return status;
			
			//generating random number (nonce)
			SecureRandom random = new SecureRandom();
			byte bytes[] = new byte[20];
			random.nextBytes(bytes);
			
			Random rnd = Random.getInstance(Random.ALG_SECURE_RANDOM);
			rnd.generateData(RP, (short)0, (short)16);
		}
		



// communication with the smartcard
//have to create two temp buffer on smartcard for the process
/**
 * @param apdu
 * @param dest
 * @param offset
 * @param length
 */
private void readBuffer(APDU apdu, byte[] dest, short offset, short length) {
	byte[] buf = apdu.getBuffer();
	temp_short_1 = apdu.setIncomingAndReceive();
	temp_short_2 = 0;
	Util.arrayCopy(buf, OFFSET_CDATA, dest, offset, temp_short_1);
	while ((short) (temp_short_2 + temp_short_1) < length) {
		temp_short_2 += temp_short_1;
		offset += temp_short_1;
		temp_short_1 = (short) apdu.receiveBytes(OFFSET_CDATA);
		Util.arrayCopy(Buff, OFFSET_CDATA, dest, offset, temp_short_1);
	}


SecureRandom random = new SecureRandom();
		byte[] random_nonce = new byte[8];
		random.nextBytes(random_nonce);
		capdu = new CommandAPDU(CLA_ISSUE, SET_RANDOM_DATA_NONCE, (byte) 0, (byte) 0, random_nonce, 8);
		terminal.sendCommandAPDU(capdu); 
		}
/*private String convertLongDateToString(long expDate){
    Date date=new Date(expDate);
    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");
    String dateText = df2.format(date);
    System.out.println(dateText);
    return dateText;*/
}


	}


