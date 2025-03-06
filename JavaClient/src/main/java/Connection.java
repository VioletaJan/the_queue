import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Connection {

	private String server;
	private int requestPort;
	private int subscribePort;
	private ZMQ.Context context;
	private String name;
	private String clientId;
	private ZMQ.Socket requestSocket;
	private ZMQ.Socket subscribeSocket;
	private Gui gui;

	private volatile String msg;
	private String receivedQueueMsg;
	private String receivedSuperMsg;
	private String receivedUserMsg;
	
	private String fakeMessage = "Hello MEn";
	
	
	private String fakeSupervisorMsg = "["
	        + "{\"name\":\"Erik\", \"status\":\"occupied\", \"client\":{\"ticket\":1, \"name\":\"Violeta\"}},"
	        + "{\"name\":\"Simon\", \"status\":\"available\", \"client\":null},"
	        + "{\"name\":\"Andras\", \"status\":\"occupied\", \"client\":{\"ticket\":4, \"name\":\"Isak\"}}"
	        + "]";
	

	// Constructor accepts server, request port (for REQ) and subscribe port (for
	// SUB)
	public Connection(String server, String clientId, int requestPort, int subscribePort, Gui gui) {
		this.server = server;
		this.requestPort = requestPort;
		this.subscribePort = subscribePort;
		this.clientId = clientId;
		this.context = ZMQ.context(1); // Initialize the ZContext
		this.name = "defaultName";
		this.gui = gui;

	}

	public void setName(String name) {
		this.name = name;
	}

	// Method to send a request using REQ socket
	public void sendRequest() {
		try {
			// Ensure the request socket is initialized once and reused
			if (requestSocket == null) {
				createRequestSocket();
			}

			// Create a JSON object for the request
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("clientId", clientId);
			jsonObject.put("name", name);
			jsonObject.put("enterQueue", true);

			// Send JSON object as a string
			String requestMessage = jsonObject.toString();
			requestSocket.send(requestMessage);
			System.out.println("Request sent: " + requestMessage);

			// Wait for the reply (since REQ/REP requires this)
			byte[] reply = requestSocket.recv();
			String replyStr = new String(reply, ZMQ.CHARSET);
			System.out.println("Received reply: " + replyStr);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createRequestSocket() {
		requestSocket = context.socket(ZMQ.REQ); // Create a REQ socket
		requestSocket.connect("tcp://" + server + ":" + requestPort); // Connect to the server with provided port
	}

	public ZMQ.Socket getSocket() {
		// Ensure the socket is created and connected
		if (requestSocket == null) {
			System.out.println("IS IT NULLLL???");
			createRequestSocket();
		}
		return requestSocket;
	}


public void RunnablesubscribeToQueue() {
    try {
        // Use the existing context instead of creating a new one
        if (subscribeSocket == null) {
            subscribeSocket = context.socket(ZMQ.SUB); // Reuse the class-level context
            subscribeSocket.connect("tcp://" + server + ":" + subscribePort); // Connect to the server
            subscribeSocket.subscribe("queue".getBytes(ZMQ.CHARSET)); // Subscribe to the "queue" topic
            subscribeSocket.subscribe("supervisors".getBytes(ZMQ.CHARSET)); // Subscribe to the "supervisors" topic
            subscribeSocket.subscribe(name.getBytes(ZMQ.CHARSET)); // Subscribe to the "<name of user>" topic
            System.out.println("Subscribed to topics: queue, supervisors, and <name of user>.");
        }
        
      //  simulateSupervisorMessage();
        

        // Continuously receive messages in a loop
        while (!Thread.currentThread().isInterrupted()) {
            // Receive the topic and the message from the server
            String topic = new String(subscribeSocket.recv(), ZMQ.CHARSET); // Get the topic
            String receivedMsg = new String(subscribeSocket.recv(), ZMQ.CHARSET); // Get the message

            // Store the message based on the topic
            if (topic.equals("queue")) {
                synchronized (this) {
                    receivedQueueMsg = receivedMsg; // Store the message in receivedQueueMsg
                }
                System.out.println("Received queue message: " + receivedQueueMsg);

                // Update the queue GUI, if needed
               List<String> formattedList = getParsedQueue(); // Pass the message to the parser

                SwingUtilities.invokeLater(() -> {
                    gui.clearQueueList();
                    gui.populateQueue(formattedList); // Update the GUI with the parsed and formatted list
                   // gui.showPopupMessage(fakeMessage);
                });
            }else if (topic.equals("supervisors")) {
                synchronized (this) {
	                   receivedSuperMsg = receivedMsg; // Store the message in receivedSuperMsg
	                    
	                    
	                    List<String> formattedList2 = getParsedSuper();
	                    
	                    SwingUtilities.invokeLater(() -> {
	                       gui.clearSupervisorList();
	                        gui.populateSupervisors(formattedList2); // Update the GUI with the parsed and formatted list
	                    });
	                }
	                System.out.println("Received supervisors message: " + receivedSuperMsg);
                
            }else if (topic.equals(name)) {  // or use clientId if needed
                synchronized (this) {
                    receivedUserMsg = receivedMsg; // Store the message

                    // Parse the JSON message from the supervisor
                    JSONObject receivedJson = new JSONObject(receivedUserMsg);
                    String supervisorName = receivedJson.getString("supervisor");
                    String message = receivedJson.getString("message");

                    // Display the message using GUI
                    String displayMessage ="Dear " + name + " a message from " + supervisorName + ": " + message;
                   // gui.showMessage(displayMessage);
                    gui.showPopupMessage(displayMessage); // Show the message in a pop-up or other UI element
                    
                    
                }
                System.out.println("Received user message: " + receivedUserMsg);
          
            } else {
                System.out.println("Unknown topic: " + topic);
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        // Close the socket when done or on error
        if (subscribeSocket != null) {
            subscribeSocket.close();
        }
    }
}



public void simulateSupervisorMessage() {
    String fakeSupervisorMsg = "["
        + "{\"name\":\"Erik\", \"status\":\"occupied\", \"client\":{\"ticket\":1, \"name\":\"Violeta\"}},"
        + "{\"name\":\"Simon\", \"status\":\"available\", \"client\":null},"
        + "{\"name\":\"Andras\", \"status\":\"occupied\", \"client\":{\"ticket\":4, \"name\":\"Isak\"}}"
        + "]";

    // Call the parser with the fake message
    List<String> parsedSupervisors = parseAndFormatSuper(fakeSupervisorMsg);

    // Print the parsed results to the console
   for (String parsed : parsedSupervisors) {
        System.out.println(parsed);
        SwingUtilities.invokeLater(() -> {
            gui.clearSupervisorList();
            gui.populateSupervisors(parsedSupervisors); // Update the GUI with the parsed and formatted list
        });
    }
}

	public synchronized String getQueue() {
		return receivedQueueMsg; // Ensure thread safety when accessing msg
	}
	
	public synchronized String getSupervisor() {
		
		return receivedSuperMsg;
		
		//return fakeSupervisorMsg;
		
	
	}
	
	public synchronized String getMessage() {
		return receivedUserMsg;
	}
	
	

	public List<String> getParsedQueue() {
		try {
			String currentMsg = getQueue(); // Get the current message safely
			if (currentMsg == null || currentMsg.isEmpty()) {
				return new ArrayList<>(); // Return empty list if message is empty
			}
			// Parse the received message (msg) as a JSON array and return the formatted
			// result
			return parseAndFormat(currentMsg);
		} catch (Exception e) {
			// If parsing fails, print the error and return an empty list
			e.printStackTrace();
			return new ArrayList<>(); // Return an empty list if parsing fails
		}
	}
	
	public List<String> getParsedSuper() {
	    try {
	        String currentMsg = getSupervisor(); // Get the current message safely
	        if (currentMsg == null || currentMsg.isEmpty()) {
	            return new ArrayList<>(); // Return empty list if message is empty
	        }
	        // Parse the received message (msg) as a JSON array and return the formatted result
	        return parseAndFormatSuper(currentMsg);
	    } catch (Exception e) {
	        // If parsing fails, print the error and return an empty list
	        e.printStackTrace();
	        return new ArrayList<>(); // Return an empty list if parsing fails
	    }
	}
	
	
	public List<String> getParsedSuperMessage() {
	    try {
	        String currentMsg = getSupervisor(); // Get the current message safely
	        if (currentMsg == null || currentMsg.isEmpty()) {
	            return new ArrayList<>(); // Return empty list if message is empty
	        }
	        // Parse the received message (msg) as a JSON object and return the formatted result
	        return parseAndFormatSuperMessage(currentMsg);
	    } catch (Exception e) {
	        // If parsing fails, print the error and return an empty list
	        e.printStackTrace();
	        return new ArrayList<>(); // Return an empty list if parsing fails
	    }
	}
	
	public static List<String> parseAndFormat(String jsonString) {
	    // Create a list to store the formatted results
	    List<String> formattedList = new ArrayList<>();
	    
	    try {
	        // Check if the input string starts with a '[' (indicating a JSON array)
	        if (!jsonString.trim().startsWith("[")) {
	            throw new JSONException("Expected JSON Array but got something else: " + jsonString);
	        }
	        
	        // Parse the JSON array
	        JSONArray jsonArray = new JSONArray(jsonString);
	        
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
	    } catch (JSONException e) {
	        System.err.println("Error parsing JSON: " + e.getMessage());
	        // Optionally rethrow the exception or return an empty list
	        // throw e;
	    }
	    
	    // Return the list of formatted strings
	    return formattedList;
	}
	
	
	public static List<String> parseAndFormatSuperMessage(String jsonString) {
	    // Create a list to store the formatted results
	    List<String> formattedList = new ArrayList<>();

	    try {
	        // Parse the JSON object (not an array)
	        JSONObject obj = new JSONObject(jsonString);

	        // Extract the supervisor's name and message
	        String supervisorName = obj.getString("supervisor");
	        String message = obj.getString("message");

	        // Format the string: "Supervisor Name: Message"
	        String formatted = supervisorName + ": " + message;

	        // Add the formatted string to the list
	        formattedList.add(formatted);
	        
	    } catch (JSONException e) {
	        System.err.println("Error parsing JSON: " + e.getMessage());
	        // Optionally rethrow the exception or return an empty list
	    }

	    // Return the list of formatted strings
	    return formattedList;
	}
	
	

	public static List<String> parseAndFormatSuper(String jsonString) {
	    // Create a list to store the formatted results
	    List<String> formattedList = new ArrayList<>();

	    try {
	        // Check if the input string starts with a '[' (indicating a JSON array)
	        if (!jsonString.trim().startsWith("[")) {
	            throw new JSONException("Expected JSON Array but got something else: " + jsonString);
	        }

	        // Parse the JSON array
	        JSONArray jsonArray = new JSONArray(jsonString);

	        // Loop through each JSON object in the array
	        for (int i = 0; i < jsonArray.length(); i++) {
	            // Get the current JSON object
	            JSONObject obj = jsonArray.getJSONObject(i);

	            // Extract the supervisor's name and status
	            String supervisorName = obj.getString("name");
	            String status = obj.getString("status");

	            // Extract the client field, which can be either undefined (null) or a JSON object
	            JSONObject client = obj.optJSONObject("client"); // Use optJSONObject to avoid an exception if client is undefined

	            String formatted;
	            if (client == null) {
	                // No client assigned, format it as "Supervisor Name (Status)"
	                formatted = supervisorName + " (" + status + ")";
	            } else {
	                // Client is assigned, extract the client ticket and name
	                int ticket = client.getInt("ticket");
	                String clientName = client.getString("name");
	                // Format as "Supervisor Name (Status) - Handling Client: Ticket - Client Name"
	                formatted = supervisorName + " (" + status + ") - Handling Client: " + ticket + " - " + clientName;
	            }

	            // Add the formatted string to the list
	            formattedList.add(formatted);
	        }
	    } catch (JSONException e) {
	        System.err.println("Error parsing JSON: " + e.getMessage());
	        // Optionally rethrow the exception or return an empty list
	        // throw e;
	    }

	    // Return the list of formatted strings
	    return formattedList;
	}



	// Clean up context
	public void close() {
		context.close();
	}
}
