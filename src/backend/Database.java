package backend;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
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
	}

	/**
	 * Return a ResultSet with all data about a specific card identified by the publicKey
	 * 
	 * @param publicKey
	 * @return 
	 */
	public ResultSet selectCard(byte[] bPublicKey) {
	    Statement stmt;
	    ResultSet rs = null;
	    RSAPublicKey pubKey = null;
	    try {
	    	X509EncodedKeySpec pubspec = new X509EncodedKeySpec(bPublicKey);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			pubKey = (RSAPublicKey) factory.generatePublic(pubspec);
	    } catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery( "SELECT * FROM card WHERE publicKey = \"" + pubKey.getEncoded() + "\");" );
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

	public Integer insertCustomer(String customerName) {
	    Statement stmt;
	    Integer customerId = -1;
		try {

		    // Check if someone with this name already exists
		    stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT id FROM card WHERE name = \"" + customerName + "\");" );
		      while ( rs.next() ) {
		         customerId = rs.getInt("id");
		      }
		      rs.close();
		      stmt.close();
		      
		      if (customerId == -1) {
				stmt = conn.createStatement();
				stmt.executeUpdate( "INSERT INTO customer (name) VALUES (\"" + customerName + "\");"  );
				stmt.close();
				conn.commit();
				
				// Now get the id of the newly inserted customer
				stmt = conn.createStatement();
				ResultSet rs2 = stmt.executeQuery( "SELECT id FROM card WHERE name = \"" + customerName + "\");" );
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
	public void addSmartcard(int customerId, long expiration, RSAPublicKey publicKey) {
		Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate( "INSERT INTO card (customerId, totalKm, expiration, revocation, publicKey) "
					+ "VALUES (\"" + customerId + "\", \"0\", \"" + expiration + "\", \"" +
					false + "\", \"" + publicKey.getEncoded() + "\");"  );
		    stmt.close();
		    conn.commit();
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
	    Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate( "UPDATE card SET revoked = \"" + true + "\" WHERE publicKey = \"" + publicKey + "\";"  );
		    stmt.close();
		    conn.commit();
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
	    Statement stmt;
	    String rev = "";
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT revoked FROM card WHERE publicKey = \"" + publicKey + "\");" );
		      while ( rs.next() ) {
		         rev = rs.getString("revoked");
		      }
		      rs.close();
		      stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (rev.equalsIgnoreCase("true") || rev.equalsIgnoreCase("false")) {
		    return Boolean.valueOf(rev);
		} else {
		    return false; //TODO: We return false if the card is not found???
		}
	}

	/**
	 * Add a vehicle terminal to the database
	 * 
	 * @return terminal id
	 */
	public int addVehicleTerminal(RSAPublicKey publicKey, byte[] secretKey) {
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
	
	/*
	 * @fitria
	 * select the latest id from database customer
	 * Need for RentalTerminal_classes.RegisterNewCustomer
	 * Use case: Register Customer
	 */
	public int getLastID() {
		Statement stmt;
	    int id = 0;
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select max(id) as id from customer" );
			while(rs.next())
				id = rs.getInt("id");
		    rs.close();
		    stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return id;
	}	

}
