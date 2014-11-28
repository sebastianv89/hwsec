package smartcar;

import org.globalplatform.GPSystem;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class Smartcard extends Applet {

	// INStruction codes
	public static final byte INS_PT_PERSONALIZE_SK = 0x10;
	public static final byte INS_PT_PERSONALIZE_VKCA = 0x12;
	public static final byte INS_PT_PERSONALIZE_CERT_DATA = 0x14;
	public static final byte INS_PT_PERSONALIZE_CERT_SIG = 0x16;
	public static final byte INS_RT_AUTH_1 = 0x20;
	public static final byte INS_RT_AUTH_2 = 0x22;
	public static final byte INS_RT_AUTH_4 = 0x24;
	public static final byte INS_VT_AUTH_1 = 0x30;
	public static final byte INS_VT_AUTH_2 = 0x32;
	public static final byte INS_VT_AUTH_4 = 0x34;
	public static final byte INS_VT_START = 0x40;
	public static final byte INS_VT_TICK_KM = 0x42;
	public static final byte INS_VT_STOP = 0x44;
	public static final byte INS_RT_RENEW_CERT = 0x50;
	public static final byte INS_RT_TOPUP_KM_3 = 0x52;
	public static final byte INS_RT_TOPUP_KM_7 = 0x54;
	public static final byte INS_RT_REFUND_KM_7 = 0x56;

	// State variable for receiving multi-APDU data
	public static final byte STATE_INIT = 0x00;
	public static final byte STATE_PERSONALIZED = 0x01;

	// Crypto material
	private SecureData storage;

	// Private storage km
	private short km;

	// Card state
	private byte state;

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Smartcard()
				.register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	private Smartcard() {
		storage = new SecureData();
		state = STATE_INIT;
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		byte ins = buf[ISO7816.OFFSET_INS];
		short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);

		// FIXME: check the length when/before receiving, this could lead to
		// bugs/vulnerabilities

		// Check the state of the smartcard
		switch (state) {
		case STATE_INIT:
			switch (ins) {
			case INS_PT_PERSONALIZE_SK:
				apdu.setIncomingAndReceive();
				storage.setSignKeyExp(buf, ISO7816.OFFSET_CDATA);
				break;
			case INS_PT_PERSONALIZE_VKCA:
				apdu.setIncomingAndReceive();
				storage.setCAVerifKeyMod(buf, ISO7816.OFFSET_CDATA);
				break;
			case INS_PT_PERSONALIZE_CERT_DATA:
				apdu.setIncomingAndReceive();
				storage.setCertData(buf, ISO7816.OFFSET_CDATA);
				storage.setSignKeyMod(buf, (short) (ISO7816.OFFSET_CDATA + 1));
			case INS_PT_PERSONALIZE_CERT_SIG:
				apdu.setIncomingAndReceive();
				storage.setCertSig(buf, ISO7816.OFFSET_CDATA);
				state = STATE_PERSONALIZED;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		case STATE_PERSONALIZED:
			switch (ins) {
			case INS_RT_AUTH_1:
				// TODO
				ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		default:
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}

	}
}
