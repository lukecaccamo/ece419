# Milestone 2: Scalable Storage Service
By: Yi Zhou, Matthew Lee, Luke Caccamo

## Design

### External configuration service (ECS)

#### Failure detection and Recovery

#### Adding and Removing Nodes with Replication

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
- testReplication - Set up 2 servers, put a value to server1. Then connect to server2 and execute a get request. Check
that the you can get the value that was put in server1 from server2.

- testClientGetTimeout - Connect a client to the server. Kill the server then execute a get request. Check that a TimeOutException
                         is thrown by the client.

- testClientPutTimeout - Connect a client to the server. Kill the server then execute a put request. Check that a TimeOutException
                       is thrown by the client.   

- testClientDeleteTimeout - Connect a client to the server. Kill the server then execute a delete request. Check that a TimeOutException
                          is thrown by the client.     

- testStop - Start the ECS and servers with new SSH script accounting for user and password. Send an ECS STOP request and check
             that the servers have stopped.  

- testStart - Start the ECS and servers with new SSH script accounting for user and password. Send an ECS START request and check
            that the servers have started.  

- testShutdown - Start the ECS and servers with new SSH script accounting for user and password. Send an ECS SHUTDOWN request and check
               that the servers have shutdown and the processes have exit.  

- testRemoveNode - Start the ECS and spin up multiple servers. Remove a node and see that the client that was connected to that node times out.
                Then connect to the other server2 and execute a get request to check that the value of the removed server was replicated to the 
                second server.  

- testAddNode - Start the ECS and spin up multiple servers. Put a value to server1. Remove a server2. Then add back server2. Then 
                connect to the other server and execute a get request to check that the value of the removed server was replicated to the 
                second server. 

- testAddTwoNodes - Same as the above test but with 3 servers. Check that they all can serve the same key from a get request
                    since they all replicate to eachother.
  
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