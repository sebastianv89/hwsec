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
	
	// Messages (_OK or errors are replies, MSG_IGNITION etc. are to be sent by the terminal)
	public static final byte MSG_IGNITION = 0x45;
	public static final byte MSG_IGNITION_OK = 0x46;
	public static final byte MSG_DEDUCT_KM = 0x47;
	public static final byte MSG_DEDUCT_OK = 0x48;
	public static final byte MSG_NOT_ENOUGH_KM = 0x49;
	public static final byte MSG_STOP = 0x4a;
	public static final byte MSG_STOP_OK = 0x4b;
	public static final byte MSG_TOPUP = 0x4c;
	public static final byte MSG_TOPUP_OK = 0x4d;
	public static final byte MSG_REFUND = 0x4e;
	public static final byte MSG_REFUND_OK = 0x4f;

	// Response status words (encoded as integers)
	public static final int ISO7816_SW_NO_ERROR = 0x900;
	public static final int ISO7816_SW_CONDITIONS_NOT_SATISIFIED = 0x6985;

	// Constants
	ConstantValues cv;
	ByteUtils bu;
	
	// Connection with the card
	private CardChannel applet;

	private CardThread thread;
	private boolean ready;
	private boolean stopThread;

	public VehicleTerminal() {
		// Read the VT keys:
		readPrivateKey("VTPrivateKey");
		readPublicKey("VTPublicKey");
		readPublicCAKey("CAPublicKey");
		readCertificate("VTCert");
		
		cv = new ConstantValues();
		bu = new ByteUtils();

		ready = false;
		stopThread = false;
		thread = new CardThread();
		thread.start();

		// Wait for thread to set up the connection
		while (!ready)
			;

		mutualAuthentication();
		startIgnition();
		driving();

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
			System.out.println(capdu + " Data: " + bu.toHexString(data));
			rapdu = applet.transmit(capdu);
			System.out.println(rapdu);
			if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
				if (rapdu.getData() != null) { //if we got a reply
					//decrypt the rapdu
				} else if (rapdu.getData()[0] == MSG_STOP_OK) {
					// dont decrypt the rapdu
				} else { // error
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
	 * Mutual authentication
	 * Authenticates with the card and sets the card modulus in a Card object
	 */
	private void mutualAuthentication() {
//		MutualAuthentication mu = new MutualAuthentication();
		byte[] cert = mu.TerminalMutualAuth(vtCertificate, vtPrivKey);
		// get the card pubkey (=mod) from the certificate
		byte[] cardmod = new byte[128];
		System.arraycopy(cert, 1, cardmod, 0, 128);
		card.setCardModulus(cardmod);
	}
	
	
	/*
	 * We receive a signal here when the card wants to ignite the vehicle
	 */
	private void startIgnition() {
		byte[] ignition = new byte[1];
		ignition[0] = MSG_IGNITION;
		byte[] ignitionReply = sendToCard(ignition, INS_VT_START, sessionKey);
		
		// Split this block into: reply data and signature
		byte[] rData = new byte[3]; //we expect iR[0] for the status msg, plus a short "km" (2 bytes)
		System.out.println(ignitionReply.length);
		
		if (rData[0] == MSG_IGNITION_OK ) { // Iginition ok
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
				short km2 = bu.bytesToShort(km);
				card.setKilometers(km2);
				addLogEntry(km, signature);

			} else {
				//TODO: Signature didnt match, abort ABORT LEAVE THE SHIP NOW!!!
				card.setSessionKey(null);
				//TODO: Display message
			}
		} else if (rData[0] == MSG_NOT_ENOUGH_KM ) {
			byte[] signature = new byte[128];
			System.arraycopy(ignitionReply, 1, signature, 0, 128);
			addLogEntry("Not enough km".getBytes(), signature);
		}
	}
	
	/*
	 * This is the driving function which will do the ticking
	 */
	private void driving() {
		// Send the deduct one message to the card
		byte[] message = new byte[1];
		message[0] = MSG_DEDUCT_KM;
		byte[] cardreply = sendToCard(message, INS_VT_TICK_KM, sessionKey);
		byte[] rData = new byte[3]; // 1 byte status, 2 bytes (short) km
		System.arraycopy(cardreply, 0, rData, 0, 3);
		byte[] signature = new byte[128];
		System.arraycopy(cardreply, 3, signature, 0, 128);
		boolean sigCheck = mu.sigVerif(rData, card.getCardModulus(), signature);
		
		//TODO: Start a timer and perform safestop after 10seconds no reply from the card!
		
		if (sigCheck == true) {
			//rData = signed status message MSG_DEDUCT_OK or MSG_NOT_ENOUGH_KM, plus a short with the new km (2bytes)
			if (rData[0] == MSG_DEDUCT_OK) {
				short oldKm = (short) card.getKilometers();
				System.out.println("Old km: " + oldKm);
				byte[] newKm = new byte[2];
				System.arraycopy(rData, 1, newKm, 0, 2);
				short sNewKm = bu.bytesToShort(newKm);
				System.out.println("New km: " + sNewKm);
				if (sNewKm == (oldKm - 1)) {
					addLogEntry(rData); // log the reply, we're good
				} else { //card did not deduct correctly!
					safeStop();
				}
			} else if (rData[0] == MSG_NOT_ENOUGH_KM) {
				// Nothing left on the card, log it and perform safe stop
				addLogEntry(rData, signature);
				System.out.println("Not enough km, performing safe stop");
				safeStop();
			}
		} else { //Signature check failed!
			safeStop();
		}
		
	}
	
	/*
	 * stopVehicle() is a "correct" stop when the key is turned 
	 */
	private void stopVehicle() {
		byte[] message = new byte[1];
		message[0] = MSG_STOP;
		byte[] cardreply = sendToCard(message, INS_VT_STOP, sessionKey);
		
		// Before we do anything, destroy the session key
		card.setSessionKey(null);
		
		byte[] rData = new byte[1]; // 1 byte status
		System.arraycopy(cardreply, 0, rData, 0, 1);
		byte[] signature = new byte[128];
		System.arraycopy(cardreply, 1, signature, 0, 128);
		boolean sigCheck = mu.sigVerif(rData, card.getCardModulus(), signature);
		
		if (sigCheck == true) {
			if (rData[0] == MSG_STOP_OK) {
				addLogEntry(rData, signature);
				stopThread = true;
				//TODO: Display a message that it's safe to remove the card
			} else { 
				//TODO: Got a different message as what we expected, ABORT ALL THE THINGS!
				card.setSessionKey(null);
				stopThread = true;
				//TODO: Display a message that the usr should re-insert the card
			}
		} else {
			//TODO: Signature check failed, ABORT!
			card.setSessionKey(null);
			stopThread = true;
			//TODO: Display a message that the user should re-insert the card
		}
		
		
		
	}

	private void safeStop() {
		//perform a safeStop
		// Destroy session key and stop the communication with the card
		card.setSessionKey(null);
		stopThread = true;
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
			e.printStackTrace();
		}
	}
	
	private void addLogEntry(byte[] data, byte[] signature) {
		FileOutputStream file;
		try {
			file = new FileOutputStream("VTLogFile", True); //True to append
			file.write(data);
			file.write(signature);
			file.close();
		} catch ( IOException e) {
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
			e.printStackTrace();
		} catch (IOException e) {
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
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
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
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}


	/** The CardThread handles the connection with the smartcard */
	class CardThread extends Thread {
		public void run() {
			try {
				TerminalFactory tf = TerminalFactory.getDefault();
				CardTerminals ct = tf.terminals();
				List<CardTerminal> cs = ct.list(CardTerminals.State.CARD_PRESENT);
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

	
	public static void main(String[] args) {
		new VehicleTerminal();
		
	}
}



