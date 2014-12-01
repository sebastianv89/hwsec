package smartcar;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.CryptoException;

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
	public static final byte INS_VT_START = 0x40;
	public static final byte INS_VT_TICK_KM = 0x42;
	public static final byte INS_VT_STOP = 0x44;
	public static final byte INS_RT_RENEW_CERT_1 = 0x50;
	public static final byte INS_RT_RENEW_CERT_2 = 0x52;
	public static final byte INS_RT_TOPUP_KM = 0x54;
	public static final byte INS_RT_REFUND_KM = 0x56;

	// Length value of incoming message data
	public static final short SIZE_PT_PERSONALIZE_SK = SecureData.SIZE_RSA_KEY_PRIV_EXP;
	public static final short SIZE_PT_PERSONALIZE_VKCA = SecureData.SIZE_RSA_KEY_MOD;
	public static final short SIZE_PT_PERSONALIZE_CERT_DATA = SecureData.SIZE_CERT_DATA_CARD;
	public static final short SIZE_PT_PERSONALIZE_CERT_SIG = SecureData.SIZE_RSA_SIG;
	public static final short SIZE_AUTH_1 = SecureData.SIZE_CERT_DATA_TERM;
	public static final short SIZE_AUTH_2 = SecureData.SIZE_RSA_SIG
			+ SecureData.SIZE_NONCE;
	public static final short SIZE_AUTH_6 = SecureData.SIZE_AES_BLOCKSIZE;
	// FIXME: encryption
	public static final short SIZE_VT_START = 1; // SecureData.SIZE_AES_BLOCKSIZE;
	public static final short SIZE_VT_TICK_KM = 1; // SecureData.SIZE_AES_BLOCKSIZE;
	public static final short SIZE_VT_STOP = 1; // SecureData.SIZE_AES_BLOCKSIZE;
	public static final short SIZE_RT_RENEW_CERT_1 = SecureData.SIZE_CERT_DATA_CARD;
	public static final short SIZE_RT_RENEW_CERT_2 = SecureData.SIZE_RSA_SIG;
	public static final short SIZE_RT_TOPUP_KM = 1; // SecureData.SIZE_AES_BLOCKSIZE;
	public static final short SIZE_RT_REFUND_KM = 1; // SecureData.SIZE_AES_BLOCKSIZE;

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

			switch (state) {
			case STATE_INIT:
				switch (ins) {
				case INS_PT_PERSONALIZE_SK:
					if (lc != SIZE_PT_PERSONALIZE_SK) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					personalizeSK(apdu, buf);
					break;
				case INS_PT_PERSONALIZE_VKCA:
					if (lc != SIZE_PT_PERSONALIZE_VKCA) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (getLastIns() != INS_PT_PERSONALIZE_SK) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					personalizeVKCA(apdu, buf);
					break;
				case INS_PT_PERSONALIZE_CERT_DATA:
					if (lc != SIZE_PT_PERSONALIZE_CERT_DATA) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (getLastIns() != INS_PT_PERSONALIZE_VKCA) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					personalizeCertData(apdu, buf);
					break;
				case INS_PT_PERSONALIZE_CERT_SIG:
					if (lc != SIZE_PT_PERSONALIZE_CERT_SIG) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (getLastIns() != INS_PT_PERSONALIZE_CERT_DATA) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					personalizeCertSig(apdu, buf);
					if (!sd.init()) {
						ISOException.throwIt(SW_WRONG_DATA);
					}
					state = STATE_PERSONALIZED;
					break;
				default:
					ISOException.throwIt(SW_INS_NOT_SUPPORTED);
				}
				break;
			case STATE_PERSONALIZED:
				switch (ins) {
				case INS_AUTH_1:
					if (lc != SIZE_AUTH_1) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					authenticate1(apdu, buf);
					break;
				case INS_AUTH_2:
					if (lc != SIZE_AUTH_2) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (getLastIns() != INS_AUTH_1) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					authenticate2(apdu, buf);
					break;
				case INS_AUTH_3:
					if (getLastIns() != INS_AUTH_2) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					authenticate3(apdu, buf);
					break;
				case INS_AUTH_4:
					if (getLastIns() != INS_AUTH_3) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					authenticate4(apdu, buf);
					break;
				case INS_AUTH_5:
					if (getLastIns() != INS_AUTH_4) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					authenticate5(apdu, buf);
					break;
				case INS_AUTH_6:
					if (lc != SIZE_AUTH_6) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (getLastIns() != INS_AUTH_5) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					authenticate6(apdu, buf);
					break;
				case INS_VT_START:
					if (lc != SIZE_VT_START) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (!isAuthenticated() || getTermType() != TYPE_TERMINAL_VT) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					startVehicle(apdu, buf);
					break;
				case INS_VT_TICK_KM:
					if (lc != SIZE_VT_TICK_KM) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (!isAuthenticated() || getTermType() != TYPE_TERMINAL_VT
							|| !isIgnited()) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					tick(apdu, buf);
					break;
				case INS_VT_STOP:
					if (lc != SIZE_VT_STOP) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (!isAuthenticated() || getTermType() != TYPE_TERMINAL_VT
							|| !isIgnited()) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					stopVehicle(apdu, buf);
					break;
				case INS_RT_RENEW_CERT_1:
					if (lc != SIZE_RT_RENEW_CERT_1) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (!isAuthenticated() || getTermType() != TYPE_TERMINAL_RT
							|| getLastIns() != INS_AUTH_6) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					renewCertificateData(apdu, buf);
					break;
				case INS_RT_RENEW_CERT_2:
					if (lc != SIZE_RT_RENEW_CERT_2) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (!isAuthenticated() || getTermType() != TYPE_TERMINAL_RT
							|| getLastIns() != INS_RT_RENEW_CERT_1) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					renewCertificateSig(apdu, buf);
					break;
				case INS_RT_TOPUP_KM:
					if (lc != SIZE_RT_TOPUP_KM) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (!isAuthenticated() || getTermType() != TYPE_TERMINAL_RT
							|| getLastIns() != INS_RT_RENEW_CERT_2) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					topup(apdu, buf);
					break;
				case INS_RT_REFUND_KM:
					if (lc != SIZE_RT_REFUND_KM) {
						ISOException.throwIt(SW_WRONG_LENGTH);
					}
					if (!isAuthenticated() || getTermType() != TYPE_TERMINAL_RT
							|| getLastIns() != INS_RT_RENEW_CERT_2) {
						ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
					}
					refund(apdu, buf);
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
		Util.arrayCopy(buf, (short) OFFSET_CDATA, tmp, (short) 0,
				SecureData.SIZE_CERT_DATA_TERM);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 1 (part 2) and step 2 (part 1) */
	private void authenticate2(APDU apdu, byte[] buf) {
		apdu.setIncomingAndReceive();
		// read the certificate signature
		Util.arrayCopy(buf, (short) OFFSET_CDATA, tmp,
				SecureData.SIZE_CERT_DATA_TERM, SecureData.SIZE_RSA_SIG);

		// validate the terminal certificate
		if (!sd.validateCert(tmp, (short) 0, tmp,
				SecureData.SIZE_CERT_DATA_TERM)) {
			ISOException.throwIt(SW_WRONG_DATA);
		}

		// store certificate properties only after validation of the cert
		setTermType(tmp[0]);
		sd.setPubEncryptKeyMod(tmp, (short) 1);
		// move nonce to correct position
		Util.arrayCopy(buf, SecureData.SIZE_RSA_SIG, tmp,
				SecureData.SIZE_CERT_CARD, SecureData.SIZE_NONCE);

		// create the unencrypted response and set the temporary symmetric key
		try {
			sd.createAuthResponse(tmp);
		} catch (CryptoException e) {
			Util.setShort(buf, OFFSET_CDATA, e.getReason());
			apdu.setOutgoingAndSend(OFFSET_CDATA, (short) 2);
		}

		// send back the first encrypted blob
		short len = sd.publicEncrypt(tmp, (byte) 0, buf, (short) 0);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 2 (part 2) */
	private void authenticate3(APDU apdu, byte[] buf) {
		short len = sd.publicEncrypt(tmp, (byte) 1, buf, (short) 0);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 2 (part 3) */
	private void authenticate4(APDU apdu, byte[] buf) {
		short len = sd.publicEncrypt(tmp, (byte) 2, buf, (short) 0);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 2 (part 4) */
	private void authenticate5(APDU apdu, byte[] buf) {
		short len = sd.publicEncrypt(tmp, (byte) 3, buf, (short) 0);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocols 6.4 and 6.5 (mutual auth), step 4 */
	private void authenticate6(APDU apdu, byte[] buf) {
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

		/*
		 * FIXME encryption short len = sd.sessionDecrypt(buf, (short) 0,
		 * SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		 */
		short len = 1;
		tmp[0] = buf[OFFSET_CDATA];

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
			/*
			 * FIXME: encryption len = sd.sessionEncrypt(tmp, (short) 0, len,
			 * buf, (short) 0);
			 */
			Util.arrayCopy(tmp, (short) 0, buf, (short) 0, len);
			apdu.setOutgoingAndSend((short) 0, len);
		} else {
			// reply "not enough km"
			tmp[0] = MSG_NOT_ENOUGH_KM;
			len = 1;
			sd.sign(tmp, (short) 0, len, tmp, len);
			len += SecureData.SIZE_RSA_SIG;
			/*
			 * FIXME: encryption len = sd.sessionEncrypt(tmp, (short) 0, len,
			 * buf, (short) 0);
			 */
			Util.arrayCopy(tmp, (short) 0, buf, (short) 0, len);
			apdu.setOutgoingAndSend((short) 0, len);
		}
	}

	/** Protocol 6.7 (driving) */
	private void tick(APDU apdu, byte[] buf) {
		// read tick message (required for authentication)
		apdu.setIncomingAndReceive();

		/*
		 * FIXME: decryption short len = sd.sessionDecrypt(buf, (short) 0,
		 * SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		 */
		short len = 1;
		tmp[0] = buf[OFFSET_CDATA];

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
			/*
			 * FIXME encryption len = sd.sessionEncrypt(tmp, (short) 0, len,
			 * buf, (short) 0);
			 */
			Util.arrayCopy(tmp, (short) 0, buf, (short) 0, len);
			apdu.setOutgoingAndSend((short) 0, len);
		} else {
			// reply "not enough km"
			tmp[0] = MSG_NOT_ENOUGH_KM;
			len = 1;
			sd.sign(tmp, (short) 0, len, tmp, len);
			len += SecureData.SIZE_RSA_SIG;
			/*
			 * FIXME encryption len = sd.sessionEncrypt(tmp, (short) 0, len,
			 * buf, (short) 0);
			 */
			Util.arrayCopy(tmp, (short) 0, buf, (short) 0, len);
			apdu.setOutgoingAndSend((short) 0, len);
		}
	}

	/** Protocol 6.8 (stopping the vehicle) */
	private void stopVehicle(APDU apdu, byte[] buf) {
		// read ignition message (required for authentication)
		apdu.setIncomingAndReceive();

		/*
		 * FIXME: decryption short len = sd.sessionDecrypt(buf, (short) 0,
		 * SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		 */

		short len = 1;
		tmp[0] = buf[OFFSET_CDATA];

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
		// step 4
		apdu.setIncomingAndReceive();
		Util.arrayCopy(buf, OFFSET_CDATA, tmp, (short) 0,
				SecureData.SIZE_CERT_DATA_CARD);
		if (!sd.checkCertUpdate(tmp, (short) 0)) {
			ISOException.throwIt(SW_DATA_INVALID);
		}
	}

	/**
	 * Protocol 6.9 (renewing the certificate), step 4 (part 2) and 5. Protocol
	 * 6.10/6.11 (topup/refund), step 3
	 */
	private void renewCertificateSig(APDU apdu, byte[] buf) {
		// 6.9: step 4 (part 2)
		apdu.setIncomingAndReceive();
		Util.arrayCopy(buf, OFFSET_CDATA, tmp, SecureData.SIZE_CERT_DATA_CARD,
				SecureData.SIZE_RSA_SIG);

		// check the certificate by validating its correctness
		if (!sd.validateCert(tmp, (short) 0, tmp,
				SecureData.SIZE_CERT_DATA_CARD)) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		// 6.9: step 5
		sd.setCert(tmp, (short) (0));

		// 6.10/6.11: step 3
		Util.setShort(tmp, (short) 0, km.getKm());
		short len = 2;
		sd.sign(tmp, (short) 0, len, tmp, len);
		len += SecureData.SIZE_RSA_SIG;
		/*
		 * FIXME: encryption len = sd.sessionEncrypt(tmp, (short) 0, len, buf,
		 * (short) 0);
		 */
		Util.arrayCopy(tmp, (short) 0, buf, (short) 0, len);
		apdu.setOutgoingAndSend((short) 0, len);
	}

	/** Protocol 6.10 (topup), step 7-9 */
	private void topup(APDU apdu, byte[] buf) {
		// step 7
		apdu.setIncomingAndReceive();
		/*
		 * FIXME: decryption
		 * short len = sd.sessionDecrypt(buf, (short) 0, SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		 */
		short len = 3;
		Util.arrayCopy(buf, OFFSET_CDATA, tmp, (short) 0, len);

		if (len != 3 || tmp[0] != MSG_TOPUP) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		short amount = Util.getShort(tmp, (short) 1);
		if (!km.topupAllowed(amount)) {
			ISOException.throwIt(SW_DATA_INVALID);
		}

		// step 8
		tmp[0] = MSG_TOPUP_OK;
		len = 3;
		sd.sign(tmp, (short) 0, len, tmp, len);
		len += SecureData.SIZE_RSA_SIG;
		/*
		 * FIXME: encryption len = sd.sessionEncrypt(tmp, (short) 0, len, buf,
		 * (short) 0);
		 */
		Util.arrayCopy(tmp, (short) 0, buf, (short) 0, len);
		apdu.setOutgoingAndSend((short) 0, len);

		// step 9
		km.topup(amount);
	}

	/** Protocol 6.11 (refund), step 8-9 */
	private void refund(APDU apdu, byte[] buf) {
		// step 7
		apdu.setIncomingAndReceive();
		/*
		 * FIXME: decryption short len = sd.sessionDecrypt(buf, (short) 0,
		 * SecureData.SIZE_AES_BLOCKSIZE, tmp, (short) 0);
		 */
		
		short len = 1;
		tmp[0] = buf[OFFSET_CDATA]; 

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
		/*
		 * FIXME: encryption len = sd.sessionEncrypt(tmp, (short) 0, len, buf,
		 * (short) 0);
		 */
		Util.arrayCopy(tmp, (short) 0, buf, (short) 0, len);
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
