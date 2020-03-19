# Milestone 2: Scalable Storage Service
By: Yi Zhou, Matthew Lee, Luke Caccamo

## Design

### External configuration service (ECS)
The ECS begins by creating a ZooKeeper process and then initializing the client library. The ECS is designed such that we have a queue of free servers and a hash ring for our used servers. Servers that have not been initialized sit in the free servers queue. Once they are added to the hash ring, their hash ranges will be computed before the SSH call initializes the server. The initialized server will create a ZooKeeper node, which the ECS will wait until it is created. Afterwards, the ECS will set the `KVAdminMessage` on ZooKeeper consisting of the hashring, the node, and the associated action. Once the `KVAdminMessage` have been acknowledged, the ECS will update the node in its hash ring, then invoke `LOCK_WRITE` in the respective servers it needs to do transfers with. The ECS will then call `MOVE_DATA` on the respective servers, and finally, call `UNLOCK_WRITE`. Removing the node is done in a similar manner, but will involve calling `SHUT_DOWN` on the node after data migration is complete.

### Client library (KVStore)
The client first connects, initializes with empty metadata, to a known server. When the user puts in a put/get request: if the key in the request hashes to the server it connected to the server services the request, if the key hashes to a different server on the ring,--the server sends a 'SERVER_NOT_RESPONSIBLE' message to the client with the serialized metadata of the entire ring. The client deserializes the message, finds the correct server to send to and retries the request. 

### Storage server (KVServer)
The KVServer now has an additional 'KVAdminCommModule' to communicate to the ECS through zookeeper. The server is started by zookeeper in the 'STOPPED' state where is rejects all client requests. The server starts processing client requests once it receives the initServer command from the ECS and is started.
The server then processes client requests as usual. In the case of a 'MOVE_DATA' command from the ECS the server connects to the server to transfer data to, sends the data in a map, then removes the data from its own database AFTER it has gotten confirmation from the destination server that it has finished copying the data (remove after to keep servicing read requests).
Another design change here is that the server now sends messages in JSON, both the KVSimpleMessage for communicating with the client and KVAdminMessages to the ECS. Anytime the ECS sends a command, the server replies with an acknowledgement to change the server state of the corresponding ECSNode.

## JUnit Tests
testResponsibleServers - checks that client switches server when connected to wrong one.  
testNoServerSwitch - checks that client stays on correct server.  
testMoveData - checks moveData function, transfer keys between servers.  
testGetServerName - ECSNode gets correct server name.  
testGetPort - ECSNode gets correct port.  
testGetHostName - ECSNode gets correct host name.  
testGetHashKey - ECSNode gets correct hashkey.  
testFlagDefaultShutdown - ECSNode gets SHUTDOWN as default status.  
testSetCacheSize - ECSNode sets correct cache size.  
testSetCachePolicy - ECSNode sets correct cache policy.  
## Performance
Sorry-it's good we promise