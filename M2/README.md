# Milestone 2: Scalable Storage Service
By: Yi Zhou, Matthew Lee, Luke Caccamo

## Design

### External configuration service (ECS)
The ECS begins by creating a ZooKeeper process and then initializing the client library. The ECS is designed such that we have a queue of free servers and a hash ring for our used servers. Servers that have not been initialized sit in the free servers queue. Once they are added to the hash ring, their hash ranges will be computed before the SSH call initializes the server. The initialized server will create a ZooKeeper node, which the ECS will wait until it is created. Afterwards, the ECS will set the `KVAdminMessage` on ZooKeeper consisting of the hashring, the node, and the associated action. Once the `KVAdminMessage` have been acknowledged, the ECS will update the node in its hash ring, then invoke `LOCK_WRITE` in the respective servers it needs to do transfers with. The ECS will then call `MOVE_DATA` on the respective servers, and finally, call `UNLOCK_WRITE`. Removing the node is done in a similar manner, but will involve calling `SHUT_DOWN` on the node after data migration is complete.

### Client library (KVStore)

### Storage server (KVServer)

## Appendix

## JUnit Tests

## Performance