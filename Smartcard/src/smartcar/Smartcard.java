package smartcar;

import org.globalplatform.GPSystem;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class Smartcard extends Applet {

	// INStruction codes
	public static final byte INS_PT_PERSONALIZE = 0x10;
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
	

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Smartcard()
				.register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		// Check the state of the smartcard
		switch (GPSystem.getCardContentState()) {
		case GPSystem.APPLICATION_SELECTABLE:
			switch (buf[ISO7816.OFFSET_INS]) {
			case INS_PT_PERSONALIZE:
				personalize(apdu, buf);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		case GPSystem.SECURITY_DOMAIN_PERSONALIZED:
			switch (buf[ISO7816.OFFSET_INS]) {
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

	/**
	 * Personalizing the smartcard PT -> S: SK_S, VK_CA, cert_S SK_S: 634 bytes
	 * VK_CA: 162 bytes cert_S: 293 bytes
	 */
	private void personalize(APDU apdu, byte[] buf) {
		// TODO
		ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
	}

}
