package backend;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
	}

	/**
	 * Return a ResultSet with all data about a specific card identified by the publicKey
	 * 
	 * @param publicKey
	 * @return 
	 * @Fitria: change the query. Need to get the result from card and customer at once.
	 */
	public ResultSet selectCard(byte[] publicKey) {
		//Statement stmt;
		PreparedStatement ps;
		ResultSet rs = null;

		try {
			//stmt = conn.createStatement();
			ps = conn.prepareStatement("SELECT card.publicKey as id, card.customerId as custID, "
					+ "customer.name as custName, card.totalKm as km, card.expiration as exp, "
					+ "card.revocation as revoke, card.publicKey as cardPK "
					+ "FROM card, customer "
					+ "WHERE card.customerID = customer.id and card.publicKey = ?" );
			ps.setBytes(1, publicKey);
			rs = ps.executeQuery();
			
			/*rs = stmt.executeQuery("SELECT card.publicKey as id, card.customerId as custID, "
					+ "customer.name as custName, card.totalKm as km, card.expiration as exp, "
					+ "card.revocation as revoke, card.publicKey as cardPK "
					+ "FROM card, customer "
					+ "WHERE card.customerID = customer.id and card.publicKey = \"" + strPublicKey + "\"" );*/
			//rs.close();
			//stmt.close();
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
			ResultSet rs = stmt.executeQuery( "SELECT name FROM customer WHERE id = \"" + customerId + "\"" );
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

	public Integer insertCustomer(String customerName) {
		Statement stmt;
		Integer customerId = -1;
		try {

			// Check if someone with this name already exists
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT id FROM customer WHERE name = \"" + customerName + "\"" );
			while ( rs.next() ) {
				customerId = rs.getInt("id");
			}
			rs.close();
			stmt.close();

			if (customerId == -1) {
				stmt = conn.createStatement();
				
				stmt.executeUpdate( "INSERT INTO customer (name) VALUES (\"" + customerName + "\")"  );
				//				conn.commit();
				stmt.close();


				// Now get the id of the newly inserted customer
				stmt = conn.createStatement();
				ResultSet rs2 = stmt.executeQuery( "SELECT id FROM customer WHERE name = \"" + customerName + "\"" );
				while ( rs2.next() ) {
					customerId = rs2.getInt("id");
				}
				rs2.close();
				stmt.close();

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return customerId;
	}

	/**
	 * Add a smartcard to the database
	 * 
	 * @Fitria edit 24/11/14: 
	 * add totalKm in the insert function
	 * 
	 * @Max: Total kilometers is always 0 when you add a new smartcard so removed it again
	 */
	public void addSmartcard(int customerId, long expiration, byte[] strPublicKey) {
		
		//Statement stmt;
		PreparedStatement ps;
		try {
			//stmt = conn.createStatement();
			ps = conn.prepareStatement("INSERT INTO card (customerId, totalKm, expiration, revocation, publicKey) "
					+ "VALUES (?, 0, ?, 0, ?)" );
			ps.setInt(1, customerId);
			ps.setLong(2, expiration);
			ps.setBytes(3, strPublicKey);
			
			ps.executeUpdate();
			//stmt.executeUpdate( "INSERT INTO card (customerId, totalKm, expiration, revocation, publicKey) "
			//		+ "VALUES (\"" + customerId + "\", \"0\", \"" + expiration + "\", \"0\", \"" + strPublicKey + "\")"  );
		//	stmt.close();
			//		    conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set the revoked flag to TRUE
	 * 
	 * @param publicKey		Used for identifying the correct card
	 */
	public void revokeSmartcard(byte[] publicKey) {
		//Statement stmt;
		PreparedStatement ps;
		try {
			//stmt = conn.createStatement();
			ps = conn.prepareStatement("UPDATE card SET revocation = \"1\" WHERE publicKey = ?");
			ps.setBytes(1, publicKey);
			ps.executeUpdate();
			ps.close();
			//stmt.executeUpdate( "UPDATE card SET revocation = \"1\" WHERE publicKey = \"" + publicKey + "\""  );
			//stmt.close();
			//		    conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Check the revoked status flag
	 * 
	 * @param publicKey		Used for identifying the correct card
	 * @return the status of the revoked flag (true = revoked, false = not revoked)
	 */
	public boolean isRevoked(byte[] publicKey) {
		//Statement stmt;
		PreparedStatement ps;
		String rev = "";

		try {
			//stmt = conn.createStatement();
			ps = conn.prepareStatement("SELECT revocation FROM card WHERE publicKey = ?" );
			ps.setBytes(1, publicKey);
			ResultSet rs = ps.executeQuery();//stmt.executeQuery( "SELECT revocation FROM card WHERE publicKey = \"" + strPublicKey + "\"" );
			while ( rs.next() ) {
				rev = rs.getString("revocation");
			}
			rs.close();
			ps.close();
			//stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (rev.equalsIgnoreCase("1") || rev.equalsIgnoreCase("0")) {
			return rev.equals("1"); //Will return true if it equals 1 and false if it equals 0
		} else {
			return false; //TODO: We return false if the card is not found???
		}
//		return false;
	}

	/**
	 * Add a vehicle terminal to the database
	 * 
	 * @return terminal id
	 */
	public int addVehicleTerminal(String strPublicKey, String strPrivateKey) {
		// TODO: implement
		// sql("INSERT INTO vehicleTerms ('publicKey', 'secretKey') VALUES
		// $publicKey, $secretKey");
		return 0;
	}


	/**
	 * @Fitria
	 * for Use case Top Up 
	 * when the kilometers is top up, the expiration date is also expanded
	 * @param kmNew : new value of kilometers
	 * @param expirationNew : new value of expiration date
	 * @param id : id of the rows that will be updated
	 * @return states: equal to true if succeed, false otherwise 
	 */
	public boolean updateKilometersExpiration(int kmTopUp, long expirationNew, byte[] publicKey){
		boolean states = false;
		//Statement stmt;
		PreparedStatement ps;
		try {
			//stmt = conn.createStatement();
			ps = conn.prepareStatement("SELECT totalKM FROM card WHERE publicKey = ?");
			ps.setBytes(1, publicKey);
			ResultSet rs = ps.executeQuery();
			int rev = 0;
			
			//ResultSet rs = stmt.executeQuery( "SELECT totalKM FROM card WHERE publicKey = \"" + strPublicKey + "\"" );
			while ( rs.next() ) {
				rev = rs.getInt("totalKM");
			}
			rs.close();
			ps.close();
			//stmt.close();
			
			//long totalKm = Long.parseLong(rev);
			
			long kmNew = kmTopUp + rev; // this is the old value from db + topup value
			ps = conn.prepareStatement("update card set totalKM = ?, expiration = ? WHERE publicKey = ?");
			ps.setLong(1, kmNew);
			ps.setLong(2, expirationNew);
			ps.setBytes(3, publicKey);
			ps.executeUpdate();
			ps.close();
			//stmt = conn.createStatement();
			//stmt.executeUpdate("update card set totalKM = \""+ kmNew +"\", expiration = \""+ expirationNew +"\" WHERE publicKey = \"" + strPublicKey + "\"");
			//stmt.close();
			states = true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return states;
	}

}
