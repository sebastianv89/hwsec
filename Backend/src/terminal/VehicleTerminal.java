package terminal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import backend.ByteUtils;
import backend.ConstantValues;
import backend.MutualAuthentication;

public class VehicleTerminal {

	private static final boolean True = true;
	private RSAPrivateKey vtPrivKey;
	public RSAPublicKey vtPubKey;
	private RSAPublicKey caPubKey;
	public byte[] vtCertificate = new byte[257];
	byte[] sessionKey = null;
	terminal.Card card = new terminal.Card();
	MutualAuthentication mu = new MutualAuthentication();
	
	// Set up the card:
	// Selection APDU parameters
	public static final byte ISO7816_CLA = 0x00;
	public static final byte ISO7816_INS_SELECT = (byte) 0xA4;
	public static final byte[] APPLET_AID = "smartcar".getBytes();
	public static final CommandAPDU SELECT_APDU = new CommandAPDU(ISO7816_CLA,
			ISO7816_INS_SELECT, 0x04, 0x00, APPLET_AID);

	// Vehicle Terminal APDU parameters
	public static final byte CLA_CRYPTO = (byte) 0xCA;
	public static final byte INS_AUTH_1 = 0x20;
	public static final byte INS_AUTH_2 = 0x22;
	public static final byte INS_AUTH_3 = 0x24;
	public static final byte INS_AUTH_4 = 0x26;
	public static final byte INS_AUTH_5 = 0x28;
	public static final byte INS_AUTH_6 = 0x2A;
	public static final byte INS_VT_START = 0x40;
	public static final byte INS_VT_TICK_KM = 0x42;
	public static final byte INS_VT_STOP = 0x44;

	// Response status words (encoded as integers)
	public static final int ISO7816_SW_NO_ERROR = 0x900;
	public static final int ISO7816_SW_CONDITIONS_NOT_SATISIFIED = 0x6985;

	// Constants
	ConstantValues cv;
	ByteUtils bu;
	
	// Connection with the card
	private CardChannel applet;

	private CardThread thread;
	private Boolean ready;
	private Boolean stopThread;

	public VehicleTerminal() {
		// Read the VT keys:
		readPrivateKey("VTPrivateKey");
		readPublicKey("VTPublicKey");
		readPublicCAKey("CAPublicKey");
		readCertificate("VTCert");
		
		cv = new ConstantValues();

		ready = false;
		stopThread = false;
		thread = new CardThread();
		thread.start();

		// Wait for thread to set up the connection
		while (!ready)
			;

//		InitData data = backend.registerNewCard();
//		personalize(data.privateKey, data.caVerifKey, data.certificate);

		// card is now personalized

		stopThread = true;

	}
	
	/*
	 * To send stuff to the card
	 * TODO: Change the type of the sessionKey to whatever the symmetric key type will be
	 * 
	 * @return byte[] cardResponse
	 */
	private byte[] sendToCard(byte[] data, byte instruction, byte[] sessionKey) { 
		//TODO: Symmetric crypto with the session key
		CommandAPDU capdu;
		ResponseAPDU rapdu = null;

		try {
			// send private exponent of the smartcard signature key
			capdu = new CommandAPDU(CLA_CRYPTO, instruction, 0x00,
					0x00, data);
			rapdu = applet.transmit(capdu);
			System.out.println(rapdu);
			if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
				if (rapdu.getData() == null) { //if we got a reply
					throw new CardException(rapdu.toString());
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return rapdu.getData();

	}
	
	
	/*
	 * We receive a signal here when the card wants to ignite the vehicle
	 * (Just a simulation, we dont actually do that)
	 */
	private void startIgnition() {
		String message = new String();
		message = "ignition";
		byte[] data = message.getBytes();
		byte[] ignitionReply = sendToCard(data, INS_VT_START, sessionKey);
		// Split this block into: reply data and signature
		byte[] rData = new byte[3]; //we expect iR[0] = 1 for ignition ok, and iR[0] = 0 for not enough km, plus a short "km" (2 bytes)
		System.arraycopy(ignitionReply, 0, rData, 0, 3);
		byte[] signature = new byte[128];
		System.arraycopy(ignitionReply, 3, signature, 0, 128);
		boolean sigCheck = mu.sigVerif(rData, card.getCardModulus(), signature);
		
		if (sigCheck == true) { //if the signature matches
			// Check what the message was ("ignition ok", km) or ("not enough km")
			// Only in case of ignition ok, call startVehicle()
			byte[] km = new byte[2];
			System.arraycopy(rData, 1, km, 0, 2);
			ByteUtils bu = new ByteUtils();
			long km2 = bu.bytesToLong(km);
			card.setKilometers(km2); //TODO: Read this from the replyData
			addLogEntry(km2);
			
		} else {
			//TODO: Signature didnt match, what do we do?
		}
		
		// Call driving???
	}
	
	/*
	 * This is the driving function which will do the ticking
	 */
	private void driving() {
		// wait 3 seconds to simulate a KM driven
		try {
			wait(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Send the deduct one message to the card
		sendToCard("deduct km".getBytes(), sessionKey);
		
	}
	

	
	private boolean startVehicle() {
		return true; // VROOOOOOM!!!
	}
	
	private void addLogEntry(byte[] data) {
		FileOutputStream file;
		try {
			file = new FileOutputStream("VTLogFile", True); //True to append
			file.write(data);
			file.close();
		} catch ( IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	
	private void readCertificate(String filename) {
		FileInputStream file;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			vtCertificate = bytes;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readPrivateKey(String filename) { 
		FileInputStream file;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			vtPrivKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readPublicCAKey(String filename) { 
		FileInputStream file;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			X509EncodedKeySpec pubspec = new X509EncodedKeySpec(bytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			caPubKey = (RSAPublicKey) factory.generatePublic(pubspec);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readPublicKey(String filename) { 
		FileInputStream file;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			X509EncodedKeySpec pubspec = new X509EncodedKeySpec(bytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			vtPubKey = (RSAPublicKey) factory.generatePublic(pubspec);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	// VT -> S:cert_VT, nounce
	// S -> RT:{|cert_S,{<nounce, K_tmp>}SK_s|}EK_VT
	// RT : checks that expiration <= current time
	// RT -> S:{K_VT,S}K_tmp

//	public boolean isCardPresent() {
//
//
//		TerminalFactory factory = TerminalFactory.getDefault();
//		CardTerminals ct = factory.terminals();
//		List<CardTerminal> cs;
//
//		try {
//			cs = ct.list(CardTerminals.State.CARD_PRESENT);
//			for (CardTerminal c : cs) {
//				if (c.isCardPresent()){
//					return this.ready;
//				}
//			}
//		} catch (CardException e) {
//			// Do nothing
//		}
//
//		return false;
//	}
//
//	public void IssuingCommandsHandler(VehicleTerminal vt) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
//	}
//
//
//
//	public boolean MutualAuthenticationVT_S(){
//		boolean status = true;
//		//check mutual authentication between vehicle terminal and card
//
//		return status;
//
//		//generating random number (nonce)
//		SecureRandom random = new SecureRandom();
//		byte bytes[] = new byte[20];
//		random.nextBytes(bytes);
//
//		Random rnd = Random.getInstance(Random.ALG_SECURE_RANDOM);
//		rnd.generateData(RP, (short)0, (short)16);
//	}
//
//
//
//
//	// communication with the smartcard
//	//have to create two temp buffer on smartcard for the process
//	/**
//	 * @param apdu
//	 * @param dest
//	 * @param offset
//	 * @param length
//	 */
//	private void readBuffer(APDU apdu, byte[] dest, short offset, short length) {
//		byte[] buf = apdu.getBuffer();
//		temp_short_1 = apdu.setIncomingAndReceive();
//		temp_short_2 = 0;
//		Util.arrayCopy(buf, OFFSET_CDATA, dest, offset, temp_short_1);
//		while ((short) (temp_short_2 + temp_short_1) < length) {
//			temp_short_2 += temp_short_1;
//			offset += temp_short_1;
//			temp_short_1 = (short) apdu.receiveBytes(OFFSET_CDATA);
//			Util.arrayCopy(Buff, OFFSET_CDATA, dest, offset, temp_short_1);
//		}
//
//
//		SecureRandom random = new SecureRandom();
//		byte[] random_nonce = new byte[8];
//		random.nextBytes(random_nonce);
//		capdu = new CommandAPDU(CLA_ISSUE, SET_RANDOM_DATA_NONCE, (byte) 0, (byte) 0, random_nonce, 8);
//		terminal.sendCommandAPDU(capdu); 
//	}
//	/*private String convertLongDateToString(long expDate){
//    Date date=new Date(expDate);
//    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");
//    String dateText = df2.format(date);
//    System.out.println(dateText);
//    return dateText;*/

	/** The CardThread handles the connection with the smartcard */
	class CardThread extends Thread {
		public void run() {
			try {
				TerminalFactory tf = TerminalFactory.getDefault();
				CardTerminals ct = tf.terminals();
				List<CardTerminal> cs = ct
						.list(CardTerminals.State.CARD_PRESENT);
				if (cs.isEmpty()) {
					System.err.println("No terminals with a card found.");
					return;
				}

				while (!stopThread) {
					try {
						for (CardTerminal c : cs) {
							if (c.isCardPresent()) {
								try {
									Card card = c.connect("*");
									try {
										applet = card.getBasicChannel();
										ResponseAPDU resp = applet
												.transmit(SELECT_APDU);
										if (resp.getSW() != 0x9000) {
											throw new Exception("Select failed");
										}

										ready = true;

										// Wait for the card to be removed
										while (c.isCardPresent() && !stopThread)
											;

										ready = false;
										break;
									} catch (Exception e) {
										System.err
												.println("Card does not contain Smartcar applet?!");
										sleep(2000);
										continue;
									}
								} catch (CardException e) {
									System.err
											.println("Couldn't connect to card!");
									sleep(2000);
									continue;
								}
							} else {
								System.err.println("No card present!");
								sleep(2000);
								continue;
							}
						}
					} catch (CardException e) {
						System.err.println("Card status problem!");
					}
				}
			} catch (Exception e) {
				ready = false;
				System.err.println("ERROR: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

}



