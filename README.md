## Frameworks
```
+----------------------+       +-----------------------+
|   Booking Service    |       |   Transaction Coord.  |
|   (Publishes events) | <---- |   (Manages 2PC)       |
+----------------------+       +-----------------------+
             |                              |
             v                              |
+----------------------+                    |
|  Pub/Sub System      | <------------------+
+----------------------+
             |
             v
+----------------------+
|  Flight Booking Svc  | (Subscribes to events, participates in 2PC)
+----------------------+
             |
             v
+----------------------+
|  Hotel Booking Svc   | (Subscribes to events, participates in 2PC)
+----------------------+
             |
             v
+----------------------+
|  Firestore Database  | (Stores data, participates in 2PC)
+----------------------+
             |
             v
+----------------------+
|  RAFT Consensus      | (Ensures log consistency across nodes -- Already implemented in Firestore)
+----------------------+
             |
             v
+----------------------+
|  PBFT Algorithm      | (Practically Handles Byzantine Faults)
+----------------------+
```

## Architectural Overview

1. **Booking Service**: Publishes booking events (e.g., hotel or flight booking requests) to the Pub/Sub system. It interacts with the Transaction Coordinator for managing distributed transactions.
2. **Transaction Coordinator Service**: Manages two-phase commit (2PC) protocols to ensure atomic and consistent transactions across multiple services (e.g., flight booking service, hotel booking service, and Firestore).
3. **Pub/Sub System**: Acts as a message broker to facilitate communication between services. The Booking Service publishes events, and the Flight and Hotel Booking Services subscribe to these events.
4. **Flight Booking Service**: Subscribes to events from the Pub/Sub system and participates in 2PC managed by the Transaction Coordinator.
5. **Hotel Booking Service**: Similarly, subscribes to events and participates in 2PC.
6. **Firestore Database**: Stores data and participates in 2PC to ensure consistent state across the distributed system.
7. **RAFT Consensus**: Ensures log consistency across distributed nodes, which helps maintain a consistent state in the system. It is particularly useful for leader election and ensuring a single source of truth in the system. (Already implemented in Firestore)
8. **PBFT Algorithm**: Handles Byzantine faults to ensure the system can tolerate and function correctly even if some nodes exhibit arbitrary or malicious behavior. This is particularly important in environments where nodes may not be fully trusted.

## Interaction Flow
Interaction and Integration
Given the scenario where the travel agency interacts with suppliers (Hotel and Flight) using a Pub/Sub system, and then processes bookings with a transaction coordinator to ensure consistency and fault tolerance, here's how the components should interact:

1. **Client Requests Travel Packages**:
    - **Client -> BookingController**: Client requests travel packages.
    - **BookingController -> Pub/Sub System**: Publishes messages to Hotel and Flight Pub/Sub topics.
    - **Hotel/Flight Services -> Pub/Sub System**: Hotel and Flight services respond with available packages.
    - **Pub/Sub System -> BookingController**: Aggregates responses and returns them to the client.
  
2. **Client Initiates Booking**:
    - **Client -> BookingController**: Client sends a request to book a travel package.
    - **BookingController -> TransactionCoordinatorService**: Forwards the booking request to `TransactionCoordinatorService`.
    - **TransactionCoordinatorService -> 2PC**: Initiates a 2PC to ensure all involved services can commit to the booking.
    - **TransactionCoordinatorService -> Firestore**: Stores the booking details and updates the travel packages in Firestore corresponding to respective users.

3. **User Profile and Customer Management**:
    - **Client -> FirestoreController**: Client manages user profiles and customer-related data.
    - **FirestoreController -> Firestore**: Performs CRUD operations on user and customer data.

## Detailed Interaction:

#### Step 1: Client Requests Travel Packages
- **Client -> BookingController**: Client sends a request to get travel packages.
- **BookingController -> Pub/Sub System**: `BookingController` publishes messages to Hotel and Flight Pub/Sub topics.
- **Hotel/Flight Services -> Pub/Sub System**: Hotel and Flight services respond with available packages.
- **Pub/Sub System -> BookingController**: `BookingController` aggregates the responses and returns them to the client.

#### Step 2: Client Initiates Booking
- **Client -> BookingController**: Client sends a request to book a travel package.
- **BookingController -> TransactionCoordinatorService**: `BookingController` forwards the booking request to `TransactionCoordinatorService`.
- **TransactionCoordinatorService -> 2PC**: `TransactionCoordinatorService` initiates a 2PC to ensure all involved services (Hotel and Flight) can commit to the booking.
- **TransactionCoordinatorService -> Firestore**: `TransactionCoordinatorService` stores the booking details under the user's document in Firestore.

## Step-by-Step Flow:

1. **Initiating a Booking (Pub/Sub + Transactional RPC)**:
   - The client initiates a booking request.
   - The booking service publishes a message to a Pub/Sub topic indicating a new booking request.
   - Services like hotel booking, flight booking, and payment subscribe to this topic.

2. **Coordinating the Transaction (Transaction Coordinator + 2PC)**:
   - The transaction coordinator starts a new transaction.
   - The transaction coordinator sends prepare requests (part of 2PC) to the hotel booking, flight booking, and payment services.
   - Each service prepares by checking if the resources (rooms, flights, funds) are available and locks them.
   - Each service responds with a yes or no vote.

3. **Executing the Transaction (Transactional RPC + Firestore)**:
   - If all services vote yes, the transaction coordinator sends a commit request to all services.
   - Each service then commits the transaction, updating their state and Firestore with the booking details.
   - If any service votes no, the transaction coordinator sends an abort request, and all services revert any changes.

4. **Ensuring Consistency and Fault Tolerance (RAFT)**:
   - RAFT is used to ensure that the logs of operations (e.g., bookings) are consistent across all nodes.
   - If a node fails, RAFT ensures that a new leader is elected, and the state is consistent across the remaining nodes.

5. **Securing Fault Tolerance (PBFT)**:
   - Ensures the system can handle and recover from arbitrary (Byzantine) failures.
   - Requires additional complexity and integration of BFT algorithms.

## Components Overview:
Correct, the `BookingController` and `BrokerPublisherService` do not directly utilize `BookingResponse` or `BookingTransaction`. These classes are used internally within the `TransactionCoordinatorService` to manage and coordinate the transactions. Here's a summary of their roles:

1. **`BookingController`**:
   - Handles incoming requests from the client for booking travel packages.
   - Forwards booking details to the `BrokerPublisherService` to publish messages to the Pub/Sub system.

2. **`BrokerPublisherService`**:
   - Publishes booking messages to the Pub/Sub system.
   - Handles incoming push notifications from the Pub/Sub system and forwards them to the `TransactionCoordinatorService`.

3. **`TransactionCoordinatorService`**:
   - Manages the booking transactions.
   - Uses `BookingResponse` to parse responses from services.
   - Uses `BookingTransaction` to store and manage the transaction details in Firestore.
   - Implements the 2PC protocol to coordinate and commit/abort transactions.

4. **`FirestoreController`**: 
   - Manages user profiles and customer-related operations.

## Implementing Fault Tolerance with PBFT
Given the use cases and applications for PBFT, it should be integrated into critical areas where Byzantine faults can cause significant issues:

1. **Booking Confirmation**:
    - Before finalizing a booking, use PBFT to ensure that all nodes agree on the booking details. This prevents any malicious node from altering the booking information.

2. **Payment Processing**:
    - Integrate PBFT to validate payment transactions. This ensures that all nodes verify and agree on the payment details before processing.

3. **Data Consistency**:
    - Use PBFT to ensure that the data stored in Firestore remains consistent across all nodes. This can be done by having PBFT consensus before writing any critical data.

4. **RAFT and PBFT Integration**:
    - RAFT ensures log consistency and leader election, while PBFT ensures the integrity of the data even in the presence of malicious nodes. Combining these ensures a robust and fault-tolerant system.

By integrating PBFT in these critical areas, the system ensures high reliability and security, preventing arbitrary or malicious failures from disrupting the service.

## Add firebaseadminsdk
in the src/main/java/be.kuleuven.dsgt4/auth, create a new file called
```
firebase-adminsdk.json
```
Then copy and paste the follwing to this json file
```
The content is in our Whatsapp
```

## Setup and Execution
### Step 1: Run the Emulator Locally
    ./firebase-tools-linux emulators:start

    or

    firebase emulators:start

To change it to use the cloud, please change the index.js and the WebSecurityConfig
### Step 2: Start Spring application
    mvn spring-boot:run

### Step 3: Access the Application and Sign Up
  http://localhost:8080
  <img src="signup.png" alt="alt text" width="600" height="300">

### Step 4: Access the Firestore Emulator Firestore (database monitor)
  http://localhost:8081/firestore
  <img src="firestore_signup_result.png" alt="alt text" width="600" height="300">

### 3.1.1 REST Controller and WebClient
- **REST Controller**: Verify the REST controller included in the sample project is functioning as intended.
- **WebClient Development**: The WebClient component is missing and needs to be developed to interact with web services.

### 3.1.2 Web Security Configuration
- **Search for Configuration**: In the sample project, look for the following imports, which are related to web security:
  ```java
  import org.springframework.security.config.annotation.web.builders.HttpSecurity;
  import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
  ```
- **Current Configuration**: The "auth.WebSecurityConfig" and "SecurityFilter" in the sample code use a configuration that includes deprecated APIs.
- **Adaptation Strategy**: Update the security configuration by:
  1. Analyzing how Professor Bert configured the project.
  2. Reviewing how the sample project implemented these configurations.
  3. Developing our version based on the above findings.

### 3.1.3 Security Filter Comparison
- **Security Filter Analysis**: Compare the "SecurityFilter" implementation in our sample code, particularly looking at "JwtAuthenticationFilter", to identify similarities and differences.

### 3.1.4 Data Repository Integration
- **Repository Implementation**: The sample project uses a Spring Framework data repository.
- **Modification Requirement**: Modify the existing data repository implementation to utilize Firestore, aligning it with the project provided by Professor Bert.

This structured breakdown provides a clearer roadmap for your project tasks, focusing on what needs to be checked, developed, or adapted in your software development effort.
