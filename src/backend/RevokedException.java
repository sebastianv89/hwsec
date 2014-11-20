package backend;

public class RevokedException extends Exception {

	// generated serial UID (to shut up Eclipse)
	private static final long serialVersionUID = 7467925598945879897L;

	public RevokedException() {
		super("Smartcard was revoked");
	};
}
