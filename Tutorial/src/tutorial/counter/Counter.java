package tutorial.counter;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class Counter extends Applet {

	public static final byte INS_GET_BALANCE = 0x02;
	public static final byte INS_CREDIT = 0x04;
	public static final byte INS_DEBIT = 0x06;

	public static final short MAX_BALANCE = 10000;
	public static final short MAX_CREDIT = 5000;
	public static final short MAX_DEBIT = 1000;

	private short balance;

	private Counter() {
		balance = 0;
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Counter().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		switch (buf[ISO7816.OFFSET_INS]) {
		case INS_GET_BALANCE:
			getBalance(apdu, buf);
			return;
		case INS_CREDIT:
			credit(apdu, buf);
			return;
		case INS_DEBIT:
			debit(apdu, buf);
			return;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	private void getBalance(APDU apdu, byte[] buf) {
		Util.setShort(buf, (short) 0, balance);
		apdu.setOutgoingAndSend((short) 0, (short) 2);
	}

	private void credit(APDU apdu, byte[] buf) {
		short credit;
		
		// receive data
		if (apdu.setIncomingAndReceive() != 2) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		credit = Util.getShort(buf, ISO7816.OFFSET_CDATA);
		
		// check credit amount
		if (credit <= 0 || MAX_BALANCE-balance < credit || credit > MAX_CREDIT) {
			ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		}
		
		// add credit to balance
		balance += credit;
		
		// return new balance
		getBalance(apdu, buf);
	}

	private void debit(APDU apdu, byte[] buf) {
		short debit;

		// receive data
		if (apdu.setIncomingAndReceive() != 2) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		debit = Util.getShort(buf, ISO7816.OFFSET_CDATA);

		// check debit amount
		if (debit > balance || debit <= 0 || debit > MAX_DEBIT) {
			ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		}

		// deduct the debit from the balance
		balance -= debit;

		// return the new balance
		getBalance(apdu, buf);
	}

}
