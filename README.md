 # Tinyqueue assignment

## Authors: 
- Violeta Janikuniene (a22vioja)
- Isak Karatay (d22isaka)

- To implement our solution we were using Python 3.12.4 and Java"OpenJDK version 21.0.1"programming languages. 
- Python was used to create a server and Java was used for student client and supervisor.

## Server
### Application Configuration, Compilation, and Execution

- The server application is a Python script that uses ZeroMQ for communication between a queue server and multiple clients. The server manages a queue of clients, assigning ticket numbers, and broadcasting queue updates via ZeroMQ's PUB-SUB model.
- ZeroMQ was used to handle communication between the server and clients.The server listens for the client requests using the REP (request-reply) pattern on port 5567 and broadcasts queue updates using the PUB (publish-subscribe) pattern on port 5555. The server has a queue for clients, tracks heartbeat signals and removes inactive clients if heartbeat was missed.

- Server runs in a loop and listens for incoming requests from clients and assigns them unique tickets. When server gets a request it assigns the client a ticket, and adds the client to the queue. Also heartbeats from the clients are tracked, and clients that miss heartbeats are removed from the queue.

- Server has a different queue and request handling for Supervisors. It allows supervisors to send request and attend the first student in the queue. The student client is removed and student is served by supervisor. 

- Any events in the form of connecting clients or changes in the queue is printed in console.

- If two clients connecting with the same name they hold the same place in the queue.

- The server complies with the Tinyqueue APILinks to an external site and communicates broadcast messages using a ZMQ publisher-socket. 

- Individual client requests is handled using a ZMQ response socket.

- The server is able to handle at least 5 clients connected simultaneously, with up to 5 different students in queue.

- The server allows several ZMQ clients associated with the same student (name).

### Installing ZeroMQ and Dependencies

 - Pyzmq, json, and threading dependencies were installed using terminal with the command "pip install pyzmq" . The json module is part of Python's standard library, so there was no need to install it separately. "collections.deque" also comes in Python's standard library and was used to maintain the queue of clients. Same as "threading.Thread" dependency, which server uses to handle the threads allowing the application to perform other tasks while the server listens for requests. "time" dependency is used to track heartbeats and manage timing.

## Client

### Application Configuration, Compilation, and Execution

- The Client application consists of multiple Java classes: 
- * Client: sends requests to the server and receiving messages from the queue.
- * Connection: manages ZeroMQ sockets for communication between the client and server.
- * Gui: provides a simple graphical interface for user interaction, allowing users to input their name, server address, and port number.It outputs the queue of the student clients and Supervisors.
- * Heartbeats: sends heartbeat messages to the server to maintain the connection.
- When compiled, the application is executed from the Client class, which has the main method.
- The client uses the ZeroMQ library for communication with the server. A REQ socket is used for sending requests to the server.A SUB socket is used for subscribing to messages from the server.
-Available supervisors is displayed in the GUI, along with status information and information about which student they are currently helping. 
- The users place in the queue is clearly visible.
- The user is notified when it is their turn, and message from the supervisor is displayed.


### Installing ZeroMQ and Dependencies
- To include the ZeroMQ JAR file and org.json.JSONObject in our projec, "pom.xml" file was updated with dependencies for Maven. 

'	<dependency>
   			 <groupId>org.zeromq</groupId>
    		<artifactId>jeromq</artifactId>
    		<version>0.6.0</version>
		</dependency>

     <dependency>
		    <groupId>org.json</groupId>
		    <artifactId>json</artifactId>
		    <version>20210307</version>
	  </dependency> 

	  <dependency>
	    <groupId>org.zeromq</groupId>
	    <artifactId>jeromq</artifactId>
	    <version>0.5.2</version>
	  </dependency>
'
## Supervisor

- The supervisor is a copy of a student client with some modifications for funktionality.
- Supervisor can attend the firs student in student client queue and send the message for the student as a pop-up table that student is being served.
- Supervisor can see all the students in the queue and all the supervisors which are available or occupied. 
- The supervisor is able to specify a message that the student receives when notified about supervision.
- The supervisor client extends the Tinyqueue APILinks to an external site. with messages for connecting as supervisor, updating supervisor status, and attending students.
- The server is updated to accommodate the supervisor client.


## Reliability

- Student client can maintain their place in the queue even with an unreliable connection.
- The server removes clients that are inactive for more than 4 seconds, but shorter breaks in connection is handled transparently to the user.
- If the server crashes and restarts, clients transparently reconnects and re-establish their place in the queue.


## ISSUE to fix

After reloading the server all clients are reconecting in the same order as they had tickets assigned. Supervisors reconecting as well, because it can attend the students, send messages and send heartbeats. The only thing is that server does not broadcast the queue status for the supervisors and clients after reconection. 
The save_state (state_data) shows that supervisors are saved in the clients queue: We managed to fixed it and we have this code where the load_state and save_state is working as expected, but then other functionality is destroyed (when to clients with same names are conected and one of them leaves, another one disappears.) We chose to stay with this version instead, to fullfill all the requirements for this assigment with a small issue (supervisor is not showing up on GUI after server reconects.)