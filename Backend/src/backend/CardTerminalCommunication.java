package backend;

import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;


public class CardTerminalCommunication {
	// Selection APDU parameters
		public static final byte ISO7816_CLA = 0x00;
		public static final byte ISO7816_INS_SELECT = (byte) 0xA4;
		public static final byte[] APPLET_AID = "smartcar".getBytes();
		public static final CommandAPDU SELECT_APDU = new CommandAPDU(ISO7816_CLA,
				ISO7816_INS_SELECT, 0x04, 0x00, APPLET_AID);

		// Mutual Auth APDU parameters
		public static final byte CLA_CRYPTO = (byte) 0xCA;
		public static final byte INS_AUTH_1 = 0x20;
		public static final byte INS_AUTH_2 = 0x22;
		public static final byte INS_AUTH_3 = 0x24;
		public static final byte INS_AUTH_4 = 0x26;
		public static final byte INS_AUTH_5 = 0x28;
		public static final byte INS_AUTH_6 = 0x2A;
		
		//Rental Terminal APDU parameters		
		public static final byte INS_RT_RENEW_CERT_1 = 0x50;
		public static final byte INS_RT_RENEW_CERT_2 = 0x52;
		public static final byte INS_RT_TOPUP_KM = 0x54;
		public static final byte INS_RT_REFUND_KM = 0x56;
		
		public static final byte MSG_TOPUP = 0x4c;
		public static final byte MSG_TOPUP_OK = 0x4d;
		public static final byte MSG_REFUND = 0x4e;
		public static final byte MSG_REFUND_OK = 0x4f;
		

		// Response status words (encoded as integers)
		public static final int ISO7816_SW_NO_ERROR = 0x9000;
		public static final int ISO7816_SW_INS_NOT_SUPPORTED = 0x6D00;
		
		// Connection with the card
		private CardChannel applet;
		private ByteUtils bu;

		private CardThread thread;
		private Boolean ready;
		private Boolean stopThread;
		
		public CardTerminalCommunication(){
			StartThread();
			bu = new ByteUtils();
		}
		
		public void StartThread(){
			ready = false;
			stopThread = false;
			thread = new CardThread();
			thread.start();

			// Wait for thread to set up the connection
			while (!ready)
				;
		}
		
		public void stopThread(){
			stopThread = true;
		}
		
		
		/*
		 * To send stuff to the card
		 * TODO: Change the type of the sessionKey to whatever the symmetric key type will be
		 * 
		 * @return byte[] cardResponse
		 */
		public byte[] sendToCard(byte[] data, byte instruction) { 
			//TODO: Symmetric crypto with the session key
			CommandAPDU capdu;
			ResponseAPDU rapdu = null;

			try {
				// send private exponent of the smartcard signature key
				capdu = new CommandAPDU(CLA_CRYPTO, instruction, 0x00,
						0x00, data);
				System.out.println(capdu + " Data: " + bu.toHexString(capdu.getData()));
				rapdu = applet.transmit(capdu);
				System.out.println(rapdu + " Data: " + bu.toHexString(rapdu.getData()));
				if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
					throw new CardException(rapdu.toString());
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			return rapdu.getData();

		}
		
		
		/*
		 * To send stuff to the card
		 * TODO: Change the type of the sessionKey to whatever the symmetric key type will be
		 * 
		 * @return byte[] cardResponse
		 */
		public byte[] sendToCard(byte[] data, byte instruction, byte[] sessionKey) { 
			//TODO: Symmetric crypto with the session key
			CommandAPDU capdu;
			ResponseAPDU rapdu = null;

			try {
				// send private exponent of the smartcard signature key
				capdu = new CommandAPDU(CLA_CRYPTO, instruction, 0x00,
						0x00, data);
				System.out.println(capdu + " Data: " + bu.toHexString(capdu.getData()));
				rapdu = applet.transmit(capdu);
				System.out.println(rapdu + " Data: " + bu.toHexString(rapdu.getData()));
				if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
					throw new CardException(rapdu.toString());
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			return rapdu.getData();

		}
		
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
