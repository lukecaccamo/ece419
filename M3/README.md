# Milestone 3: Scalable Storage Service
By: Yi Zhou, Matthew Lee, Luke Caccamo

## Design Document

### External configuration service (ECS)

#### Failure detection and Recovery

For this milestone, the KVServer is changed such that it is initialized with a shutdown hook that will be called when the process gets killed unexpectedly. This shutdown hook will force the KVServer process to disconnect from the ZooKeeper node.

The ECS is changed to detect node failures by creating a new thread that polls if all ZooKeeper nodes currently exists. This failure detection thread gets disabled when handling user commands to prevent command conflicts. If one ZooKeeper node fails, its shutdown hook would immediately disconnect from the ZooKeeper node. The monitoring thread will detect this change and immediately update the metadata for all other nodes such that the node failure is acknowledged and the responsibilities change to the next respective node in the hash ring - similar to a traditional `removeNodes` call. Then, it attempts to start another node in its stead, this is a call to `addNode`.

On ECS shutdown, it sends a request to all of its nodes to finish processing its last command before shutting down. Currently, there is no mechanism to restore its preexisting state and each of its nodes' preexisting state after shutdown, however, this can be implemented in the future by saving the state to storage for persistence. On ECS recovery, it would read its preexisting state from storage to restore each nodes' metadata so that it can serve requests within the hash ranges that was defined prior to the shutdown.

#### Adding and Removing Nodes with Replication

### Client library (KVStore)

#### Handling Server Failure

### Storage server (KVServer)

#### Replication Mechanism

## JUnit Tests
Since the ECS, KVServer, and KVClient must utilize ZooKeeper to mimic its real behavior, we decided to run tests holistically to determine if each function was implemented correctly.
We decided to write integration tests so that we can mimic a functionality's behavior. This includes the setup/teardown of the ECS, resetting each KVServer's cache and database, and running a `GET`/`PUT`/`DELETE` on the KVClient to determine its correctness.

| Test                    | Functionality                                                                                                               | Passed |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------- | ------ |
| testReplication         |                                                                                                                             | True   |
| testClientGetTimeout    |                                                                                                                             | True   |
| testClientPutTimeout    |                                                                                                                             | True   |
| testClientDeleteTimeout |                                                                                                                             | True   |
| testStop                | Tests if all KVServers are stopped correctly and returns `SERVER_STOPPED` on KVClient.                                      | True   |
| testStart               | Tests if all KVServers are started correctly and returns `GET_SUCCESS` and `PUT_SUCCESS` on KVClient.                       | True   |
| testShutdown            | Tests if all KVServers are shutdown correctly and its designated port is closed.                                            | True   |
| testRemoveNode          | Tests if a node is removed correctly by checking the respective nodes' updated hash range/transferred data/replicated data. | True   |
| testAddNode             | Tests if a node is added correctly by checking the respective updated hash range/transferred data/replicated data.          | True   |
| testAddTwoNodes         | Tests if 2 nodes are added correctly by checking the respective updated hash range/transferred data/replicated data.        | True   |
  
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