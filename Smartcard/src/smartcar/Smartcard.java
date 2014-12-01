package smartcar;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class Smartcard extends Applet implements ISO7816 {

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
	public static final byte INS_AUTH_7 = 0x2C;
	public static final byte INS_VT_START = 0x40;
	public static final byte INS_VT_TICK_KM = 0x42;
	public static final byte INS_VT_STOP = 0x44;
	public static final byte INS_RT_RENEW_CERT_1 = 0x50;
	public static final byte INS_RT_RENEW_CERT_2 = 0x52;
	public static final byte INS_RT_TOPUP_KM = 0x54;
	public static final byte INS_RT_REFUND_KM = 0x56;

	// TODO: Expected message lengths
	public static final short SIZE_PT_PERSONALIZE_SK = 0x00;
	public static final short SIZE_PT_PERSONALIZE_VKCA = 0x00;
	public static final short SIZE_PT_PERSONALIZE_CERT_DATA = 0x00;
	public static final short SIZE_PT_PERSONALIZE_CERT_SIG = 0x00;
	public static final short SIZE_AUTH_1 = 0x00;
	public static final short SIZE_AUTH_2 = 0x00;
	public static final short SIZE_AUTH_3 = 0x00;
	public static final short SIZE_AUTH_4 = 0x00;
	public static final short SIZE_AUTH_5 = 0x00;
	public static final short SIZE_AUTH_6 = 0x00;
	public static final short SIZE_AUTH_7 = 0x00;
	public static final short SIZE_VT_START = 0x00;
	public static final short SIZE_VT_TICK_KM = 0x00;
	public static final short SIZE_VT_STOP = 0x00;
	public static final short SIZE_RT_RENEW_CERT = 0x00;
	public static final short SIZE_RT_TOPUP_KM_3 = 0x00;
	public static final short SIZE_RT_TOPUP_KM_7 = 0x00;
	public static final short SIZE_RT_REFUND_KM_7 = 0x00;

	// State for receiving multi-APDU data
	public static final byte STATE_INIT = 0x00;
	public static final byte STATE_PERSONALIZED = 0x01;
	public static final byte STATE_TERMINATED = 0x02;

	// Terminal type identifier (corresponds with certificate types)
	public static final byte TYPE_TERMINAL_RT = 0x01;
	public static final byte TYPE_TERMINAL_VT = 0x02;
	public static final byte TYPE_TERMINAL_PT = 0x03;

	// Messages
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

	// Permanent card state
	private byte state;

	/*
	 * Transient card state: protocolState[0]: previous instruction;
	 * protocolState[1]: terminal type; protocolState[2]: achieved multiple
	 * authentication?; protocolState[3]: ignited?
	 */
	private byte[] protocolState;

	// Crypto material
	private SecureData sd;

	// Private storage km
	private Kilometer km;

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
		protocolState = JCSystem.makeTransientByteArray((short) 4,
				JCSystem.CLEAR_ON_DESELECT);
		short maxLength = SecureData.SIZE_CERT_CARD + SecureData.SIZE_NONCE
				+ SecureData.SIZE_AES_KEY + SecureData.SIZE_RSA_SIG;
		tmp = JCSystem.makeTransientByteArray(maxLength,
				JCSystem.CLEAR_ON_DESELECT);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		byte ins = buf[OFFSET_INS];
		short lc = (short) (buf[OFFSET_LC] & 0x00FF);

		try {

			// check the instruction format (might not be necessary)
			if (buf[OFFSET_CLA] != CLA_CRYPTO) {
				setLastIns(ins);
				ISOException.throwIt(SW_CLA_NOT_SUPPORTED);
			}
			if (Util.getShort(buf, OFFSET_P1) != P1P2) {
				setLastIns(ins);
				ISOException.throwIt(SW_INCORRECT_P1P2);
			}

			// FIXME: check the lc when/before receiving, not doing this could
			// lead to bugs/vulnerabilities

			switch (state) {
			case STATE_INIT:
				switch (ins) {
				case INS_PT_PERSONALIZE_SK:
					if (lc == SIZE_PT_PERSONALIZE_SK) {
						personalizeSK(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_PT_PERSONALIZE_VKCA:
					if (getLastIns() == INS_PT_PERSONALIZE_SK
							&& lc == SIZE_PT_PERSONALIZE_VKCA) {
						personalizeVKCA(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_PT_PERSONALIZE_CERT_DATA:
					if (getLastIns() == INS_PT_PERSONALIZE_VKCA
							&& lc == SIZE_PT_PERSONALIZE_CERT_DATA) {
						personalizeCertData(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_PT_PERSONALIZE_CERT_SIG:
					if (getLastIns() == INS_PT_PERSONALIZE_CERT_DATA
							&& lc == SIZE_PT_PERSONALIZE_CERT_SIG) {
						personalizeCertSig(apdu, buf);
						sd.init();
						state = STATE_PERSONALIZED;
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				default:
					ISOException.throwIt(SW_INS_NOT_SUPPORTED);
				}
				break;
			case STATE_PERSONALIZED:
				switch (ins) {
				case INS_AUTH_1:
					if (lc == SIZE_AUTH_1) {
						authenticate1(apdu, buf);
					} else {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					break;
				case INS_AUTH_2:
					if (getLastIns() == INS_AUTH_1) {
						authenticate2(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_AUTH_3:
					if (getLastIns() == INS_AUTH_2) {
						authenticate3(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_AUTH_4:
					if (getLastIns() == INS_AUTH_3) {
						authenticate4(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_AUTH_5:
					if (getLastIns() == INS_AUTH_4) {
						authenticate5(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_AUTH_6:
					if (getLastIns() == INS_AUTH_5) {
						authenticate6(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_AUTH_7:
					if (getLastIns() == INS_AUTH_6) {
						authenticate7(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_VT_START:
					if (isAuthenticated() && getTermType() == TYPE_TERMINAL_VT) {
						startVehicle(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_VT_TICK_KM:
					if (isAuthenticated() && getTermType() == TYPE_TERMINAL_VT
							&& isIgnited()) {
						tick(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_VT_STOP:
					if (isAuthenticated() && getTermType() == TYPE_TERMINAL_VT
							&& isIgnited()) {
						stopVehicle(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_RT_RENEW_CERT_1:
					if (isAuthenticated() && getTermType() == TYPE_TERMINAL_RT
							&& getLastIns() == INS_AUTH_7) {
						renewCertificateData(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_RT_RENEW_CERT_2:
					if (isAuthenticated() && getTermType() == TYPE_TERMINAL_RT
							&& getLastIns() == INS_RT_RENEW_CERT_1) {
						renewCertificateSig(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_RT_TOPUP_KM:
					if (isAuthenticated() && getTermType() == TYPE_TERMINAL_RT
							&& getLastIns() == INS_RT_RENEW_CERT_2) {
						topup(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				case INS_RT_REFUND_KM:
					if (isAuthenticated() && getTermType() == TYPE_TERMINAL_RT
							&& getLastIns() == INS_RT_RENEW_CERT_2) {
						refund(apdu, buf);
					} else {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					break;
				default:
					ISOException.throwIt(SW_INS_NOT_SUPPORTED);
				}
				break;
			default:
				ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
			}
		} finally {
			setLastIns(ins);
		}
	}

	private void personalizeSK(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		sd.setSignKeyExp(buf, OFFSET_CDATA);
	}

	private void personalizeVKCA(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		sd.setCAVerifKeyMod(buf, OFFSET_CDATA);
	}

	private void personalizeCertData(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		sd.setCertData(buf, OFFSET_CDATA);
		sd.setSignKeyMod(buf, (short) (OFFSET_CDATA + 1));
	}

	private void personalizeCertSig(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		sd.setCertSig(buf, OFFSET_CDATA);
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
		apdu.setIncomingAndReceive();
		// read the certificate signature
		Util.arrayCopy(buf, (short) 0, tmp, SecureData.SIZE_CERT_DATA_TERM,
				SecureData.SIZE_RSA_SIG);

		// validate the terminal certificate
		if (!sd.validateCert(tmp, (short) 0, tmp,
				SecureData.SIZE_CERT_DATA_TERM)) {
			ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
		}

		// store certificate properties only after validation of the cert
		setTermType(tmp[0]);
		sd.setPubEncryptKeyMod(tmp, (short) 1);
		// move nonce to correct position
		Util.arrayCopy(tmp, SecureData.SIZE_RSA_KEY_MOD, tmp,
				SecureData.SIZE_CERT_CARD, SecureData.SIZE_NONCE);

		// create the unencrypted response and set the temporary symmetric key
		sd.createAuthResponse(tmp);

		// send back the first encrypted blob
		short len = sd.publicEncrypt(tmp, (byte) 0, buf);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 2 (part 2) */
	private void authenticate3(APDU apdu, byte[] buf) {
		short len = sd.publicEncrypt(tmp, (byte) 1, buf);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 2 (part 3) */
	private void authenticate4(APDU apdu, byte[] buf) {
		short len = sd.publicEncrypt(tmp, (byte) 2, buf);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 2 (part 4) */
	private void authenticate5(APDU apdu, byte[] buf) {
		short len = sd.publicEncrypt(tmp, (byte) 3, buf);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 2 (part 5) */
	private void authenticate6(APDU apdu, byte[] buf) {
		short len = sd.publicEncrypt(tmp, (byte) 4, buf);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 4 */
	private void authenticate7(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, OFFSET_CDATA,
				SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		if (len != SecureData.SIZE_AES_KEY) {
			ISOException.throwIt(SW_DATA_INVALID);
		}
		sd.setSessionKey(tmp, (short) 0);
		setAuthenticated(true);
	}

	/** Protocol 6.6 (vehicle ignition) */
	private void startVehicle(APDU apdu, byte[] buf) {
		// read ignition message (required for authentication)
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, (short) 0,
				SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		if (len != 1 || tmp[0] != MSG_IGNITION) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		if (km.start()) {
			setIgnited(true);

			// reply "ignition ok"
			tmp[0] = MSG_IGNITION_OK;
			Util.setShort(tmp, (short) 1, km.getKm());
			len = 3;
			sd.sign(tmp, (short) 0, len, tmp, len);
			len += SecureData.SIZE_RSA_SIG;
			len = sd.sessionEncrypt(tmp, (short) 0, len, buf, (short) 0);
			apdu.setOutgoingAndSend((short) 0, len);
		} else {
			// reply "not enough km"
			tmp[0] = MSG_NOT_ENOUGH_KM;
			len = 1;
			sd.sign(tmp, (short) 0, len, tmp, len);
			len += SecureData.SIZE_RSA_SIG;
			len = sd.sessionEncrypt(tmp, (short) 0, len, buf, (short) 0);
			apdu.setOutgoingAndSend((short) 0, len);
		}
	}

	/** Protocol 6.7 (driving) */
	private void tick(APDU apdu, byte[] buf) {
		// read tick message (required for authentication)
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, (short) 0,
				SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		if (len != 1 || tmp[0] != MSG_DEDUCT_KM) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		if (km.tick()) {
			// reply "deduct km ok"
			tmp[0] = MSG_DEDUCT_OK;
			Util.setShort(tmp, (short) 1, km.getKm());
			len = 3;
			sd.sign(tmp, (short) 0, len, tmp, len);
			len += SecureData.SIZE_RSA_SIG;
			len = sd.sessionEncrypt(tmp, (short) 0, len, buf, (short) 0);
			apdu.setOutgoingAndSend((short) 0, len);
		} else {
			// reply "not enough km"
			tmp[0] = MSG_NOT_ENOUGH_KM;
			len = 1;
			sd.sign(tmp, (short) 0, len, tmp, len);
			len += SecureData.SIZE_RSA_SIG;
			len = sd.sessionEncrypt(tmp, (short) 0, len, buf, (short) 0);
			apdu.setOutgoingAndSend((short) 0, len);
		}
	}

	/** Protocol 6.8 (stopping the vehicle) */
	private void stopVehicle(APDU apdu, byte[] buf) {
		// read ignition message (required for authentication)
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, (short) 0,
				SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		if (len != 1 || tmp[0] != MSG_STOP) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		// step 4
		sd.destroySessionKey();
		setAuthenticated(false);
		setIgnited(false);

		// step 5
		km.stop();

		// step 6: reply "stop ok"
		len = 1;
		tmp[0] = MSG_STOP_OK;
		len += sd.sign(tmp, (short) 0, len, tmp, len);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/**
	 * Protocol 6.9 (renewing the certificate), step 4 (part 1). Protocol
	 * 6.10/6.11 (topup/refund)
	 */
	private void renewCertificateData(APDU apdu, byte[] buf) {
		// FIXME: in our protocol description, this message is not encrypted
		// with the session key
		// step 4
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, (short) 0,
				SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		if (len != SecureData.SIZE_CERT_DATA_CARD) {
			ISOException.throwIt(SW_DATA_INVALID);
		}
	}

	/**
	 * Protocol 6.9 (renewing the certificate), step 4 (part 2) and 5. Protocol
	 * 6.10/6.11 (topup/refund), step 3
	 */
	private void renewCertificateSig(APDU apdu, byte[] buf) {
		// FIXME: in our protocol description, this message is not encrypted
		// with the session key
		// 6.9: step 4 (part 2)
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, (short) 0,
				SecureData.SIZE_AES_BLOCKSIZE, tmp,
				SecureData.SIZE_CERT_DATA_CARD);

		// check the certificate by validating its correctness
		if (len != SecureData.SIZE_RSA_SIG
				|| !sd.validateCert(tmp, (short) 0, tmp,
						SecureData.SIZE_CERT_DATA_CARD)) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		// 6.9: step 5
		sd.setCert(tmp, (short) (0));

		// 6.10/6.11: step 3
		Util.setShort(tmp, (short) 0, km.getKm());
		len = 2;
		sd.sign(tmp, (short) 0, len, tmp, len);
		len += SecureData.SIZE_RSA_SIG;
		len = sd.sessionEncrypt(tmp, (short) 0, len, buf, (short) 0);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocol 6.10 (topup), step 7-9 */
	private void topup(APDU apdu, byte[] buf) {
		// step 7
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, (short) 0,
				SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		if (len != 3 || tmp[0] != MSG_TOPUP) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		short topupAmount = Util.getShort(tmp, (short) 1);
		if (!km.topupAllowed(topupAmount)) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		// step 8
		len = 1;
		tmp[0] = MSG_TOPUP_OK;
		Util.setShort(tmp, len, km.getKm());
		len += 2;
		sd.sign(tmp, (short) 0, len, tmp, len);
		len += SecureData.SIZE_RSA_SIG;
		len = sd.sessionEncrypt(tmp, (short) 0, len, buf, (short) 0);
		apdu.setOutgoingAndSend((short) 0, len);

		// step 9
		km.topup(topupAmount);
	}

	/** Protocol 6.11 (refund), step 8-9 */
	private void refund(APDU apdu, byte[] buf) {
		// step 7
		apdu.setIncomingAndReceive();
		short len = sd.sessionDecrypt(buf, (short) 0,
				SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		if (len != 1 || tmp[0] != MSG_REFUND) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		// step 8
		km.reset();

		// step 9
		len = 1;
		tmp[0] = MSG_REFUND_OK;
		sd.sign(tmp, (short) 0, len, tmp, len);
		len += SecureData.SIZE_RSA_SIG;
		len = sd.sessionEncrypt(tmp, (short) 0, len, buf, (short) 0);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	private void setLastIns(byte ins) {
		protocolState[0] = ins;
	}

	private byte getLastIns() {
		return protocolState[0];
	}

	private void setTermType(byte type) {
		protocolState[1] = type;
	}

	private byte getTermType() {
		return protocolState[1];
	}

	private void setAuthenticated(boolean auth) {
		protocolState[2] = (byte) (auth ? 0x01 : 0x00);
	}

	private boolean isAuthenticated() {
		return protocolState[2] == 0x01 && sd.hasSessionKey();
	}

	private void setIgnited(boolean ignited) {
		protocolState[3] = (byte) (ignited ? 0x01 : 0x00);
	}

	private boolean isIgnited() {
		return protocolState[3] == 0x01;
	}
}
