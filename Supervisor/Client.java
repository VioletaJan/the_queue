import java.util.List;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.zeromq.ZMQ;

//TODO Fix clientId so it's not duplicate
//TODO Fix queuelist showcased on the GUI
//TODO Check so the Server/Message is proper
//TODO PARSE the JSON to the QUeue list so it looks OK.
//TODO Make this client work with vios client
//TODO Fix Supervisor CLIENT

public class Client {
	// Declare instance variables
	private String userName;
	private String serverAddress;
	private String clientId;
	private int requestPort;
	private int subscribePort = 5555;
	private ZMQ.Socket beatSocket;
	private Gui gui;
	private Connection connection;

	public static void main(String[] args) throws InterruptedException {
		Client client = new Client();
		String uniqueClientId = UUID.randomUUID().toString();
		client.run(uniqueClientId);

	}

	// Main entry point to run the application
	private void run(String clientId) throws InterruptedException {
		this.clientId = clientId;
		initializeGui();
		getUserInput();
		setupConnection();
		gui.setConnection(connection);
		sendRequest();
		startHeartbeat();
		connection.RunnablesubscribeToQueue();
	}

	// Initialize the GUI
	private void initializeGui() {
		gui = new Gui();
		gui.SimpleGUI();
	}

	// Get user input from the GUI
	private void getUserInput() throws InterruptedException {
		serverAddress = gui.getEnteredAdress();
		userName = gui.getEnteredName();
		requestPort = gui.getEnteredPort();
	}

	// Set up the connection to the server
	private void setupConnection() {
		connection = new Connection(serverAddress, clientId, requestPort, subscribePort, gui);
		connection.setName(userName);
		beatSocket = connection.getSocket();

	}

	// Start the heart beat thread
	private void startHeartbeat() {
		Heartbeats heartBeats = new Heartbeats(clientId, userName, beatSocket);
		Thread heartbeatThread = new Thread(heartBeats);
		heartbeatThread.start();

	}

	// Send a request to the server
	private void sendRequest() {
		connection.sendRequest();
	}

}