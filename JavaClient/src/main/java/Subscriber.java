import org.zeromq.ZMQ;
import java.util.List;

public class Subscriber implements Runnable {
	private String server;
	private String msg;
	private int subscribePort;
	private ZMQ.Socket subscribeSocket;
	private ZMQ.Context context;

	// Constructor to initialize server and port
	public Subscriber(String server, int subscribePort) {
		this.server = server;
		this.subscribePort = subscribePort;
		this.context = ZMQ.context(1); // Initialize the ZMQ context here
	}

	// Method to subscribe to the queue
	public void RunnablesubscribeToQueue() {
		try {
			// Create a SUB socket to receive messages
			subscribeSocket = context.socket(ZMQ.SUB);
			subscribeSocket.connect("tcp://" + server + ":" + subscribePort); // Connect to the server

			// Subscribe to the "queue" topic
			subscribeSocket.subscribe("".getBytes(ZMQ.CHARSET));
			System.out.println("Subscribed to 'queue' topic.");

			// Continuously receive messages in a loop
			while (!Thread.currentThread().isInterrupted()) {
				// Receive the message
				String receivedMessage = new String(subscribeSocket.recv(), ZMQ.CHARSET);

				// Print the received message
				System.out.println("Received message: " + receivedMessage);
				this.msg = receivedMessage; // Store the message if needed
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Close the socket when done (on interrupt or error)
			if (subscribeSocket != null) {
				subscribeSocket.close();
			}
		}
	}

	@Override
	public void run() {
		// Call the subscription method when the thread starts
		RunnablesubscribeToQueue();
	}

	// Optional method to get the latest message
	public String getLatestMessage() {
		return msg;
	}
}