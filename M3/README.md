# Milestone 3: Scalable Storage Service
By: Yi Zhou, Matthew Lee, Luke Caccamo

## Design Document

### External configuration service (ECS)

#### Failure detection and Recovery

For this milestone, the KVServer is changed such that it is initialized with a shutdown hook that will be called when the process gets killed unexpectedly. This shutdown hook will force the KVServer process to disconnect from the ZooKeeper node.

The ECS is changed to detect node failures by creating a new thread that polls if all ZooKeeper nodes currently exists. This failure detection thread gets disabled when handling user commands to prevent command conflicts. If one ZooKeeper node fails, its shutdown hook would immediately disconnect from the ZooKeeper node. The monitoring thread will detect this change and immediately update the metadata for all other nodes such that the node failure is acknowledged and the responsibilities change to the next respective node in the hash ring - similar to a traditional `removeNodes` call. Then, it attempts to start another node in its stead, this is a call to `addNode`.

On ECS shutdown, it sends a request to all of its nodes to finish processing its last command before shutting down. Currently, there is no mechanism to restore its preexisting state and each of its nodes' preexisting state after shutdown, however, this can be implemented in the future by saving the state to storage for persistence. On ECS recovery, it would read its preexisting state from storage to restore each nodes' metadata so that it can serve requests within the hash ranges that was defined prior to the shutdown.

#### Adding and Removing Nodes with Replication

When adding a node, the ECS first updates the metadata and broadcasts the update to the nodes. Then the ECS moves the data in the new node's hash range from the new node's successor to the new node. Then the new node's predecessor moves data from its own range to new node. Similarily, the new node's predecessor's predecessor moves data from its own range to the new node to satisfy replication. We do not delete stale data from the new node's successor (the successor will not need to keep track of data served by 3 servers behind it) and instead implement a range check on each put/get. 

When removing a node, the ECS first moves the data in the removing node's hash range from the removing node to the successor. Then the predecessor of the predecessor of the removing node sends its own data to the successor to satisfy replication. We do not do this with the predecssors as the successor should already have the replication. Then the hash ranges (metadata) are updated (and broadcasted to the nodes) and the data in the new succesor's hash range from the successor is sent to its own successor and its successor's successor. This is done to satisfy replication of the new data (any data that the removing node was reposnsible for).

### Client library (KVStore)

#### Handling Server Failure

In KVStore we added a timeout that runs in a separate thread and times how long the client is blocking on
waiting for a response from the server (on inputStream.read()). If the client has waited too long it is assumed the 
server has died and the client attempts to reconnect to another server in the metadata. If no metadata is present it returns
the user to the prompt. This behaviour is present for the put, get, and delete operations. Then current default timeout
is 2 seconds.

### Storage server (KVServer)

#### Replication Mechanism

The replication mechanism on the server side works as follows:

- Each server attempts to connect to the next two servers in the has ring.
- When the server receives a put request it inserts the value in it's own db before sending replicate messages 
to the two successive servers.
- When a client requests a put, the server checks that it is the coordinator. If it is not the coordinator it updates the client's
metadata and the client reconnects to the coordinator
## JUnit Tests

Since the ECS, KVServer, and KVClient must utilize ZooKeeper to mimic its real behavior, we decided to run tests holistically to determine if each function was implemented correctly.
We decided to write integration tests so that we can mimic a functionality's behavior. This includes the setup/teardown of the ECS, resetting each KVServer's cache and database, and running a `GET`/`PUT`/`DELETE` on the KVClient to determine its correctness.

| Test                    | Functionality                                                                                                                                                                                                                                                                                                                                                                                           |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| testReplication         | Set up 2 servers, `PUT` a value to `server1`. Then connect to `server2` and execute a `GET` request. Check                                                                                                                                                that the you can get the value that was put in `server1` from `server2`.                                                                                  |
| testClientGetTimeout    | Connect a client to the server. Kill the server then execute a `GET` request. Check that a `TimeOutException` is thrown by the client.                                                                                                                                                                                                                                                                      |
| testClientPutTimeout    | Connect a client to the server. Kill the server then execute a `PUT` request. Check that a `TimeOutException` is thrown by the client.                                                                                                                                                                                                                                                                      |
| testClientDeleteTimeout | Connect a client to the server. Kill the server then execute a delete request. Check that a `TimeOutException` is thrown by the client.                                                                                                                                                                                                                                                                   |
| testStop                | Tests if all KVServers were stopped correctly and returns `SERVER_STOPPED` on KVClient. Start the ECS and servers with new SSH script accounting for user and password. Send an ECS `STOP` request and check that the servers have stopped.                                                                                                                                                                |
| testStart               | Tests if all KVServers were started correctly and returns `GET_SUCCESS` and `PUT_SUCCESS` on KVClient. Start the ECS and servers with new SSH script accounting for user and password. Send an ECS `START` request and check that the servers have started.                                                                                                                                                |
| testShutdown            | Tests if all KVServers were shutdown correctly and its designated port is closed. Start the ECS and servers with new SSH script accounting for user and password. Send an ECS `SHUTDOWN` request and check that the servers have shutdown and the processes have exit.                                                                                                                                     |
| testRemoveNode          | Tests if a node was removed correctly by checking the respective nodes' updated hash range/transferred data/replicated data. Start the ECS and spin up multiple servers. Remove a node and see that the client that was connected to that node times out. Then connect to the other `server2` and execute a `GET` request to check that the value of the removed server was replicated to the second server. |
| testAddNode             | Tests if a node was added correctly by checking the respective updated hash range/transferred data/replicated data. Start the ECS and spin up multiple servers. Put a value to `server1`. Remove a `server2`. Then add back `server2`. Then connect to the other server and execute a `GET` request to check that the value of the removed server was replicated to the second server.                           |
| testAddTwoNodes         | Tests if 2 nodes are added correctly by checking the respective updated hash range/transferred data/replicated data. Same as the above test but with 3 servers. Check that they all can serve the same key from a `GET` request since they all replicate to eachother.                                                                                                                                    |
  
## Performance

The following tests were all run with the following settings: Cache Strategy: FIFO, Cache Size: 100
These values were kept constant for fair comparison and the cache size was kept large to limit the impact of
disk IO on the test. the goal was to isolate the features developed in milestone 3 such as replication which
is bottle-necked by the number of network requests. The effect of cache strategy is negligible as can be seen in
the milestone 1 report.

The key value pairs were email messages from the Enron dataset.

100 Puts, 200 Gets

| Number Servers | Number Clients | Time (s) | Requests/Second | Latency (ms) |
| -------------- | -------------- | -------- | --------------- | ------------ |
| 1              | 1              | 1.159    | 258.8438309     | 2.865        |
| 1              | 2              | 1.102    | 272.2323049     | 2.925833333  |
| 1              | 5              | 1.137    | 263.8522427     | 2.704166667  |
| 1              | 10             | 1.227    | 244.4987775     | 2.911666667  |
| 3              | 1              | 3.687    | 81.36696501     | 8.885        |
| 3              | 2              | 3.293    | 91.10233829     | 7.041666667  |
| 3              | 5              | 4.119    | 72.83321194     | 7.366666667  |
| 3              | 10             | 5.635    | 53.23868678     | 8.71         |
| 5              | 1              | 5.886    | 50.96839959     | 14.70583333  |
| 5              | 2              | 6.923    | 43.33381482     | 14.9675      |
| 5              | 5              | 7.102    | 42.24162208     | 13.12833333  |
| 5              | 10             | 8.79     | 34.12969283     | 19.34583333  |

400 Puts, 800 Gets

| Number Servers | Number Clients | Time (s) | Requests/Second | Latency (ms) |
| -------------- | -------------- | -------- | --------------- | ------------ |
| 1              | 1              | 3.438    | 349.0401396     | 11.46        |
| 1              | 2              | 3.511    | 341.7829678     | 11.70333333  |
| 1              | 5              | 3.245    | 369.7996918     | 10.81666667  |
| 1              | 10             | 3.494    | 343.4459073     | 11.64666667  |
| 3              | 1              | 10.662   | 112.5492403     | 35.54        |
| 3              | 2              | 8.45     | 142.0118343     | 28.16666667  |
| 3              | 5              | 8.84     | 135.7466063     | 29.46666667  |
| 3              | 10             | 10.452   | 114.8105626     | 34.84        |
| 5              | 1              | 17.647   | 68.00022667     | 58.82333333  |
| 5              | 2              | 17.961   | 66.81142475     | 59.87        |
| 5              | 5              | 15.754   | 76.17113114     | 52.51333333  |
| 5              | 10             | 23.215   | 51.69071721     | 77.38333333  |

From these tables it can be observed that there is a linear increase in runtime as the number of clients 
and servers increases. This is due to the fact that for every additional server there is an added 2 replication
requests for every put request. Furthermore as the number of servers increases the number of times a 
client has to reconnect to a new server to put or get the correct data. There are cases where the number of clients decreases
the runtime since each client has a thread they can run in parallel eg. in the 400 put, 800 get table with 3 servers the times
for 2 and 5 clients are significantly lower.