package tutorial.passwdmgr;

import javacard.framework.JCSystem;
import javacard.framework.Util;

/**
 * Declare the PasswordEntry data, but also construct a linked-list that chains
 * them together.
 */
public class PasswordEntry {

	// Maximum lengths
	public static final byte SIZE_ID = 16;
	public static final byte SIZE_USERNAME = 24;
	public static final byte SIZE_PASSWORD = 16;

	// Data values
	private byte[] id;
	private byte[] username;
	private byte[] password;

	// Data value lengths
	private byte idLength;
	private byte usernameLength;
	private byte passwordLength;

	// Linked list variables
	private PasswordEntry next;
	private static PasswordEntry first;
	private static PasswordEntry deleted;

	// TODO: could be improved with a transaction. Right now, data could be
	// allocated but not added to the list (because first is not updated).
	private PasswordEntry() {
		// allocate data fields
		id = new byte[SIZE_ID];
		username = new byte[SIZE_USERNAME];
		password = new byte[SIZE_PASSWORD];

		// update linked list
		next = first;
		first = this;
	}

	// factory pattern replaced constructor
	static PasswordEntry getInstance() {
		if (deleted == null) {
			// no elements to recycle
			return new PasswordEntry();
		}

		// recycle deleted element
		PasswordEntry recycled = deleted;
		// IMPORTANT: no nested transactions
		// JCSystem.beginTransaction();
		deleted = recycled.next;
		recycled.next = first;
		first = recycled;
		// JCSystem.commitTransaction();
		return recycled;
	}

	static PasswordEntry search(byte[] buf, short ofs, byte len) {
		for (PasswordEntry pe = first; pe != null; pe = pe.next) {
			if (pe.idLength != len) {
				continue;
			}
			if (Util.arrayCompare(pe.id, (short) 0, buf, ofs, len) == 0) {
				// found it
				return pe;
			}
		}
		// element is not in list
		return null;
	}

	// removes itself from the list
	private void remove() {
		if (first == this) {
			first = this.next;
		} else {
			for (PasswordEntry pe = first; pe != null; pe = pe.next) {
				if (pe.next == this) {
					pe.next = this.next;
				}
			}
		}
	}

	// recycle itself
	private void recycle() {
		this.next = deleted;
		deleted = this;
	}

	// the actual deletion of an entry
	void delete() {
		JCSystem.beginTransaction();
		remove();
		recycle();
		JCSystem.commitTransaction();
	}

	static PasswordEntry getFirst() {
		return first;
	}

	PasswordEntry getNext() {
		return next;
	}

	byte getId(byte[] buf, short ofs) {
		Util.arrayCopy(id, (short) 0, buf, ofs, idLength);
		return idLength;
	}

	void setId(byte[] buf, short ofs, byte len) {
		idLength = len;
		Util.arrayCopy(buf, ofs, id, (short) 0, len);
	}

	byte getIdLength() {
		return idLength;
	}

	byte getUsername(byte[] buf, short ofs) {
		Util.arrayCopy(username, (short) 0, buf, ofs, usernameLength);
		return usernameLength;
	}

	void setUsername(byte[] buf, short ofs, byte len) {
		usernameLength = len;
		Util.arrayCopy(buf, ofs, username, (short) 0, len);
	}

	byte getPassword(byte[] buf, short ofs) {
		Util.arrayCopy(password, (short) 0, buf, ofs, passwordLength);
		return passwordLength;
	}

	void setPassword(byte[] buf, short ofs, byte len) {
		passwordLength = len;
		Util.arrayCopy(buf, ofs, password, (short) 0, len);
	}

}
