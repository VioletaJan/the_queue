import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

public class Gui {
	private String enteredName;
	private String enteredAdress;
	private String enteredPort;
	private String message;
	private final Object lock = new Object(); // Synchronization object
	private JLabel externalNameLabel;
	private JLabel externalSupervisorLabel;
	private JButton submitButton;
	private JButton attendButton;
	private static DefaultListModel<String> nameListModel;
	private static DefaultListModel<String> supervisorListModel;
	private Connection connection;


	// Constructor to set up the GUI
	public void SimpleGUI() {
		
		
		JFrame frame = new JFrame("Supervisor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(700, 400); // Adjust size for better visibility
		frame.setLayout(new GridBagLayout()); // Use GridBagLayout for more control

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(1, 1, 1, 1); // Margins between components
		gbc.fill = GridBagConstraints.HORIZONTAL; // Make components fill the available space

		// Create components
		JLabel label = new JLabel("Supervisor name: ");
		JLabel label2 = new JLabel("Enter IP: ");
		JLabel label3 = new JLabel("Port: ");
		// JTextField with preferred size for larger input fields
		JTextField nameField = new JTextField();
		nameField.setPreferredSize(new Dimension(200, 20)); // Width: 200, Height: 30

		JTextField connectionField = new JTextField();
		connectionField.setPreferredSize(new Dimension(200, 20)); // Width: 200, Height: 30

		JTextField portField = new JTextField();
		portField.setPreferredSize(new Dimension(50, 20));
		
		JTextField messageField = new JTextField();
		messageField.setPreferredSize(new Dimension(100,50));

		JButton submitButton = new JButton("Connect");
		JButton attendButton = new JButton("Attend 1st in queue");

		// External name list (for a list of names to be updated)
		JLabel externalNameLabel = new JLabel("Queue: ");
		nameListModel = new DefaultListModel<>();
		JList<String> externalNameList = new JList<>(nameListModel);
		externalNameList.setVisibleRowCount(5); // Display 5 names at a time
		JScrollPane nameScrollPane = new JScrollPane(externalNameList); // Scroll for multiple names

		JLabel externalSupervisorLabel = new JLabel("Supervisors: ");
		supervisorListModel = new DefaultListModel<>();
		JList<String> supervisorNameList = new JList<>(supervisorListModel);
		supervisorNameList.setVisibleRowCount(5); // Display 5 names at a time
		JScrollPane nameScrollPane2 = new JScrollPane(supervisorNameList);

		//populateTemporaryNames();

		// Add action listener for the submit button
		submitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				synchronized (lock) {
					enteredName = nameField.getText();
					enteredAdress = connectionField.getText();
					enteredPort = portField.getText();
					System.out.println("Name entered: " + enteredName + ", Address entered: " + enteredAdress + "Port"
							+ enteredPort);

					// Simulate adding external names (e.g., from a queue or external source)
					//addExternalName("Entered: " + enteredName);

					lock.notify(); // Notify any waiting thread
				}
			}
		});
		
		attendButton.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        // Prepare the attend request message (optional: customize the message)
		        message = messageField.getText();

		        // Call the sendAttendRequest() method of the Connection object
		        if (connection != null) {
		            connection.sendAttendRequest();
		            System.out.print("Message Sent:" + message);
		        } else {
		            System.out.println("Message:" + message + " Sent");
		        }
		    }
		});


		// Add components to the frame using GridBagLayout
		gbc.gridx = 0;
		gbc.gridy = 0; // First row, first column
		frame.add(label, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0; // First row, second column
		frame.add(nameField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1; // Second row, first column
		frame.add(label2, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1; // Second row, second column
		frame.add(connectionField, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1; // Second row, second column
		frame.add(label3, gbc);

		gbc.gridx = 4;
		gbc.gridy = 1;
		frame.add(portField, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 1; // Third row, spanning two columns
		frame.add(submitButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 1; // Fourth row, spanning two columns
		frame.add(externalNameLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 1; // Fourth row, spanning two columns
		frame.add(externalSupervisorLabel, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 1; // Fourth row, spanning two columns
		frame.add(attendButton, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 5;
		gbc.gridwidth = 1;
		frame.add(messageField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 1; // Fifth row, spanning two columns
		frame.add(nameScrollPane, gbc);
		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 1; // Fifth row, spanning two columns
		frame.add(nameScrollPane2, gbc);

		// Set frame visibility
		frame.setVisible(true);
	}
	
    // Setter for the Connection object
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

	/*private static void populateTemporaryNames() {
		List<String> temporaryNames = Arrays.asList("Erik", "Simon");
		// List<String> temporarySupervisor = Arrays.asList("Hello", "There");

		// Add names to the list model
		for (String name : temporaryNames) {
			//nameListModel.addElement(name);
			supervisorListModel.addElement(name);
		}
	}*/

	public void populateQueue(List<String> externalList) {
		// Loop over the externalList directly
		for (String name : externalList) {
			nameListModel.addElement(name); // Assuming nameListModel is your list model
		}
	}
	
	 public void populateSupervisors(List<String> supervisorList) {
	        for (String name : supervisorList) {
	            supervisorListModel.addElement(name);
	        }
	    }

	public void clearList() {
		supervisorListModel.clear(); // Clear the existing list in the model
	}
	
	public void clearQueueList() {
		nameListModel.clear(); // Clear the existing list in the model
	}
	
	public void clearSupervisorList() {
		supervisorListModel.clear(); // Clear the existing list in the model
	}

	public void showPopupMessage(String message) {
	    JOptionPane.showMessageDialog(null, message, "Message", JOptionPane.INFORMATION_MESSAGE);
	}
	
	// Method to simulate adding external names (like receiving from a queue)
	public void addExternalName(String name) {
		nameListModel.addElement(name);
	}

	public JLabel getExternalNameLabel() {
		return externalNameLabel;
	}

	// Method to get the submit button for adding action listeners
	public JButton getSubmitButton() {
		return submitButton; // Returns the submit button so you can add a listener
	}
	
	public String getMessage() {
		return message;
	}

	// Method to update external name in the label
	public String updateExternalName(JLabel externalNameLabel, String newName) {
		externalNameLabel.setText("External Name: " + newName);
		return newName;
	}

	// Getter method to retrieve the entered name
	public String getEnteredName() throws InterruptedException {
		synchronized (lock) {
			while (enteredName == null) {
				lock.wait(); // Wait until the user enters a name
			}
			return enteredName;
		}
	}

	// Getter method to retrieve the entered name
	public String getEnteredAdress() throws InterruptedException {
		synchronized (lock) {
			while (enteredAdress == null) {
				lock.wait(); // Wait until the user enters a name
			}
			return enteredAdress;
		}

	}

	public int getEnteredPort() throws InterruptedException {
		synchronized (lock) {
			while (enteredPort == null) {
				lock.wait(); // Wait until the user enters a port number
			}
			try {
				return Integer.parseInt(enteredPort); // Parse the string to an integer
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid port number entered: " + enteredPort);
			}
		}
	}
}