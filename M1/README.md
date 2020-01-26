# Milestone 1: Client and Persistent Storage Server

## Design
The objective is to implement a persistent storage server with a key-value query interface. A cache is configured in the server's main memory for a small fraction of the data to allow for quick accesses for frequently used key-value pairs. These tasks require the development of components such as the communication protocol, storage server, and storage client. The design is explained below.

### Communication Logic
The object serialization and deserialization is handled by a simple communication protocol to allow for easy maintainability . It is currently a simple string consisting of `<StatusType> <Key> <Value>` that is sent as a byte array through the network. As our API scales and we start to handle more complex objects, we may transition to use json as our message format. But currently we believe that there is no need to constrain ourselves to more complex formats until we realize our future milestones.

### Storage Server


### Storage Client

The KVClient is similar to the echoClient in milestone 0. We kept the user interface consistent with echoClient's user interface because it is functional, simple, and easily scalable for new features. KVStore currently acts as a wrapper for the communication module (KVCommModule), which is shared between KVStore and KVServer. KVStore might be expanded upon in future milestones and we decided it is a good idea for now to leave all communication logic in KVCommModule. The client-side KVCommModule primarily deals with socket connection, socket disconnection, socket error-handling, and sending/recieving messages.

## Tests


## Performance