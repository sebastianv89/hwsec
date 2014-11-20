package backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

	private Connection conn;

	public Database() {
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:test.db");
		} catch (ClassNotFoundException e) {
			// should never happen
			System.err.println("Class not found: " + e.getMessage());
			System.exit(0);
		} catch (SQLException e) {
			System.err.println("Error connecting to database: "
					+ e.getMessage());
			System.exit(0);
		}
	}

	/**
	 * Add a smartcard to the database
	 */
	public void addSmartcard(int customerId, long expiration, byte[] publicKey) {
		// TODO: implement
		// sql("INSERT INTO smartcards ('customerId', 'totalKm', 'expiration',
		// 'revocation', 'publicKey') VALUES ($customerId, 0, $expiration,
		// FALSE, $publicKey)");
		return;
	}
	
	/**
	 * Set the revoked flag to TRUE
	 * 
	 * @param publicKey		Used for identifying the correct card
	 */
	public void revokeSmartcard(byte[] publicKey) {
		// TODO: implement
		// sql("UPDATE smartcards SET revoked=TRUE WHERE publicKey=$publicKey");
	}
	
	/**
	 * Check the revoked status flag
	 * 
	 * @param publicKey		Used for identifying the correct card
	 * @return the status of the revoked flag
	 */
	public boolean isRevoked(byte[] publicKey) {
		// TODO: implement
		// sql("SELECT revoked FROM smartcards WHERE publicKey=$publicKey"); 
		return false;
	}

	/**
	 * Add a vehicle terminal to the database
	 * 
	 * @return terminal id
	 */
	public int addVehicleTerminal(byte[] publicKey, byte[] secretKey) {
		// TODO: implement
		// sql("INSERT INTO vehicleTerms ('publicKey', 'secretKey') VALUES
		// $publicKey, $secretKey");
		return 0;
	}

	/**
	 * Add a customer to the database
	 * 
	 * @return customer id
	 */
	public int addCustomer(String name) {
		// TODO: implement
		// customerID = sql("INSERT INTO customers ('name') VALUES $name");
		return 0;
	}

	

}
