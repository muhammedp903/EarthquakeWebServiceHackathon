package uk.ac.mmu.advprog.hackathon;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

/**
 * Handles database access from within your web service
 * @author You, Mainly!
 */
public class DB implements AutoCloseable {

	//allows us to easily change the database used
	private static final String JDBC_CONNECTION_STRING = "jdbc:sqlite:./data/earthquakes.db";
	
	//allows us to re-use the connection between queries if desired
	private Connection connection = null;
	
	/**
	 * Creates an instance of the DB object and connects to the database
	 */
	public DB() {
		try {
			connection = DriverManager.getConnection(JDBC_CONNECTION_STRING);
		}
		catch (SQLException sqle) {
			error(sqle);
		}
	}
	
	/**
	 * Returns the number of entries in the database, by counting rows
	 * @return The number of entries in the database, or -1 if empty
	 */
	public int getNumberOfEntries() {
		int result = -1;
		try {
			Statement s = connection.createStatement();
			ResultSet results = s.executeQuery("SELECT COUNT(*) AS count FROM earthquakes");
			while(results.next()) { //will only execute once, because SELECT COUNT(*) returns just 1 number
				result = results.getInt(results.findColumn("count"));
			}
		}
		catch (SQLException sqle) {
			error(sqle);
			
		}
		return result;
	}

	/**
	 * Returns the number of earthquakes in the database with a magnitude of at least the value passed in
	 * @param magnitude The minimum magnitude to match
	 * @return The number of earthquakes in the database with magnitude equal to or greater than the magnitude parameter
	 */
	public int getQuakeCount(double magnitude) {
		int result = -1;
		try {
			PreparedStatement s = connection.prepareStatement("SELECT COUNT(*) AS Number FROM earthquakes WHERE mag >= ?");
			s.setDouble(1, magnitude);

			ResultSet results = s.executeQuery();
			while(results.next()) {
				result = results.getInt(results.findColumn("number"));
			}
		}
		catch (SQLException sqle) {
			error(sqle);

		}
		return result;
	}

	/**
	 * Returns a JSONArray of earthquakes from the given year with a magnitude of at least the value passed in
	 * @param year The year to match
	 * @param magnitude The minimum magnitude to match
	 * @return A JSONArray of earthquakes from the given year with a magnitude of at least the value passed in
	 */
	public JSONArray getQuakesByYear(int year, double magnitude){
		JSONArray result = new JSONArray();
		try {
			PreparedStatement s = connection.prepareStatement("SELECT * FROM earthquakes WHERE time LIKE ? AND mag >= ? ORDER BY time ASC");
			s.setString(1, year + "%"); // The time field is a string with the date at the start, so the wildcard will match it
			s.setDouble(2, magnitude);

			ResultSet results = s.executeQuery();
			while(results.next()) {
				JSONObject quake = new JSONObject();
				quake.put("id", results.getString(results.findColumn("id")));
				quake.put("magnitude", results.getDouble(results.findColumn("mag")));

				String timeString = results.getString(results.findColumn("time")); // The date and time are separated by a "T" in the time field
				quake.put("date", timeString.split("T")[0]);
				quake.put("time", timeString.split("T")[2]);

				JSONObject location = new JSONObject(); // The location is a JSON object within the JSON object of the earthquake
				location.put("latitude", results.getDouble(results.findColumn("latitude")));
				location.put("longitude", results.getDouble(results.findColumn("longitude")));
				location.put("description", results.getString(results.findColumn("place")));
				quake.put("location", location); // Add the location object to the earthquake object

				result.put(quake); // Add the earthquake object to the result array
			}
		}
		catch (SQLException sqle) {
			error(sqle);

		}
		return result;
	}

	/**
	 * Returns an XML document as a String with the 10 closest earthquakes to the given location with a magnitude of at least the value passed in
	 * @param latitude The latitude of the location to match
	 * @param longitude The longitude of the location to match
	 * @param magnitude The minimum magnitude to match
	 * @return An XML document in String format with the 10 closest earthquakes to the given location with a magnitude of at least the value passed in
	 */
	public String getQuakesByLocation(double latitude, double longitude, double magnitude){
		// Create a new XML document
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc;
		try {
			doc = dbf.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		try {
			PreparedStatement s = connection.prepareStatement(
					"""
						SELECT * FROM earthquakes
						WHERE mag >= ?
						ORDER BY
						(
						((? - Latitude) * (? - Latitude)) + (0.595 * ((? - Longitude) * (? - Longitude)))
						)
						ASC
						LIMIT 10;
					""");

			//  Set the parameters for the distance calculation in the sql query
			s.setDouble(1, magnitude);
			s.setDouble(2, latitude);
			s.setDouble(3, latitude);
			s.setDouble(4, longitude);
			s.setDouble(5, longitude);

			ResultSet results = s.executeQuery();

			Element quakes = doc.createElement("Earthquakes"); // The root element of the XML document
			doc.appendChild(quakes);

			while(results.next()) {
				Element quake = doc.createElement("Earthquake"); // Each earthquake is a child of the root element
				quake.setAttribute("id", results.getString(results.findColumn("id")));

				Element mag = doc.createElement("Magnitude");
				mag.setTextContent(results.getString(results.findColumn("mag")));
				quake.appendChild(mag);

				Element date = doc.createElement("Date");
				date.setTextContent(results.getString(results.findColumn("time")).split("T")[0]);
				quake.appendChild(date);

				Element time = doc.createElement("Time");
				time.setTextContent(results.getString(results.findColumn("time")).split("T")[1]);
				quake.appendChild(time);

				Element location = doc.createElement("Location"); // The lat, long and description are inside a location element

				Element lat = doc.createElement("Latitude");
				lat.setTextContent(results.getString(results.findColumn("latitude")));
				location.appendChild(lat);

				Element lon = doc.createElement("Longitude");
				lon.setTextContent(results.getString(results.findColumn("longitude")));
				location.appendChild(lon);

				Element description = doc.createElement("Description");
				description.setTextContent(results.getString(results.findColumn("place")));
				location.appendChild(description);

				quake.appendChild(location); // Add the location element to the earthquake element

				quakes.appendChild(quake); // Add the earthquake element to the root element
			}
		}
		catch (SQLException sqle) {
			error(sqle);
		}

		try{
			// Turning the XML document into a String
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Writer outputWriter = new StringWriter();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(doc), new StreamResult(outputWriter));

			return outputWriter.toString(); // Return the String representation of the XML document
		} catch (TransformerException e) {
			System.out.println(e);
        }
		return null; // If anything went wrong
	}
	
	/**
	 * Closes the connection to the database, required by AutoCloseable interface.
	 */
	@Override
	public void close() {
		try {
			if ( !connection.isClosed() ) {
				connection.close();
			}
		}
		catch(SQLException sqle) {
			error(sqle);
		}
	}
	
	/**
	 * Prints out the details of the SQL error that has occurred, and exits the programme
	 * @param sqle Exception representing the error that occurred
	 */
	private void error(SQLException sqle) {
		System.err.println("Problem Accessing Database! " + sqle.getClass().getName());
		sqle.printStackTrace();
		System.exit(1);
	}
}