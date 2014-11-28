package tutorial.passwdmgr;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class PasswordManager extends Applet {

	// INStructions
	public final static byte INS_ADD_PASSWORDENTRY = 0x30;
	public final static byte INS_GET_PASSWORDENTRY = 0x32;
	public final static byte INS_DELETE_PASSWORDENTRY = 0x34;
	public final static byte INS_LIST_IDENTIFIERS = 0x36;

	// Status Words (if not defined in ISO7816)
	public final static short SW_DUPLICATE_ENTRY = (short) 0x6A8A;
	public final static short SW_IDENTIFIER_NOT_FOUND = (short) 0x6A82;
	public final static short SW_THERES_MORE_WERE_THAT_CAME_FROM = (short) 0x6310;

	// Tags for structuring data (not really necessary)
	public final static byte TAG_ID = (byte) 0xf1;
	public final static byte TAG_USERNAME = (byte) 0xf2;
	public final static byte TAG_PASSWORD = (byte) 0xf3;

	// Keep track of latest not-yet-returned pe (in list identifiers action)
	// Needs to be kept in RAM, so we need a trasient array
	private Object[] current = JCSystem.makeTransientObjectArray((short) 1,
			JCSystem.CLEAR_ON_DESELECT);

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new PasswordManager().register(bArray, (short) (bOffset + 1),
				bArray[bOffset]);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		
		// reset current[0] if required
		if (buf[ISO7816.OFFSET_INS] != INS_LIST_IDENTIFIERS) {
			current[0] = null;
		}
		
		switch (buf[ISO7816.OFFSET_INS]) {
		case INS_ADD_PASSWORDENTRY:
			processAddPasswordEntry(apdu, buf);
			break;
		case INS_GET_PASSWORDENTRY:
			processGetPasswordEntry(apdu, buf);
			break;
		case INS_DELETE_PASSWORDENTRY:
			processDeletePasswordEntry(apdu, buf);
			break;
		case INS_LIST_IDENTIFIERS:
			processListIdentifiers(apdu, buf);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	// This method is doing the reading of the data through
	// apdu.setIncomingAndReceive()
	void processAddPasswordEntry(APDU apdu, byte[] buf) {
		// check P1:P2 == 00:00
		if (Util.getShort(buf, ISO7816.OFFSET_P1) != 0) {
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
		
		// This is just (buf[offset_lc] < 3), but the casting is required
		// because Java makes everything signed by default, so that 0xff < 3,
		// (because 0xff == -1)
		short len = (short)(buf[ISO7816.OFFSET_LC] & 0xFF);
		if (len < 3) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		// receive the data and check the length
		if (apdu.setIncomingAndReceive() != len) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		// check identifier
		short ofsId = ISO7816.OFFSET_CDATA;
		short ofsUsername = checkTLV(buf, ofsId, TAG_ID, PasswordEntry.SIZE_ID);
		// TODO: check if there is room for the username

		// check username
		short ofsPassword = checkTLV(buf, ofsUsername, TAG_USERNAME,
				PasswordEntry.SIZE_USERNAME);
		// TODO: check if there is room for the password

		// check password
		// TODO: check if we're at the end of the buffer data

		// check if the entry is not a duplicate
		if (PasswordEntry.search(buf, (short) (ofsId + 2), buf[ofsId + 1]) != null) {
			ISOException.throwIt(SW_DUPLICATE_ENTRY);
		}

		// (finally) add the password entry
		JCSystem.beginTransaction();
		PasswordEntry pe = PasswordEntry.getInstance();
		pe.setId(buf, (short) (ofsId + 2), buf[ofsId + 1]);
		pe.setUsername(buf, (short) (ofsUsername + 2), buf[ofsUsername + 1]);
		pe.setPassword(buf, (short) (ofsPassword + 2), buf[ofsPassword + 1]);
		JCSystem.commitTransaction();
	}

	// This method is responding with data through apdu.setOutgoingAndSend
	// The data still fits into one apdu
	void processGetPasswordEntry(APDU apdu, byte[] buf) {
		// TODO: various checks on options and length

		apdu.setIncomingAndReceive();

		// TODO: more checks on TLV

		// search for the specified pe
		PasswordEntry pe = PasswordEntry.search(buf,
				(short) (ISO7816.OFFSET_CDATA + 2),
				buf[ISO7816.OFFSET_CDATA + 1]);
		if (pe == null) {
			ISOException.throwIt(SW_IDENTIFIER_NOT_FOUND);
		}

		// prepare the outgoing data
		short outOfs = 0;
		buf[outOfs++] = TAG_USERNAME;
		byte len = pe.getUsername(buf, (short) (outOfs + 1));
		buf[outOfs++] = len;
		outOfs += len;
		buf[outOfs++] = TAG_PASSWORD;
		len = pe.getPassword(buf, (short) (outOfs + 1));
		buf[outOfs++] = len;
		outOfs += len;

		// send outgoing data
		apdu.setOutgoingAndSend((short) 0, outOfs);
	}

	// delete the entry
	void processDeletePasswordEntry(APDU apdu, byte[] buf) {
		// TODO: checks

		apdu.setIncomingAndReceive();

		// TODO: checks

		PasswordEntry pe = PasswordEntry.search(buf,
				(short) (ISO7816.OFFSET_CDATA + 2),
				buf[ISO7816.OFFSET_CDATA + 1]);
		if (pe == null) {
			ISOException.throwIt(SW_IDENTIFIER_NOT_FOUND);
		}

		pe.delete();
	}

	/**
	 * Return all identifiers, which might not fit into one APDU. Therefore, we
	 * use the P2 option and SW to indicate that more data is required.
	 */
	void processListIdentifiers(APDU apdu, byte[] buf) {
		// check options P1 and P2
		if (buf[ISO7816.OFFSET_P1] != 0x00) {
			current[0] = null;
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
		if (buf[ISO7816.OFFSET_P2] == 0x00) {
			current[0] = PasswordEntry.getFirst();
		} else if (buf[ISO7816.OFFSET_P2] != 0x01 || current[0] == null) {
			current[0] = null;
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
		
		if (current[0] == null) {
			// database empty
			ISOException.throwIt(SW_IDENTIFIER_NOT_FOUND);
		}

		// prepare the outgoing data
		short ofs = 0;
		while (current[0] != null) {
			// send only complete identifiers, until the next one won't fit
			// anymore
			byte len = ((PasswordEntry) current[0]).getIdLength();
			if ((short) ((short) (ofs + len) + 2) > 255) {
				break;
			}
			buf[ofs++] = TAG_ID;
			buf[ofs++] = len;
			((PasswordEntry) current[0]).getId(buf, ofs);
			ofs += len;

			current[0] = ((PasswordEntry) current[0]).getNext();
		}

		apdu.setOutgoingAndSend((short) 0, ofs);
		if (current[0] != null) {
			ISOException.throwIt(SW_THERES_MORE_WERE_THAT_CAME_FROM);
		}
	}

	// helper function: check if tag matches, return place of next tlv
	short checkTLV(byte[] buf, short ofs, byte tag, short maxlen) {
		if (buf[ofs++] != tag) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		short len = buf[ofs++];
		if (len > maxlen) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		return (short) (ofs + len);
	}

}
