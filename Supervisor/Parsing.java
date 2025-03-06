import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Parsing {

	// Method to parse and format the JSON string, now returning a List<String>
	public static List<String> parseAndFormat(String jsonString) {
		// Parse the JSON array
		JSONArray jsonArray = new JSONArray(jsonString);

		// Create a list to store the formatted results
		List<String> formattedList = new ArrayList<>();

		// Loop through each JSON object in the array
		for (int i = 0; i < jsonArray.length(); i++) {
			// Get the current JSON object
			JSONObject obj = jsonArray.getJSONObject(i);

			// Extract the ticket and name fields
			int ticket = obj.getInt("ticket");
			String name = obj.getString("name");

			// Format the string: "ticket - name"
			String formatted = ticket + " - " + name;

			// Add the formatted string to the list
			formattedList.add(formatted);
		}

		// Return the list of formatted strings
		return formattedList;
	}
}