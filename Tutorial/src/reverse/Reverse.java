package reverse;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class Reverse extends Applet {

	public static final byte INS_REVERSE = 0x66;
	public static final byte INS_REFLECT = 0x68;
	public static final byte INS_TRANS_SET = 0x70;
	public static final byte INS_TRANS_GET = 0x72;
	public static final byte INS_PERM_SET = 0x74;
	public static final byte INS_PERM_GET = 0x76;

	// byte transByte; // weird behaviour, don't do this
	private byte permByte; // EEPROM
	//private byte[] transByte = JCSystem.makeTransientByteArray((short) 1,
	//		JCSystem.CLEAR_ON_DESELECT);

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Reverse().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		switch (buf[ISO7816.OFFSET_INS]) {
		case INS_REFLECT:
			reflect(apdu, buf);
			break;
		case INS_REVERSE:
			reverse(apdu, buf);
			break;
		case INS_TRANS_SET:
//			transByte[0] = buf[ISO7816.OFFSET_CDATA];
			break;
		case INS_TRANS_GET:
//			buf[0] = transByte[0];
//			apdu.setOutgoingAndSend((short) 0, (short) 1);
			break;
		case INS_PERM_SET:
			permByte = buf[ISO7816.OFFSET_CDATA];
			break;
		case INS_PERM_GET:
			buf[0] = permByte;
			apdu.setOutgoingAndSend((short) 0, (short) 1);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	void reflect(APDU apdu, byte[] buf) {
		// check P1:P2 == 00:00
		if (Util.getShort(buf, ISO7816.OFFSET_P1) != 0) {
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
		// arbitrary minimum length
		short len = (short) (buf[ISO7816.OFFSET_LC] & 0XFF);
		if (len < 3) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		// receive (exactly) enough bytes
		if (apdu.setIncomingAndReceive() != len) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, len);
	}

	void reverse(APDU apdu, byte[] buf) {
		// ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);

		// check P1:P2 == 00:00
		if (Util.getShort(buf, ISO7816.OFFSET_P1) != 0) {
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}

		// prepare a transient array for reversing
		short len = (short) (buf[ISO7816.OFFSET_LC] & 0xFF);
		byte[] tmp = JCSystem.makeTransientByteArray(len,
				JCSystem.CLEAR_ON_DESELECT);

		// receive (exactly) enough bytes
		if (apdu.setIncomingAndReceive() != len) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		// nonatomic is probably faster??
		Util.arrayCopyNonAtomic(buf, ISO7816.OFFSET_CDATA, tmp, (short) 0, len);
		for (byte i = 0; i < len; ++i) {
			buf[i] = tmp[len - i - 1];
		}

		apdu.setOutgoingAndSend((short) 0, len);
	}
}
