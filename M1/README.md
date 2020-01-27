# Milestone 1: Client and Persistent Storage Server
By: Yi Zhou, Matthew Lee, Luke Caccamo

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

    1. LRU - Least Recently Used
    2. LFU - Least Frequently Used
    3. FIFO - First in First Out
    4. None - All key value pairs are written directly to persistent storage.

When the cache size is exceeded the cache manager evicts entries from the cache based on
the set policy and saves them to persistent storage. Persistent storage is a simple
.db file of key value entries. Each entry consists of 1 valid byte, 4 bytes for the key length, 4 bytes for value length, 
a maximum of 20 bytes for the key string, and a maximum of 128000 bytes for value (since server 
 should be able to take messages of 128kB). The location of the key in the file is saved for quick lookup
and deletion.

### Storage Client

The KVClient is similar to the echoClient in milestone 0. We kept the user interface consistent with echoClient's user interface because it is functional, simple, and easily scalable for new features. KVStore currently acts as a wrapper for the communication module (KVCommModule), which is shared between KVStore and KVServer. KVStore might be expanded upon in future milestones and we decided it is a good idea for now to leave all communication logic in KVCommModule. The client-side KVCommModule primarily deals with socket connection, socket disconnection, socket error-handling, and sending/recieving messages.

## Tests

Additional tests:

    1. testClientDisconnect
        Tests that the KVStore can connect and disconnect to the server.
    2. testMultipleClientConnectSuccess
        Tests that multiple (10) KVStore clients can connect to the server.
    3. testMultipleClientPutSuccess
        Tests that multiple (10) KVStore clients can successfully do a `PUT` request to the server.
    4. testPersistence
        Tests that persistence is working. Created new server with new key value pair and then killed said server and then checked if key value pair was still there.
    5. testGetDisconnected
        Test that the get command fails if the client is not connected.
    6. testPutGet
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

Performance test composed of sequential puts and gets.
Below are the performance numbers for 5000 operations across the various cache strategies:

### LRU

#### Cache size = 5000
| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 67.6696152     | 73.88840597        | 0.01353392304   |
| 80% PUT  20% GET | 43.0623541     | 116.1106982        | 0.008612470821  |
| 50% PUT  50% GET | 17.22682439    | 290.2450206        | 0.003445364879  |
| 20% PUT  80% GET | 3.118042507    | 1603.570185        | 0.0006236085014 |

#### Cache size = 2500
| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 68.10415815    | 73.41695626        | 0.01362083163   |
| 80% PUT  20% GET | 43.89612015    | 113.9052833        | 0.00877922403   |
| 50% PUT  50% GET | 17.5785095     | 284.4382228        | 0.003515701899  |
| 20% PUT  80% GET | 3.144871448    | 1589.890106        | 0.0006289742896 |

#### Cache size = 1000

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 67.7097015     | 73.84466169        | 0.0135419403    |
| 80% PUT  20% GET | 42.80214635    | 116.8165717        | 0.008560429271  |
| 50% PUT  50% GET | 17.06292046    | 293.033072         | 0.003412584092  |
| 20% PUT  80% GET | 3.050123619    | 1639.277821        | 0.0006100247238 |

### LFU

#### Cache size = 5000

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 68.38616856    | 73.11419992        | 0.01367723371   |
| 80% PUT  20% GET | 43.93218851    | 113.8117669        | 0.008786437703  |
| 50% PUT  50% GET | 17.44733449    | 286.5767263        | 0.003489466898  |
| 20% PUT  80% GET | 3.093696787    | 1616.189415        | 0.0006187393574 |

#### Cache size = 2500

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 67.67014305    | 73.88782962        | 0.01353402861   |
| 80% PUT  20% GET | 43.35383779    | 115.3300435        | 0.008670767559  |
| 50% PUT  50% GET | 17.3620709     | 287.9840792        | 0.00347241418   |
| 20% PUT  80% GET | 3.116849568    | 1604.183933        | 0.0006233699136 |

#### Cache size = 1000

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 67.2354303     | 74.36555366        | 0.01344708606   |
| 80% PUT  20% GET | 42.77798038    | 116.8825633        | 0.008555596076  |
| 50% PUT  50% GET | 17.10559654    | 292.3019954        | 0.003421119308  |
| 20% PUT  80% GET | 3.177489064    | 1573.569538        | 0.0006354978128 |

### FIFO

#### Cache size = 5000

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 67.72273616    | 73.83044874        | 0.01354454723   |
| 80% PUT  20% GET | 43.12752688    | 115.9352358        | 0.008625505375  |
| 50% PUT  50% GET | 17.17778572    | 291.0736041        | 0.003435557144  |
| 20% PUT  80% GET | 3.109879237    | 1607.779473        | 0.0006219758474 |

#### Cache size = 2500

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 67.18933573    | 74.4165714         | 0.01343786715   |
| 80% PUT  20% GET | 42.7281874     | 117.0187715        | 0.008545637481  |
| 50% PUT  50% GET | 17.47456607    | 286.130138         | 0.003494913214  |
| 20% PUT  80% GET | 3.024481727    | 1653.1758          | 0.0006048963454 |

#### Cache size = 1000

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 67.79431852    | 73.75249297        | 0.0135588637    |
| 80% PUT  20% GET | 43.24245401    | 115.6271103        | 0.008648490802  |
| 50% PUT  50% GET | 17.01425244    | 293.8712717        | 0.003402850487  |
| 20% PUT  80% GET | 3.118430699    | 1603.370568        | 0.0006236861398 |

### None

| Test             | (s) | (Req/s) | (s/Req) |
|------------------|----------------|--------------------|-----------------|
| 100% PUT         | 65.5684938     | 76.2561363         | 0.01311369876   |
| 80% PUT  20% GET | 42.63341174    | 117.2789086        | 0.008526682349  |
| 50% PUT  50% GET | 16.82100722    | 297.2473606        | 0.003364201444  |
| 20% PUT  80% GET | 3.06518826     | 1631.221177        | 0.000613037652  |