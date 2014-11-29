package smartcar;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class Smartcard extends Applet {

	// Default format of APDUs
	public static final byte CLA_CRYPTO = (byte) 0xCA;
	public static final short P1P2 = (short) 0x0000;

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

	// Expected message lengths
	public static final short SIZE_PT_PERSONALIZE_SK = 0x10;
	public static final short SIZE_PT_PERSONALIZE_VKCA = 0x12;
	public static final short SIZE_PT_PERSONALIZE_CERT_DATA = 0x14;
	public static final short SIZE_PT_PERSONALIZE_CERT_SIG = 0x16;
	public static final short SIZE_AUTH_1 = 0x20;
	public static final short SIZE_AUTH_2 = 0x22;
	public static final short SIZE_AUTH_3 = 0x24;
	public static final short SIZE_AUTH_4 = 0x26;
	public static final short SIZE_AUTH_5 = 0x28;
	public static final short SIZE_AUTH_6 = 0x2A;
	public static final short SIZE_VT_START = 0x40;
	public static final short SIZE_VT_TICK_KM = 0x42;
	public static final short SIZE_VT_STOP = 0x44;
	public static final short SIZE_RT_RENEW_CERT = 0x50;
	public static final short SIZE_RT_TOPUP_KM_3 = 0x52;
	public static final short SIZE_RT_TOPUP_KM_7 = 0x54;
	public static final short SIZE_RT_REFUND_KM_7 = 0x56;

	// State for receiving multi-APDU data
	public static final byte STATE_INIT = 0x00;
	public static final byte STATE_PERSONALIZED = 0x01;
	public static final byte STATE_TERMINATED = 0x02;

	// Terminal type identifier (corresponds with certificate types)
	public static final byte TYPE_TERMINAL_RT = 0x01;
	public static final byte TYPE_TERMINAL_VT = 0x02;
	// public static final byte TYPE_TERMINAL_PT = 0x03;

	// Permanent card state
	private byte state;

	/*
	 * Transient card state: protocolState[0]: previous instruction;
	 * protocolState[1]: terminal type; protocolState[2]: achieved multiple
	 * authentication?
	 */
	private byte[] protocolState;

	// Crypto material
	private SecureData sd;

	// Private storage km
	private short km;

	// RAM buffer
	byte[] tmp;

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Smartcard()
				.register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	private Smartcard() {
		sd = new SecureData();
		state = STATE_INIT;
		protocolState = JCSystem.makeTransientByteArray((short) 3,
				JCSystem.CLEAR_ON_DESELECT);
		tmp = JCSystem.makeTransientByteArray((short) 0x0300,
				JCSystem.CLEAR_ON_DESELECT); // TODO: tighter length
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		byte ins = buf[ISO7816.OFFSET_INS];
		short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);

		try {

			// check the instruction format (might not be necessary)
			if (buf[ISO7816.OFFSET_CLA] != CLA_CRYPTO) {
				protocolState[0] = ins;
				ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
			}
			if (Util.getShort(buf, ISO7816.OFFSET_P1) != P1P2) {
				protocolState[0] = ins;
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
			}

			// FIXME: check the lc when/before receiving, not doing this could
			// lead
			// to bugs/vulnerabilities

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
					sd.init();
					state = STATE_PERSONALIZED;
					break;
				default:
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
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
				}
				break;
			default:
				ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
			}
		} finally {
			protocolState[0] = ins;
		}
	}

	private void personalizeSK(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		sd.setSignKeyExp(buf, ISO7816.OFFSET_CDATA);
	}

	private void personalizeVKCA(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_PT_PERSONALIZE_SK) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		apdu.setIncomingAndReceive();
		sd.setCAVerifKeyMod(buf, ISO7816.OFFSET_CDATA);
	}

	private void personalizeCertData(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_PT_PERSONALIZE_VKCA) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		apdu.setIncomingAndReceive();
		sd.setCertData(buf, ISO7816.OFFSET_CDATA);
		sd.setSignKeyMod(buf, (short) (ISO7816.OFFSET_CDATA + 1));
	}

	private void personalizeCertSig(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_PT_PERSONALIZE_CERT_DATA) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		apdu.setIncomingAndReceive();
		sd.setCertSig(buf, ISO7816.OFFSET_CDATA);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 1 (part 1) */
	private void authenticate1(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		// read the certificate data
		Util.arrayCopy(buf, (short) 0, tmp, (short) 0,
				SecureData.SIZE_CERT_DATA_TERM);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 1 (part 2) and step 2 (part 1) */
	private void authenticate2(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_2) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}

		apdu.setIncomingAndReceive();
		// read the certificate signature
		Util.arrayCopy(buf, (short) 0, tmp, SecureData.SIZE_CERT_DATA_TERM,
				SecureData.SIZE_RSA_SIG);

		// validate the terminal certificate
		if (!sd.validateCert(tmp, (short) 0, tmp,
				SecureData.SIZE_CERT_DATA_TERM)) {
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}

		// store certificate properties only after validation of the cert
		protocolState[1] = tmp[0]; // set type of terminal
		sd.setTermEncryptKeyMod(tmp, (short) 1);
		// move nonce to correct position
		Util.arrayCopy(tmp, SecureData.SIZE_RSA_KEY_MOD, tmp,
				SecureData.SIZE_CERT_CARD, SecureData.SIZE_NONCE);

		// create the unencrypted response
		sd.createAuthResponse(tmp);

		// send back the first encrypted blob
		sd.publicEncrypt(tmp, (byte) 0, buf);
		apdu.setOutgoingAndSend((short) 0, SecureData.SIZE_PUBENC_CIPH);
	}

	private void authenticate3(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_2) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		sd.publicEncrypt(tmp, (byte) 0, buf);
		apdu.setOutgoingAndSend((short) 0, SecureData.SIZE_PUBENC_CIPH);
	}

	private void authenticate4(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_3) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		sd.publicEncrypt(tmp, (byte) 0, buf);
		apdu.setOutgoingAndSend((short) 0, SecureData.SIZE_PUBENC_CIPH);
	}

	private void authenticate5(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_4) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		sd.publicEncrypt(tmp, (byte) 0, buf);
		apdu.setOutgoingAndSend((short) 0, SecureData.SIZE_PUBENC_CIPH);
	}

	private void authenticate6(APDU apdu, byte[] buf) {
		if (protocolState[0] != INS_AUTH_5) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		sd.publicEncrypt(tmp, (byte) 0, buf);
		apdu.setOutgoingAndSend((short) 0, SecureData.SIZE_PUBENC_CIPH);
	}
}
