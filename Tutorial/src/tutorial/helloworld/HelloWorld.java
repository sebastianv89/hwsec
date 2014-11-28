package tutorial.helloworld;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class HelloWorld extends Applet {

	private static byte[] hello = { 0x48, 0x65, 0x6c, 0x6c, 0x6f };

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new HelloWorld().register(bArray, (short) (bOffset + 1),
				bArray[bOffset]);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		switch (buf[ISO7816.OFFSET_INS]) {
		case (byte) 0x40:
			Util.arrayCopy(hello, (short) 0, buf, ISO7816.OFFSET_CDATA,
					(short) 5);
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 5);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

}
