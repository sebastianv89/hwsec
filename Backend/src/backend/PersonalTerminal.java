package backend;

import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public class PersonalTerminal {

	// Selection APDU parameters
	public static final byte ISO7816_CLA = 0x00;
	public static final byte ISO7816_INS_SELECT = (byte) 0xA4;
	public static final byte[] APPLET_AID = "smartcar".getBytes();
	public static final CommandAPDU SELECT_APDU = new CommandAPDU(ISO7816_CLA,
			ISO7816_INS_SELECT, 0x04, 0x00, APPLET_AID);

	// Personalization APDU parameters
	public static final byte CLA_CRYPTO = (byte) 0xCA;
	public static final byte INS_PT_PERSONALIZE_SK = 0x10;
	public static final byte INS_PT_PERSONALIZE_VKCA = 0x12;
	public static final byte INS_PT_PERSONALIZE_CERT_DATA = 0x14;
	public static final byte INS_PT_PERSONALIZE_CERT_SIG = 0x16;

	// Response status words (encoded as integers)
	public static final int ISO7816_SW_NO_ERROR = 0x900;
	public static final int ISO7816_SW_CONDITIONS_NOT_SATISIFIED = 0x6985;

	// Connection with the card
	private CardChannel applet;

	private CardThread thread;
	private Boolean ready;
	private Boolean stopThread;

	// Connection with the backend
	private Backend backend;

	/**
	 * Run the Personalization Terminal (PT). Starting a PT will personalize one
	 * card and stop running as soon as it is done.
	 */
	public PersonalTerminal() {
		backend = new Backend();

		ready = false;
		stopThread = false;
		thread = new CardThread();
		thread.start();

		// Wait for thread to set up the connection
		while (!ready)
			;

		InitData data = backend.registerNewCard();
		personalize(data.privateKey, data.caVerifKey, data.certificate);

		// card is now personalized
		
		stopThread = true;
	}

	private void personalize(RSAPrivateKey signKey, RSAPublicKey caVerifKey,
			byte[] cert) {
		CommandAPDU capdu;
		ResponseAPDU rapdu;

		try {
			// send private exponent of the smartcard signature key
			byte[] exponent = getBytes(signKey.getPrivateExponent());
			capdu = new CommandAPDU(CLA_CRYPTO, INS_PT_PERSONALIZE_SK, 0x00,
					0x00, exponent);
			System.out.println(capdu);
			rapdu = applet.transmit(capdu);
			System.out.println(rapdu);
			if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
				if (rapdu.getSW() == ISO7816_SW_CONDITIONS_NOT_SATISIFIED) {
					throw new CardException(
							"Card is (probably) already personalized");
				}
				throw new CardException(rapdu.toString());
			}

			// send public modulus of the ca verification key
			byte[] modulus = getBytes(caVerifKey.getModulus());
			capdu = new CommandAPDU(CLA_CRYPTO, INS_PT_PERSONALIZE_VKCA, 0x00,
					0x00, modulus);
			System.out.println(capdu + "Data: " + capdu.getData());
			rapdu = applet.transmit(capdu);
			System.out.println(rapdu);
			if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
				throw new CardException(rapdu.toString());
			}

			// send data part of the smartcard certificate
			int certDataLen = 1 + ConstantValues.PUBMODULUS + ConstantValues.EXP_LENGTH;
			byte[] certData = new byte[certDataLen];
			System.arraycopy(cert, 0, certData, 0, certDataLen);
			capdu = new CommandAPDU(CLA_CRYPTO, INS_PT_PERSONALIZE_CERT_DATA,
					0x00, 0x00, certData);
			System.out.println(capdu + "Data: " + capdu.getData());
			rapdu = applet.transmit(capdu);
			System.out.println(rapdu);
			if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
				throw new CardException(rapdu.toString());
			}

			// send signature part of the smartcard certificate
			byte[] certSig = new byte[ConstantValues.SIG_LENGTH];
			System.arraycopy(cert, certDataLen, certSig, 0, ConstantValues.SIG_LENGTH);
			capdu = new CommandAPDU(CLA_CRYPTO, INS_PT_PERSONALIZE_CERT_SIG,
					0x00, 0x00, certSig);
			System.out.println(capdu + "Data: " + capdu.getData());
			rapdu = applet.transmit(capdu);
			System.out.println(rapdu);
			if (rapdu.getSW() != ISO7816_SW_NO_ERROR) {
				throw new CardException(rapdu.toString());
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			// Revoke the card in the backend in case personalization failed
			backend.revokeSmartcard(cert);
		}
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

	/** BigInteger to bytes (without leading zero) */
	byte[] getBytes(BigInteger big) {
		byte[] data = big.toByteArray();
		if (data[0] == 0) {
			byte[] tmp = data;
			data = new byte[tmp.length - 1];
			System.arraycopy(tmp, 1, data, 0, tmp.length - 1);
		}
		return data;
	}

	/** Get bytes in hexadecimal String */
	String toHexString(byte[] in) {
		StringBuilder out = new StringBuilder(2 * in.length);
		for (int i = 0; i < in.length; i++) {
			out.append(String.format("%02x ", (in[i] & 0xFF)));
		}
		return out.toString().toUpperCase();
	}

	public static void main(String[] arg) {
		new PersonalTerminal();
	}

}
