import zmq
import os
import json
import time
from collections import deque, defaultdict
from threading import Thread, Event
from threading import Lock

HEARTBEAT_TIMEOUT = 4  # seconds
GRACE_PERIOD = 4  # Additional grace period before actual removal
# File path to store the queue state
STATE_FILE = "queue_state30.json"

def queue_server():
    # Initialize ZeroMQ context and sockets 
    context = zmq.Context()
    request_socket = context.socket(zmq.REP)
    request_socket.bind("tcp://*:5567")

    # PUB socket for broadcasting queue status
    pub_socket = context.socket(zmq.PUB)
    pub_socket.bind("tcp://*:5555")

    # Queue and state variables
    global queue
    queue = deque()
    supervisors_queue = deque()
    last_heartbeat = {}  # to track the heartbeats
    missing_heartbeat = {}  # Track clients who missed heartbeat for the first time
    client_tickets = {}  # Store unique ticket numbers for each client name
    client_names = defaultdict(list)  # Map client_id to list of client names
    client_ids = defaultdict(list)  # Map client names to list of client IDs
    supervisor_names = defaultdict(list)
    ticket_counter = 0
    running = True
    broadcast_event = Event()  # Event to trigger the broadcast

    # Declare supervisor_attendance here
    supervisor_attendance = {}

    def save_state():
        # Save the current state of the queue and clients to a file.
        state_data = {
            "queue": list(queue),  # Save client queue
            "client_names": {client_id: names for client_id, names in client_names.items()},
            "supervisor_names": {client_id: names for client_id, names in supervisor_names.items()},
            "client_tickets": client_tickets,
            "supervisors_queue": list(supervisors_queue),  # Save supervisor queue
            "supervisor_attendance": supervisor_attendance  # Save supervisor attendance
        }
        with open(STATE_FILE, 'w') as f:
            json.dump(state_data, f)
        print("State saved successfully.")
        # print(f"State saved successfully.{state_data}")

    def load_state():
        # Load the state from a file and repopulate queue and client data.
        if os.path.exists(STATE_FILE):
            with open(STATE_FILE, 'r') as f:
                state_data = json.load(f)
            
            global queue, supervisors_queue, supervisor_attendance
            queue = deque(state_data.get("queue", []))
            supervisors_queue = deque(state_data.get("supervisors_queue", []))
            client_names.update(state_data.get("client_names", {}))
            supervisor_names.update(state_data.get("supervisor_names", {}))
            client_tickets.update(state_data.get("client_tickets", {}))
            supervisor_attendance = state_data.get("supervisor_attendance", {})
            print("State loaded successfully.")
           # print(f"State loaded successfully: {state_data}")
        else:
            print("No saved state found. Starting fresh.")
    
    def server_loop():
        load_state()
        broadcast_event.set()
        while running:
            try:
                # Receive a message from clients
                request = request_socket.recv_string(zmq.NOBLOCK)
                response = handle_request(json.loads(request))  # Process the request and generate a response
                request_socket.send_string(json.dumps(response))  # Send the response back immediately
            except zmq.Again:
                pass  # No message received, continue the loop
            # Periodically check for clients that missed heartbeat
            check_heartbeats()

    def assign_ticket(client_id, name):
        # Assign a unique ticket number to the client or supervisor.
        nonlocal ticket_counter
        if name not in client_tickets:
            client_tickets[name] = ticket_counter
            ticket_counter += 1  # Increment ticket counter for the next client   
        client_names[client_id].append(name)  # Associate client_id with name
        client_ids[name].append(client_id)
        print(f"Assigned ticket {client_tickets[name]} to client {name}.")


    def remove_all_clients_by_name(name):
        global queue
        # Filter out all clients with the same name from the queue
        queue = deque([client_id for client_id in queue if client_names[client_id][0] != name])

        # Clean up client_ids and client_names entries for the specified name
        if name in client_ids:
            for client_id in client_ids[name]:
                if client_id in client_names:
                    del client_names[client_id]
            del client_ids[name]

        print(f"Removed all clients with the name {name} from the queue.")

    def handle_request(data):
        print(f"Received request: {data}")  # Log received requests

        client_id = data.get("clientId")
        is_heartbeat = data.get("heartbeat", False)
        is_supervisor = data.get("isSupervisor", False)
        enter_queue = data.get("enterQueue", False)
        attend_request = data.get("attend", False)

        if is_heartbeat:
            last_heartbeat[client_id] = time.time()  # Update heartbeat timestamp
            if client_id in missing_heartbeat:
                del missing_heartbeat[client_id]
            print(f"Heartbeat acknowledged for client: {client_id}")
            #return {"status": "heartbeat_ack"}  # Return heartbeat acknowledgment

            # Check if the supervisor needs to be re-added
            if is_supervisor and client_id not in supervisors_queue:
                supervisors_queue.append(client_id)
                print(f"Supervisor {client_id} re-added to the supervisors queue after reconnection.")
                broadcast_event.set()  # Trigger broadcast to update GUI
            
            return {"status": "heartbeat_ack"}  # Return heartbeat acknowledgment

        name = data.get("name")
        message = data.get("message")

        if enter_queue:
            if client_id not in queue and not is_supervisor:
                queue.append(client_id)
                print(f"Client {name} with ID {client_id} added to the queue.")
                assign_ticket(client_id, name)
                last_heartbeat[client_id] = time.time()
                broadcast_event.set()  # Trigger immediate broadcast when joining
                

            elif is_supervisor and client_id not in supervisors_queue:
                supervisors_queue.append(client_id)
                print(f"Supervisor {name} added to the supervisors queue.")
                assign_ticket(client_id, name)
                last_heartbeat[client_id] = time.time()
                broadcast_event.set()  # Trigger broadcast for supervisor entry

        # If the supervisor sends an attend request
        if attend_request and is_supervisor:
            if len(queue) > 0:
                # Find the client with the lowest ticket number
                lowest_ticket_client_id = min(queue, key=lambda client_id: client_tickets[client_names[client_id][0]])
                attended_student_name = client_names[lowest_ticket_client_id][0]

                # Remove the attended client from the queue
                queue.remove(lowest_ticket_client_id)
                print(f"Supervisor {name} attended to {attended_student_name}.")
                supervisor_attendance[name] = attended_student_name  # Track which student the supervisor is attending to

                # Remove all other clients with the same name from the queue
                remove_all_clients_by_name(attended_student_name)

                # Send message to the attended client
                send_message_to_client(attended_student_name, name, message)

                broadcast_event.set()  # Broadcast after attending
                return {"status": "attended", "attended_student": attended_student_name}
            else:
                print("No students in queue to attend.")
                return {"status": "no_students_in_queue"}

        ticket = str(client_tickets[name])
        return {"name": name, "ticket": ticket}

    def send_message_to_client(client_name, supervisor_name, message):
        data = {
            "supervisor": supervisor_name,
            "message": message
        }
        client_topic = client_name
        print(f"Sending message to client {client_name} on topic {client_topic}: {data}")
        pub_socket.send_string(client_topic, zmq.SNDMORE)
        pub_socket.send_json(data)

    def check_heartbeats():
        current_time = time.time()
        to_remove_clients = []
        to_remove_supervisors = []

        # Iterate over the heartbeat tracking dictionary
        for client_id, last_seen in list(last_heartbeat.items()):
            # Check if the client has missed the heartbeat timeout
            if client_id in queue and current_time - last_seen > HEARTBEAT_TIMEOUT:
                if client_id not in missing_heartbeat:
                    missing_heartbeat[client_id] = current_time
                    print(f"Client {client_id} missed heartbeat, marking as potentially inactive.")
                elif current_time - missing_heartbeat[client_id] > GRACE_PERIOD:
                    # Find the client's name using client_id from the `client_names` dictionary
                    names = client_names.get(client_id)
                    if names:
                        for name in names:
                            print(f"Removing client {client_id} ({name}) due to missed heartbeat after grace period.")
                            to_remove_clients.append((client_id, name))  # Append client_id for removal

        # Check supervisor heartbeats
        for client_id, last_seen in list(last_heartbeat.items()):
            if client_id in supervisors_queue and current_time - last_seen > HEARTBEAT_TIMEOUT:
                if client_id not in missing_heartbeat:
                    missing_heartbeat[client_id] = current_time
                    print(f"Supervisor {client_id} missed heartbeat, marking as potentially inactive.")
                elif current_time - missing_heartbeat[client_id] > GRACE_PERIOD:
                            print(f"Removing supervisor {client_id}) due to missed heartbeat after grace period.")
                            to_remove_supervisors.append((client_id))

        # Remove clients that missed heartbeat
        for client_id, name in to_remove_clients:
            if client_id in last_heartbeat:
                del last_heartbeat[client_id]
            if client_id in missing_heartbeat:
                del missing_heartbeat[client_id]
            if client_id in client_names:
                if name:
                    client_names[client_id].remove(name)
                if not client_names[client_id]:
                    del client_names[client_id]
            if name and client_id in client_ids[name]:
                client_ids[name].remove(client_id)
                if not client_ids[name]:
                    del client_ids[name]
            if client_id in queue:
                queue.remove(client_id)
                print(f"Client {name} with ID {client_id} removed from the queue.")

        # Remove supervisors that missed heartbeat
        for client_id in to_remove_supervisors:
            if client_id in last_heartbeat:
                del last_heartbeat[client_id]
            if client_id in missing_heartbeat:
                del missing_heartbeat[client_id]
            if client_id in supervisor_names:
                del supervisor_names[client_id]
            if client_id in supervisors_queue:
                supervisors_queue.remove(client_id)
                print(f"Supervisor with ID {client_id} removed from the supervisors queue.")

        # Trigger broadcast and save state whenever a client or supervisor is removed
        if to_remove_clients or to_remove_supervisors:
            broadcast_event.set()
            save_state()  # Ensure state is saved after removals        

    def broadcast_queue_status():
        while running:
            broadcast_event.wait()  # Wait until broadcast is triggered
            broadcast_event.clear()  # Reset the event
            time.sleep(1)

            # Prepare queue status with unique names and lowest tickets
            distinct_queue_status = {}
            for client_id in queue:
                name = client_names[client_id][0]
                ticket = client_tickets.get(name)
                if name and ticket is not None:
                    # Only keep the entry if it's the lowest ticket for this name
                    if name not in distinct_queue_status or ticket < distinct_queue_status[name]["ticket"]:
                        distinct_queue_status[name] = {"ticket": ticket, "name": name}

            # Convert to list and sort by ticket number
            queue_status = sorted(distinct_queue_status.values(), key=lambda x: x["ticket"])

            # Send queue status to clients
            pub_socket.send_string("queue", zmq.SNDMORE)
            pub_socket.send_json(queue_status)

            # Prepare supervisors status
            supervisors_queue_status = []
            for client_id in supervisors_queue:
                name = client_names[client_id][0]
                supervisor_status = {
                    "name": name,
                    "status": "occupied" if name in supervisor_attendance else "available",
                    "client": {
                        "ticket": client_tickets[supervisor_attendance[name]],
                        "name": supervisor_attendance[name]
                    } if name in supervisor_attendance else None
                }
                if name:  # Only add supervisors with valid names
                    supervisors_queue_status.append(supervisor_status)

            pub_socket.send_string("supervisors", zmq.SNDMORE)
            pub_socket.send_json(supervisors_queue_status)
            save_state()

    # Start main server loop and broadcast thread
    Thread(target=server_loop, daemon=True).start()
    Thread(target=broadcast_queue_status, daemon=True).start()

    def stop():
        nonlocal running
        running = False
        request_socket.close()
        pub_socket.close()
        context.term()

    return stop

def start_server():
    stop_server = queue_server()
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        stop_server()

if __name__ == "__main__":
    start_server()