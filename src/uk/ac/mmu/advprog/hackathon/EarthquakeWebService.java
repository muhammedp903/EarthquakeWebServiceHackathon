package uk.ac.mmu.advprog.hackathon;
import static spark.Spark.get;
import static spark.Spark.port;

import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles the setting up and starting of the web service
 * You will be adding additional routes to this class, and it might get quite large
 * You should push some of the work into additional child classes, like I did with DB
 * @author You, Mainly!
 */
public class EarthquakeWebService {
	
	/**
	 * Main program entry point, starts the web service
	 * @param args not used
	 */
	public static void main(String[] args) {		
		port(8088);	
		
		//You can check the web service is working by loading http://localhost:8088/test in your browser
		get("/test", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				try (DB db = new DB()) {
					return "Number of entries: " + db.getNumberOfEntries();
				}
			}
		});

		//  Gets the number of earthquakes with a magnitude greater than the specified value (e.g. http://localhost:8088/quakecountmagnitude=5.5)
        get("/quakecount", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {

				// Make sure the parameter is not empty or missing
				if (request.queryParams("magnitude") == null) {
					return "Invalid Magnitude";
				}

				double magnitude;

				try {
					magnitude = Double.parseDouble(request.queryParams("magnitude"));
				} catch (NumberFormatException e) {
					// If the magnitude is not a number, return an error
					return "Invalid Magnitude";
				}

				try (DB db = new DB()) {
					// Call the getQuakeCount method from the DB class and return the result
                    return db.getQuakeCount(magnitude);
                }
            }
        });

		// Gets a list of earthquakes with a magnitude greater than the specified value in a particular year (e.g. http://localhost:8088/quakesbyyear?year=2022&magnitude=6.5)
		get("/quakesbyyear", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {

				// Make sure the parameters are not empty or missing
				if (request.queryParams("year") == null || request.queryParams("magnitude") == null) {
					return "Invalid Magnitude";
				}

				double magnitude;
				int year;

				try {
					magnitude = Double.parseDouble(request.queryParams("magnitude"));
					year = Integer.parseInt(request.queryParams("year"));
				} catch (NumberFormatException e) {
					// If the magnitude or year is not a number, return an error
					return "Invalid parameters";
				}

				try (DB db = new DB()) {
					// Call the getQuakesByYear method from the DB class and return the result
					return db.getQuakesByYear(year, magnitude);
				}
			}
		});

		// Gets a list of 10 closest earthquakes with a magnitude greater than the specified value to a particular location (e.g. http://localhost:8088/quakesbylocation?latitude=53.472&longitude=-2.244&magnitude=6.0)
		get("/quakesbylocation", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				response.header("Content-Type", "application/xml");

				// Make sure the parameters are not empty or missing
				if (request.queryParams("latitude") == null || request.queryParams("longitude") == null || request.queryParams("magnitude") == null) {
					return "Invalid parameters";
				}

				double magnitude;
				double latitude;
				double longitude;

				try {
					magnitude = Double.parseDouble(request.queryParams("magnitude"));
					latitude = Double.parseDouble(request.queryParams("latitude"));
					longitude = Double.parseDouble(request.queryParams("longitude"));
				} catch (NumberFormatException e) {
					// If the magnitude, latitude or longitude is not a number, return an error
					return "Invalid parameters";
				}

				try (DB db = new DB()) {
					// Call the getQuakesByLocation method from the DB class and return the result
					return db.getQuakesByLocation(latitude, longitude, magnitude);
				}
			}
		});
		
		System.out.println("Web Service Started. Don't forget to kill it when done testing!");
	}
}
