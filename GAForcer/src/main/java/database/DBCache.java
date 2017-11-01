/**
 * This class is made to handle database-type operations. This is for caching 
 * to avoid sending unnecessary requests (improved stealth), and to provide a persistent
 * record of results. 
 * A fair amount referenced from https://www.tutorialspoint.com/sqlite/sqlite_java.htm
 * @author Christopher Ellis (ChrisJoinEngine)
 */

package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBCache 
{
	private static final String DB_NAME="Discovered_Links.sqlite";
	private static final String TABLE_NAME="RESULTS";
	private static final String KEY_NAME="GENERATED_URL";
	private static final String VALUE_NAME="ACTUAL_URL";
	
	private static Connection databaseConnection = null;

	/**
	 * This creates the cache database or creates a connection to it, ensures that necessary tables
	 * are in place, and updates the DBCache class reference to the database so insertion and selection
	 * statements can use it.
	 */
	public static void initialize()
	{
		boolean mustBeCreated=false;
		
		File file=new File(DB_NAME);
	    if (!file.exists())
	    	mustBeCreated=true;
		
	    try 
	    {
	      Class.forName("org.sqlite.JDBC");
	      databaseConnection = DriverManager.getConnection("jdbc:sqlite:"+DB_NAME);
	      databaseConnection.setAutoCommit(false);
	    } 
	    catch ( Exception e ) 
	    {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	    System.out.println("Opened database successfully");
	    if (mustBeCreated)
	    	createTable(databaseConnection);
	}
	
	/**
	 * This creates the default table in the cache database
	 * @param databaseConnection the database connection to use for table creation (basically a reference
	 * to the cache database).
	 */
	private static void createTable(Connection databaseConnection)
	{	
		try
		{
			Statement createTableStatement = databaseConnection.createStatement();
			String createTableCommand = "CREATE TABLE "+ TABLE_NAME +
										"("+KEY_NAME+" TEXT PRIMARY KEY NOT NULL," +
										" "+VALUE_NAME+" TEXT)"; 
			createTableStatement.executeUpdate(createTableCommand);
			databaseConnection.commit();
			createTableStatement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * This adds a record to the cache. It pretty much just uses key value pairs, so that's all this takes. 
	 * @param key the value to insert into the key section
	 * @param value the value to insert into the value section
	 */
	public static void addRecord(String key, String value)
	{    
	    try (PreparedStatement insertStatement=databaseConnection.prepareStatement("INSERT INTO "+TABLE_NAME+"("+KEY_NAME+","+VALUE_NAME+") VALUES (?,?);"))    
	    {
	    	insertStatement.setString(1, key);
	    	insertStatement.setString(2, value);
	    	insertStatement.execute();
	    	databaseConnection.commit();	     
	    } 
	  	catch ( Exception e ) 
	    {
	  		e.printStackTrace();
	      System.out.println("That value was already in the databaase. If you see this often,");
	      System.out.println("then there may be a problem reading the DB. Do you have multiple");
	      System.out.println("program instances running?");
	    }
	}
	
	/**
	 * This checks if the existing key already exists in the db, if it does, it returns the value of that key
	 * @param key the key to search for
	 * @return the corresponding value to return
	 */
	public static String checkForRecord(String key)
	{
		try (PreparedStatement selectStatement = databaseConnection.prepareStatement("SELECT "+VALUE_NAME+" FROM "+TABLE_NAME+" WHERE "+KEY_NAME+"=?;" )) 
	    {	    
			
			selectStatement.setString(1, key);
			ResultSet resultSet = selectStatement.executeQuery();
			String toReturn=resultSet.getString(VALUE_NAME);
			return toReturn;
	    }
		catch ( Exception e ) 
		{/**System.out.println("No value in DB");*/}	
		return null;
	}
	
	/**
	 * Attempts to close the database connection. If the database connection
	 * cannot be closed, the program is terminated.
	 */
	public static void closeDatabase()
	{
		try 
		{
			databaseConnection.close();
		} 
		catch (Exception e) 
		{
			
		}
	}
}
	
