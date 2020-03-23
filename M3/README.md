# Milestone 2: Scalable Storage Service
By: Yi Zhou, Matthew Lee, Luke Caccamo

## Design

### External configuration service (ECS)

#### Failure detection and Recovery

#### Adding and Removing Nodes with Replication

### Client library (KVStore)

#### Handling Server

### Storage server (KVServer)

#### Replication Mechanism

## JUnit Tests
Since the ECS, KVServer, and KVClient must utilize ZooKeeper to mimic its real behavior, we decided to run tests holistically to determine if each function was implemented correctly.
We decided to write integration tests so that we can mimic a functionality's behavior. This includes the setup of the ECS, resetting each KVServer's cache and database, and running a KVClient to determine its output.

| Test | Number Clients | Time (s) |
|----------------|----------------|----------|
| testReplication              | 1              | 1.159    |
| testClientGetTimeout              | 2              | 1.102    |
| testClientPutTimeout              | 5              | 1.137    |
| testClientDeleteTimeout              | 10             | 1.227    |
| testStop              | 1              | 3.687    |
| testStart              | 2              | 3.293    |
| testShutdown              | 5              | 4.119    |
| testRemoveNode              | 10             | 5.635    |
| testAddNode              | 1              | 5.886    |
| testAddTwoNodes              | 2              | 6.923    |
  
## Performance

The following tests were all run with the following settings: Cache Strategy: FIFO, Cache Size: 100
These values were kept constant for fair comparison and the cache size was kept large to limit the impact of
disk IO on the test. the goal was to isolate the features developed in milestone 3 such as replication which
is bottle-necked by the number of network requests. The effect of cache strategy is negligible as can be seen in
the milestone 1 report.

The key value pairs were email messages from the Enron dataset.

100 Puts, 200 Gets

| Number Servers | Number Clients | Time (s) |
|----------------|----------------|----------|
| 1              | 1              | 1.159    |
| 1              | 2              | 1.102    |
| 1              | 5              | 1.137    |
| 1              | 10             | 1.227    |
| 3              | 1              | 3.687    |
| 3              | 2              | 3.293    |
| 3              | 5              | 4.119    |
| 3              | 10             | 5.635    |
| 5              | 1              | 5.886    |
| 5              | 2              | 6.923    |
| 5              | 5              | 7.102    |
| 5              | 10             | 8.79     |

400 Puts, 800 Gets

| Number Servers | Number Clients | Time (s) |
|----------------|----------------|----------|
| 1              | 1              | 3.438    |
| 1              | 2              | 3.511    |
| 1              | 5              | 3.245    |
| 1              | 10             | 3.494    |
| 3              | 1              | 10.662   |
| 3              | 2              | 8.45     |
| 3              | 5              | 8.84     |
| 3              | 10             | 10.452   |
| 5              | 1              | 17.647   |
| 5              | 2              | 17.961   |
| 5              | 5              | 15.754   |
| 5              | 10             | 23.215   |

From these tables it can be observed that there is a linear increase in runtime as the number of clients 
and servers increases. This is due to the fact that for every additional server there is an added 2 replication
requests for every put request. Furthermore as the number of servers increases the number of times a 
client has to reconnect to a new server to put or get the correct data. There are cases where the number of clients decreases
the runtime since each client has a thread they can run in parallel eg. in the 400 put, 800 get table with 3 servers the times
for 2 and 5 clients are significantly lower.