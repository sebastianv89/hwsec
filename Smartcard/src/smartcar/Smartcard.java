package smartcar;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.TransactionException;

public class Smartcard extends Applet {

	// INStruction codes
	public static final byte INS_PT_PERSONALIZE_SK = 0x10;
	public static final byte INS_PT_PERSONALIZE_VKCA = 0x12;
	public static final byte INS_PT_PERSONALIZE_CERT_DATA = 0x14;
	public static final byte INS_PT_PERSONALIZE_CERT_SIG = 0x16;
	public static final byte INS_AUTH_1 = 0x20;
	public static final byte INS_AUTH_2 = 0x22;
	public static final byte INS_AUTH_3 = 0x24;
	public static final byte INS_AUTH_4 = 0x26;
	public static final byte INS_AUTH_5 = 0x28;
	public static final byte INS_AUTH_6 = 0x2A;
	public static final byte INS_VT_START = 0x40;
	public static final byte INS_VT_TICK_KM = 0x42;
	public static final byte INS_VT_STOP = 0x44;
	public static final byte INS_RT_RENEW_CERT = 0x50;
	public static final byte INS_RT_TOPUP_KM_3 = 0x52;
	public static final byte INS_RT_TOPUP_KM_7 = 0x54;
	public static final byte INS_RT_REFUND_KM_7 = 0x56;

	// State for receiving multi-APDU data
	public static final byte STATE_INIT = 0x00;
	public static final byte STATE_PERSONALIZED = 0x01;
	public static final byte STATE_TERMINATED = 0x02;

	// Terminal type identifier
	public static final byte TYPE_TERMINAL_PT = 0x01;
	public static final byte TYPE_TERMINAL_RT = 0x02;
	public static final byte TYPE_TERMINAL_VT = 0x03;

	// Permanent card state
	private byte state;

	/**
	 *  Transient card state:
	 *    protocolState[0]: previous instruction
	 *    protocolState[1]: terminal type
	 *    protocolState[2]: is terminal authenticated?
	 */
	private byte[] protocolState;

	// Crypto material
	private SecureData storage;

	// Private storage km
	private short km;

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Smartcard()
				.register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	private Smartcard() {
		storage = new SecureData();
		state = STATE_INIT;
		protocolState = JCSystem.makeTransientByteArray((short) 3,
				JCSystem.CLEAR_ON_DESELECT);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		byte ins = buf[ISO7816.OFFSET_INS];
		short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);

		// FIXME: check the lc when/before receiving, this could lead to
		// bugs/vulnerabilities

		// FIXME: check the state when sending multiple data

		// Check the state of the smartcard
		switch (state) {
		case STATE_INIT:
			switch (ins) {
			case INS_PT_PERSONALIZE_SK:
				personalizeSK(apdu, buf);
				break;
			case INS_PT_PERSONALIZE_VKCA:
				personalizeVKCA(apdu, buf);
				break;
			case INS_PT_PERSONALIZE_CERT_DATA:
				personalizeCertData(apdu, buf);
				break;
			case INS_PT_PERSONALIZE_CERT_SIG:
				personalizeCertSig(apdu, buf);
				// FIXME: check that this is the last step in personalization
				storage.init();
				state = STATE_PERSONALIZED;
				break;
			default:
				protocolState[0] = ins;
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		case STATE_PERSONALIZED:
			switch (ins) {
			case INS_AUTH_1:
				authenticate1(apdu, buf);
				break;
			case INS_AUTH_2:
				authenticate2(apdu, buf);
				break;
			case INS_AUTH_3:
				authenticate3(apdu, buf);
				break;
			case INS_AUTH_4:
				authenticate4(apdu, buf);
				break;
			case INS_AUTH_5:
				authenticate5(apdu, buf);
				break;
			case INS_AUTH_6:
				authenticate6(apdu, buf);
				break;
			default:
				protocolState[0] = ins;
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		default:
			protocolState[0] = ins;
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		protocolState[0] = ins;
	}

	private void personalizeSK(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		storage.setSignKeyExp(buf, ISO7816.OFFSET_CDATA);
	}

	private void personalizeVKCA(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_PT_PERSONALIZE_SK) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		apdu.setIncomingAndReceive();
		storage.setCAVerifKeyMod(buf, ISO7816.OFFSET_CDATA);
	}

	private void personalizeCertData(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_PT_PERSONALIZE_VKCA) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		apdu.setIncomingAndReceive();
		storage.setCertData(buf, ISO7816.OFFSET_CDATA);
		storage.setSignKeyMod(buf, (short) (ISO7816.OFFSET_CDATA + 1));
	}

	private void personalizeCertSig(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_PT_PERSONALIZE_CERT_DATA) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		apdu.setIncomingAndReceive();
		storage.setCertSig(buf, ISO7816.OFFSET_CDATA);
	}
	
	private void authenticate1(APDU apdu, byte[] buf) {
		ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
	}
	
	private void authenticate2(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_1) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
	}
	
	private void authenticate3(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_2) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
	}
	
	private void authenticate4(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_3) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
	}
	
	private void authenticate5(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_4) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
	}
	
	private void authenticate6(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_5) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
	}
}
