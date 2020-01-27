# Milestone 1: Client and Persistent Storage Server

## Design
The objective is to implement a persistent storage server with a key-value query interface. A cache is configured in the server's main memory for a small fraction of the data to allow for quick accesses for frequently used key-value pairs. These tasks require the development of components such as the communication protocol, storage server, and storage client. The design is explained below.

### Communication Logic
The object serialization and deserialization is handled by a simple communication protocol to allow for easy maintainability . It is currently a simple string consisting of `<StatusType> <Key> <Value>` that is sent as a byte array through the network. As our API scales and we start to handle more complex objects, we may transition to use json as our message format. But currently we believe that there is no need to constrain ourselves to more complex formats until we realize our future milestones.

### Storage Server
The storage server binds to the specified port and starts a thread when the KVServer constructor
is initialized. When the server accepts a new client connection, the server initializes and instance of 
KVCommModule and spawns a new thread for the connection. Each client connection has it's own
thread for concurrency. The KVCommModule handles all the sending and receiving between the client
and server. When a put or get is received by the server the action is executed in the main server thread
to guarantee consistency. Should an error occur during a put, get or delete, the appropriate
exception is thrown (PutException, GetException, DeleteException) and the error message is sent
to the client.

### Caching and Storage
The following caching strategies can be configured:

    1. LRU - Least recently used
    2. LFU - Least frequently used
    3. FIFO - First in First Out
    4. None - All key value pairs are written directly to persistent storage.

When the cache size is exceeded the cache manager evicts entries from the cache based on
the set policy and saves them to persistent storage. Persistent storage is a simple
text file of key value pairs. The location of the key in the file is saved for quick lookup
and deletion.

### Storage Client

The KVClient is similar to the echoClient in milestone 0. We kept the user interface consistent with echoClient's user interface because it is functional, simple, and easily scalable for new features. KVStore currently acts as a wrapper for the communication module (KVCommModule), which is shared between KVStore and KVServer. KVStore might be expanded upon in future milestones and we decided it is a good idea for now to leave all communication logic in KVCommModule. The client-side KVCommModule primarily deals with socket connection, socket disconnection, socket error-handling, and sending/recieving messages.

## Tests

Additional tests:

    1. testStoreDisconnect
        Tests that the KVStore can connect and disconnect to the server.
    2. testMultipleStoreConnectSuccess
        Tests that multiple (10) KVStore clients can connect to the server.
    3. testGetDisconnected
        Test that the get command fails if the client is not connected.
    4. testPutGet
        Tests that put and get functions in KVServer function correctly.
        Checks the inCache function for consistency.
    
    The following test input corner cases:
    
    5. testDeleteNonExistingKey
        Ensure that the server returns a DELETE_ERROR when the client attempts 
        to delete a nonexistent entry.
    6. testPutWithSpacesInValue
        Ensures a value with spaces remains intact throughout serialization/deserialization.
    7. testPutWithSpacesInKey
        Ensures a PUT_ERROR is returned when there are spaces in the key.
    8. testPutWithEmptyKey
        Ensures a PUT_ERROR is returned when the key is empty.
    9. testPutWithLongKey
        Ensures a PUT_ERROR is returned when the key is longer than 20 bytes.
    10.testDeleteWithEmptyValue
        Makes sure a command of put "key" "" executes a delete.

## Performance

Below are the performance numbers for 20,000 operations:


Cache size = 20,000

Cache size = 10,000

Cache size = 5000

No cache