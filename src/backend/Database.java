package backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
		System.out.println("Success");
	}

	/**
	 * Return a ResultSet with all data about a specific card identified by the publicKey
	 * 
	 * @param publicKey
	 * @return 
	 */
	public ResultSet selectCard(byte[] publicKey) {
	    Statement stmt;
	    ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery( "SELECT * FROM card WHERE publicKey = \"" + publicKey + "\");" );
		    //rs.close();
		    stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
		
	}
	
	/**
	 * Resolves customerId into name
	 * 
	 * @param customerId
	 * @return name
	 */
	public String selectCustomer(Integer customerId) {
	      Statement stmt;
	      String name = "";
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT name FROM card WHERE customerId = \"" + customerId + "\");" );
		      while ( rs.next() ) {
		         name = rs.getString("name");
		      }
		      rs.close();
		      stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return name;
	}
	
	public void insertCustomer(String customerName) {
	    Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate( "INSERT INTO customer (name) VALUES (\"" + customerName + "\");"  );
		    stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Add a smartcard to the database
	 */
	public void addSmartcard(int customerId, long expiration, byte[] publicKey) {
	    Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate( "INSERT INTO card (customerId, expiration, revocation, publicKey) VALUES (\"" + customerId + "\", \"" + expiration + "\", \"" +
					false + "\", \"" + publicKey + "\");"  );
		    stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
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
