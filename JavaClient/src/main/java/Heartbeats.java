import org.json.JSONObject;
import org.zeromq.ZMQ;

public class Heartbeats implements Runnable {

	private static final int HEARTBEAT_INTERVAL_MS = 3000;
	private String clientId;
	private String name;
	private ZMQ.Socket requestSocket;
	private ZMQ.Context context;

	// Constructor that accepts a client ID and the socket
	public Heartbeats(String clientId, String name, ZMQ.Socket requestSocket) {
		this.clientId = clientId;
		this.name = name;
		this.requestSocket = requestSocket;
	}

	@Override
	public void run() {
		sendHeartbeats();
	}

	private void sendHeartbeats() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				// Send heart beat message
				JSONObject heartbeatMessage = new JSONObject();
				heartbeatMessage.put("clientId", clientId);
				heartbeatMessage.put("heartbeat", true);
				// heartbeatMessage.put("name", name);// Use the passed client ID
				requestSocket.send(heartbeatMessage.toString());

				// Receive acknowledgment from the server
				byte[] ackBytes = requestSocket.recv();
				String ack = new String(ackBytes, ZMQ.CHARSET);
				System.out.println("Server acknowledgment: " + ack);

				// Wait before sending the next heart beat
				Thread.sleep(HEARTBEAT_INTERVAL_MS);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // Restore interrupt status
			} catch (Exception e) {
				System.err.println("Error sending heartbeat: " + e.getMessage());
				break; // Exit the loop on error
			}
		}
	}
}
